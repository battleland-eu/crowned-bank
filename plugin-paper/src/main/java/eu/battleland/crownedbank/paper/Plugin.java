package eu.battleland.crownedbank.paper;

import eu.battleland.crownedbank.CrownedBankAPI;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;


@Log4j2
public class Plugin
        extends JavaPlugin {


    @Getter
    private static Plugin instance;
    {
        instance = this;
    }

    @Getter
    private PaperCrownedBank paperCrownedBank;

    @Override
    public void onEnable() {
        {
            this.paperCrownedBank
                    = new PaperCrownedBank(this);
            Bukkit.getServicesManager()
                    .register(CrownedBankAPI.class, this.paperCrownedBank, this, ServicePriority.High);
            log.info("Registered bukkit service.");
        }
    }

    @Override
    public void onDisable() {
    }

}
