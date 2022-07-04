package eu.battleland.crownedbank.helper;

import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.remote.Remote;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * @return response   Transaction's response.
     */
    boolean handle(@NotNull Currency.Storage currency,
                   @NotNull Float amount,
                   @NotNull Account account);

    /**
     * Relays withdraw transactions directly to remote.
     *
     * @param remote Default remote.
     * @return Relay
     */
    public static @NonNull TransactionHandler.RemoteWithdrawTransactionRelay remoteWithdrawRelay(@Nullable Remote remote) {
        return new RemoteWithdrawTransactionRelay(remote);
    }

    /**
     * Relays deposit transactions directly to remote.
     *
     * @param remote Default remote.
     * @return Relay
     */
    public static @NonNull TransactionHandler.RemoteDepositTransactionRelay remoteDepositRelay(@Nullable  Remote remote) {
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
        public boolean handle(@NotNull Currency.Storage currency,
                                    @NotNull Float amount,
                                    @NotNull Account account) {
            var remote = currency.getCurrency().getRemote();
            if (remote == null)
                remote = this.remote;
            if(remote == null)
                return false;

            try {
                return remote.handleWithdraw(account, currency, amount).get();
            } catch (Exception ignored) {
                return false;
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
        public boolean handle(@NotNull Currency.Storage currency,
                                           @NotNull Float amount,
                                           @NotNull Account account) {
            var remote = currency.getCurrency().getRemote();
            if (remote == null)
                remote = this.remote;
            try {
                return remote.handleDeposit(account, currency, amount).get();
            } catch (Exception x) {
                return false;
            }
        }
    }

    record Result(boolean accepted, float status, Runnable post) {

    }

}
