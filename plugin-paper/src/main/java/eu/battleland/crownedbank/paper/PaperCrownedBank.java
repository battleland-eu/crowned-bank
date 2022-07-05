package eu.battleland.crownedbank.paper;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.i18n.TranslationRegistry;
import eu.battleland.crownedbank.paper.remote.ProxyRemote;
import eu.battleland.crownedbank.remote.SqlRemote;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Paper implementation of the CrownedBankAPI.
 */
@Log4j2
@Accessors(fluent = true)
public class PaperCrownedBank
        extends CrownedBankAPI.Base
        implements Listener {

    @Getter
    private static BankPlugin pluginInstance;

    @Getter
    private TranslationRegistry<Component> translationRegistry
            = new TranslationRegistry.Base<Component>() {
        @Override
        public Map<String, Component> processSource(InputStream stream) {
            final var result = new HashMap<String, Component>();
            try(InputStreamReader reader = new InputStreamReader(stream)) {
                final var yaml = new YamlConfiguration();
                yaml.load(reader);

            } catch (Exception x) {
                throw new IllegalStateException(x);
            }
            return result;
        }

        @Override
        public Map<Locale, InputStream> findSources() {
            return null;
        }

        @Override
        public Component translatable(@NotNull Locale locale, @NotNull String key) {
            return null;
        }

        @Override
        public Component translatable(@NotNull Locale locale, @NotNull String key, Object... params) {
            return null;
        }
    };


    /**
     * Default constructor.
     *
     * @param plugin Plugin instance.
     */
    public PaperCrownedBank(@NonNull BankPlugin plugin) {
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
    }

    @Override
    public void terminate() {

    }
}
