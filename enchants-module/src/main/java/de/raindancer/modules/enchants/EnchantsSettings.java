package de.raindancer.modules.enchants;

import de.raindancer.core.data.settings.Choices;
import de.raindancer.core.data.settings.Describe;
import de.raindancer.core.data.settings.In;
import de.raindancer.core.data.settings.Key;
import de.raindancer.core.data.settings.Range;
import de.raindancer.core.data.settings.Settings;
import de.raindancer.core.data.settings.Title;
import de.raindancer.core.data.settings.Topic;
import org.bukkit.Material;

/** What an owner decides about the enchantments. The schema of the file and of the /settings page. */
@Settings(id = "enchants", topics = {
        @Topic(path = "enchants", title = "Enchantments", icon = Material.ENCHANTED_BOOK),
        @Topic(path = "enchants/abilities", title = "Abilities tree", icon = Material.NETHER_STAR),
})
public record EnchantsSettings(

        @In("enchants") @Title("Flight seconds per level") @Range(min = 1, max = 3600)
        @Describe("How many seconds of flight each level of the Flight enchantment holds: level III at 30 "
                + "is a minute and a half in the air per charge.")
        @Key("flight-seconds-per-level")
        int flightSecondsPerLevel,

        @In("enchants") @Title("Seconds to recharge from empty") @Range(min = 1, max = 3600)
        @Describe("How long standing on the ground takes to refill an empty Flight charge. Flying, "
                + "or hanging in the air, does not recharge it.")
        @Key("flight-recharge-seconds")
        int flightRechargeSeconds,

        @In("enchants") @Title("Soft landing when flight ends") @Range(min = 0, max = 60)
        @Describe("Seconds of Slow Falling given when Flight runs out or is taken off in mid-air, so "
                + "running dry is not a death sentence. Zero gives none.")
        @Key("landing-grace-seconds")
        int landingGraceSeconds,

        @In("enchants") @Title("Double Jump power, percent") @Range(min = 10, max = 300)
        @Describe("How hard Double Jump pushes: 100 is a little over two blocks of extra height.")
        @Key("double-jump-power-percent")
        int doubleJumpPowerPercent,

        @In("enchants") @Title("Seconds for a Boost charge to come back") @Range(min = 5, max = 3600)
        @Describe("An elytra with Boost holds one charge per level; each spent one comes back after this long, "
                + "one at a time, flying or not.")
        @Key("boost-recharge-seconds")
        int boostRechargeSeconds,

        @In("enchants") @Title("Boost speed, percent of a rocket") @Range(min = 50, max = 1000)
        @Describe("How fast a Boost pulls: 100 is a firework rocket, 150 half as fast again. Far above 400 the "
                + "ground ahead may not be loaded in time, and an anti-cheat may take the speed for a cheat.")
        @Key("boost-speed-percent")
        int boostSpeedPercent,

        @In("enchants") @Title("Launch speed, metres per second") @Range(min = 20, max = 300)
        @Describe("How fast Launch throws the player forward once the elytra opens. The air slows a glider "
                + "down quickly, and the faster this is the more chunks the server has to load at once.")
        @Key("launch-speed")
        int launchSpeed,

        @In("enchants") @Title("Launch cooldown, seconds") @Range(min = 10, max = 86400)
        @Describe("How long Launch I waits between launches; level II waits two thirds of it, III not at all.")
        @Key("launch-cooldown-seconds")
        int launchCooldownSeconds,

        @In("enchants") @Title("Wisdom, extra experience per level, percent") @Range(min = 1, max = 500)
        @Describe("How much more experience mobs and blocks give per level of Wisdom: 50 makes Wisdom III "
                + "two and a half times as much.")
        @Key("wisdom-percent-per-level")
        int wisdomPercentPerLevel,

        @In("enchants") @Title("Autopilot III top speed, percent of a rocket") @Range(min = 100, max = 400)
        @Describe("How fast Autopilot keeps an elytra going at level III: 100 is a firework rocket's top speed "
                + "(about 33 blocks a second), 200 twice that. Levels I and II get a third and two thirds of the extra. "
                + "Far above 300 the land ahead may not load in time.")
        @Key("autopilot-top-speed-percent")
        int autopilotTopSpeedPercent,

        @In("enchants") @Title("Reinforced, wear saved per level, percent") @Range(min = 1, max = 30)
        @Describe("How likely each point of wear is skipped per level of Reinforced, on top of Unbreaking: 25 has "
                + "Reinforced III skip three in four. Never more than nine in ten, so nothing becomes unbreakable.")
        @Key("reinforced-percent-per-level")
        int reinforcedPercentPerLevel,

        @In("enchants") @Title("Switched-off enchantments")
        @Choices(value = EnchantsDefinitions.Keys.class, listedMeansOff = true)
        @Describe("A switched-off enchantment does nothing, is not sold and cannot be put on at an "
                + "anvil. Items keep it, so switching it back on brings it back as it was. The Fully Enchanted "
                + "advancement counts the ones on at the last start.")
        @Key("switched-off")
        java.util.List<String> switchedOff,

        @In("enchants/abilities") @Title("Abilities tree")
        @Describe("Double Jump, Swiftness and Night Vision can be unlocked in /enchants abilities (and /skills) "
                + "with skill points, instead of enchanting boots, leggings and a helmet. Needs the skills plugin. "
                + "Off stops buying new ranks; ranks already unlocked keep working.")
        @Key("abilities.enabled")
        boolean abilitiesEnabled) {

    public static final EnchantsSettings DEFAULTS = new EnchantsSettings(30, 30, 8, 100, 90, 150, 200, 600, 50,
            200, 25,
            java.util.List.of(de.raindancer.core.content.enchant.RainsEnchants.LAUNCH),
            true);
}
