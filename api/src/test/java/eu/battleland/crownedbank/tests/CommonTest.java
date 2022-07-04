package eu.battleland.crownedbank.tests;

import eu.battleland.crownedbank.CrownedBank;
import eu.battleland.crownedbank.model.Account;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class CommonTest {

    @Test
    public void testIdentity() {
        {
            final var identity0 = new Account.Identity(UUID.randomUUID(), "name");
            final var identity1 = new Account.Identity(identity0.uuid(), "name2");
            Assertions.assertEquals(identity0, identity1);
        }
        {
            CrownedBank.setIdentityNameMajor(true);
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

}
