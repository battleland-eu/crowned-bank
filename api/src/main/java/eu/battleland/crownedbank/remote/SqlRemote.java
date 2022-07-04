package eu.battleland.crownedbank.remote;

import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.battleland.crownedbank.CrownedBank;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class SqlRemote
        implements Remote {

    private final String identifier;

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

    public SqlRemote(@NonNull String identifier) {
        this.identifier = identifier;
    }

    public static Factory factory() {
        return new Factory() {
            @Override
            public Remote build(Profile profile) {
                return new SqlRemote(profile.id()).configure(profile);
            }

            @Override
            public @NonNull String identifier() {
                return "sql";
            }
        };
    }

    @Override
    public Remote configure(@NonNull Profile profile) {
        final var data = profile.parameters();

        if (!data.has("jdbc_url")
                && !data.has("username")
                && !data.has("password")
                && !data.has("pool_size")) {
            throw new IllegalStateException("Data are missing a required field. (jdbc_url, username, password, pool_size)");
        }
        {
            config.setJdbcUrl(data.getAsJsonPrimitive("jdbc_url")
                    .getAsString());
            config.setUsername(data.getAsJsonPrimitive("username")
                    .getAsString());
            config.setPassword(data.getAsJsonPrimitive("password")
                    .getAsString());
            config.setMaximumPoolSize(data.getAsJsonPrimitive("pool_size")
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

        return this;
    }

    @Override
    public @NonNull String identifier() {
        return this.identifier;
    }

    @Override
    public CompletableFuture<Boolean> storeAccount(@NonNull Account account) {
        return CompletableFuture.supplyAsync(() -> {
            final var identity = account.getIdentity();
            try (final var connection = this.dataSource.getConnection();
                 final var statement = connection.createStatement()) {

                final var json = Account.Data
                        .encode(account.getData(), Predicate.isEqual(this));

                return statement.execute(
                        String.format(storeCommand,
                                tablePrefix,
                                identity.name(),
                                identity.uuid().toString(),
                                json
                        ));
            } catch (Exception x) {
                x.printStackTrace();
                throw new IllegalStateException("Communication failure.");
            }
        });
    }

    @Override
    public CompletableFuture<Account.@Nullable Data> fetchAccount(@NonNull Account.Identity identity) {
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

                final var json = JsonParser
                        .parseString(result.getString("json_data"))
                        .getAsJsonObject();

                return Account.Data.decode(json, Predicate.isEqual(this));
            } catch (Exception x) {
                x.printStackTrace();
                return null;
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
                                 CrownedBank.getWealthyCheckAccountLimit()
                         ))) {
                final var list = new ArrayList<Account>();
                if(result.getFetchSize() == 0)
                    return list;

                do {
                    final var account = CrownedBank.GSON
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
    public CompletableFuture<Boolean> handleWithdraw(final Account account,
                                                     final Currency.Storage currencyStorage,
                                                     final float amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
               if(currencyStorage.withdraw(amount)) {
                   storeAccount(account);
                   return true;
               } else
                   return false;
            } catch (Exception x) {
                throw new IllegalStateException(x);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> handleDeposit(Account account, Currency.Storage currencyStorage, float amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if(currencyStorage.deposit(amount)) {
                    storeAccount(account);
                    return true;
                } else
                    return false;
            } catch (Exception x) {
                throw new IllegalStateException(x);
            }
        });
    }
}
