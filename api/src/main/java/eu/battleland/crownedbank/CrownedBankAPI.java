package eu.battleland.crownedbank;

import eu.battleland.crownedbank.model.Account;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * Crowned Bank API
 */
public interface CrownedBankAPI {

    /**
     * Creates account for identity.
     * @param identity Identity.
     * @return Account.
     */
    Account createAccount(@NonNull Account.Identity identity);

    /**
     * Retrieves account for identity.
     * @param identity Identity.
     * @return Future of Account.
     */
    CompletableFuture<Account> retrieveAccount(@NonNull Account.Identity identity);


    /**
     * @return Currency repository.
     */
    CurrencyRepository getCurrencyRepository();

}
