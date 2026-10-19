package de.raindancer.modules.enchants.listener;

import de.raindancer.core.content.enchant.EnchantShards;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Shards made before shards had their own looks get the key the pack picks their model by, wherever they turn up. */
public final class ShardLookListener implements IEnchantsListener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        dress(event.getPlayer().getInventory());
        dress(event.getPlayer().getEnderChest());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        dress(event.getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        Item item = event.getItem();
        ItemStack stack = item.getItemStack();
        if (EnchantShards.dress(stack)) {
            item.setItemStack(stack);
        }
    }

    static void dress(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (EnchantShards.dress(contents[slot])) {
                inventory.setItem(slot, contents[slot]);
            }
        }
    }
}
