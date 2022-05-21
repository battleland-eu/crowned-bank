package eu.battleland.crownedbank.paper.remote;

import com.google.common.io.ByteStreams;
import eu.battleland.crownedbank.helper.Pair;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.proxy.ProxyConstants;
import eu.battleland.crownedbank.proxy.ProxyOperation;
import eu.battleland.crownedbank.remote.Remote;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class ProxyRemote
        implements Remote, Listener {


    private final JavaPlugin plugin;

    private final Map<Account.Identity, CompletableFuture<Pair<Boolean, Float>>> withdrawFutures
            = new ConcurrentHashMap<>();
    private final Map<Account.Identity, CompletableFuture<Pair<Boolean, Float>>> depositFutures
            = new ConcurrentHashMap<>();
    private final Map<Account.Identity, CompletableFuture<Account>> provideFutures
            = new ConcurrentHashMap<>();

    @Getter
    private InetAddress toleratedAddress
            = InetAddress.getLoopbackAddress();

    public ProxyRemote(@NonNull JavaPlugin plugin) {
        this.plugin = plugin;


        // Register outgoing channel
        Bukkit.getServer().getMessenger()
                .registerOutgoingPluginChannel(this.plugin, ProxyConstants.CHANNEL);
        // Register incoming channel listener
        Bukkit.getServer().getMessenger()
                .registerIncomingPluginChannel(this.plugin, ProxyConstants.CHANNEL,
                (channel, player, message) -> {
                    if(channel.equals(ProxyConstants.CHANNEL))
                        return;
                    final var stream
                            = ByteStreams.newDataInput(message);
                    try {
                        // read operation
                        final var op = ProxyOperation
                                .values()[stream.readByte()];

                        // read account identity
                        final var identity = ProxyConstants.GSON
                                .fromJson(stream.readUTF(), Account.Identity.class);


                        switch (op) {
                            case PROVIDE_RESPONSE -> {
                                this.provideFutures.get(identity)
                                        .complete(ProxyConstants.GSON.fromJson(stream.readUTF(), Account.class));
                                log.info("Provided new account for '{}'", identity);
                            }
                            case WITHDRAW_RESPONSE -> {
                                this.withdrawFutures.get(identity)
                                        .complete(Pair.of(stream.readBoolean(), stream.readFloat()));
                                log.info("Withdraw from account '{}' completed.", identity);
                            }
                            case DEPOSIT_RESPONSE -> {
                                this.depositFutures.get(identity)
                                        .complete(Pair.of(stream.readBoolean(), stream.readFloat()));
                                log.info("Deposit to account '{}' completed.", identity);
                            }
                        }
                    } catch (Exception ignored) {

                    }
                });
    }

    @Override
    public void configure(@NonNull Profile profile) {
        final var address = profile.parameters()
                .getAsJsonPrimitive("tolerated_address")
                .getAsString();

        try {
            try {
                this.toleratedAddress = InetAddress.getByName(address);
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Invalid address: " + address);
            }
        } catch (IllegalStateException x) {
            log.error("Couldn't read parameters", x);
        }
    }

    @Override
    public CompletableFuture<@Nullable Account> provideAccount(@NonNull Account.Identity identity) {
        final var future = new CompletableFuture<Account>();
        this.provideFutures.put(identity, future);

        // request
        {
            final var data = ByteStreams.newDataOutput();
            data.writeByte(ProxyOperation.PROVIDE_REQUEST.ordinal());
            Bukkit.getServer()
                    .sendPluginMessage(this.plugin, ProxyConstants.CHANNEL, data.toByteArray());
        }

        return future;
    }

    @Override
    public CompletableFuture<Pair<Boolean, Float>> handleWithdraw(@NonNull Account account,
                                                                  float amount) {
        final var future = new CompletableFuture<Pair<Boolean, Float>>();
        this.withdrawFutures.put(account.getIdentity(), future);

        // request
        {
            final var data = ByteStreams.newDataOutput();
            data.writeByte(ProxyOperation.WITHDRAW_REQUEST.ordinal());
            data.writeUTF(ProxyConstants.GSON.toJson(account.getIdentity()));
            data.writeFloat(amount);

            Bukkit.getServer()
                    .sendPluginMessage(this.plugin, ProxyConstants.CHANNEL, data.toByteArray());
        }

        return future;
    }

    @Override
    public CompletableFuture<Pair<Boolean, Float>> handleDeposit(@NonNull Account account,
                                                                 float amount) {
        final var future = new CompletableFuture<Pair<Boolean, Float>>();
        this.depositFutures.put(account.getIdentity(), future);

        // request
        {
            final var data = ByteStreams.newDataOutput();
            data.writeByte(ProxyOperation.DEPOSIT_REQUEST.ordinal());
            data.writeUTF(ProxyConstants.GSON.toJson(account.getIdentity()));
            data.writeFloat(amount);

            Bukkit.getServer()
                    .sendPluginMessage(this.plugin, ProxyConstants.CHANNEL, data.toByteArray());
        }

        return future;
    }
}
