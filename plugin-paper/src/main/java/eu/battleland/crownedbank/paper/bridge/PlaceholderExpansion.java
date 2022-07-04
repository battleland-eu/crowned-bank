package eu.battleland.crownedbank.paper.bridge;

import eu.battleland.crownedbank.CrownedBank;
import eu.battleland.crownedbank.paper.helper.PlayerIdentity;
import lombok.extern.log4j.Log4j2;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

@Log4j2(topic = "CrownedBank Placeholders")
public class PlaceholderExpansion
        extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "crownedbank";
    }

    @Override
    public @NotNull String getAuthor() {
        return "rgnt.xyz";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        final var args = params.split("_");

        try {
            // account status
            if(args[0].equalsIgnoreCase("status")) {
                final var currencyIdentifier = args[1];
                final var currency = CrownedBank.getApi()
                        .getCurrencyRepository()
                        .retrieve(currencyIdentifier);
                if(currency == null)
                    return null;

                try {
                    final var account = CrownedBank.getApi()
                            .retrieveAccount(PlayerIdentity.of(player))
                            .get(1, TimeUnit.SECONDS);
                    return String.format("%.2f", account.status(currency));
                } catch (Exception e) {
                    return "n/a";
                }
            }
        } catch (Exception x) {
            log.error("Invalid placeholder: " + params);
        }
        return null;
    }
}
