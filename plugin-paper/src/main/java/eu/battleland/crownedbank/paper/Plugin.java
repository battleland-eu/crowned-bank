package eu.battleland.crownedbank.paper;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.config.GlobalConfig;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.paper.helper.PlayerIdentity;
import eu.battleland.crownedbank.paper.remote.ProxyRemote;
import eu.battleland.crownedbank.remote.DatabaseRemote;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;


@Log4j2
public class Plugin
        extends JavaPlugin {
    /**
     * Plugin Instance.
     */
    @Getter
    private static Plugin instance;

    {
        instance = this;
    }

    /**
     * API Instance.
     */
    @Getter
    private final CrownedBankAPI api
            = new PaperCrownedBank(this);

    /**
     * Config Instance.
     */
    @Getter
    private final GlobalConfig configuration
            = new GlobalConfig(api, new File(this.getDataFolder(), "config.json")) {
        @Override
        public InputStream provide() {
            return Plugin.this.getResource("resources/config.json");
        }
    };

    @Override
    public void onEnable() {

        // register default remotes
        api.getRemoteRepository()
                .register(new DatabaseRemote());
        api.getRemoteRepository()
                .register(new ProxyRemote(this));

        // intialize configuration
        try {
            configuration.initialize();
        } catch (Exception e) {
            log.error("Couldn't initialize global configuration", e);
        }

        // register service
        {
            Bukkit.getServicesManager()
                    .register(CrownedBankAPI.class, this.api, this, ServicePriority.High);
            log.info("Registered bukkit service.");
        }

        // test command
        Bukkit.getServer().getCommandMap().register("test", new Command("test") {
            @Override
            public boolean execute(@NotNull CommandSender sender,
                                   @NotNull String commandLabel,
                                   @NotNull String[] args) {
                final var player = (Player) sender;

                // Retrieve bank API
                final var bank = Bukkit
                        .getServicesManager()
                        .load(CrownedBankAPI.class);

                // Retrieve account
                bank.retrieveAccount(PlayerIdentity.of(player)).thenAccept((account) -> {
                    // Deposit currency
                    account.deposit(Currency.majorCurrency, 10f).thenAccept((accepted) -> {
                        player.sendMessage(accepted
                                ? Component.text("Successfully deposited money to your account")
                                : Component.text("Failed to deposit money to your account")
                        );
                    });
                });
                return false;
            }
        });
    }

    @Override
    public void onDisable() {
    }


}
