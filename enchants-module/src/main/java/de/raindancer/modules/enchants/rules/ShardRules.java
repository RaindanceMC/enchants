package de.raindancer.modules.enchants.rules;

import de.raindancer.core.content.enchant.RainsEnchants;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Which enchantment's shard a monster drops: the useful-every-day ones often, the strong ones rarely, and the
 * legendary ones only from a warden or a wither.
 */
public final class ShardRules {

    private static final int COMMON = 10;
    private static final int UNCOMMON = 5;
    private static final int RARE = 2;
    private static final int LEGENDARY = 1;

    private static final Map<String, Integer> WEIGHTS = new LinkedHashMap<>();

    static {
        for (String key : new String[]{RainsEnchants.NIGHT_VISION, RainsEnchants.DOUBLE_JUMP, RainsEnchants.SWIFTNESS,
                RainsEnchants.HASTE, RainsEnchants.WISDOM, RainsEnchants.HARVESTER, RainsEnchants.SMELTING,
                RainsEnchants.TELEKINESIS, RainsEnchants.XP_SIPHON}) {
            WEIGHTS.put(key, COMMON);
        }
        for (String key : new String[]{RainsEnchants.VEINMINER, RainsEnchants.EXCAVATOR, RainsEnchants.CHEST_LINK,
                RainsEnchants.AUTO_SELL, RainsEnchants.BOOST, RainsEnchants.VOLLEY, RainsEnchants.BLEED,
                RainsEnchants.VENOM, RainsEnchants.CLEAVE, RainsEnchants.ARMOR_PIERCE}) {
            WEIGHTS.put(key, UNCOMMON);
        }
        for (String key : new String[]{RainsEnchants.FLIGHT, RainsEnchants.LAUNCH, RainsEnchants.SOULBOUND,
                RainsEnchants.HEAT_SEEKING,
                RainsEnchants.LIFESTEAL, RainsEnchants.EXECUTIONER, RainsEnchants.BERSERK, RainsEnchants.OVERPOWER,
                RainsEnchants.UNDYING, RainsEnchants.FUSION, RainsEnchants.AUTOPILOT}) {
            WEIGHTS.put(key, RARE);
        }
        for (String key : new String[]{RainsEnchants.THOR, RainsEnchants.REINFORCED}) {
            WEIGHTS.put(key, LEGENDARY);
        }
    }

    private ShardRules() {
    }

    public static int weight(String enchantment) {
        return WEIGHTS.getOrDefault(enchantment, 0);
    }

    public static boolean isLegendary(String enchantment) {
        return weight(enchantment) == LEGENDARY;
    }

    /** A pick from an ordinary monster, which never drops a legendary shard. */
    public static String pick(RandomGenerator random, int rareFactor) {
        return pick(random, rareFactor, false);
    }

    /**
     * @param rareFactor how much likelier the rare and legendary shards are — the monster's {@link ShardTier#rareFactor()}
     * @param legendary  whether this one may drop legendary shards: a warden or a wither
     */
    public static String pick(RandomGenerator random, int rareFactor, boolean legendary) {
        return pick(random, rareFactor, legendary, java.util.Set.of());
    }

    /** @return null when everything that could drop is switched off */
    public static String pick(RandomGenerator random, int rareFactor, boolean legendary,
                              java.util.Collection<String> switchedOff) {
        int total = 0;
        for (Map.Entry<String, Integer> each : WEIGHTS.entrySet()) {
            total += switchedOff.contains(each.getKey()) ? 0 : adjusted(each.getValue(), rareFactor, legendary);
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (Map.Entry<String, Integer> each : WEIGHTS.entrySet()) {
            if (switchedOff.contains(each.getKey())) {
                continue;
            }
            roll -= adjusted(each.getValue(), rareFactor, legendary);
            if (roll < 0) {
                return each.getKey();
            }
        }
        throw new IllegalStateException("The weights do not add up.");
    }

    private static int adjusted(int weight, int rareFactor, boolean legendary) {
        if (weight == LEGENDARY) {
            return legendary ? weight * Math.max(1, rareFactor) : 0;
        }
        return weight == RARE ? weight * Math.max(1, rareFactor) : weight;
    }
}
