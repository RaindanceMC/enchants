package de.raindancer.modules.enchants.store;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

/**
 * The ranks a player has unlocked in the abilities tree, one number per tree node on the player itself.
 * They live in {@link AirStore#NAMESPACE}, which Core already carries between the servers.
 */
public final class AbilityStore {

    private static NamespacedKey key(String node) {
        return new NamespacedKey(AirStore.NAMESPACE, "ability_" + node);
    }

    public int rank(Player player, String node) {
        return Math.max(0, player.getPersistentDataContainer().getOrDefault(key(node), PersistentDataType.INTEGER, 0));
    }

    public void set(Player player, String node, int rank) {
        player.getPersistentDataContainer().set(key(node), PersistentDataType.INTEGER, rank);
    }
}
