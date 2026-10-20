package de.raindancer.modules.enchants.listener;

import de.raindancer.core.content.enchant.EnchantShards;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.modules.enchants.service.WeaponLooks;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Items that look the way their enchantments say: shards made before shards had their own looks, and weapons that
 * become something else ({@link WeaponLooks}: Mjölnir). An enchantment can come or go in many places — anvil,
 * grindstone, enchanting table, smithing, loot — so items are dressed wherever they turn up.
 *
 * <p>The station results are dressed at MONITOR because Fusion builds its anvil result at HIGHEST; only the look
 * changes there, never what the result is.
 */
public final class LookListener implements IEnchantsListener {

    private final Plugin plugin;

    public LookListener(Plugin plugin) {
        this.plugin = plugin;
    }

    static boolean dress(ItemStack stack) {
        boolean shard = EnchantShards.dress(stack);
        return WeaponLooks.dress(stack) | shard;
    }

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
        if (dress(stack)) {
            item.setItemStack(stack);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (dress(result)) {
            event.setResult(result);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGrindstone(PrepareGrindstoneEvent event) {
        ItemStack result = event.getResult();
        if (dress(result)) {
            event.setResult(result);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSmithing(PrepareSmithingEvent event) {
        ItemStack result = event.getResult();
        if (dress(result)) {
            event.setResult(result);
        }
    }

    /** Two worn pieces repaired in the crafting grid lose their enchantments. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (dress(result)) {
            event.getInventory().setResult(result);
        }
    }

    /** The table adds the enchantments after the event, so the item is dressed a tick later. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Inventory table = event.getInventory();
        Scheduling.entityLater(plugin, event.getEnchanter(), 1L, () -> {
            ItemStack item = table.getItem(0);
            if (dress(item)) {
                table.setItem(0, item);
            }
        });
    }


    static void dress(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (dress(contents[slot])) {
                inventory.setItem(slot, contents[slot]);
            }
        }
    }
}
