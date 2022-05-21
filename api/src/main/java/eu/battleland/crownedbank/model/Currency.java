package eu.battleland.crownedbank.model;

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
public class Currency {

    /**
     * Major currency to be used.
     */
    public static @NonNull Currency majorCurrency;
    /**
     * Minor currency to be used.
     */
    public static @NonNull Currency minorCurrency;


    @Getter
    private String identifier;

    @Getter
    private Component namePlural;
    @Getter
    private Component nameSingular;

    @Getter
    private String format;

    @Getter
    private Remote remote;

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
}
