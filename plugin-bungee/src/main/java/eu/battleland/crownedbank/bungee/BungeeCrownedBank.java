package eu.battleland.crownedbank.bungee;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.CurrencyRepository;
import eu.battleland.crownedbank.RemoteRepository;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeCrownedBank
        extends CrownedBankAPI.Base
        implements Listener {

    private final Plugin plugin;

    @Getter
    private final CurrencyRepository currencyRepository
            = new CurrencyRepository();
    @Getter
    private final RemoteRepository remoteRepository
            = new RemoteRepository();


    public BungeeCrownedBank(Plugin plugin) {
        this.plugin = plugin;
    }
}
