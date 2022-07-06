package eu.battleland.crownedbank.model;

import com.google.gson.JsonObject;
import eu.battleland.crownedbank.CrownedBank;
import eu.battleland.crownedbank.helper.TransactionHandler;
import eu.battleland.crownedbank.remote.Remote;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Builder
public class Account {

    @Getter
    private Account.Identity identity;

    @Getter
    @Builder.Default
    private Account.Data data = Account.Data.empty();

    @Builder.Default
    private transient TransactionHandler withdrawHandler = null;

    @Builder.Default
    private transient TransactionHandler depositHandler = null;


    /**
     * Withdraw currency from sender(calling object) and deposits it to receiver.
     * @param receiver Receiver.
     * @param currency Currency.
     * @param amount   Amount of currency.
     * @return Boolean true if successful.
     */
    public CompletableFuture<Boolean> pay(final Account receiver,
                                                    final Currency currency,
                                                    final float amount) {
        CompletableFuture.supplyAsync(() -> {
            final var withdrawResult = LogBook.RecordResult.byBoolean(
                    this.transaction(withdrawHandler, currency, amount, null, null)
            );

            boolean result = false;
            var depositResult = LogBook.RecordResult.NOT_EXECUTED;
            if(withdrawResult.equals(LogBook.RecordResult.SUCCESS)) {
                depositResult = LogBook.RecordResult.byBoolean(
                        receiver.transaction(receiver.depositHandler, currency, amount, null, null)
                );

                if(depositResult.equals(LogBook.RecordResult.SUCCESS))
                    result = true;
            }

            LogBook.logPayment(this, receiver, currency, amount, withdrawResult, depositResult);
            return result;
        });
    }

    /**
     * Withdraws currency from account.
     *
     * @param currency Currency
     * @param amount   Amount
     * @return Completable Future which completes with boolean true if withdraw was completed successfully. Completes with null, on unhandled exceptions.
     */
    public CompletableFuture<@Nullable Boolean> withdraw(final Currency currency,
                                                         float amount) {
        return CompletableFuture.supplyAsync(() -> {
            final var result = this.transaction(this.withdrawHandler, currency, amount, () -> {
                CrownedBank.getLogger().info(String.format(
                        "Successfully withdrawn '%.2f' %s from account '%s'.", amount, currency.identifier(), identity
                ));
            }, () -> {
                CrownedBank.getLogger().info(String.format(
                        "Failed to withdraw '%.2f' %s from account '%s'.", amount, currency.identifier(), identity
                ));
            });

            LogBook.logWithdraw(this, currency, amount, LogBook.RecordResult.byBoolean(result));
            return result;
        });
    }

    /**
     * Deposits currency to account.
     *
     * @param currency Currency
     * @param amount   Amount
     * @return Completable Future which completes with boolean true if deposit was completed successfully.
     */

    public CompletableFuture<@Nullable Boolean> deposit(final Currency currency,
                                              float amount) {
        return CompletableFuture.supplyAsync(() -> {
            final var result = this.transaction(this.depositHandler, currency, amount, () -> {
                CrownedBank.getLogger().info(String.format(
                        "Successfully deposited '%.2f' %s to account '%s'.", amount, currency.identifier(), identity
                ));
            }, () -> {
                CrownedBank.getLogger().info(String.format(
                        "Failed to deposit '%.2f' %s to account '%s'.", amount, currency.identifier(), identity
                ));
            });

            LogBook.logDeposit(this, currency, amount, LogBook.RecordResult.byBoolean(result));
            return result;
        });
    }

    /**
     * @param currency Currency
     * @return Currency amount.
     */
    public float status(final Currency currency) {
        final var storage = this.data.currencies.get(currency);
        if (storage != null)
            return storage.amount();
        return 0;
    }

    /**
     * Account Data
     *
     * @param currencies
     */
    public record Data(Map<Currency, Currency.Storage> currencies) {

        /**
         * Iterate through all currencies and call handler on them and their values.
         *
         * @param handler Handler.
         */
        public void iterateCurrencies(final BiConsumer<Currency, Float> handler) {
            this.currencies.forEach((currency, storage) -> {
                handler.accept(currency, storage.amount());
            });
        }

        /**
         * Decode Account data from json.
         *
         * @param json Json Object.
         * @return Account data
         */
        public static @NonNull Data decode(final JsonObject json,
                                           final Predicate<Remote> remoteRequirement) {
            final var data = empty();
            // iterate through currencies, create storage, and assign them to account data.
            {
                json.entrySet().forEach(entry -> {
                    final var currency = CrownedBank.getApi()
                            .currencyRepository()
                            .retrieve(entry.getKey());
                    if (currency == null)
                        return;
                    if (!remoteRequirement.test(currency.getRemote()))
                        return;

                    final var amount = entry.getValue()
                            .getAsJsonPrimitive()
                            .getAsFloat();
                    data.currencies.put(currency, currency.newStorage().change(amount));
                });
            }
            return data;
        }

        public static @NonNull JsonObject encode(final Data data,
                                                 final Predicate<Remote> remoteRequirement) {
            final var json = new JsonObject();
            {
                // iterate through all currencies
                data.iterateCurrencies((currency, amount) -> {
                    // if their remote is this, add them to json we will be storing in database.
                    if (remoteRequirement.test(currency.getRemote()))
                        json.addProperty(currency.identifier(), amount);
                });
            }
            return json;
        }

        /**
         * Join data.
         *
         * @param data Other data, which will be destroyed after joining this data.
         */
        public synchronized void join(@NonNull Account.Data data) {
            this.currencies.putAll(data.currencies);
            data.destroy();
        }

        /**
         * Destroy all data.
         */
        public synchronized void destroy() {
            this.currencies.clear();
        }

        /**
         * @return Empty Data
         */
        public static Data empty() {
            return new Account.Data(new HashMap<>());
        }
    }

    /**
     * Account identity
     */
    public record Identity(UUID uuid, String name) {

        @Override
        public String toString() {
            return String.format("%s(%s)", this.name, this.uuid);
        }

        /**
         * Equals operator
         *
         * @param o Other object.
         * @return Boolean true if identity matches by uuid. If {@link CrownedBank#isIdentityNameMajor()} is set to true, returns boolean true if identity matches by name.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Identity identity = (Identity) o;

            if (CrownedBank.isIdentityNameMajor())
                return Objects.equals(name, identity.name);
            else
                return Objects.equals(uuid, identity.uuid);
        }

        /**
         * @return Returns hash code of uuid. If {@link CrownedBank#isIdentityNameMajor()} is set to true, returns hash code of name instead.
         */
        @Override
        public int hashCode() {
            return CrownedBank.isIdentityNameMajor() ? name.hashCode() : uuid.hashCode();
        }
    }


    /**
     * Private implementation of transaction handling.
     */
    private synchronized Boolean transaction(final TransactionHandler handler,
                                             final Currency currency,
                                             final float amount,
                                             Runnable onSuccess,
                                             Runnable onFailure) {


        boolean result;
        try {
            // call transaction handler
             result = handler.handle(this.data.currencies.computeIfAbsent(currency, (t) -> currency.newStorage()), amount, this);
        } catch (final Exception x) {
            CrownedBank.getLogger().severe("Transaction handler threw exception");
            x.printStackTrace();

            result = false;
        }

       try {
           // transaction accepted
           if (result) {
               onSuccess.run();
               return true;
           } else {
               // transaction not accepted
               onFailure.run();
               return false;
           }
       } catch (Exception x) {
           CrownedBank.getLogger().severe("Transaction callbacks threw exception");
           x.printStackTrace();

            return null;
       }
    }

}
