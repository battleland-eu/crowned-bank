package eu.battleland.crownedbank.model;

import eu.battleland.crownedbank.abstracted.Identifiable;
import eu.battleland.crownedbank.remote.Remote;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

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
    private Component namePlural;
    @Getter
    private Component nameSingular;

    @Getter
    private String format;

    @Getter
    private Remote remote;

    @Override
    public @NonNull String identifier() {
        return this.identifier;
    }

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
