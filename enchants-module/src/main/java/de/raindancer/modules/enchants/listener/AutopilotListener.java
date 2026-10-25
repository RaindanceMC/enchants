package de.raindancer.modules.enchants.listener;

import de.raindancer.modules.enchants.service.AutopilotService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/** Autopilot's controls in flight: the scroll wheel sets the cruise speed, F economy mode, sneaking switches it off. */
public final class AutopilotListener implements IEnchantsListener {

    private final AutopilotService autopilot;

    public AutopilotListener(AutopilotService autopilot) {
        this.autopilot = autopilot;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onScroll(PlayerItemHeldEvent event) {
        if (autopilot.scrolled(event.getPlayer(), event.getPreviousSlot(), event.getNewSlot())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (autopilot.swapped(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            autopilot.disengage(event.getPlayer(), "enchants.autopilot-disengaged");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        autopilot.join(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        autopilot.quit(event.getPlayer());
    }
}
