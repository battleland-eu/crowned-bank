package eu.battleland.crownedbank;

import eu.battleland.crownedbank.helper.Handler;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.remote.Remote;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crowned Bank API
 */
public interface CrownedBankAPI {


    /**
     * Creates account.
     * @param identity Identity.
     * @return Account.
     */
    Account createAccount(@NonNull Account.Identity identity);

    /**
     * Retrieves account for identity.
     * @param identity Identity.
     * @return Future of Account. May be from cache, or remote.
     */
    CompletableFuture<Account> retrieveAccount(@NonNull Account.Identity identity);


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
        private Map<Account.Identity, CompletableFuture<Account>> futures
                = new ConcurrentHashMap<>();

        @Override
        public Account createAccount(@NonNull Account.Identity identity) {
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

            // check if account is already being retrieved,
            // if yes, return it's future
            synchronized (this) {
                final var future
                        = this.futures.get(identity);
                if(future != null)
                    return future; // return existing future
            }

            final var future = CompletableFuture.supplyAsync(() -> {
                // fetch account from remote
                Account account = null;
                try {
                    final var fetchFuture = this.remote.fetchAccount(identity);

                    try {
                        account = fetchFuture.get();
                    } catch (Exception ignored) {}

                    if((!fetchFuture.isCompletedExceptionally()) && account == null) {
                        account = createAccount(identity);
                        this.remote.storeAccount(account);
                    } if(account != null) {
                        this.cachedAccounts.put(identity, account);
                    }
                    return account;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                } finally {
                    synchronized (this) {
                        this.futures.remove(identity); // remove account future
                    }
                }
            });

            // store future
            synchronized (this) {
                this.futures.put(identity, future); // register account future
            }
            return future;
        }
    }

}
