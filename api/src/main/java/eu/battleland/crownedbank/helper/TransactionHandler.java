package eu.battleland.crownedbank.helper;

import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.remote.Remote;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;

/**
 * Transaction handler. Its layer between local account and remote account.
 */
@FunctionalInterface
public interface TransactionHandler {

    /**
     * Handle transaction.
     *
     * @param currency    Currency.
     * @param amount      Amount.
     * @param account     Account.
     * @param postHandler Post-transaction handler.
     * @return response   Transaction's response.
     */
    Pair<Boolean, Float> handle(@NotNull Currency currency,
                                @NotNull Float amount,
                                @NotNull Account account,
                                CompletableFuture<Void> postHandler);

    /**
     * Relays withdraw transactions directly to remote.
     * @return Relay
     */
    public static @NonNull TransactionHandler.RemoteWithdrawTransactionRelay remoteWithdrawRelay(@NonNull Remote remote) {
        return new RemoteWithdrawTransactionRelay(remote);
    }

    /**
     * Relays deposit transactions directly to remote.
     * @return Relay
     */
    public static @NonNull TransactionHandler.RemoteDepositTransactionRelay remoteDepositRelay(@NonNull Remote remote) {
        return new RemoteDepositTransactionRelay(remote);
    }

    /**
     * Any remote relay.
     */
    abstract class RemoteTransactionRelay implements TransactionHandler {
        protected final Remote remote;

        public RemoteTransactionRelay(Remote remote) {
            this.remote = remote;
        }
    }

    /**
     * Withdraw transaction handler relaying directly to remote.
     */
    class RemoteWithdrawTransactionRelay
            extends RemoteTransactionRelay {

        public RemoteWithdrawTransactionRelay(Remote remote) {
            super(remote);
        }

        @Override
        public Pair<Boolean, Float> handle(@NotNull Currency currency,
                                           @NotNull Float amount,
                                           @NotNull Account account,
                                           CompletableFuture<Void> postHandler) {
            var remote = currency.getRemote();
            if (remote == null)
                remote = this.remote;
            try {
                return remote.handleWithdraw(account, currency, amount, postHandler).get();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    /**
     * Deposit transaction handler relaying directly to remote.
     */
    class RemoteDepositTransactionRelay
            extends RemoteTransactionRelay {

        public RemoteDepositTransactionRelay(Remote remote) {
            super(remote);
        }


        @Override
        public Pair<Boolean, Float> handle(@NotNull Currency currency,
                                           @NotNull Float amount,
                                           @NotNull Account account,
                                           CompletableFuture<Void> postHandler) {
            var remote = currency.getRemote();
            if (remote == null)
                remote = this.remote;
            try {
                return remote.handleDeposit(account, currency, amount, postHandler).get();
            } catch (Exception x) {
               return null;
            }
        }
    }

    record Result(boolean accepted, float status, Runnable post) {

    }

}
