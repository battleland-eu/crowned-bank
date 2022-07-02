package eu.battleland.crownedbank;

import eu.battleland.crownedbank.helper.Handler;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.remote.Remote;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Crowned Bank API
 */
public interface CrownedBankAPI {


    /**
     * Creates dummy account.
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
     * @return Remote repository.
     */
    RemoteRepository getRemoteRepository();

    /**
     * @return Currency repository.
     */
    CurrencyRepository getCurrencyRepository();


    /**
     * Implementation base
     */
    abstract class Base
            implements CrownedBankAPI {

        {
            CrownedBankConstants.setApi(this);
        }

        @Getter
        @Setter(AccessLevel.PROTECTED)
        private Remote remote;

        @Getter(AccessLevel.PROTECTED)
        @Setter(AccessLevel.PROTECTED)
        private Map<Account.Identity, Account> cachedAccounts
                = new ConcurrentHashMap<>();

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

        @Getter(AccessLevel.PUBLIC)
        @Setter(AccessLevel.PROTECTED)
        private long lastWealthCheck = 0;


        @Override
        public Account account(@NonNull Account.Identity identity) {
            return Account.builder()
                    .identity(identity)
                    .depositHandler(Handler.remoteDepositRelay(this.remote))
                    .withdrawHandler(Handler.remoteWithdrawRelay(this.remote))
                    .build();
        }


        @Override
        public CompletableFuture<Account> retrieveAccount(@NotNull Account.Identity identity) {
            Objects.requireNonNull(remote, "Remote not present");

            // return cached account
            if (this.cachedAccounts.containsKey(identity))
                return CompletableFuture.completedFuture(this.cachedAccounts.get(identity));

            synchronized (this) {
                // check if account is already being retrieved,
                // if yes, return it's future
                final var existingFuture
                        = this.accountFutures.get(identity);
                if (existingFuture != null)
                    return existingFuture; // return existing future

                // fetch account from remote
                final var future = CompletableFuture.supplyAsync(() -> {
                    Account dummyAccount = null;
                    try {
                        final var fetchFuture = this.remote.fetchAccount(identity);

                        try {
                            dummyAccount = fetchFuture.get(); // fetch dummy
                        } catch (Exception x) {
                            x.printStackTrace();
                        }

                        if(fetchFuture.isCompletedExceptionally())
                            return null; // exception occurred when fetching user, they might have an account...

                        Account liveAccount = account(identity);
                        if (dummyAccount == null)
                            this.remote.storeAccount(liveAccount); // create new account for user
                        else
                            liveAccount.feedFromDummy(dummyAccount); // feed from existing dummy account

                        this.cachedAccounts.put(identity, liveAccount);

                        // supply account
                        return liveAccount;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    } finally {
                        synchronized (this) {
                            this.accountFutures.remove(identity); // remove account future
                        }
                    }
                });

                // store account future
                this.accountFutures.put(identity, future);
                return future;
            }
        }

        @Override
        public CompletableFuture<List<Account>> retrieveWealthyAccounts(@NonNull Currency currency) {
            final boolean triggerCheck = (System.currentTimeMillis() - this.lastWealthCheck) > CrownedBankConstants.getWealthyCheckMillis()
                    || !this.wealthyAccounts.containsKey(currency); // trigger check if outdated or not present
            if (!triggerCheck)
                return CompletableFuture.completedFuture(this.wealthyAccounts.get(currency));

            synchronized (this) {
                if (this.wealthyAccountsFuture != null)
                    return this.wealthyAccountsFuture;

                final var future = CompletableFuture.supplyAsync(() -> {
                    try {
                        final var result = this.remote.fetchWealthyAccounts(currency).get();

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
    }

}
