package eu.battleland.crownedbank.paper;

import cloud.commandframework.arguments.standard.FloatArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.config.GlobalConfig;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.paper.bridge.PlaceholderExpansion;
import eu.battleland.crownedbank.paper.helper.PlayerIdentity;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.util.function.Function;
import java.util.stream.Collectors;


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

    @Getter
    private PaperCommandManager<CommandSender> commandManager;

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

        {
            try {
                this.commandManager = new PaperCommandManager<>(this,
                        AsynchronousCommandExecutionCoordinator.simpleCoordinator(),
                        Function.identity(),
                        Function.identity());
                if (this.commandManager.hasCapability(CloudBukkitCapabilities.BRIGADIER))
                    this.commandManager.registerBrigadier();
                if (this.commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION))
                    this.commandManager.registerAsynchronousCompletions();
            } catch (Exception e) {
                log.error("Couldn't initialize cloud command framework", e);
            }
        }

        commands();
        placeholders();

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

    }

    @Override
    public void onDisable() {

    }


    public void placeholders() {
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            log.info("Registered PlaceholderAPI Expansion");
            new PlaceholderExpansion().register();
        }
    }

    public void commands() {
        final var root = this.commandManager
                .commandBuilder("crownedbank", "bank", "cb")
                .permission("crownedbank.admin");

        this.commandManager.command(root.literal("withdraw")
                .argument(PlayerArgument.of("target"))
                .argument(StringArgument.<CommandSender>newBuilder("currency")
                        .withSuggestionsProvider((ctx, label) -> api.getCurrencyRepository().all().stream().map(Currency::identifier).collect(Collectors.toList())))
                .argument(FloatArgument.of("amount"))
                .handler(ctx -> {
                    final var sender = ctx.getSender();
                    final Player target = ctx.get("target");
                    final String currencyIdentifier = ctx.get("currency");
                    final float currencyAmount = ctx.get("amount");

                    final Currency currency = api.getCurrencyRepository().retrieve(currencyIdentifier);
                    if (currency == null) {
                        sender.sendMessage(Component.text("Invalid currency.")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    api.retrieveAccount(PlayerIdentity.of(target)).thenAccept((account -> {
                        account.withdraw(currency, currencyAmount).thenAccept((result) -> {
                            sender.sendMessage(result
                                    ? Component.text(String.format("Successfully withdrawn '%.2f' %s from %s", currencyAmount, currencyIdentifier, target.getName())).color(NamedTextColor.GREEN)
                                    : Component
                                    .text(String.format("Failed to withdraw '%.2f' %s from %s", currencyAmount, currencyIdentifier, target.getName())).color(NamedTextColor.RED));
                        });
                    }));
                }));
        this.commandManager.command(root.literal("deposit")
                .argument(PlayerArgument.of("target"))
                .argument(StringArgument.<CommandSender>newBuilder("currency")
                        .withSuggestionsProvider((ctx, label) -> api.getCurrencyRepository().all().stream().map(Currency::identifier).collect(Collectors.toList())))
                .argument(FloatArgument.of("amount"))
                .handler(ctx -> {
                    final var sender = ctx.getSender();
                    final Player target = ctx.get("target");
                    final String currencyIdentifier = ctx.get("currency");
                    final float currencyAmount = ctx.get("amount");

                    final Currency currency = api.getCurrencyRepository().retrieve(currencyIdentifier);
                    if (currency == null) {
                        sender.sendMessage(Component.text("Invalid currency.")
                                .color(NamedTextColor.RED));
                        return;
                    }

                    api.retrieveAccount(PlayerIdentity.of(target)).thenAccept((account -> {
                        account.deposit(currency, currencyAmount).thenAccept((result) -> {
                            sender.sendMessage(result
                                    ? Component.text(String.format("Successfully deposited '%.2f' %s to %s", currencyAmount, currencyIdentifier, target.getName())).color(NamedTextColor.GREEN)
                                    : Component
                                    .text(String.format("Failed to deposit '%.2f' %s from %s", currencyAmount, currencyIdentifier, target.getName())).color(NamedTextColor.RED));
                        });
                    }));
                }));
        this.commandManager.command(root.literal("status")
                .argument(PlayerArgument.of("target"))
                .argument(StringArgument.<CommandSender>newBuilder("currency")
                        .withSuggestionsProvider((ctx, label) -> api.getCurrencyRepository().all().stream().map(Currency::identifier).collect(Collectors.toList()))
                        .asOptional())
                .handler(ctx -> {
                    final var sender = ctx.getSender();
                    final Player target = ctx.get("target");
                    final String currencyIdentifier = ctx.getOrDefault("currency", null);

                    final Currency currency;
                    if (currencyIdentifier != null) {
                        currency = api.getCurrencyRepository().retrieve(currencyIdentifier);
                    } else {
                        currency = null;
                    }

                    api.retrieveAccount(PlayerIdentity.of(target)).thenAccept((account -> {
                        Component response = Component.text(String.format("Account status of '%s': ", target.getName()))
                                .color(NamedTextColor.GRAY);

                        if (currency != null) {
                            response = response.append(Component.newline());
                            response = response
                                    .append(Component.text("    - ")
                                            .color(NamedTextColor.DARK_GRAY))
                                    .append(Component.text(currency.identifier())
                                            .color(NamedTextColor.WHITE))
                                    .append(Component.text(String.format(" %.2f", account.status(currency)))
                                            .color(NamedTextColor.GREEN));
                        } else {
                            for (Currency c : api.getCurrencyRepository().all()) {
                                response = response.append(Component.newline());
                                response = response
                                        .append(Component.text("    - ")
                                                .color(NamedTextColor.DARK_GRAY))
                                        .append(Component.text(c.identifier())
                                                .color(NamedTextColor.WHITE))
                                        .append(Component.text(String.format(" %.2f", account.status(c)))
                                                .color(NamedTextColor.GREEN));
                            }
                        }
                        sender.sendMessage(response);
                    }));
                }));
    }


}
