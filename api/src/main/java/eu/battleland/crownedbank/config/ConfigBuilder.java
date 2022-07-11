package eu.battleland.crownedbank.config;

import com.google.gson.JsonParser;
import eu.battleland.crownedbank.CrownedBank;
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
public abstract class ConfigBuilder
        implements Controllable {

    private final CrownedBankAPI api;
    private final File configFile;


    public ConfigBuilder(@NonNull CrownedBankAPI api,
                         @NonNull File configFile) {
        this.api = api;
        this.configFile = configFile;
    }

    public FileReader streamConfiguration() throws Exception {
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

        // stream configuration from filesystem
        return new FileReader(configFile);
    }

    @Override
    public void initialize() throws Exception {
        // read configuration
        try (final var stream = streamConfiguration()) {
            final var root = JsonParser.parseReader(stream).getAsJsonObject();
            {
                {
                    // terminate all existing remotes.
                    api.remoteRepository().all()
                            .forEach(remote -> {
                                try {
                                    remote.terminate();
                                } catch (Exception x) {
                                    CrownedBank.getLogger().severe("Couldn't terminate remote " + remote.identifier());
                                    x.printStackTrace();
                                }

                            });
                }

                // configure remotes
                root.getAsJsonArray("remotes").forEach(profileJson -> {
                    final var json = profileJson.getAsJsonObject();

                    final var remoteIdentifier = json.getAsJsonPrimitive("id").getAsString();
                    final var remoteType = json.getAsJsonPrimitive("type").getAsString();
                    final var remoteParameters = json.getAsJsonObject("parameters");

                    final var remoteProfile = new Remote.Profile(
                            remoteIdentifier, remoteParameters
                    );

                    final var remoteFactory = api.remoteFactoryRepository()
                            .retrieve(remoteType);
                    if (remoteFactory == null)
                        throw new IllegalStateException("No such remote factory " + remoteType);

                    final var remote = remoteFactory.build(remoteProfile);

                    api.remoteRepository()
                            .register(remote);
                });

                {
                    // initialize all registered remotes
                    api.remoteRepository().all()
                            .forEach(registeredRemote -> {
                                try {
                                    registeredRemote.initialize();
                                } catch (Exception e) {
                                    CrownedBank.getLogger().severe("Couldn't initialize remote " + registeredRemote.identifier());
                                    e.printStackTrace();
                                }
                            });
                }

                final var componentDeserializer = GsonComponentSerializer.gson();
                // configure currencies
                root.getAsJsonArray("currencies").forEach(inferior -> {
                    final var currencyJson = inferior.getAsJsonObject();

                    final var remoteIdentifier = currencyJson.getAsJsonPrimitive("remote_id")
                            .getAsString();
                    final var remote = api.remoteRepository().retrieve(remoteIdentifier);
                    if (remote == null)
                        throw new IllegalStateException("No such remote identified by " + remoteIdentifier);

                    final var currencyBuilder = Currency.builder()
                            .identifier(currencyJson.getAsJsonPrimitive("id").getAsString())
                            .format(currencyJson.getAsJsonPrimitive("format").getAsString())
                            .allowDecimal(currencyJson.getAsJsonPrimitive("allow_decimal").getAsBoolean())
                            .remote(remote);

                    if (currencyJson.has("namePlural"))
                        currencyBuilder.namePlural(componentDeserializer.deserializeFromTree(currencyJson.get("namePlural")));
                    if (currencyJson.has("nameSingular"))
                        currencyBuilder.nameSingular(componentDeserializer.deserializeFromTree(currencyJson.get("nameSingular")));

                    api.currencyRepository()
                            .register(currencyBuilder.build());
                });

                // configure defaults
                final var majorCurrency = api.currencyRepository()
                        .retrieve(root.getAsJsonPrimitive("major_currency")
                                .getAsString());
                final var minorCurrency = api.currencyRepository()
                        .retrieve(root.getAsJsonPrimitive("minor_currency")
                                .getAsString());


                if (majorCurrency != null) {
                    api.currencyRepository().setMajorCurrency(
                            majorCurrency
                    );
                } else
                    throw new Exception("Unknown major currency");

                if (minorCurrency != null) {
                    api.currencyRepository().setMinorCurrency(
                            minorCurrency
                    );
                }

                {
//                    final var limit = root.getAsJsonPrimitive("wealth_check_account_limit");
//                    final var timer = root.getAsJsonPrimitive("wealth_check_every_minutes");
//                    if(timer != null)
//                        CrownedBank.setWealthyCheckMillis(timer.getAsLong() * 60 * 1000); // to seconds, to milliseconds
//                    if(limit != null)
//                        CrownedBank.setWealthyCheckAccountLimit(limit.getAsInt());
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
