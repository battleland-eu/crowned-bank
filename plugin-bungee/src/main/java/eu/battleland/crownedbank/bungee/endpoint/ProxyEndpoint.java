package eu.battleland.crownedbank.bungee.endpoint;

import com.google.common.io.ByteStreams;
import eu.battleland.crownedbank.CrownedBankConstants;
import eu.battleland.crownedbank.abstracted.Controllable;
import eu.battleland.crownedbank.bungee.BankPlugin;
import eu.battleland.crownedbank.model.Account;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.proxy.ProxyConstants;
import eu.battleland.crownedbank.proxy.ProxyOperation;
import lombok.NonNull;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ProxyEndpoint
        implements Listener, Controllable {

    private final BankPlugin plugin;

    /**
     * Constructor.
     *
     * @param plugin Plugin Instance.
     */
    public ProxyEndpoint(@NonNull BankPlugin plugin) {
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

            // read sub-channel
            final var sub = request.readUTF();
            if (!ProxyConstants.SUB_CHANNEL.equals(sub))
                return;

            // read operation
            final var op = ProxyOperation
                    .values()[request.readByte()];

            // read identity
            final var identity = CrownedBankConstants.GSON.fromJson(
                    request.readUTF(), Account.Identity.class
            );

            // start building response
            {
                // write subchannel
                response.writeUTF(ProxyConstants.SUB_CHANNEL);
            }

            switch (op) {
                case FETCH_REQUEST -> {
                    // write operation
                    response.writeByte(ProxyOperation.FETCH_RESPONSE.ordinal());
                    // write identity
                    response.writeUTF(CrownedBankConstants.GSON.toJson(identity));

                    this.plugin.getLogger().info(String.format("Fetch request for '%s'.", identity));

                    // retrieve account, and continue building response.
                    plugin.getApi().retrieveAccount(identity).thenAccept((account -> {
                        if (account != null)
                            response.writeUTF(CrownedBankConstants.GSON.toJson(account));
                        else
                            response.writeUTF("null");
                        this.plugin.getLogger().info(String.format("Responded to fetch request for '%s'.", identity));
                    }));
                }
                case WITHDRAW_REQUEST -> {
                    // write operation
                    response.writeByte(ProxyOperation.WITHDRAW_RESPONSE.ordinal());
                    // write identity
                    response.writeUTF(CrownedBankConstants.GSON.toJson(identity));

                    try {
                        final Currency currency = plugin
                                .getApi()
                                .getCurrencyRepository()
                                .retrieve(request.readUTF());
                        if (currency == null)
                            throw new Exception("Unknown currency");
                        float amount = request.readFloat();

                        plugin.getApi().retrieveAccount(identity).thenAccept((account -> {
                            try {
                                final var result = account.withdraw(currency, amount).get();
                                response.writeBoolean(result);
                                response.writeFloat(account.status(currency));
                            } catch (Exception e) {
                                plugin.getLogger().warning("Couldn't withdraw from account " + identity);
                            }
                        }));

                        this.plugin.getLogger().info(String.format("Withdraw request for '%s'.", identity));
                    } catch (Exception x) {
                        plugin.getLogger().warning("Couldn't handle withdraw request for " + identity);

                        // decline withdraw
                        response.writeBoolean(false);
                        response.writeFloat(0);
                    }
                }
                case DEPOSIT_REQUEST -> {
                    // write operation
                    response.writeByte(ProxyOperation.DEPOSIT_RESPONSE.ordinal());
                    // write identity
                    response.writeUTF(CrownedBankConstants.GSON.toJson(identity));

                    try {
                        final Currency currency = plugin
                                .getApi()
                                .getCurrencyRepository()
                                .retrieve(request.readUTF());
                        if (currency == null)
                            throw new Exception("Unknown currency");
                        float amount = request.readFloat();

                        plugin.getApi().retrieveAccount(identity).thenAccept((account -> {
                            try {
                                final var result = account.deposit(currency, amount).get();
                                response.writeBoolean(result);
                                response.writeFloat(account.status(currency));
                            } catch (Exception e) {
                                plugin.getLogger().warning("Couldn't deposit to account " + identity);
                            }
                        }));

                        this.plugin.getLogger().info(String.format("Deposit request for '%s'.", identity));
                    } catch (Exception x) {
                        plugin.getLogger().warning("Couldn't handle deposit request for " + identity);

                        // decline deposit
                        response.writeBoolean(false);
                        response.writeFloat(0);
                    }
                }
            }

            // send response
            requestee.sendData(ProxyConstants.CHANNEL, response.toByteArray());

        } catch (Exception x) {
            this.plugin.getLogger().severe("Malformed proxy request");
            x.printStackTrace();
        } finally {
            event.setCancelled(true);
        }
    }
}
