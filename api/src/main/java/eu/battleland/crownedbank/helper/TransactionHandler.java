package eu.battleland.crownedbank.helper;

import eu.battleland.crownedbank.CrownedBank;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.remote.Remote;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Transaction handler. It serves as a handling layer between local and remote.
 */
@FunctionalInterface
public interface TransactionHandler {

    /**
     * Handle transaction.
     *
     * @param currency Currency.
     * @param amount   Amount.
     * @param account  Account.
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
    static @NonNull TransactionHandler.RemoteWithdrawTransactionRelay remoteWithdrawRelay(@Nullable Remote remote) {
        return new RemoteWithdrawTransactionRelay(remote);
    }

    /**
     * Relays deposit transactions directly to remote.
     *
     * @param remote Default remote.
     * @return Relay
     */
    static @NonNull TransactionHandler.RemoteDepositTransactionRelay remoteDepositRelay(@Nullable Remote remote) {
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
            if (remote == null) {
                CrownedBank.getLogger()
                        .severe("Remote withdraw transaction handler (relay), does not have any remote to relay to.");
                return false;
            }

            try {
                return remote.handleWithdraw(account, currency, amount)
                        .get(CrownedBank.getConfig().remoteTimeoutMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception x) {
                CrownedBank.getLogger().severe("Handling remote withdrawal threw an exception.");
                x.printStackTrace();
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
            if (remote == null) {
                CrownedBank.getLogger()
                        .severe("Remote deposit transaction handler (relay), does not have any remote to relay to.");
                return false;
            }

            try {
                return remote.handleDeposit(account, currency, amount).get();
            } catch (Exception x) {
                CrownedBank.getLogger().severe("Handling remote deposit threw an exception.");
                x.printStackTrace();

                return false;
            }
        }
    }
}
