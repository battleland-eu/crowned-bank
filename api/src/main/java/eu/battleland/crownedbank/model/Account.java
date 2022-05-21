package eu.battleland.crownedbank.model;

import com.google.gson.JsonObject;
import eu.battleland.crownedbank.CrownedBankConstants;
import eu.battleland.crownedbank.helper.Pair;
import eu.battleland.crownedbank.helper.TriFunction;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

@Builder
public class Account {

    @Getter
    private Account.Identity identity;

    /**
     * Cached currencies held by account.
     */
    private Map<Currency, Float> currencies;

    /**
     * param Currency - Currency to withdraw.<br>
     * param Float    - Amount to withdraw.<br>
     * param Account  - Account reference.
     * ret   Boolean, Float  - Whether the operation was successful & new currency amount<br><br>
     * <p>
     * Withdraw handler.
     * Returns true if withdrawal from account was successful,
     * otherwise returns false.
     */
    private transient final TriFunction<Currency, Float, Account, Pair<Boolean, Float>> withdrawHandler;

    /**
     * param Currency - Currency to deposit.<br>
     * param Float    - Amount to deposit.<br>
     * param Account  - Account reference.
     * ret   Boolean, Float  - Whether the operation was successful & new currency amount<br><br>
     * <p>
     * Deposit handler.
     * Returns true if deposit to account was successful,
     * otherwise returns false.
     */
    private transient final TriFunction<Currency, Float, Account, Pair<Boolean, Float>> depositHandler;

    /**
     * Withdraws currency from account.
     *
     * @param currency Currency
     * @param amount   Amount
     * @return Completable Future which completes with boolean true if withdraw was completed successfully.
     */
    public CompletableFuture<Boolean> withdraw(final Currency currency,
                                               float amount) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                try {
                    final var result = this.withdrawHandler.apply(currency, amount, this);
                    if (!result.getFirst())
                        return false;
                    this.currencies.put(currency, result.getSecond());
                    return true;
                } catch (Exception x) {
                    x.printStackTrace();
                    return false;
                }
            }
        });
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
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                try {
                    final var result = this.depositHandler.apply(currency, amount, this);
                    if (!result.getFirst())
                        return false;
                    this.currencies.put(currency, result.getSecond());
                    return true;
                } catch (Exception x) {
                    x.printStackTrace();
                    return false;
                }
            }
        });
    }


    /**
     * @param currency Currency
     * @return Currency status
     */
    public float status(final Currency currency) {
        return this.currencies.getOrDefault(currency, 0f);
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
