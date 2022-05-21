package eu.battleland.crownedbank.paper;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.CurrencyRepository;
import eu.battleland.crownedbank.helper.Handler;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.paper.remote.ProxyRemote;
import eu.battleland.crownedbank.remote.Remote;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.Validate;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.SpigotConfig;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paper implementation of the CrownedBankAPI.
 */
public class PaperCrownedBank
        implements CrownedBankAPI, Listener {

    @Getter
    private final Plugin plugin;

    @Getter
    private Remote remote;

    @Getter
    private final CurrencyRepository currencyRepository
            = new CurrencyRepository();

    private final Map<Account.Identity, Account> cachedAccounts
            = new ConcurrentHashMap<>();

    public PaperCrownedBank(@NonNull Plugin plugin) {
        this.plugin = plugin;
        if (SpigotConfig.bungee) {
            this.remote = new ProxyRemote(plugin);
        } else {

        }
    }

    @Override
    public Account createAccount(Account.@NotNull Identity identity) {
        Validate.notNull(remote, "Remote not present");

        return Account.builder()
                .identity(identity)
                .depositHandler(Handler.remoteDepositRelay(remote))
                .withdrawHandler(Handler.remoteWithdrawRelay(remote))
                .build();
    }

    @Override
    public CompletableFuture<Account> retrieveAccount(Account.@NotNull Identity identity) {
        Validate.notNull(remote, "Remote not present");

        return CompletableFuture.supplyAsync(() -> {
            if (this.cachedAccounts.containsKey(identity))
                return this.cachedAccounts.get(identity);
            final var account = createAccount(identity);
            this.cachedAccounts.put(identity, account);
            return account;
        });
    }
}
