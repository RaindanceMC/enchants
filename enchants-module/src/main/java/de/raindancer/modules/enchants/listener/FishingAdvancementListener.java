package de.raindancer.modules.enchants.listener;

import de.raindancer.core.content.enchant.CustomEnchantments;
import de.raindancer.core.content.enchant.RainsEnchants;
import de.raindancer.modules.enchants.EnchantAdvancements;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * The fishing enchantments' first use: a catch landed with them on the rod. What they do to a catch is RainsFishing's
 * to decide; that one came up with them on is all the advancements need, and it is the same event either way.
 */
public final class FishingAdvancementListener implements IEnchantsListener {

    private static final List<String> FISHING = List.of(RainsEnchants.DEEP_BITE, RainsEnchants.TWIN_CATCH,
            RainsEnchants.TREASURE_HUNTER, RainsEnchants.MYTHIC_LURE);

    private final EnchantAdvancements advancements;

    public FishingAdvancementListener(EnchantAdvancements advancements) {
        this.advancements = advancements;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCatch(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (rod.getType() != Material.FISHING_ROD) {
            rod = player.getInventory().getItemInOffHand();
        }
        for (String enchantment : FISHING) {
            if (CustomEnchantments.levelOn(rod, enchantment) > 0) {
                advancements.used(player.getUniqueId(), enchantment);
            }
        }
    }

    @Override
    public void forget(UUID player) {
        // Nothing remembered per player.
    }
}
