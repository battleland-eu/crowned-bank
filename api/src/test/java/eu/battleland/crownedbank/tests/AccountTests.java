package eu.battleland.crownedbank.tests;

import eu.battleland.crownedbank.helper.Pair;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class AccountTests {

    @Test
    public void testThreadSafety() throws ExecutionException, InterruptedException {

        final var identity = new Account.Identity(UUID.randomUUID(), "name");
        final Account account = Account.builder()
                .identity(identity)
                .withdrawHandler((currency, amount, accountRef) -> {
                    final var status = accountRef.status(currency);
                    try {
                        Thread.sleep(new Random().nextInt(100));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(status - amount < 0)
                        return Pair.of(false, status);
                    return Pair.of(true, status - amount);
                })
                .depositHandler((currency, amount, accountRef) -> Pair.of(true, accountRef.status(currency) + amount))
                .build();
        final var currency = Currency.builder()
                .identifier("cookies")
                .format("%.2f")
                .build();

        account.deposit(currency, 100).get();

        AtomicInteger accepted = new AtomicInteger();
        for (int i = 0; i < 24; i++) {
            new Thread(() -> {
                account.withdraw(currency, 20).thenAccept((value) -> {
                    synchronized (accepted) {
                        if(value)
                            accepted.incrementAndGet();
                    }
                });
            }).start();
        }
        Thread.sleep(2000);
        Assertions.assertEquals(5, accepted.get());
    }

}
