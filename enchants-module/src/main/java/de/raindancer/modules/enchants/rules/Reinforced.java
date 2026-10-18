package de.raindancer.modules.enchants.rules;

import java.util.random.RandomGenerator;

/**
 * Reinforced: each point of wear an item would still take — after Unbreaking had its say — is skipped by
 * chance, so the two multiply instead of one replacing the other.
 */
public final class Reinforced {

    /** Gear never becomes unbreakable, whatever an owner sets. */
    static final double MOST_SAVED = 0.9;

    private Reinforced() {
    }

    public static double savedChance(int level, int percentPerLevel) {
        if (level <= 0 || percentPerLevel <= 0) {
            return 0;
        }
        return Math.min(MOST_SAVED, level * percentPerLevel / 100.0);
    }

    /** The wear left of {@code damage} points. */
    public static int damageAfter(int damage, int level, int percentPerLevel, RandomGenerator random) {
        double chance = savedChance(level, percentPerLevel);
        if (damage <= 0 || chance <= 0) {
            return Math.max(0, damage);
        }
        int left = 0;
        for (int point = 0; point < damage; point++) {
            if (random.nextDouble() >= chance) {
                left++;
            }
        }
        return left;
    }
}
