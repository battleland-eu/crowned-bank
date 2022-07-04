package eu.battleland.crownedbank.paper;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.CurrencyRepository;
import eu.battleland.crownedbank.RemoteRepository;
import eu.battleland.crownedbank.paper.remote.ProxyRemote;
import eu.battleland.crownedbank.remote.SqlRemote;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.bukkit.event.Listener;
import org.spigotmc.SpigotConfig;

/**
 * Paper implementation of the CrownedBankAPI.
 */
@Log4j2
public class PaperCrownedBank
        extends CrownedBankAPI.Base
        implements Listener {

    @Getter
    private final BankPlugin plugin;

    @Getter
    private final CurrencyRepository currencyRepository
            = new CurrencyRepository();
    @Getter
    private final RemoteRepository remoteRepository
            = new RemoteRepository();

    /**
     * Default constructor.
     *
     * @param plugin Plugin instance.
     */
    public PaperCrownedBank(@NonNull BankPlugin plugin) {
        this.plugin = plugin;

        final var proxyRemote = new ProxyRemote(plugin);
        {
            getRemoteRepository()
                    .register(proxyRemote);
        }
        final var databaseRemote = new SqlRemote();
        {
            getRemoteRepository()
                    .register(databaseRemote);
        }

        // select default remote
        if (SpigotConfig.bungee)
            this.setRemote(proxyRemote);
        else
            this.setRemote(databaseRemote);
    }
}
