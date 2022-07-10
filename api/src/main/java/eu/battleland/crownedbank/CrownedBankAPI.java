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
    CompletableFuture<Account> retrieveAccount(@NonNull Account.Identity identity);

    /**
     * Retrieves wealthy accounts.
     *
     * @param currency Currency.
     * @return Future list of dummy accounts. May be from cache, or remote.
     */
    CompletableFuture<List<Account>> retrieveWealthyAccounts(@NonNull Currency currency);

    /**
     * @return Account cache.
     */
    AccountCache accountCache();

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
     * @return Translation registry.
     */
    TranslationRegistry<?> translationRegistry();


    class AccountCache {

        @Getter(AccessLevel.PROTECTED)
        @Setter(AccessLevel.PROTECTED)
        private Map<Account.Identity, Account> cachedAccounts
                = new ConcurrentHashMap<>();

        /**
         * Cache account.
         *
         * @param account Account
         */
        public void put(@NotNull Account account) {
            this.cachedAccounts.put(account.getIdentity(), account);
        }

        /**
         * @param identity Account identity
         * @return Account
         */
        public @Nullable Account get(@NotNull Account.Identity identity) {
            return this.cachedAccounts.get(identity);
        }

        /**
         * Invalidates cache.
         */
        public void invalidate() {
            this.cachedAccounts.clear();
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
        private final AccountCache accountCache
                = new AccountCache();

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
        public CompletableFuture<Account> retrieveAccount(@NotNull Account.Identity identity) {

            // Check if account is cached
            {
                final var cachedAccount = this.accountCache.get(identity);
                if (cachedAccount != null)
                    return CompletableFuture.completedFuture(this.accountCache.get(identity));
            }

            synchronized (this) {
                // Check if account is already being retrieved,
                // if yes, return it's future
                {
                    final var existingFuture
                            = this.accountFutures.get(identity);
                    if (existingFuture != null)
                        return existingFuture; // return existing future
                }

                {
                    // Fetch the account from remote
                    final var future = CompletableFuture.supplyAsync(() -> {
                        final var account = account(identity);
                        currenciesByRemotes().forEach((remote, currencies) -> {
                            try {
                                final var data = remote.fetchAccount(identity).get();
                                if (data != null)
                                    account.getData().join(data);

                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

                        this.accountCache.put(account);
                        this.accountFutures.remove(identity);
                        return account;
                    });
                    // Store the account retrieval future
                    this.accountFutures.put(identity, future);
                    return future;
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
