package eu.battleland.crownedbank.bungee;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.i18n.TranslationRegistry;
import eu.battleland.crownedbank.remote.SqlRemote;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.plugin.Listener;

import java.util.logging.Logger;

@Accessors(fluent = true)
public class BungeeCrownedBank
        extends CrownedBankAPI.Base
        implements Listener {

    @Getter
    private static BungeePlugin pluginInstance;

    @Getter
    private final TranslationRegistry<String> translationRegistry = null;

    public BungeeCrownedBank(BungeePlugin plugin) {
        pluginInstance = plugin;

        // register remote factories
        {
            this.remoteFactoryRepository()
                    .register(SqlRemote.factory());
        }
    }

    @Override
    protected Logger provideLogger() {
        return pluginInstance.getLogger();
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void terminate() {

    }
}
