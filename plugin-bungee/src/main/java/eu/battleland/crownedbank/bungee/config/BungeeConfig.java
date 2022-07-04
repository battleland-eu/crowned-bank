package eu.battleland.crownedbank.bungee.config;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.config.GlobalConfig;
import lombok.NonNull;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.InputStream;

public class BungeeConfig
        extends GlobalConfig {

    private Configuration configuration;

    public BungeeConfig(@NonNull CrownedBankAPI api, @NonNull File configFile) {
        super(api, configFile);
    }

    @Override
    public void initialize() throws Exception {
        try(final var stream = streamConfiguration()) {
            this.configuration = YamlConfiguration
                    .getProvider(YamlConfiguration.class)
                    .load(stream);
        }
    }

    @Override
    public void terminate() {
        super.terminate();
    }

    @Override
    public InputStream provide() {
        return null;
    }
}
