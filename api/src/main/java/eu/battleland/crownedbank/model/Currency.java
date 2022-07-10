package eu.battleland.crownedbank.model;

import eu.battleland.crownedbank.abstracted.Identifiable;
import eu.battleland.crownedbank.remote.Remote;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.Objects;

/**
 * Represents currency.
 */
@Builder
public class Currency
    implements Identifiable<String> {

    /**
     * Major currency to be used.
     */
    public static @NonNull Currency majorCurrency;
    /**
     * Minor currency to be used.
     */
    public static @NonNull Currency minorCurrency;

    private String identifier;

    @Getter
    @Builder.Default
    private Component namePlural
            = Component.empty();

    @Getter
    @Builder.Default
    private Component nameSingular
            = Component.empty();

    @Getter
    private boolean allowDecimal;

    @Getter
    private String format;

    @Getter
    private Remote remote;

    @Override
    public @NonNull String identifier() {
        return this.identifier;
    }

    /**
     * @return Storage.
     */
    public @NonNull Currency.Storage newStorage() {
        return new Storage(this);
    }


    /**
     * Compare this instance with instance {@code o} or compare identifiers.
     *
     * @param o Other object
     * @return Boolean true when matching instances, or identifiers.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Currency currency = (Currency) o;

        return Objects.equals(identifier, currency.identifier);
    }

    @Override
    public int hashCode() {
        return identifier != null ? identifier.hashCode() : 0;
    }

    /**
     * @param currency Currency.
     * @param amount Amount of currency.
     * @return Formatted value and currency name as Component.
     */
    public static @NonNull Component prettyCurrencyAmountComponent(final Currency currency, float amount) {
        return Component.text(String.format(currency.format + " ", amount))
                .append(amount == 1 ? currency.nameSingular : currency.namePlural);
    }

    /**
     * @param currency Currency.
     * @param amount Amount of currency.
     * @return Formatted value and currency name as String.
     */
    public static @NonNull String prettyCurrencyAmountString(final Currency currency, float amount) {

        // account's transaction handler rounds if decimal is not allowed
        // reflect that in pretty string
        if(!currency.allowDecimal)
            amount = Math.round(amount);

        return String.format(currency.format + " %s", amount, amount == 1 ? ((TextComponent)currency.nameSingular).content() : ((TextComponent)currency.namePlural).content() );
    }

    /**
     * Storage of currency.
     */
    public static class Storage {

        @Getter
        private final Currency currency;
        private volatile float value = 0;

        /**
         * New Instance of currency storage.
         *
         * @param currency Currency.
         */
        public Storage(final Currency currency) {
            this.currency = currency;
        }

        /**
         * @return Amount of the currency.
         */
        public synchronized float amount() {
            return value;
        }

        /**
         * Change amount of currency.
         *
         * @param val Value.
         */
        public synchronized Storage change(float val) {
            value = val;
            return this;
        }

        /**
         * Deposit an amount of currency.
         *
         * @param val Value.
         * @return Boolean true if deposit was made.
         */
        public synchronized boolean deposit(float val) {
            value += val;
            return true;
        }

        /**
         * Deposit an amount of currency.
         *
         * @param val Value.
         * @return Boolean true if withdraw was made.
         */
        public synchronized boolean withdraw(float val) {
            final var modified = (this.value - val);
            if (modified >= 0) {
                value -= val;
                return true;
            }
            return false;
        }
    }
}
