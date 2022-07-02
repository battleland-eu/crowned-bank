package eu.battleland.crownedbank.model;

import eu.battleland.crownedbank.CrownedBankConstants;
import eu.battleland.crownedbank.helper.TransactionHandler;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Builder
public class Account {

    public static final Logger logger = Logger.getLogger("CrownedBank");

    @Getter
    private Account.Identity identity;

    /**
     * Cached currencies held by account.
     */
    @Builder.Default
    private final Map<Currency, Float> currencies
            = new ConcurrentHashMap<>();

    @Builder.Default
    private transient TransactionHandler withdrawHandler = null;

    @Builder.Default
    private transient TransactionHandler depositHandler = null;

    /**
     * @return Boolean true if account is dummy.
     */
    public boolean isDummy() {
        return withdrawHandler == null || depositHandler == null;
    }


    /**
     * Withdraws currency from account.
     *
     * @param currency Currency
     * @param amount   Amount
     * @return Completable Future which completes with boolean true if withdraw was completed successfully.
     */
    public CompletableFuture<Boolean> withdraw(final Currency currency,
                                               float amount) {
        if (isDummy())
            return CompletableFuture.completedFuture(false);

        return CompletableFuture.supplyAsync(() -> this.transaction(this.withdrawHandler, currency, amount, () -> {
            logger.info(String.format(
                    "Successfully withdrawn '%.2f' %s from account '%s'.", amount, currency.identifier(), identity
            ));
        }, () -> {
            logger.info(String.format(
                    "Failed to withdraw '%.2f' %s from account '%s'.", amount, currency.identifier(), identity
            ));
        }));
    }

    /**
     * Deposits currency to account.
     *
     * @param currency Currency
     * @param amount   Amount
     * @return Completable Future which completes with boolean true if deposit was completed successfully.
     */

    public CompletableFuture<Boolean> deposit(final Currency currency,
                                              float amount) {
        if (isDummy())
            return CompletableFuture.completedFuture(false);

        return CompletableFuture.supplyAsync(() -> this.transaction(this.depositHandler, currency, amount, () -> {
            logger.info(String.format(
                    "Successfully deposited '%.2f' %s to account '%s'.", amount, currency.identifier(), identity
            ));
        }, () -> {
            logger.info(String.format(
                    "Failed to deposit '%.2f' %s to account '%s'.", amount, currency.identifier(), identity
            ));
        }));
    }


    private synchronized boolean transaction(final TransactionHandler handler,
                                             final Currency currency,
                                             final float amount,
                                             Runnable onSuccess,
                                             Runnable onFailure) {
        // post handler, should be completed after transaction is over.
        // remote is then notified that we processed its response, and it can trigger some actions... ie. saving data
        final var postHandler = new CompletableFuture<Void>();
        try {
            // call transaction handler
            final var result = handler.handle(currency, amount, this, postHandler);
            if (result.getFirst()) {
                // transaction accepted
                onSuccess.run();
                this.currencies.put(currency, result.getSecond());
                return true;
            } else {
                // transaction not accepted
                onFailure.run();
                return false;
            }
        } catch (Exception x) {
            x.printStackTrace();
            return false;
        } finally {
            // complete post handler
            postHandler.complete(null);
        }
    }

    /**
     * @param currency Currency
     * @return Currency status
     */
    public float status(final Currency currency) {
        return this.currencies.getOrDefault(currency, 0f);
    }

    /**
     * Feed from other account.
     *
     * @param other Other Account.
     * @return This.
     */
    public Account feedFromDummy(@Nullable Account other) {
        if (other != null && other.currencies != null)
            this.currencies.putAll(other.currencies);
        return this;
    }


    /**
     * Account identity
     */
    public record Identity(UUID uuid, String name) {

        @Override
        public String toString() {
            return String.format("%s(%s)", this.name, this.uuid);
        }

        /**
         * Equals operator
         *
         * @param o Other object.
         * @return Boolean true if identity matches by uuid. If {@link CrownedBankConstants#isIdentityNameMajor()} is set to true, returns boolean true if identity matches by name.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Identity identity = (Identity) o;

            if (CrownedBankConstants.isIdentityNameMajor())
                return Objects.equals(name, identity.name);
            else
                return Objects.equals(uuid, identity.uuid);
        }

        /**
         * @return Returns hash code of uuid. If {@link CrownedBankConstants#isIdentityNameMajor()} is set to true, returns hash code of name instead.
         */
        @Override
        public int hashCode() {
            return CrownedBankConstants.isIdentityNameMajor() ? name.hashCode() : uuid.hashCode();
        }
    }

}
