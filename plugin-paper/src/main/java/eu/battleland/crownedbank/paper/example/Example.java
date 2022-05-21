package eu.battleland.crownedbank.paper.example;

import eu.battleland.crownedbank.CrownedBankAPI;
import eu.battleland.crownedbank.model.Currency;
import eu.battleland.crownedbank.paper.helper.PlayerIdentity;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public class Example {

    public void test() {
        // Retrieve player
        final @NonNull var player = Bukkit.getPlayer("rgnter");
        // Retrieve bank API
        final var bank = Bukkit
                .getServicesManager()
                .load(CrownedBankAPI.class);

        if(bank == null || player == null)
            return;

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

    }

}
