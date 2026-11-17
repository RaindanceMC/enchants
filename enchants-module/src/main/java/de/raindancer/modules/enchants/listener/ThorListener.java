package de.raindancer.modules.enchants.listener;

import de.raindancer.modules.enchants.service.ThorService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Thor's throws and storms. */
public final class ThorListener implements IEnchantsListener {

    private final ThorService thor;

    public ThorListener(ThorService thor) {
        this.thor = thor;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onThrow(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR && event.getHand() == EquipmentSlot.HAND
                && thor.throwHammer(event.getPlayer(), event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHit(ProjectileHitEvent event) {
        Location at = event.getHitEntity() != null ? event.getHitEntity().getLocation()
                : event.getHitBlock() != null ? event.getHitBlock().getLocation().add(0.5, 1, 0.5)
                : event.getEntity().getLocation();
        thor.hit(event.getEntity(), event.getHitEntity(), at);
    }
}
