package eu.battleland.crownedbank.i18n;

import eu.battleland.crownedbank.CrownedBank;
import eu.battleland.crownedbank.abstracted.Controllable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Translation registry.
 * @param <T> Translation class.
 */
public interface TranslationRegistry<T>
        extends Controllable {

    /**
     * @param locale Translation locale.
     * @param key    Translation key.
     * @return Translation.
     */
    T translatable(@NotNull Locale locale, @NotNull String key);

    /**
     * @param locale Translation locale.
     * @param key    Translation key.
     * @param params Translation params.
     * @return Translation.
     */
    T translatable(@NotNull Locale locale, @NotNull String key, Object... params);


    /**
     * Basic abstract implementation of registry.
     * @param <T> Translation class.
     */
    abstract class Base<T>
            implements TranslationRegistry<T> {

        private final Map<Locale, Map<String, T>> registry
                = new HashMap<>();

        /**
         * Process translation source.
         * @param stream Translation source.
         * @return Processed translation source.
         */
        public abstract Map<String, T> processSource(final InputStream stream);

        /**
         * @return Map of locale translation sources.
         */
        public abstract Map<Locale, InputStream> findSources();

        @Override
        public void initialize() {
            this.findSources().forEach((locale, src) -> {
                try(src) {
                    this.processSource(src);
                } catch (Exception x) {
                    CrownedBank.getLogger().severe("Couldn't process translation source.");
                    x.printStackTrace();
                }
            });
        }

        @Override
        public void terminate() {

        }

    }
}
