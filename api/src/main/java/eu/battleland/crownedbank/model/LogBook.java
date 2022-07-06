package eu.battleland.crownedbank.model;

import eu.battleland.crownedbank.CrownedBank;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class LogBook {

    private static final List<Withdraw> withdrawRecords = new ArrayList<>();
    private static final List<Deposit> depositRecords = new ArrayList<>();
    private static final List<Payment> paymentRecords = new ArrayList<>();

    public enum RecordResult {
        SUCCESS,
        FAILURE,
        ERROR,
        NOT_EXECUTED;

        public static RecordResult byBoolean(@Nullable Boolean b){
            if(b == null) return ERROR;
            return b ? SUCCESS : FAILURE;
        }
    }

    /**
     * Log withdrawal from account.
     * @param account  Account.
     * @param currency Currency.
     * @param amount   Amount of currency.
     * @param result   Result of withdrawal.
     */
    public static synchronized void logWithdraw(final Account account,
                                         final Currency currency,
                                         final float amount,
                                         @Nullable RecordResult result) {
        final var record = new Withdraw(account, currency, amount, result, Instant.now().getEpochSecond());
        CrownedBank.getLogger().info(record.toString());
        withdrawRecords.add(record);
    }

    /**
     * Log deposit to account.
     * @param account  Account.
     * @param currency Currency.
     * @param amount   Amount of currency.
     * @param result   Result of deposit.
     */
    public static synchronized void logDeposit(final Account account,
                                        final Currency currency,
                                        final float amount,
                                        final RecordResult result) {
        final var record = new Deposit(account, currency, amount, result, Instant.now().getEpochSecond());
        CrownedBank.getLogger().info(record.toString());
        depositRecords.add(record);
    }

    /**
     * Log payment.
     * @param sender   Sender.
     * @param receiver Receiver.
     * @param currency Currency.
     * @param amount   Amount of currency.
     * @param withdrawResult Sender's account withdrawal result.
     * @param depositResult  Receiver's account deposit result.
     */
    public static synchronized void logPayment(final Account sender,
                                        final Account receiver,
                                        final Currency currency,
                                        final float amount,
                                        final RecordResult withdrawResult,
                                        final RecordResult depositResult) {
        final var record = new Payment(sender, receiver, currency, amount, withdrawResult, depositResult, Instant.now().getEpochSecond());
        CrownedBank.getLogger().info(record.toString());
        paymentRecords.add(record);
    }


    /**
     * Withdraw Log
     *
     * @param account   Account
     * @param currency  Currency
     * @param amount    Amount of currency
     * @param result    Result of withdraw.
     * @param timestamp Timestamp of withdraw.
     */
    record Withdraw(Account account,
                    Currency currency,
                    float amount,
                    RecordResult result,
                    long timestamp) {
        @Override
        public String toString() {
            return String.format("Withdraw from '%s' of currency '%s' with value '%.2f' was a %s.",
                    account.getIdentity(), currency.identifier(), amount, result);
        }
    }

    /**
     * Deposit Log
     *
     * @param account   Account
     * @param currency  Currency
     * @param amount    Amount of currency
     * @param result    Result of deposit.
     * @param timestamp Timestamp of deposit.
     */
    record Deposit(Account account,
                   Currency currency,
                   float amount,
                   RecordResult result,
                   long timestamp) {
        @Override
        public String toString() {
            return String.format("Deposit to '%s' of currency '%s' with value '%.2f' was a %s.",
                    account.getIdentity(), currency.identifier(), amount, result);
        }
    }

    /**
     * @param sender         Sender of payment
     * @param receiver       Receiver of payment
     * @param currency       Currency
     * @param amount         Amount of currency
     * @param withdrawResult Withdraw result
     * @param depositResult  Deposit result
     * @param timestamp      Timestamp of payment.
     */
    record Payment(Account sender,
                   Account receiver,
                   Currency currency,
                   float amount,
                   RecordResult withdrawResult,
                   RecordResult depositResult,
                   long timestamp) {
        @Override
        public String toString() {
            return String.format("Payment from '%s' to '%s' of currency '%s' with value '%.2f'" +
                            " was a %s from senders side, and %s from receivers side.",
                    sender.getIdentity(), receiver.getIdentity(), currency.identifier(), amount,
                    withdrawResult, depositResult);
        }
    }
}
