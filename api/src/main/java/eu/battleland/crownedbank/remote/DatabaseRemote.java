package eu.battleland.crownedbank.remote;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.battleland.crownedbank.helper.Pair;
import eu.battleland.crownedbank.model.Account;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

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

        if(!data.has("jdbcUrl")
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
    }

    @Override
    public CompletableFuture<@Nullable Account> provideAccount(@NonNull Account.Identity identity) {
        return null;
    }

    @Override
    public CompletableFuture<Pair<Boolean, Float>> handleWithdraw(Account account,
                                                                  float amount) {
        return null;
    }

    @Override
    public CompletableFuture<Pair<Boolean, Float>> handleDeposit(Account account,
                                                                 float amount) {
        return null;
    }
}
