package de.raindancer.modules.enchants.service;

import de.raindancer.core.content.enchant.CustomEnchantments;
import de.raindancer.core.content.enchant.RainsEnchants;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.enchants.EnchantAdvancements;
import de.raindancer.modules.enchants.EnchantsSettings;
import de.raindancer.modules.enchants.rules.Autopilot;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Autopilot: switched on, it keeps a gliding player going at the cruise speed they picked (sneak and scroll), burning
 * Boost charges and then rockets from anywhere in their inventory, and holds the altitude they level off at. The
 * player steers throughout; F switches economy mode, a tap of sneak or {@code /autopilot} switches it off, and landing does
 * too. Runs on the player's own thread, once a tick from {@link #tick}.
 */
public final class AutopilotService {

    /** Where a flight of the old, cinematic Autopilot began: read once on join to hand back anybody caught in one. */
    private static final NamespacedKey LEGACY_FROM = new NamespacedKey("rainsenchants", "autopilot_from");
    private static final NamespacedKey CRUISE = new NamespacedKey("rainsenchants", "autopilot_cruise");
    private static final NamespacedKey ECONOMY = new NamespacedKey("rainsenchants", "autopilot_economy");
    private static final double TAKEOFF_UP = 1.0;
    private static final int TAKEOFF_TICKS = 10;
    /** Not gliding this long in mid-air (the elytra folded, say) ends it. */
    private static final int FOLDED_TICKS = 10;
    /** A move further than this in a tick is a teleport, not flying. */
    private static final double TELEPORT = 12;
    private static final int HUD_EVERY = 5;

    private final Plugin plugin;
    private final Messages messages;
    private final Supplier<EnchantsSettings> settings;
    private final EnchantAdvancements advancements;
    private final ElytraService elytra;
    private final Map<UUID, Flight> flights = new ConcurrentHashMap<>();

    private static final class Flight {
        int takeoff;
        int folded;
        int tick;
        int burnLeft;
        Double holdY;
        Location last;
        double distance;
        double economyDistance;
        int economyFuel;
        boolean warnedEmpty;
        double speed;
        double heldFor;
        /** Scrolled while sneak was held: letting go of sneak then is not a switch-off. */
        boolean sneakScrolled;
        double unCounted;
    }

    public AutopilotService(Plugin plugin, Messages messages, Supplier<EnchantsSettings> settings,
                            EnchantAdvancements advancements, ElytraService elytra) {
        this.plugin = plugin;
        this.messages = messages;
        this.settings = settings;
        this.advancements = advancements;
        this.elytra = elytra;
    }

    public boolean engaged(UUID player) {
        return flights.containsKey(player);
    }

    private static int level(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        return chest == null || chest.getType() != Material.ELYTRA ? 0
                : CustomEnchantments.levelOn(chest, RainsEnchants.AUTOPILOT);
    }

    private double top(Player player) {
        return Autopilot.topSpeed(level(player), settings.get().autopilotTopSpeedPercent());
    }

    /** The cruise speed the player picked, in blocks a second; the level's top speed until they pick one. */
    public double cruisePerSecond(Player player) {
        double top = Autopilot.blocksPerSecond(top(player));
        Double picked = player.getPersistentDataContainer().get(CRUISE, PersistentDataType.DOUBLE);
        return picked == null ? top : Math.clamp(picked, Math.min(Autopilot.SLOWEST, top), top);
    }

    public boolean economy(Player player) {
        return player.getPersistentDataContainer().getOrDefault(ECONOMY, PersistentDataType.BOOLEAN, false);
    }

    /** {@code /autopilot}: on, or off again. */
    public void toggle(Player player) {
        if (engaged(player.getUniqueId())) {
            disengage(player, "enchants.autopilot-disengaged");
        } else {
            engage(player);
        }
    }

    public void engage(Player player) {
        if (level(player) < 1) {
            messages.send(player, "enchants.autopilot-no-elytra");
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR || player.isFlying() || player.isInsideVehicle()
                || player.isInWater()) {
            messages.send(player, "enchants.autopilot-busy");
            return;
        }
        Flight flight = new Flight();
        if (!player.isGliding()) {
            if (player.isOnGround()) {
                player.setVelocity(new Vector(0, TAKEOFF_UP, 0));
                flight.takeoff = TAKEOFF_TICKS;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.8f);
            } else {
                player.setGliding(true);
            }
        }
        // Already holding sneak when switched on: that sneak's release is no switch-off.
        flight.sneakScrolled = player.isSneaking();
        flights.put(player.getUniqueId(), flight);
        messages.send(player, "enchants.autopilot-on", "speed", String.valueOf(Math.round(cruisePerSecond(player))),
                "mode", modeName(player));
        advancements.used(player.getUniqueId(), RainsEnchants.AUTOPILOT);
    }

    public void disengage(Player player, String why) {
        if (flights.remove(player.getUniqueId()) != null) {
            messages.send(player, why);
        }
    }

    /** {@code /autopilot economy}, or F in flight: economy mode on or off, kept for next time. */
    public void toggleEconomy(Player player) {
        boolean economy = !economy(player);
        player.getPersistentDataContainer().set(ECONOMY, PersistentDataType.BOOLEAN, economy);
        Flight flight = flights.get(player.getUniqueId());
        if (flight != null) {
            flight.economyDistance = 0;
            flight.economyFuel = 0;
            hud(player, flight);
        }
        messages.send(player, economy ? "enchants.autopilot-economy-on" : "enchants.autopilot-economy-off");
    }

    /** {@code /autopilot speed <blocks a second>}. */
    public void setCruise(Player player, double perSecond) {
        double top = Autopilot.blocksPerSecond(top(player));
        double picked = Math.clamp(perSecond, Math.min(Autopilot.SLOWEST, top), Math.max(top, Autopilot.SLOWEST));
        player.getPersistentDataContainer().set(CRUISE, PersistentDataType.DOUBLE, picked);
        messages.send(player, "enchants.autopilot-cruise", "speed", String.valueOf(Math.round(cruisePerSecond(player))));
    }

    /**
     * The scroll wheel in flight, with sneak held: the cruise speed. Without sneak the hotbar scrolls as always.
     *
     * @return true if it changed the cruise speed instead of the hotbar slot
     */
    public boolean scrolled(Player player, int from, int to) {
        Flight flight = flights.get(player.getUniqueId());
        int notches = Autopilot.notches(from, to);
        if (flight == null || notches == 0 || !player.isSneaking()) {
            return false;
        }
        flight.sneakScrolled = true;
        double top = Autopilot.blocksPerSecond(top(player));
        double picked = Math.clamp(cruisePerSecond(player) + notches * Autopilot.NOTCH,
                Math.min(Autopilot.SLOWEST, top), top);
        player.getPersistentDataContainer().set(CRUISE, PersistentDataType.DOUBLE, picked);
        hud(player, flight);
        return true;
    }

    /** Sneak pressed or let go in flight: a tap without scrolling switches Autopilot off. */
    public void sneaked(Player player, boolean down) {
        Flight flight = flights.get(player.getUniqueId());
        if (flight == null) {
            return;
        }
        if (down) {
            flight.sneakScrolled = false;
        } else if (!flight.sneakScrolled) {
            disengage(player, "enchants.autopilot-disengaged");
        }
    }

    /** F in flight. @return true if it switched economy mode instead of the hands */
    public boolean swapped(Player player) {
        if (!engaged(player.getUniqueId())) {
            return false;
        }
        toggleEconomy(player);
        return true;
    }

    public void tick(Player player) {
        Flight flight = flights.get(player.getUniqueId());
        if (flight == null) {
            return;
        }
        if (!player.isOnline() || player.isDead()) {
            flights.remove(player.getUniqueId());
            return;
        }
        if (level(player) < 1) {
            disengage(player, "enchants.autopilot-lost");
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR || player.isFlying() || player.isInsideVehicle()) {
            disengage(player, "enchants.autopilot-disengaged");
            return;
        }
        Location here = player.getLocation();
        flight.tick++;
        if (flight.takeoff > 0) {
            flight.takeoff--;
            if (flight.takeoff == 0) {
                player.setGliding(true);
            }
            flight.last = here;
            return;
        }
        if (!player.isGliding()) {
            if (player.isOnGround() || player.isInWater() || player.isInLava() || ++flight.folded > FOLDED_TICKS) {
                disengage(player, "enchants.autopilot-landed");
            }
            flight.last = here;
            return;
        }
        flight.folded = 0;
        double[] velocity = moved(flight, here);
        flight.last = here;
        if (velocity == null || elytra.boosting(player)) {
            return;
        }
        double speed = Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2]);
        flight.speed = speed;
        flight.distance += speed;
        boolean economy = economy(player);
        if (economy) {
            flight.economyDistance += speed;
        }
        double cruise = Autopilot.cruise(cruisePerSecond(player), top(player));
        Vector look = here.getDirection();
        double[] direction = {look.getX(), look.getY(), look.getZ()};
        Autopilot.Tick step = Autopilot.fly(velocity, direction, here.getPitch(), here.getY(), flight.holdY, cruise,
                economy, true);
        if (step.thrusting() && flight.burnLeft <= 0 && !refuel(player, flight, economy)) {
            step = Autopilot.fly(velocity, direction, here.getPitch(), here.getY(), flight.holdY, cruise, economy, false);
        }
        if (step.holdY() != null && step.holdY().equals(flight.holdY)) {
            flight.heldFor += speed;
        } else {
            flight.heldFor = 0;
        }
        flight.holdY = step.holdY();
        flight.unCounted += speed;
        if (flight.unCounted >= 1000) {
            flight.unCounted -= 1000;
            advancements.autopilotKilometre(player.getUniqueId());
        }
        if (step.thrusting()) {
            flight.burnLeft--;
            if (flight.tick % 2 == 0) {
                player.getWorld().spawnParticle(Particle.FIREWORK, here, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
        if (step.velocity() != null) {
            player.setVelocity(new Vector(step.velocity()[0], step.velocity()[1], step.velocity()[2]));
        }
        UUID id = player.getUniqueId();
        advancements.autopilotSpeed(id, Autopilot.blocksPerSecond(speed));
        if (flight.tick % 20 == 0) {
            advancements.autopilotDistance(id, flight.distance);
            advancements.autopilotLevel(id, flight.heldFor);
            if (economy) {
                advancements.autopilotEconomy(id, flight.economyDistance, flight.economyFuel);
            }
        }
        if (flight.tick % HUD_EVERY == 0) {
            hud(player, flight);
        }
    }

    /** How far the player moved since last tick, or null after a teleport or a change of world. */
    private static double[] moved(Flight flight, Location here) {
        if (flight.last == null || flight.last.getWorld() != here.getWorld()) {
            return null;
        }
        double[] velocity = {here.getX() - flight.last.getX(), here.getY() - flight.last.getY(),
                here.getZ() - flight.last.getZ()};
        double length = Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2]);
        return length > TELEPORT ? null : velocity;
    }

    /** Lights a Boost charge, or else a rocket from the inventory. @return false when there is nothing left to burn */
    private boolean refuel(Player player, Flight flight, boolean economy) {
        int charge = elytra.spendForAutopilot(player);
        if (charge > 0) {
            flight.burnLeft = charge;
        } else {
            PlayerInventory inventory = player.getInventory();
            OptionalInt slot = Autopilot.pick(rockets(inventory), economy);
            if (slot.isEmpty()) {
                if (!flight.warnedEmpty) {
                    flight.warnedEmpty = true;
                    messages.send(player, "enchants.autopilot-empty");
                    advancements.autopilotRanDry(player.getUniqueId());
                }
                return false;
            }
            ItemStack rocket = inventory.getItem(slot.getAsInt());
            if (rocket.getItemMeta() instanceof FireworkMeta meta && meta.hasEffects()) {
                advancements.autopilotStarRocket(player.getUniqueId());
            }
            flight.burnLeft = Autopilot.burnTicks(power(rocket));
            if (player.getGameMode() != GameMode.CREATIVE) {
                rocket.subtract();
                inventory.setItem(slot.getAsInt(), rocket.getAmount() > 0 ? rocket : null);
            }
            advancements.autopilotRocket(player.getUniqueId());
        }
        flight.warnedEmpty = false;
        if (economy) {
            flight.economyFuel++;
        }
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.1f);
        return true;
    }

    private static List<Autopilot.Rocket> rockets(PlayerInventory inventory) {
        List<Autopilot.Rocket> found = new ArrayList<>();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item != null && item.getType() == Material.FIREWORK_ROCKET && item.getAmount() > 0) {
                boolean stars = item.getItemMeta() instanceof FireworkMeta meta && meta.hasEffects();
                found.add(new Autopilot.Rocket(slot, power(item), stars));
            }
        }
        return found;
    }

    private static int power(ItemStack rocket) {
        return rocket.getItemMeta() instanceof FireworkMeta meta ? meta.getPower() : 1;
    }

    private static int countRockets(PlayerInventory inventory) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == Material.FIREWORK_ROCKET) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private String modeName(Player player) {
        return economy(player) ? "economy" : "cruise";
    }

    private void hud(Player player, Flight flight) {
        int[] charges = elytra.charges(player);
        String boost = charges[1] == 0 ? "" : " · Boost " + charges[0] + "/" + charges[1];
        player.sendActionBar(messages.get("enchants.autopilot-hud",
                "speed", String.valueOf(Math.round(Autopilot.blocksPerSecond(flight.speed))),
                "cruise", String.valueOf(Math.round(cruisePerSecond(player))),
                "mode", economy(player) ? "Eco" : "Cruise",
                "fuel", countRockets(player.getInventory()) + " rockets" + boost,
                "altitude", flight.holdY == null ? "—" : String.valueOf(Math.round(flight.holdY))));
    }

    public void quit(Player player) {
        flights.remove(player.getUniqueId());
    }

    /**
     * Somebody caught as a spectator in a flight of the old, cinematic Autopilot when the server stopped: back where
     * that flight began, in the game mode they had.
     */
    public void join(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        String from = data.get(LEGACY_FROM, PersistentDataType.STRING);
        if (from == null) {
            return;
        }
        data.remove(LEGACY_FROM);
        String[] part = from.split("\\|");
        GameMode mode = GameMode.SURVIVAL;
        try {
            mode = GameMode.valueOf(part[6]);
            World world = plugin.getServer().getWorld(part[0]);
            if (world != null) {
                player.teleport(new Location(world, Double.parseDouble(part[1]), Double.parseDouble(part[2]),
                        Double.parseDouble(part[3]), Float.parseFloat(part[4]), Float.parseFloat(part[5])));
            }
        } catch (RuntimeException unreadable) {
            // Left where they are, out of spectator mode at least.
        }
        player.setSpectatorTarget(null);
        player.setGameMode(mode);
        player.setFallDistance(0);
    }

    public void stopAll() {
        flights.clear();
    }
}
