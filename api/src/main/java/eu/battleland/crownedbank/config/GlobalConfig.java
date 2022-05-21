package eu.battleland.crownedbank.config;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import eu.battleland.crownedbank.CrownedBankAPI;
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
        if(!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
                try(final var output = new FileWriter(configFile);
                        final var input = new InputStreamReader(provide())) {
                    int b;
                    do {
                        b = input.read();
                        if(b != -1) {
                            output.write(b);
                        }
                    } while (b != -1);
                }
            } catch (IOException e) {
                throw new Exception("Couldn't create configuration file.", e);
            }
        }

        try (final var stream = new FileReader(configFile)) {
            final var object = JsonParser.parseReader(stream).getAsJsonObject();
            {
                // configure remotes
                object.getAsJsonArray("remotes").forEach(inferior -> {
                    final var remoteProfile = inferior.getAsJsonObject();
                    final var profile = new Remote.Profile(
                            remoteProfile.getAsJsonPrimitive("id")
                                    .getAsString(),
                            remoteProfile.getAsJsonObject("parameters")
                    );

                    final var remote = api.getRemoteRepository()
                            .retrieve(profile.id());
                    if(remote == null)
                        throw new IllegalStateException("No such remote identified by " + profile.id());
                    remote.configure(profile);
                });

                final var componentDeserializer = GsonComponentSerializer.gson();

                // configure remotes
                object.getAsJsonArray("currencies").forEach(inferior -> {
                    final var json = inferior.getAsJsonObject();
                    final var currency = Currency.builder()
                            .identifier(json.getAsJsonPrimitive("id").getAsString())
                            .namePlural(componentDeserializer.deserializeFromTree(json.get("namePlural")))
                            .nameSingular(componentDeserializer.deserializeFromTree(json.get("nameSingular")))
                            .format(json.getAsJsonPrimitive("format").getAsString())
                            .build();
                    api.getCurrencyRepository().register(currency);
                });

                api.getCurrencyRepository().setMajorCurrency(
                        api.getCurrencyRepository()
                                .retrieve(object.getAsJsonPrimitive("major_currency")
                                        .getAsString())
                );

                api.getCurrencyRepository().setMinorCurrency(
                        api.getCurrencyRepository()
                                .retrieve(object.getAsJsonPrimitive("major_currency")
                                        .getAsString())
                );
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
