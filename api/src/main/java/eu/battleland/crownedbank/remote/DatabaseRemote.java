package eu.battleland.crownedbank.remote;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.battleland.crownedbank.CrownedBankConstants;
import eu.battleland.crownedbank.helper.Pair;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DatabaseRemote
        implements Remote {

    private final HikariConfig config
            = new HikariConfig();
    private HikariDataSource dataSource;


    private String tablePrefix = "crownedbank";

    private String tableCommand = """
            create table if not exists `%s_data`
              ( `identity_name` TEXT NOT NULL , `identity_uuid` TEXT NOT NULL , `json_data` TEXT NOT NULL , UNIQUE (`identity_name`), UNIQUE (`identity_uuid`));
            """;
    private String fetchWealthyCommand = """
            select `identity_name`, `identity_uuid`, `json_data`, JSON_EXTRACT(`json_data`, '$.currencies.%2$s') as worth
            from `%1$s_data` order by worth desc limit %3$d
            """;
    private String storeCommand = """
            insert into `%s_data` (`identity_name`,`identity_uuid`,`json_data`) values('%s','%s','%s')
            on duplicate key update json_data='%4$s'
            """;
    private String fetchCommand = """
            select `json_data` from `%s_data`
            where `identity_name`='%s' OR `identity_uuid`='%s'
            """;


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

        if(data.has("tablePrefix"))
            this.tablePrefix = data.getAsJsonPrimitive("tablePrefix").getAsString();

        // create database
        try(final var connection = this.dataSource.getConnection();
            final var statement = connection.createStatement()) {
            statement.execute(String.format(tableCommand, tablePrefix));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public CompletableFuture<Boolean> storeAccount(@NonNull Account account) {
        return CompletableFuture.supplyAsync(() -> {
            final var identity = account.getIdentity();
            try (final var connection = this.dataSource.getConnection();
                 final var statement = connection.createStatement()) {
                return statement.execute(
                        String.format(storeCommand,
                                tablePrefix,
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
                         String.format(fetchCommand,
                                 tablePrefix,
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
    public CompletableFuture<List<Account>> fetchWealthyAccounts(@NonNull Currency currency) {
        return CompletableFuture.supplyAsync(() -> {
            try (final var connection = this.dataSource.getConnection();
                 final var statement = connection.createStatement();
                 final var result = statement.executeQuery(
                         String.format(fetchWealthyCommand,
                                 tablePrefix,
                                 currency.identifier(),
                                 CrownedBankConstants.getWealthyCheckAccountLimit()
                         ))) {
                final var list = new ArrayList<Account>();
                if(result.getFetchSize() == 0)
                    return list;

                do {
                    final var account = CrownedBankConstants.GSON
                            .fromJson(result.getString("json_data"), Account.class);
                    list.add(account);
                } while (result.next());

                return list;
            } catch (Exception x) {
                x.printStackTrace();
                throw new IllegalStateException("Communication failure.");
            }
        });
    }

    @Override
    public CompletableFuture<Pair<Boolean, Float>> handleWithdraw(Account account,
                                                                  Currency currency,
                                                                  float amount,
                                                                  CompletableFuture<Void> post) {
        post.thenAccept((future) -> {
            this.storeAccount(account);
        });

        return CompletableFuture.supplyAsync(() -> {
            try {
                final float currentAmount = account.status(currency);
                final float newAmount = currentAmount - amount;
                if(newAmount < 0)
                    return Pair.of(false, currentAmount);
                else {
                    storeAccount(account).get(); // store account in database
                    return Pair.of(true, newAmount);
                }
            } catch (Exception x) {
                throw new IllegalStateException(x);
            }
        });
    }

    @Override
    public CompletableFuture<Pair<Boolean, Float>> handleDeposit(Account account,
                                                                 Currency currency,
                                                                 float amount,
                                                                 CompletableFuture<Void> post) {
        post.thenAccept((future) -> {
            this.storeAccount(account);
        });

        return CompletableFuture.supplyAsync(() -> {
            try {
                return Pair.of(true, account.status(currency) + amount);
            } catch (Exception x) {
                x.printStackTrace();
                throw new IllegalStateException("Communication failure.");
            }
        });
    }
}
