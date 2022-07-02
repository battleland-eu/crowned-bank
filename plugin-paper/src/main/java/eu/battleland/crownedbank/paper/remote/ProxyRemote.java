package eu.battleland.crownedbank.paper.remote;

import com.google.common.io.ByteStreams;
import eu.battleland.crownedbank.CrownedBankConstants;
import eu.battleland.crownedbank.helper.Pair;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.paper.BankPlugin;
import eu.battleland.crownedbank.proxy.ProxyConstants;
import eu.battleland.crownedbank.proxy.ProxyOperation;
import eu.battleland.crownedbank.remote.Remote;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ProxyRemote
        implements Remote, Listener {


    private final BankPlugin plugin;

    private final Map<Account.Identity, CompletableFuture<Pair<Boolean, Float>>> withdrawFutures
            = new ConcurrentHashMap<>();
    private final Map<Account.Identity, CompletableFuture<Pair<Boolean, Float>>> depositFutures
            = new ConcurrentHashMap<>();

    private final Map<Account.Identity, CompletableFuture<Account>> fetchFutures
            = new ConcurrentHashMap<>();
    private CompletableFuture<List<Account>> fetchWealthyFuture;

    @Getter
    private boolean acceptConfiguration = true;
    @Getter
    private InetAddress toleratedAddress
            = InetAddress.getLoopbackAddress();

    public ProxyRemote(@NonNull BankPlugin plugin) {
        this.plugin = plugin;


        // Register outgoing channel
        Bukkit.getServer().getMessenger()
                .registerOutgoingPluginChannel(this.plugin, ProxyConstants.CHANNEL);

        // Register incoming channel listener
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(this.plugin, ProxyConstants.CHANNEL, (channel, player, message) -> {
            if (!channel.equals(ProxyConstants.CHANNEL))
                return;
            final var stream
                    = ByteStreams.newDataInput(message);
            try {
                // read sub-channel
                final var sub = stream.readUTF();
                if(!ProxyConstants.SUB_CHANNEL.equals(sub))
                    return;

                // read operation
                final var op = ProxyOperation
                        .values()[stream.readByte()];

                if(op == ProxyOperation.FETCH_WEALTHY_RESPONSE) {
                    if(fetchWealthyFuture == null) {
                        log.warn("Fetch wealthy response received, but not expected.");
                        return;
                    }

                    int count = stream.readInt();
                    final List<Account> accounts = new ArrayList<>(count);

                    for (int i = 0; i < count; i++) {
                        accounts.add(CrownedBankConstants.GSON.fromJson(stream.readUTF(), Account.class));
                    }
                    fetchWealthyFuture.complete(accounts);
                    fetchWealthyFuture = null;

                    log.info("Fetched wealthy accounts");
                } else {

                    // read account identity
                    final var identity = CrownedBankConstants.GSON
                            .fromJson(stream.readUTF(), Account.Identity.class);

                    switch (op) {
                        case FETCH_RESPONSE -> {
                            final var future
                                    = this.fetchFutures.remove(identity);
                            if(future == null) {
                                log.warn("Fetch response for '{}' received, but not expected.", identity);
                                return;
                            }

                            final var response = stream.readUTF();

                            if (response.length() == 0 || response.equals("null")) {
                                future.complete(null);
                                log.error("Fetched null account '{}'", identity);
                            } else {
                                final var account
                                        = CrownedBankConstants.GSON.fromJson(response, Account.class);

                                future.complete(account);
                                log.info("Fetched account for '{}'", identity);
                            }
                        }
                        case WITHDRAW_RESPONSE -> {
                            final var withdrawFuture = this.withdrawFutures.remove(identity);
                            final var data = Pair.of(
                                    stream.readBoolean(),
                                    stream.readFloat()
                            );
                            if(withdrawFuture == null) {
                                log.warn("Withdraw response for '{}' received but not expected. ({};{})",
                                        identity, data.getFirst(), data.getSecond()
                                );
                                return;
                            }
                            withdrawFuture.complete(data);
                            log.info("Withdraw from account '{}' completed.", identity);
                        }
                        case DEPOSIT_RESPONSE -> {
                            final var depositFuture = this.depositFutures.remove(identity);
                            final var data = Pair.of(
                                    stream.readBoolean(),
                                    stream.readFloat()
                            );
                            if(depositFuture == null) {
                                log.warn("Deposit response for '{}' received but not expected. ({};{})",
                                        identity, data.getFirst(), data.getSecond()
                                );
                                return;
                            }
                            depositFuture.complete(data);
                            log.info("Deposit to account '{}' completed.", identity);
                        }
                    }
                }
            } catch (Exception x) {
                log.error("Malformed proxy response", x);
            }
        });
    }

    @Override
    public @NonNull String identifier() {
        return "proxy";
    }

    @Override
    public void configure(@NonNull Profile profile) {
        if (profile.parameters()
                .has("accept_configuration")) {
            this.acceptConfiguration = profile.parameters()
                    .getAsJsonPrimitive("accept_configuration")
                    .getAsBoolean();
        }
        if (profile.parameters()
                .has("tolerated_address")) {
            final var address = profile.parameters()
                    .getAsJsonPrimitive("tolerated_address")
                    .getAsString();
            try {
                this.toleratedAddress = InetAddress.getByName(address);
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Invalid address: " + address);
            }
        }
    }

    @Override
    public CompletableFuture<Boolean> storeAccount(@NonNull Account account) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<@Nullable Account> fetchAccount(@NonNull Account.Identity identity) {
        final var future = new CompletableFuture<Account>();
        this.fetchFutures.put(identity, future);

        // request account from proxy
        {
            final var data = ByteStreams.newDataOutput();
            data.writeUTF(ProxyConstants.SUB_CHANNEL);
            data.writeByte(ProxyOperation.FETCH_REQUEST.ordinal());

            // identity
            data.writeUTF(CrownedBankConstants.GSON.toJson(identity));

            Bukkit.getServer().getOnlinePlayers().stream().findFirst().ifPresentOrElse((player) -> {
                player.sendPluginMessage(this.plugin, ProxyConstants.CHANNEL, data.toByteArray());
            }, () -> {
                log.error("There's nobody online. I can't send message through to proxy.");
            });
        }

        return future.orTimeout(10, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<List<Account>> fetchWealthyAccounts(@NonNull Currency currency) {
        final var future = new CompletableFuture<List<Account>>();
        this.fetchWealthyFuture = future;

        // request account from proxy
        {
            final var data = ByteStreams.newDataOutput();
            data.writeUTF(ProxyConstants.SUB_CHANNEL);
            data.writeByte(ProxyOperation.FETCH_WEALTHY_REQUEST.ordinal());

            // currency identifier
            data.writeUTF(currency.identifier());

            Bukkit.getServer().getOnlinePlayers().stream().findFirst().ifPresentOrElse((player) -> {
                player.sendPluginMessage(this.plugin, ProxyConstants.CHANNEL, data.toByteArray());
            }, () -> {
                log.error("There's nobody online. I can't send message through to proxy.");
            });
        }

        return future.orTimeout(10, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Pair<Boolean, Float>> handleWithdraw(@NonNull Account account,
                                                                  Currency currency,
                                                                  float amount,
                                                                  CompletableFuture<Void> postHandler) {
        final var future = new CompletableFuture<Pair<Boolean, Float>>();
        this.withdrawFutures.put(account.getIdentity(), future);

        // request withdraw from account
        {
            final var data = ByteStreams.newDataOutput();
            data.writeUTF(ProxyConstants.SUB_CHANNEL);
            data.writeByte(ProxyOperation.WITHDRAW_REQUEST.ordinal());

            // identity
            data.writeUTF(CrownedBankConstants.GSON.toJson(account.getIdentity()));
            // currency identifier
            data.writeUTF(currency.identifier());
            // amount
            data.writeFloat(amount);

            Bukkit.getServer().getOnlinePlayers().stream().findFirst().ifPresentOrElse((player) -> {
                player.sendPluginMessage(this.plugin, ProxyConstants.CHANNEL, data.toByteArray());
            }, () -> {
                log.error("There's nobody online. I can't send message through to proxy.");
            });
        }

        return future.orTimeout(10, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Pair<Boolean, Float>> handleDeposit(@NonNull Account account,
                                                                 Currency currency,
                                                                 float amount,
                                                                 CompletableFuture<Void> postHandler) {
        final var future = new CompletableFuture<Pair<Boolean, Float>>();
        this.depositFutures.put(account.getIdentity(), future);

        // request deposit from account
        {
            final var data = ByteStreams.newDataOutput();
            data.writeUTF(ProxyConstants.SUB_CHANNEL);
            data.writeByte(ProxyOperation.DEPOSIT_REQUEST.ordinal());

            // identity
            data.writeUTF(CrownedBankConstants.GSON.toJson(account.getIdentity()));
            // currency identifier
            data.writeUTF(currency.identifier());
            // amount
            data.writeFloat(amount);

            Bukkit.getServer().getOnlinePlayers().stream().findFirst().ifPresentOrElse((player) -> {
                player.sendPluginMessage(this.plugin, ProxyConstants.CHANNEL, data.toByteArray());
            }, () -> {
                log.error("There's nobody online. I can't send message through to proxy.");
            });
        }
        return future.orTimeout(10, TimeUnit.SECONDS);
    }
}
