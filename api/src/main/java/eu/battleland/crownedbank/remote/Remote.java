package eu.battleland.crownedbank.remote;

import com.google.gson.JsonObject;
import eu.battleland.crownedbank.abstracted.Identifiable;
import eu.battleland.crownedbank.helper.Pair;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Remote. Stores, retrieves accounts. Handles transactions of remote accounts.
 */
public interface Remote
        extends Identifiable {

    /**
     * Configure remote.
     * @param profile Profile.
     */
    void configure(@NonNull Profile profile);

    /**
     * Store account in remote.
     * @param account Account.
     * @return Boolean true if successful.
     */
    CompletableFuture<Boolean> storeAccount(@NonNull Account account);

    /**
     * Fetch account from remote.
     * @param identity Identity of account.
     * @return Nullable account.
     */
    CompletableFuture<@Nullable Account> fetchAccount(@NonNull Account.Identity identity);


    /**
     * Query accounts by currency.
     * @param currency Currency.
     * @return List of Accounts.
     */
    CompletableFuture<List<Account>> fetchWealthyAccounts(@NonNull Currency currency);

    /**
     * Handle account withdraw.
     * @param account Account.
     * @param amount  Amount to withdraw.
     * @param post    Future which completes when withdraw completes.
     * @return Boolean true if withdraw from account was successful & new account status.
     */
    CompletableFuture<Pair<Boolean, Float>> handleWithdraw(final Account account, final Currency currency, float amount,
                                                           CompletableFuture<Void> post);

    /**
     * Handle account deposit.
     * @param account Account.
     * @param amount  Amount to deposit.
     * @param post    Future which completes when deposit completes.
     * @return Boolean true if deposit to account was successful & new account status.
     */
    CompletableFuture<Pair<Boolean, Float>> handleDeposit(final Account account, final Currency currency, float amount,
                                                          CompletableFuture<Void> post);

    /**
     * Remote profile
     */
    public static record Profile(String id, JsonObject parameters) {}

}
