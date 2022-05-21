package eu.battleland.crownedbank.bungee;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.bungee.endpoint.ProxyEndpoint;
import eu.battleland.crownedbank.config.GlobalConfig;
import eu.battleland.crownedbank.remote.DatabaseRemote;
import lombok.Getter;

import java.io.File;
import java.io.InputStream;

public class Plugin
        extends net.md_5.bungee.api.plugin.Plugin {

    /**
     * Plugin Instance.
     */
    @Getter
    private static Plugin instance;
    {
        instance = this;
    }

    @Getter
    private ProxyEndpoint endpoint;

    /**
     * API Instance.
     */
    @Getter
    private final CrownedBankAPI api
            = new BungeeCrownedBank(this);

    /**
     * Config Instance.
     */
    @Getter
    private final GlobalConfig configuration
            = new GlobalConfig(api, new File(this.getDataFolder(), "config.json")) {
        @Override
        public InputStream provide() {
            return Plugin.this.getResourceAsStream("resources/config.json");
        }
    };


    @Override
    public void onLoad() {
        this.endpoint = new ProxyEndpoint(this);
    }

    @Override
    public void onEnable() {

        // register default remotes
        api.getRemoteRepository()
                .register(new DatabaseRemote());

        // initialize configuration
        try {
            configuration.initialize();
        } catch (Exception e) {
            getLogger().warning("Couldn't initialize global configuration");
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

