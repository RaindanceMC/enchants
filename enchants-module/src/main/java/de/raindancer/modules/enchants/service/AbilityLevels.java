package de.raindancer.modules.enchants.service;

import de.raindancer.core.content.enchant.CustomEnchantments;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** The level of an enchantment for a player: what the gear carries, and what the player has unlocked besides. */
@FunctionalInterface
public interface AbilityLevels {

    /** The gear alone, for a server where nothing is unlocked. */
    AbilityLevels GEAR_ONLY = (player, gear, enchantment) -> CustomEnchantments.levelOn(gear, enchantment);

    int effective(Player player, ItemStack gear, String enchantment);
}
