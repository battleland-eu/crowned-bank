package eu.battleland.crownedbank.tests;

import eu.battleland.crownedbank.CrownedBankConstants;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class CommonTest {

    @Test
    public void testIdentity() {
        {
            final var identity0 = new Account.Identity(UUID.randomUUID(), "name");
            final var identity1 = new Account.Identity(identity0.uuid(), "name2");
            Assertions.assertEquals(identity0, identity1);
        }
        {
            CrownedBankConstants.setIdentityNameMajor(true);
            final var identity0 = new Account.Identity(UUID.randomUUID(), "name");
            final var identity1 = new Account.Identity(UUID.randomUUID(), "name");
            Assertions.assertEquals(identity0, identity1);
        }

        {
            Assertions.assertNotEquals(
                    new Account.Identity(UUID.randomUUID(), "not_equal"),
                    new Account.Identity(UUID.randomUUID(), "or_is_it?")
            );
        }
    }

    @Test
    public void testThreadSafe() throws ExecutionException, InterruptedException {

        final var identity = new Account.Identity(UUID.randomUUID(), "name");
        final Account account = Account.builder()
                .identity(identity)
                .withdrawHandler((currency, amount) -> {
                    try {
                        Thread.sleep(new Random().nextInt(100));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return true;
                })
                .depositHandler((currency, amount) -> true)
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
        Assertions.assertEquals(accepted.get(), 5);
    }

}
