package de.raindancer.modules.enchants.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class ReinforcedTest {

    private static RandomGenerator always(double value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public double nextDouble() {
                return value;
            }
        };
    }

    @Test
    @DisplayName("without the enchantment, or with nothing to wear, the damage is what it was")
    void untouched() {
        assertThat(Reinforced.damageAfter(3, 0, 25, always(0))).isEqualTo(3);
        assertThat(Reinforced.damageAfter(0, 3, 25, always(0))).isZero();
    }

    @Test
    @DisplayName("each point of wear is saved by chance, the level's percent each: lucky saves all, unlucky none")
    void eachPoint() {
        assertThat(Reinforced.damageAfter(5, 3, 25, always(0.0))).isZero();
        assertThat(Reinforced.damageAfter(5, 3, 25, always(0.99))).isEqualTo(5);
        assertThat(Reinforced.damageAfter(1, 1, 25, always(0.24))).as("under 25 % is saved").isZero();
        assertThat(Reinforced.damageAfter(1, 1, 25, always(0.25))).isEqualTo(1);
    }

    @Test
    @DisplayName("on average level III saves three quarters of the wear left after Unbreaking")
    void onAverage() {
        Random random = new Random(3);
        int taken = 0;
        for (int i = 0; i < 4000; i++) {
            taken += Reinforced.damageAfter(1, 3, 25, random);
        }
        assertThat(taken).isBetween(900, 1100);
    }

    @Test
    @DisplayName("never unbreakable: whatever the setting, at least a tenth of the wear stays")
    void neverUnbreakable() {
        assertThat(Reinforced.savedChance(3, 100)).isEqualTo(0.9);
        assertThat(Reinforced.savedChance(9, 25)).isEqualTo(0.9);
        assertThat(Reinforced.savedChance(2, 25)).isEqualTo(0.5);
        assertThat(Reinforced.savedChance(-1, 25)).isZero();
    }
}
