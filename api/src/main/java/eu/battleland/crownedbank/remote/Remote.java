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
        extends Identifiable<Remote.Identity> {

    /**
     * Configure remote.
     *
     * @param profile Profile.
     */
    void configure(@NonNull Profile profile);

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
     * Remote identity
     */
    public static final class Identity {

        private final String type;
        private String id;

        /**
         */
        public Identity(String type, String id) {
            this.type = type;
            this.id = id;
        }

        @Override
        public String toString() {
            return String.format("%s(type: %s)", id, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Identity other)
                // return true if type is not null AND it is equal to other OR if id is not null and is equal to other
                return this.type != null && Objects.equals(this.type, other.type) || this.id != null && Objects.equals(this.id, other.id);
            return false;
        }

        public String type() {
            return type;
        }

        public void id(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id);
        }
    }

    /**
     * Remote profile
     */
    public static record Profile(String id, JsonObject parameters) {}

}
