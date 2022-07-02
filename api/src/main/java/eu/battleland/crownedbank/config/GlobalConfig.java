package eu.battleland.crownedbank.config;

import com.google.gson.JsonParser;
import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.CrownedBankConstants;
import eu.battleland.crownedbank.abstracted.Controllable;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.remote.Remote;
import lombok.NonNull;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.*;

/**
 * Global Configuration
 */
public abstract class GlobalConfig
        implements Controllable {

    private final CrownedBankAPI api;
    private final File configFile;


    public GlobalConfig(@NonNull CrownedBankAPI api,
                        @NonNull File configFile) {
        this.api = api;
        this.configFile = configFile;
    }

    @Override
    public void initialize() throws Exception {
        // create configuration file
        // in filesystem, if it does not exist.
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();

                // write default configuration
                try (final var output = new FileWriter(configFile);
                     final var input = new InputStreamReader(provide())) {
                    int b;
                    do {
                        b = input.read();
                        if (b != -1) {
                            output.write(b);
                        }
                    } while (b != -1);
                }

            } catch (IOException e) {
                throw new Exception("Couldn't create configuration file.", e);
            }
        }

        // read configuration
        try (final var stream = new FileReader(configFile)) {
            final var root = JsonParser.parseReader(stream).getAsJsonObject();
            {
                // configure remotes
                root.getAsJsonArray("remotes").forEach(profileJson -> {
                    final var remoteProfile = profileJson.getAsJsonObject();
                    final var profile = new Remote.Profile(
                            remoteProfile.getAsJsonPrimitive("id").getAsString(),
                            remoteProfile.getAsJsonObject("parameters")
                    );

                    final var remote = api.getRemoteRepository()
                            .retrieve(profile.id());
                    if (remote == null)
                        throw new IllegalStateException("No such remote identified by " + profile.id());
                    remote.configure(profile);
                });

                final var componentDeserializer = GsonComponentSerializer.gson();

                // configure currencies
                root.getAsJsonArray("currencies").forEach(inferior -> {
                    final var currencyJson = inferior.getAsJsonObject();
                    final var currency = Currency.builder()
                            .identifier(currencyJson.getAsJsonPrimitive("id").getAsString())
                            .namePlural(componentDeserializer.deserializeFromTree(currencyJson.get("namePlural")))
                            .nameSingular(componentDeserializer.deserializeFromTree(currencyJson.get("nameSingular")))
                            .format(currencyJson.getAsJsonPrimitive("format").getAsString())
                            .build();

                    api.getCurrencyRepository()
                            .register(currency);
                });

                // configure defaults
                final var majorCurrency = api.getCurrencyRepository()
                        .retrieve(root.getAsJsonPrimitive("major_currency")
                                .getAsString());
                final var minorCurrency = api.getCurrencyRepository()
                        .retrieve(root.getAsJsonPrimitive("major_currency")
                                .getAsString());


                if (majorCurrency != null) {
                    api.getCurrencyRepository().setMajorCurrency(
                            majorCurrency
                    );
                } else
                    throw new Exception("Unknown major currency");

                if (minorCurrency != null) {
                    api.getCurrencyRepository().setMinorCurrency(
                            minorCurrency
                    );
                }

                {
                    final var limit = root.getAsJsonPrimitive("wealth_check_account_limit");
                    final var timer = root.getAsJsonPrimitive("wealth_check_every_minutes");
                    if(timer != null)
                        CrownedBankConstants.setWealthyCheckMillis(timer.getAsLong() * 60 * 1000); // to seconds, to milliseconds
                    if(limit != null)
                        CrownedBankConstants.setWealthyCheckAccountLimit(limit.getAsInt());
                }

            }
        } catch (IOException e) {
            throw new Exception("Couldn't read configuration.", e);
        }
    }

    @Override
    public void terminate() {

    }

    /**
     * Provide default configuration
     *
     * @return InputStream
     */
    public abstract InputStream provide();
}
