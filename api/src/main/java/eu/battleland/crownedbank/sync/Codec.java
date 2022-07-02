package eu.battleland.crownedbank.sync;

import com.google.gson.JsonObject;
import eu.battleland.crownedbank.CurrencyRepository;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Codec operations.
 */
public class Codec {

//    /**
//     * Encodes account to {@code json}.
//     * @param account            Account.
//     * @param currencyRepository Currencies.
//     * @return Account data
//     */
//    public static @NotNull JsonObject encodeAccount(final Account account,
//                                             final CurrencyRepository currencyRepository) {
//
//        final var encoded = new JsonObject();
//        encoded.addProperty("identity", account.getIdentity().toString());
//        {
//            final var currencies = new JsonObject();
//            currencyRepository.all().forEach((currency) -> {
//                currencies.addProperty(currency.identifier(), account.status(currency));
//            });
//            encoded.add("currencies", currencies);
//        }
//        return encoded;
//    }
//
//    /**
//     * Decodes account from {@code json}.
//     * @param data               Account data.
//     * @param account            Account builder.
//     * @param currencyRepository Currencies.
//     * @return Account builder
//     */
//    public static Account.AccountBuilder decodeAccount(final @NotNull JsonObject data,
//                                                @NotNull Account.AccountBuilder account,
//                                                final @NotNull CurrencyRepository currencyRepository) {
//        final var currencies = data.getAsJsonObject("currencies");
//        final Map<Currency, Float> currencyMap = new HashMap<>();
//        currencies.keySet().forEach(currencyId -> {
//            final var currency
//                    = currencyRepository.retrieve(currencyId);
//            final var amount = currencies.getAsJsonPrimitive(currencyId)
//                    .getAsFloat();
//            currencyMap.put(currency, amount);
//        });
//        return account.currencies(currencyMap);
//    }

}
