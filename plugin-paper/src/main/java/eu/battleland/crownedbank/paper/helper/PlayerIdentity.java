package eu.battleland.crownedbank.paper.helper;

import eu.battleland.crownedbank.model.Account;
import lombok.NonNull;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Helper class
 */
public class PlayerIdentity {

    /**
     * Get player identity.
     * @param player Player.
     * @return Identity.
     */
    public static @NonNull Account.Identity of(final @NonNull OfflinePlayer player) {
        return new Account.Identity(player.getUniqueId(), player.getName());
    }

}
