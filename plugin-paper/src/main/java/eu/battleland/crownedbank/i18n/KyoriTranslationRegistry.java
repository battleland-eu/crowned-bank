package eu.battleland.crownedbank.i18n;

import eu.battleland.crownedbank.paper.PaperCrownedBank;
import io.papermc.paper.adventure.PaperAdventure;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translatable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2(topic = "CrownedBank Translations")
public class KyoriTranslationRegistry
        extends TranslationRegistry.Base<Component> {

    @Getter
    private final net.kyori.adventure.translation.TranslationRegistry kyoriTranslationRegistry =
            net.kyori.adventure.translation.TranslationRegistry.create(Key.key("crownedbank", "i18n"));

    @Override
    public Map<String, Component> processSource(Locale locale, InputStream stream) {
        final var result = new HashMap<String, Component>();

        try (InputStreamReader reader = new InputStreamReader(stream)) {
            final var yaml = new YamlConfiguration();
            yaml.load(reader);

            yaml.getKeys(false).forEach(key -> {
                final var translationString = yaml.getString(key);

                try {
                    final var translation = new MessageFormat(translationString);
                    kyoriTranslationRegistry.register(key, locale, translation);
                } catch (IllegalArgumentException x) {
                    log.error("Invalid translation string '{}', are the arguments wrong?", translationString);
                }
            });

        } catch (Exception x) {
            throw new IllegalStateException(x);
        }
        return result;
    }

    @Override
    public Map<Locale, InputStream> findSources() {
        final var result = new HashMap<Locale, InputStream>();
        {
            final var languageFolder
                    = new File(PaperCrownedBank.pluginInstance().getDataFolder(), "languages");
            final var languageFiles = languageFolder.listFiles();
            if(!languageFolder.isDirectory() || !languageFolder.exists() || languageFiles == null) {
                log.error("Language folder is not a directory, is empty, or does not exist.");
                return result;
            }

            for (File languageFile : languageFiles) {
                final var name = languageFile.getName();
                final var localeTag = name.substring(0, name.lastIndexOf('.'));
                final var locale = Locale.forLanguageTag(localeTag);

                if(locale == null) {
                    log.error("No such locale identified by '{}' from file '{}'", localeTag, languageFile.getPath());
                    continue;
                }
                try {
                    result.put(locale, new FileInputStream(languageFile));
                    log.info("Found translation source for locale {}", locale);
                } catch (FileNotFoundException e) {
                    log.error("Couldn't access language file '{}'", languageFile.getPath());
                }

            }
        }
        return result;
    }

    @Override
    public void initialize() {
        super.initialize();

        // Add our translation registry to global translator
        try {
            GlobalTranslator.get()
                    .addSource(kyoriTranslationRegistry); // since 4.0.0
        } catch (NoSuchMethodError x) {
            GlobalTranslator.translator()
                    .addSource(kyoriTranslationRegistry); // since 4.10.0
        }

        log.info("Initialized.");
    }

    @Override
    public void terminate() {
        super.terminate();
        log.info("Terminated.");
    }

    @Override
    public Component translatable(@NotNull Locale locale, @NotNull String key) {
        return GlobalTranslator.render(Component.translatable(key), locale);
    }

    @Override
    public Component translatable(@NotNull Locale locale, @NotNull String key, Object... params) {
        return GlobalTranslator.render(Component.translatable(key, Arrays.stream(params)
                .map(Object::toString)
                .map(Component::text)
                .collect(Collectors.toList())), locale);
    }

}
