package eu.battleland.crownedbank;

import eu.battleland.crownedbank.abstracted.Controllable;
import eu.battleland.crownedbank.helper.TransactionHandler;
import eu.battleland.crownedbank.i18n.TranslationRegistry;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.remote.Remote;
import eu.battleland.crownedbank.repo.CurrencyRepository;
import eu.battleland.crownedbank.repo.RemoteFactoryRepository;
import eu.battleland.crownedbank.repo.RemoteRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Crowned Bank API
 */
public interface CrownedBankAPI {

    /**
     * Creates account.
     *
     * @param identity Identity.
     * @return Account.
     */
    Account account(@NonNull Account.Identity identity);

    /**
     * Retrieves account for identity.
     *
     * @param identity Identity.
     * @return Future of dummy account. May be from cache, or remote.
     */
    default CompletableFuture<Account> retrieveAccount(@NonNull Account.Identity identity) {
        return retrieveAccount(identity, false);
    }

    /**
     * Retrieves account for identity.
     *
     * @param identity Identity.
     * @param immediate Whether to retrieve immediately.
     * @return Future of dummy account. May be from cache, or remote.
     */
    CompletableFuture<Account> retrieveAccount(@NonNull Account.Identity identity, boolean immediate);

    /**
     * Retrieves wealthy accounts.
     *
     * @param currency Currency.
     * @return Future list of dummy accounts. May be from cache, or remote.
     */
    CompletableFuture<List<Account>> retrieveWealthyAccounts(@NonNull Currency currency);

    /**
     * @return Remote repository.
     */
    RemoteRepository remoteRepository();

    /**
     * @return Currency repository.
     */
    CurrencyRepository currencyRepository();

    /**
     * @return Remote factory repository.
     */
    RemoteFactoryRepository remoteFactoryRepository();

    /**
     * @return Account storage.
     */
    AccountStorage accountStorage();

    /**
     * @return Translation registry.
     */
    TranslationRegistry<?> translationRegistry();


    class AccountStorage {

        @Getter(AccessLevel.PROTECTED)
        @Setter(AccessLevel.PROTECTED)
        private Map<Account.Identity, Account> accounts
                = new ConcurrentHashMap<>();

        @Getter(AccessLevel.PROTECTED)
        @Setter(AccessLevel.PROTECTED)
        private Map<Account.Identity, Object> identityLocks
                = new ConcurrentHashMap<>();

        /**
         * Retrieve account lock.
         * @param identity Account identity.
         */
        public @NonNull Object lock(@NotNull Account.Identity identity) {
            return identityLocks.compute(identity, (id, lock) -> {
                if(lock == null)
                    lock = new Object();
                return lock;
            });
        }

        /**
         * Store account.
         *
         * @param account Account
         */
        public void put(@NotNull Account account) {
            this.accounts.put(account.getIdentity(), account);
        }

        /**
         * @param identity Account identity
         * @return Account
         */
        public @Nullable Account get(@NotNull Account.Identity identity) {
            return this.accounts.get(identity);
        }

        /**
         * Invalidates storage.
         */
        public void invalidate() {
            this.accounts.clear();
        }
    }

    /**
     * Implementation base
     */
    @Accessors(fluent = true)
    abstract class Base
            implements CrownedBankAPI, Controllable {

        {
            CrownedBank.setApi(this);
        }


        @Getter
        @Setter(AccessLevel.PROTECTED)
        private Remote remote;

        @Getter(AccessLevel.PROTECTED)
        @Setter(AccessLevel.PROTECTED)
        private Map<Account.Identity, CompletableFuture<Account>> accountFutures
                = new ConcurrentHashMap<>();

        @Getter(AccessLevel.PROTECTED)
        @Setter(AccessLevel.PROTECTED)
        private Map<Currency, List<Account>> wealthyAccounts
                = new ConcurrentHashMap<>();
        @Getter(AccessLevel.PROTECTED)
        @Setter(AccessLevel.PROTECTED)
        private volatile CompletableFuture<List<Account>> wealthyAccountsFuture
                = null;

        @Getter
        private final AccountStorage accountStorage
                = new AccountStorage();

        @Getter
        private final RemoteRepository remoteRepository
                = new RemoteRepository();
        @Getter
        private final CurrencyRepository currencyRepository
                = new CurrencyRepository();
        @Getter
        private final RemoteFactoryRepository remoteFactoryRepository
                = new RemoteFactoryRepository();

        private long lastWealthCheck = 0;

        protected abstract Logger provideLogger();

        @Override
        public void initialize() {
            CrownedBank.setLogger(provideLogger());
        }

        @Override
        public void terminate() {

        }

        @Override
        public Account account(@NonNull Account.Identity identity) {
            return Account.builder()
                    .identity(identity)
                    .depositHandler(TransactionHandler.remoteDepositRelay(this.remote))
                    .withdrawHandler(TransactionHandler.remoteWithdrawRelay(this.remote))
                    .build();
        }

        @Override
        public CompletableFuture<Account> retrieveAccount(@NotNull Account.Identity identity, boolean immediate) {

//            return CompletableFuture.supplyAsync(() -> {
//                return null;
//            });

            // Check if account is cached
            {
                final var cachedAccount = this.accountStorage.get(identity);
                if (cachedAccount != null)
                    return CompletableFuture.completedFuture(this.accountStorage.get(identity));
            }

            synchronized (accountStorage.lock(identity)) {
                // Check if account is already being retrieved,
                // if yes, return it's future
                {
                    final var existingFuture
                            = this.accountFutures.get(identity);
                    if (existingFuture != null)
                        if(!immediate)
                            return existingFuture; // return existing future if not immediate
                        else
                            return CompletableFuture.completedFuture(null); // immediate return
                }

                {
                    // Fetch the account from remote
                    final var future = CompletableFuture.supplyAsync(() -> {
                        final var account = account(identity);
                        currenciesByRemotes().forEach((remote, currencies) -> {
                            try {
                                final var data = remote.fetchAccount(identity)
                                        .get(CrownedBank.getConfig().remoteTimeoutMillis(), TimeUnit.MILLISECONDS);
                                if (data != null)
                                    account.getData().join(data);

                            } catch (Exception e) {
                                CrownedBank.getLogger()
                                        .severe(String.format("Couldn't retrieve account data for '%s' currencies '%s' from remote '%s': %s",
                                                identity,
                                                currencies,
                                                remote.identifier(),
                                                e
                                        ));
                            }
                        });

                        this.accountStorage.put(account);
                        this.accountFutures.remove(identity);
                        return account;
                    });

                    // Store the account retrieval future
                    this.accountFutures.put(identity, future);

                    if(immediate)
                        return CompletableFuture.completedFuture(null); // immediate return
                    return future; // return account fetch future
                }
            }
        }

        @Override
        public CompletableFuture<List<Account>> retrieveWealthyAccounts(@NonNull Currency currency) {
            final boolean triggerCheck = (System.currentTimeMillis() - this.lastWealthCheck) > CrownedBank.getConfig().wealthCheckEveryMillis()
                    || !this.wealthyAccounts.containsKey(currency); // trigger check if outdated or not present
            if (!triggerCheck)
                return CompletableFuture.completedFuture(this.wealthyAccounts.get(currency));

            synchronized (this) {
                if (this.wealthyAccountsFuture != null)
                    return this.wealthyAccountsFuture;

                final var future = CompletableFuture.supplyAsync(() -> {
                    try {
                        final var result = currency.getRemote().fetchWealthyAccounts(currency).get();

                        this.wealthyAccounts.put(currency, result);
                        this.wealthyAccountsFuture = null;
                        this.lastWealthCheck = System.currentTimeMillis();

                        return result;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
                this.wealthyAccountsFuture = future;
                return future;
            }
        }

        public Map<Remote, List<Currency>> currenciesByRemotes() {
            final var result = new HashMap<Remote, List<Currency>>();

            this.currencyRepository().all().forEach((currency -> {
                var remote = currency.getRemote();
                if (remote == null)
                    remote = this.remote;

                result.computeIfAbsent(remote, (r) -> {
                    final var array = new ArrayList<Currency>();
                    array.add(currency);
                    return array;
                });
                result.computeIfPresent(remote, (r, array) -> {
                    array.add(currency);
                    return array;
                });
            }));

            return result;
        }
    }

}
