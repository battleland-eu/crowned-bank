package eu.battleland.crownedbank.helper;

import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.remote.Remote;

import lombok.NonNull;

/**
 * Default account deposit and withdraw handlers.
 */
public  class Handler {

    /**
     * Relays withdraws directly to remote, uses currency's remote if possible.
     * @return Relay
     */
    public static @NonNull RemoteWithdrawRelay remoteWithdrawRelay(@NonNull Remote remote) {
        return new RemoteWithdrawRelay(remote);
    }

    /**
     * Relays deposits directly to remote, uses currency's remote if possible.
     * @return Relay
     */
    public static @NonNull RemoteDepositRelay remoteDepositRelay(@NonNull Remote remote) {
        return new RemoteDepositRelay(remote);
    }

    /**
     * Any remote relay.
     */
    public static abstract class RemoteRelay {
        protected final Remote remote;

        public RemoteRelay(Remote remote) {
            this.remote = remote;
        }
    }

    /**
     * Withdraw handler relaying directly to remote.
     */
    public static class RemoteWithdrawRelay
            extends RemoteRelay
            implements TriFunction<Currency, Float, Account, Pair<Boolean, Float>> {

        public RemoteWithdrawRelay(Remote remote) {
            super(remote);
        }

        @Override
        public Pair<Boolean, Float> apply(Currency currency, Float amount, Account account) throws Exception {
            var remote = currency.getRemote();
            if(remote == null)
                remote = this.remote;
            return remote.handleWithdraw(account, amount).get();
        }
    }

    /**
     * Deposit handler relaying directly to remote.
     */
    public static class RemoteDepositRelay
            extends RemoteRelay
            implements TriFunction<Currency, Float, Account, Pair<Boolean, Float>> {

        public RemoteDepositRelay(Remote remote) {
            super(remote);
        }

        @Override
        public Pair<Boolean, Float> apply(Currency currency, Float amount, Account account) throws Exception {
            var remote = currency.getRemote();
            if(remote == null)
                remote = this.remote;
            return remote.handleDeposit(account, amount).get();
        }
    }

}
