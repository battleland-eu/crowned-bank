package eu.battleland.crownedbank.tests.remote;

import com.zaxxer.hikari.HikariConfig;
import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.CurrencyRepository;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.sync.Remote;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RemotesTest {

    @Test
    public void testMySQL() throws Exception {
        final var bank = new CrownedBankAPI() {
            private final CurrencyRepository repo = new CurrencyRepository();


            @Override
            public Account createAccount(Account.@NotNull Identity identity) {
                return Account.builder().identity(identity).build();
            }

            @Override
            public CompletableFuture<Account> retrieveAccount(Account.@NotNull Identity identity) {
                return null;
            }

            @Override
            public CurrencyRepository getCurrencyRepository() {
                return repo;
            }
        };
        final var account = bank.createAccount(new Account.Identity(UUID.randomUUID(), "test"));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://root@localhost:3306/test");

        final var remote = Remote.sqlRemote(bank, config);
        remote.store(account).get();
    }

}
