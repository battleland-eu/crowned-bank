package eu.battleland.crownedbank.paper;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.repo.CurrencyRepository;
import eu.battleland.crownedbank.repo.RemoteRepository;
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
    private static BankPlugin pluginInstance;

    /**
     * Default constructor.
     *
     * @param plugin Plugin instance.
     */
    public PaperCrownedBank(@NonNull BankPlugin plugin) {
        pluginInstance = plugin;

        // register remote factories
        {
            getRemoteFactoryRepository()
                    .register(ProxyRemote.factory());
            getRemoteFactoryRepository()
                    .register(SqlRemote.factory());
        }

    }

    @Override
    public void initialize() {
    }

    @Override
    public void terminate() {

    }
}
