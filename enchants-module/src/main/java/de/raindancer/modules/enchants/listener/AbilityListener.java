package de.raindancer.modules.enchants.listener;

import de.raindancer.modules.enchants.service.AbilityService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/** Keeps what each online player spent on abilities where the skill pool can ask for it from any thread. */
public final class AbilityListener implements IEnchantsListener {

    private final AbilityService abilities;

    public AbilityListener(AbilityService abilities) {
        this.abilities = abilities;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        abilities.remember(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        forget(event.getPlayer().getUniqueId());
    }

    @Override
    public void forget(UUID player) {
        abilities.forget(player);
    }
}
