package eu.battleland.crownedbank.sync;

import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.CrownedBankConstants;
import eu.battleland.crownedbank.model.Account;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Represents remote which is handling accounts.
 */
public abstract class Remote {

    /**
     * Stores account in remote.
     *
     * @param account Account to store.
     * @return Completable future which completes with true, when account is successfully stored.
     */
    public abstract CompletableFuture<Boolean> store(final Account account);

    /**
     * Retrieves account from remote.
     *
     * @param identity Identity of account which to retrieve.
     * @return Completable future which completes with optional account.
     */
    public abstract CompletableFuture<Optional<Account>> retrieve(final Account.Identity identity);

    /**
     * Withdraws {@code amount} from account.
     *
     * @param account Account.
     * @param amount  Amount.
     * @return Completable future which completes with true when amount was withdrawn.
     */
    public abstract CompletableFuture<Boolean> withdraw(final Account account,
                                                        float amount);

    /**
     * Deposits {@code amount} to account.
     *
     * @param account Account.
     * @param amount  Amount.
     * @return Completable future which completes with true when amount was deposited.
     */
    public abstract CompletableFuture<Boolean> deposit(final Account account,
                                                       float amount);


    /**
     * @param api    API instance
     * @param config SQL Configuration
     * @return SQL Remote
     */
    public static Remote sqlRemote(final @NotNull CrownedBankAPI api,
                                   final @NotNull HikariConfig config)
            throws Exception {
        return new SqlDatabase(api, config);
    }

    @Getter
    public static class SqlDatabase
            extends Remote {

        private final CrownedBankAPI api;

        private final HikariConfig config;
        private final HikariDataSource dataSource;

        public SqlDatabase(CrownedBankAPI api, HikariConfig config) {
            this.api = api;
            this.config = config;
            this.dataSource = new HikariDataSource(config);

            try {
                this.setupEnvironment();
            } catch (Exception x) {
                throw new IllegalStateException("Couldn't setup database environment", x);
            }
        }

        private void setupEnvironment() throws SQLException {
            dataSource.getConnection().createStatement().execute(
                    String.format(CrownedBankConstants.getSqlTableCommand(), CrownedBankConstants.getSqlTablePrefix())
            );
        }

        @Override
        public CompletableFuture<Boolean> store(Account account) {
            return CompletableFuture.supplyAsync(() -> {
                final var identity = account.getIdentity();
                final var accountData = Codec.encodeAccount(account, this.api.getCurrencyRepository());

                try {
                    if (!retrieve(account.getIdentity()).get().isPresent()) {
                        this.dataSource.getConnection()
                                .createStatement()
                                .execute(
                                        String.format(CrownedBankConstants.getSqlInsertCommand(),
                                                CrownedBankConstants.getSqlTablePrefix(),
                                                identity.name(), identity.uuid(),
                                                accountData
                                        )
                                );
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    this.dataSource.getConnection()
                            .createStatement()
                            .execute(
                                    String.format(CrownedBankConstants.getSqlUpdateCommand(),
                                            CrownedBankConstants.getSqlTablePrefix(),
                                            accountData,
                                            identity.name(), identity.uuid()
                                    )
                            );
                    return true;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            });
        }

        @Override
        public CompletableFuture<Optional<Account>> retrieve(Account.Identity identity) {
            return CompletableFuture.supplyAsync(() -> {
                Account.AccountBuilder account = Account.builder()
                        .identity(identity);
                try {
                    final var result = this.dataSource.getConnection()
                            .createStatement()
                            .executeQuery(
                                    String.format(CrownedBankConstants.getSqlQueryCommand(),
                                            CrownedBankConstants.getSqlTablePrefix(),
                                            identity.name(), identity.uuid()
                                    )
                            );
                    if (!result.next())
                        return Optional.empty();
                    String jsonData = result.getString("json_data");
                    return Optional.of(
                            Codec.decodeAccount(
                                    JsonParser.parseString(jsonData).getAsJsonObject(),
                                    account,
                                    api.getCurrencyRepository()
                            ).build()
                    );
                } catch (SQLException e) {
                    e.printStackTrace();
                    return Optional.empty();
                }
            });
        }

        @Override
        public CompletableFuture<Boolean> withdraw(Account account, float amount) {
            return store(account);
        }

        @Override
        public CompletableFuture<Boolean> deposit(Account account, float amount) {
            return store(account);
        }
    }

}
