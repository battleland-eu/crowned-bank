package eu.battleland.crownedbank.remote;

import com.google.gson.JsonObject;
import eu.battleland.crownedbank.abstracted.Identifiable;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Remote. Stores, retrieves accounts. Handles transactions of remote accounts.
 */
public interface Remote
        extends Identifiable<String> {

    /**
     * Configure remote.
     *
     * @param profile Profile.
     * @return Itself.
     */
    Remote configure(@NonNull Profile profile);

    /**
     * Store account state in remote.
     *
     * @param account Account.
     * @return Boolean true if successful.
     */
    CompletableFuture<Boolean> storeAccount(@NonNull Account account);

    /**
     * Fetch account from remote.
     *
     * @param identity Identity of account.
     * @return Nullable account.
     */
    CompletableFuture<Account.@Nullable Data> fetchAccount(@NonNull Account.Identity identity);


    /**
     * Query accounts by currency.
     *
     * @param currency Currency.
     * @return List of Accounts.
     */
    CompletableFuture<List<Account>> fetchWealthyAccounts(@NonNull Currency currency);

    /**
     * Handle account withdraw.
     *
     * @param account Account.
     * @param currencyStorage Currency storage.
     * @param amount  Amount to withdraw.
     * @return Boolean true if withdraw from account was successful.
     */
    CompletableFuture<Boolean> handleWithdraw(final Account account,
                                              final Currency.Storage currencyStorage,
                                              float amount);

    /**
     * Handle account withdraw.
     *
     * @param account Account.
     * @param currencyStorage Currency storage.
     * @param amount  Amount to withdraw.
     * @return Boolean true if withdraw from account was successful.
     */
    CompletableFuture<Boolean> handleDeposit(final Account account,
                                             final Currency.Storage currencyStorage,
                                             float amount);


    /**
     * Remote factory.
     */
    interface Factory extends Identifiable<String> {
        Remote build(final Profile profile);
    }

    /**
     * Remote profile.
     * @param id Unique Remote identifier.
     * @param parameters Remote parameters.
     */
    record Profile(String id, JsonObject parameters) {}

}
