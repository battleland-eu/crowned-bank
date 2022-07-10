package eu.battleland.crownedbank.bungee.endpoint;

import com.google.common.io.ByteStreams;
import eu.battleland.crownedbank.CrownedBank;
import eu.battleland.crownedbank.abstracted.Controllable;
import eu.battleland.crownedbank.bungee.BungeePlugin;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.proxy.ProxyConstants;
import eu.battleland.crownedbank.proxy.ProxyOperation;
import lombok.NonNull;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.CompletableFuture;

public class ProxyEndpoint
        implements Listener, Controllable {

    private final BungeePlugin plugin;

    /**
     * Constructor.
     *
     * @param plugin Plugin Instance.
     */
    public ProxyEndpoint(@NonNull BungeePlugin plugin) {
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
        if (!(event.getSender() instanceof Server requestee))
            return;

        // channel check
        if (!event.getTag().equals(ProxyConstants.CHANNEL))
            return;

        final var request
                = ByteStreams.newDataInput(event.getData());
        final var response
                = ByteStreams.newDataOutput();

        try {
            CompletableFuture.runAsync(() -> {
                // read sub-channel
                final var sub = request.readUTF();
                if (!ProxyConstants.SUB_CHANNEL.equals(sub))
                    return;

                // read operation
                final var op = ProxyOperation
                        .values()[request.readByte()];

                // start building response
                {
                    // write subchannel
                    response.writeUTF(ProxyConstants.SUB_CHANNEL);
                }

                if (op == ProxyOperation.FETCH_WEALTHY_REQUEST) {
                    this.plugin.getLogger().info("Fetch wealthy accounts request");

                    // write operation
                    response.writeByte(ProxyOperation.FETCH_WEALTHY_RESPONSE.ordinal());

                    try {
                        final Currency currency = plugin
                                .getApi()
                                .currencyRepository()
                                .retrieve(request.readUTF());

                        final var accounts = plugin.getApi()
                                .retrieveWealthyAccounts(currency).get(); // fetch dummy accounts

                        response.writeInt(accounts.size());
                        accounts.forEach(account -> {
                            response.writeUTF(CrownedBank.GSON.toJson(account));
                        });
                    } catch (Exception x) {
                        response.writeInt(0);
                    }

                    this.plugin.getLogger().info("Responded to fetch wealthy accounts request.");
                } else {
                    // read identity
                    final var identity = CrownedBank.GSON.fromJson(
                            request.readUTF(), Account.Identity.class
                    );

                    switch (op) {
                        case FETCH_REQUEST -> {
                            this.plugin.getLogger().info(String.format("Fetch request for '%s'.", identity));

                            // write operation
                            response.writeByte(ProxyOperation.FETCH_RESPONSE.ordinal());
                            // write identity
                            response.writeUTF(CrownedBank.GSON.toJson(identity));

                            Account account;
                            try {
                                account = plugin.getApi().retrieveAccount(identity).get(); // fetch account
                            } catch (Exception x) {
                                this.plugin.getLogger().info(String.format("Couldn't fetch account for '%s'.", identity));
                                x.printStackTrace();

                                account = null;
                            }

                            if (account != null) {
                                final var json = Account.Data.encode(account.getData(), (t) -> true);
                                response.writeUTF(json.toString());
                            }
                            else
                                response.writeUTF("null");

                            this.plugin.getLogger().info(String.format("Responded to fetch request for '%s'.", identity));
                        }
                        case WITHDRAW_REQUEST -> {
                            this.plugin.getLogger().info(String.format("Withdraw request for '%s'.", identity));

                            // write operation
                            response.writeByte(ProxyOperation.WITHDRAW_RESPONSE.ordinal());
                            // write identity
                            response.writeUTF(CrownedBank.GSON.toJson(identity));

                            try {
                                final Currency currency = plugin
                                        .getApi()
                                        .currencyRepository()
                                        .retrieve(request.readUTF());
                                if (currency == null)
                                    throw new Exception("Unknown currency");
                                float amount = request.readFloat();

                                final var account = plugin.getApi().retrieveAccount(identity).get();
                                {
                                    final var result = account.withdraw(currency, amount).get();
                                    response.writeBoolean(result);
                                    response.writeFloat(account.status(currency));
                                }
                            } catch (Exception x) {
                                this.plugin.getLogger().warning(String.format("Couldn't withdraw from account '%s'.", identity));
                                x.printStackTrace();
                            }
                        }
                        case DEPOSIT_REQUEST -> {
                            this.plugin.getLogger().info(String.format("Deposit request for '%s'.", identity));

                            // write operation
                            response.writeByte(ProxyOperation.DEPOSIT_RESPONSE.ordinal());
                            // write identity
                            response.writeUTF(CrownedBank.GSON.toJson(identity));

                            try {
                                final Currency currency = plugin
                                        .getApi()
                                        .currencyRepository()
                                        .retrieve(request.readUTF());
                                if (currency == null)
                                    throw new Exception("Unknown currency");
                                float amount = request.readFloat();

                                final var account = plugin.getApi().retrieveAccount(identity).get();
                                {
                                    final var result = account.deposit(currency, amount).get();
                                    response.writeBoolean(result);
                                    response.writeFloat(account.status(currency));
                                }
                            } catch (Exception x) {
                                this.plugin.getLogger().warning(String.format("Couldn't deposit to account '%s'.", identity));
                                x.printStackTrace();
                            }
                        }
                    }
                }

                // send response
                requestee.sendData(ProxyConstants.CHANNEL, response.toByteArray());
            });

        } catch (Exception x) {
            this.plugin.getLogger().severe("Malformed proxy request");
            x.printStackTrace();
        } finally {
            event.setCancelled(true);
        }
    }
}
