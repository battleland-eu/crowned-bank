package eu.battleland.crownedbank.paper;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.i18n.KyoriTranslationRegistry;
import eu.battleland.crownedbank.i18n.TranslationRegistry;
import eu.battleland.crownedbank.paper.remote.ProxyRemote;
import eu.battleland.crownedbank.remote.SqlRemote;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Listener;

import java.util.logging.Logger;

/**
 * Paper implementation of the CrownedBankAPI.
 */
@Log4j2
@Accessors(fluent = true)
public class PaperCrownedBank
        extends CrownedBankAPI.Base
        implements Listener {

    @Getter
    private static PaperPlugin pluginInstance;

    @Getter
    private final TranslationRegistry<Component> translationRegistry
            = new KyoriTranslationRegistry();

    /**
     * Default constructor.
     *
     * @param plugin Plugin instance.
     */
    public PaperCrownedBank(@NonNull PaperPlugin plugin) {
        pluginInstance = plugin;

        // register remote factories
        {
            this.remoteFactoryRepository()
                    .register(ProxyRemote.factory());
            this.remoteFactoryRepository()
                    .register(SqlRemote.factory());
        }

    }

    @Override
    public void initialize() {
        super.initialize();
        try {
            this.translationRegistry.initialize();
        } catch (Exception x) {
            provideLogger().severe("Couldn't initialize translation registry.");
            x.printStackTrace();
        }
    }

    @Override
    public void terminate() {

    }

    @Override
    protected Logger provideLogger() {
        return pluginInstance.getLogger();
    }
}
