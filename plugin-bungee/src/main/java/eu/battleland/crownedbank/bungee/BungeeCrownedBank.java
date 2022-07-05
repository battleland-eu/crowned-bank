package eu.battleland.crownedbank.bungee;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.remote.SqlRemote;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeCrownedBank
        extends CrownedBankAPI.Base
        implements Listener {

    @Getter
    private static Plugin pluginInstance;

    public BungeeCrownedBank(Plugin plugin) {
        pluginInstance = plugin;

        // register remote factories
        {
            this.remoteFactoryRepository()
                    .register(SqlRemote.factory());
        }

    }

    @Override
    public void initialize() throws Exception {

    }

    @Override
    public void terminate() {

    }
}
