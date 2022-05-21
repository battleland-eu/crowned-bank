package eu.battleland.crownedbank.remote;

import com.google.gson.JsonObject;
import eu.battleland.crownedbank.abstracted.Identifiable;
import eu.battleland.crownedbank.helper.Pair;
import eu.battleland.crownedbank.model.Account;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface Remote
        extends Identifiable {


    /**
     * Configure remote.
     * @param profile Profile.
     */
    void configure(@NonNull Profile profile);

    /**
     * Provide account from remote.
     * @param identity Identity of account.
     * @return Nullable account.
     */
    CompletableFuture<@Nullable Account> provideAccount(@NonNull Account.Identity identity);

    /**
     * Handle account withdraw.
     * @param account Account.
     * @param amount  Amount to withdraw.
     * @return Boolean true if withdraw from account was successful & new account status.
     */
    CompletableFuture<Pair<Boolean, Float>> handleWithdraw(final Account account, float amount);

    /**
     * Handle account deposit.
     * @param account Account.
     * @param amount  Amount to deposit.
     * @return Boolean true if deposit to account was successful & new account status.
     */
    CompletableFuture<Pair<Boolean, Float>> handleDeposit(final Account account, float amount);

    /**
     * Remote profile
     */
    public static record Profile(String id,
                                 JsonObject parameters) {}
}
