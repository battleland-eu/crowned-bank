package eu.battleland.crownedbank.bungee.helper;

import eu.battleland.crownedbank.model.Account;
import lombok.NonNull;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Helper class
 */
public class PlayerIdentity {

    /**
     * Get player identity.
     * @param player Player.
     * @return Identity.
     */
    public static @NonNull Account.Identity of(final @NonNull ProxiedPlayer player) {
        return new Account.Identity(player.getUniqueId(), player.getName());
    }

}
