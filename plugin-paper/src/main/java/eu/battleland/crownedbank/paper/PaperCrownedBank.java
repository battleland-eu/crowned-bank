package eu.battleland.crownedbank.paper;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.CurrencyRepository;
import eu.battleland.crownedbank.RemoteRepository;
import eu.battleland.crownedbank.config.GlobalConfig;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.paper.remote.ProxyRemote;
import eu.battleland.crownedbank.remote.DatabaseRemote;
import eu.battleland.crownedbank.remote.Remote;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.apache.commons.lang3.Validate;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.SpigotConfig;

import java.awt.print.Paper;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paper implementation of the CrownedBankAPI.
 */
@Log4j2
public class PaperCrownedBank
        extends CrownedBankAPI.Base
        implements Listener {

    @Getter
    private final Plugin plugin;

    @Getter
    private final CurrencyRepository currencyRepository
            = new CurrencyRepository();
    @Getter
    private final RemoteRepository remoteRepository
            = new RemoteRepository();

    /**
     * Default constructor.
     *
     * @param plugin Plugin instance.
     */
    public PaperCrownedBank(@NonNull Plugin plugin) {
        this.plugin = plugin;
        if (SpigotConfig.bungee) {
            this.setRemote(new ProxyRemote(plugin));
        } else {

        }
    }
}
