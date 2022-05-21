package eu.battleland.crownedbank;

import com.google.common.collect.ImmutableSet;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Currency repository.
 */
public class CurrencyRepository {

    private final Map<String, Currency> currencies
            = new ConcurrentHashMap<>();

    /**
     * @param identifier Currency identifier
     * @return Nullable Currency
     */
    public Currency getCurrency(final String identifier) {
        return this.currencies.get(identifier);
    }

    /**
     * Registers currency.
     *
     * @param currency Currency
     */
    public void registerCurrency(final @NonNull Currency currency) {
        this.currencies.put(currency.getIdentifier(), currency);
    }

    /**
     * @return Registered currencies
     */
    public ImmutableSet<Currency> getCurrencies() {
        return ImmutableSet.<Currency>builder().addAll(currencies.values()).build();
    }


    /**
     * Set major currency.
     * @param currency Currency.
     */
    public void setMajorCurrency(final Currency currency) {
        Currency.majorCurrency = currency;
    }

    /**
     * Set minor currency.
     * @param currency Currency.
     */
    public void setMinorCurrency(final Currency currency) {
        Currency.minorCurrency = currency;
    }

    /**
     * Get major currency.
     * @return Currency.
     */
    public Currency getMajorCurrency() {
        return Currency.majorCurrency;
    }

    /**
     * Get minor currency.
     * @return Currency.
     */
    public Currency getMinorCurrency() {
        return Currency.minorCurrency;
    }


}
