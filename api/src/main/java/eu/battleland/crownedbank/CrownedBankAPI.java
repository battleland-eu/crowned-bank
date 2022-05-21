package eu.battleland.crownedbank;

import eu.battleland.crownedbank.config.GlobalConfig;
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
     * Retrieves account for identity.
     * @param identity Identity.
     * @return Future of Account.
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
        public CompletableFuture<Account> retrieveAccount(@NotNull Account.Identity identity) {
            Objects.requireNonNull(remote, "Remote not present");

            synchronized (this) {
                final var future
                        = this.futures.get(identity);
                if(future != null)
                    return future; // return existing future
            }

            final var future = CompletableFuture.supplyAsync(() -> {
                // get cached
                if (this.cachedAccounts.containsKey(identity))
                    return this.cachedAccounts.get(identity);

                // get remote
                Account account = null;
                try {
                    account = this.remote.provideAccount(identity).get();
                    this.cachedAccounts.put(identity, account);
                    return account;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                synchronized (this) {
                    this.futures.remove(identity); // remove future
                }
                return null;
            });

            // store future
            synchronized (this) {
                this.futures.put(identity, future); // register future
            }
            return future;
        }
    }

}
