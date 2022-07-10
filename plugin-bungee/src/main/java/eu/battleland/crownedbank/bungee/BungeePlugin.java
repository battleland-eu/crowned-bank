package eu.battleland.crownedbank.bungee;

import eu.battleland.crownedbank.bungee.endpoint.ProxyEndpoint;
import eu.battleland.crownedbank.config.ConfigBuilder;
import lombok.Getter;

import java.io.File;
import java.io.InputStream;

public class BungeePlugin
        extends net.md_5.bungee.api.plugin.Plugin {

    /**
     * Plugin Instance.
     */
    @Getter
    private static BungeePlugin instance;
    {
        instance = this;
    }

    @Getter
    private ProxyEndpoint endpoint;

    /**
     * API Instance.
     */
    @Getter
    private final BungeeCrownedBank api
            = new BungeeCrownedBank(this);

    /**
     * Config Instance.
     */
    @Getter
    private final ConfigBuilder configuration
            = new ConfigBuilder(api, new File(this.getDataFolder(), "config.json")) {
        @Override
        public InputStream provide() {
            return BungeePlugin.this.getResourceAsStream("resources/config.json");
        }
    };


    @Override
    public void onLoad() {
        this.endpoint = new ProxyEndpoint(this);
    }

    @Override
    public void onEnable() {

        // initialize api
        this.api.initialize();

        // initialize configuration
        try {
            configuration.initialize();
            getLogger().info("Initialized global configuration");
        } catch (Exception e) {
            getLogger().severe("Couldn't initialize global configuration");
            e.printStackTrace();
        }

        // initialize endpoint
        this.endpoint.initialize();
    }

    @Override
    public void onDisable() {
        // terminate endpoint
        this.endpoint.terminate();
    }
}

