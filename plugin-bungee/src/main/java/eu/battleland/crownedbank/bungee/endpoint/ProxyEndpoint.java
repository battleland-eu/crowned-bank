package eu.battleland.crownedbank.bungee.endpoint;

import com.google.common.io.ByteStreams;
import eu.battleland.crownedbank.abstracted.Controllable;
import eu.battleland.crownedbank.bungee.Plugin;
import eu.battleland.crownedbank.proxy.ProxyConstants;
import lombok.NonNull;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.lang.reflect.Proxy;

public class ProxyEndpoint
    implements Listener, Controllable {

    private final Plugin plugin;

    /**
     * Constructor.
     * @param plugin Plugin Instance.
     */
    public ProxyEndpoint(@NonNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        this.plugin.getProxy().registerChannel(ProxyConstants.CHANNEL);
        this.plugin.getProxy().getPluginManager()
                .registerListener(this.plugin, this);
    }

    @Override
    public void terminate() {
        this.plugin.getProxy().unregisterChannel(ProxyConstants.CHANNEL);
    }

    @EventHandler
    public void on(final PluginMessageEvent event) {
        if(!event.getTag().equals(ProxyConstants.CHANNEL))
            return;
        final var stream = ByteStreams.newDataInput(event.getData());
    }
}
