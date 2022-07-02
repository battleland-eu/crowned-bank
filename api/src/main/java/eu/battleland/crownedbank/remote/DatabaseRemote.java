package eu.battleland.crownedbank.remote;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.battleland.crownedbank.CrownedBankConstants;
import eu.battleland.crownedbank.helper.Pair;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DatabaseRemote
        implements Remote {

    private final HikariConfig config
            = new HikariConfig();
    private HikariDataSource dataSource;

    @Override
    public @NonNull String identifier() {
        return "database";
    }

    @Override
    public void configure(@NonNull Profile profile) {
        final var data = profile.parameters();

        if (!data.has("jdbcUrl")
                && !data.has("username")
                && !data.has("password")
                && !data.has("poolSize")) {
            throw new IllegalStateException("Data are missing a required field. (jdbcUrl, username, password, poolSize)");
        }
        {
            config.setJdbcUrl(data.getAsJsonPrimitive("jdbcUrl")
                    .getAsString());
            config.setUsername(data.getAsJsonPrimitive("username")
                    .getAsString());
            config.setPassword(data.getAsJsonPrimitive("password")
                    .getAsString());
            config.setMaximumPoolSize(data.getAsJsonPrimitive("poolSize")
                    .getAsInt());
        }
        this.dataSource = new HikariDataSource(this.config);
    }

    @Override
    public CompletableFuture<Boolean> storeAccount(@NonNull Account account) {
        return CompletableFuture.supplyAsync(() -> {
            final var identity = account.getIdentity();
            try (final var connection = this.dataSource.getConnection();
                 final var statement = connection.createStatement()) {
                return statement.execute(
                        String.format(CrownedBankConstants.getSqlStoreCommand(),
                                CrownedBankConstants.getSqlTablePrefix(),
                                identity.name(),
                                identity.uuid().toString(),
                                CrownedBankConstants.GSON.toJson(account)
                        ));
            } catch (Exception x) {
                x.printStackTrace();
                throw new IllegalStateException("Communication failure.");
            }
        });
    }

    @Override
    public CompletableFuture<@Nullable Account> fetchAccount(@NonNull Account.Identity identity) {
        return CompletableFuture.supplyAsync(() -> {
            try (final var connection = this.dataSource.getConnection();
                 final var statement = connection.createStatement();
                 final var result = statement.executeQuery(
                         String.format(CrownedBankConstants.getSqlFetchCommand(),
                                 CrownedBankConstants.getSqlTablePrefix(),
                                 identity.name(),
                                 identity.uuid().toString()
                         ))) {
                if(result.getFetchSize() == 0)
                    return null;
                return CrownedBankConstants.GSON.fromJson(result.getString("json_data"), Account.class);
            } catch (Exception x) {
                x.printStackTrace();
                throw new IllegalStateException("Communication failure.");
            }

        });
    }

    @Override
    public CompletableFuture<Pair<Boolean, Float>> handleWithdraw(Account account,
                                                                  Currency currency,
                                                                  float amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                float currentAmount = account.status(currency);
                float newAmount = currentAmount - amount;
                if(newAmount < 0)
                    return Pair.of(false, currentAmount);
                else {
                    storeAccount(account);
                    return Pair.of(true, newAmount);
                }
            } catch (Exception x) {
                x.printStackTrace();
                throw new IllegalStateException("Communication failure.");
            }
        });
    }

    @Override
    public CompletableFuture<Pair<Boolean, Float>> handleDeposit(Account account,
                                                                 Currency currency,
                                                                 float amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                storeAccount(account);
                return Pair.of(true, account.status(currency) + amount);
            } catch (Exception x) {
                x.printStackTrace();
                throw new IllegalStateException("Communication failure.");
            }
        });
    }
}
