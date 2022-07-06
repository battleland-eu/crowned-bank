package eu.battleland.crownedbank.bungee;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.i18n.TranslationRegistry;
import eu.battleland.crownedbank.remote.SqlRemote;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.plugin.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

@Accessors(fluent = true)
public class BungeeCrownedBank
        extends CrownedBankAPI.Base
        implements Listener {

    @Getter
    private static BungeePlugin pluginInstance;

    @Getter
    private final TranslationRegistry<String> translationRegistry = new TranslationRegistry.Base<String>() {
        @Override
        public String translatable(@NotNull Locale locale, @NotNull String key) {
            return null;
        }

        @Override
        public String translatable(@NotNull Locale locale, @NotNull String key, Object... params) {
            return null;
        }

        @Override
        public Map<String, String> processSource(Locale locale, InputStream stream) {
            return null;
        }

        @Override
        public Map<Locale, InputStream> findSources() {
            return null;
        }
    };

    public BungeeCrownedBank(BungeePlugin plugin) {
        pluginInstance = plugin;

        // register remote factories
        {
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
        this.translationRegistry.terminate();
    }

    @Override
    protected Logger provideLogger() {
        return pluginInstance.getLogger();
    }
}
