package eu.battleland.crownedbank.repo;

import com.google.common.collect.ImmutableSet;
import eu.battleland.crownedbank.abstracted.Repository;
import eu.battleland.crownedbank.model.Currency;

/**
 * Currency repository.
 */
public class CurrencyRepository
    extends Repository<String, Currency> {

    /**
     * Register currency.
     * @param entry Currency.
     */
    @Override
    public void register(Currency entry) {
        super.register(entry);
    }

    /**
     * Retrieve currency.
     * @param id Identifier.
     * @return Currency Instance.
     */
    @Override
    public Currency retrieve(String id) {
        return super.retrieve(id);
    }

    /**
     * @return All Currencies
     */
    @Override
    public ImmutableSet<Currency> all() {
        return super.all();
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
