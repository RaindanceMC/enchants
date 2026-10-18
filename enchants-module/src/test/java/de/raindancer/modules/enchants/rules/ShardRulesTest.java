package de.raindancer.modules.enchants.rules;

import de.raindancer.core.content.enchant.RainsEnchants;
import de.raindancer.modules.enchants.EnchantsDefinitions;
import de.raindancer.core.content.enchant.EnchantDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ShardRulesTest {

    @Test
    @DisplayName("every enchantment can drop, the strong ones rarely — and a stronger mob makes them likelier")
    void weights() {
        for (EnchantDef def : EnchantsDefinitions.all()) {
            assertThat(ShardRules.weight(def.key())).as(def.key()).isPositive();
        }
        assertThat(ShardRules.weight(RainsEnchants.SOULBOUND)).isLessThan(ShardRules.weight(RainsEnchants.HASTE));

        Map<String, Integer> weak = new HashMap<>();
        Map<String, Integer> strong = new HashMap<>();
        Random random = new Random(3);
        for (int i = 0; i < 20_000; i++) {
            weak.merge(ShardRules.pick(random, 1), 1, Integer::sum);
            strong.merge(ShardRules.pick(random, 3), 1, Integer::sum);
        }
        assertThat(weak).containsOnlyKeys(EnchantsDefinitions.all().stream().map(EnchantDef::key)
                .filter(key -> !ShardRules.isLegendary(key)).toList());
        assertThat(weak.get(RainsEnchants.HASTE)).isGreaterThan(weak.get(RainsEnchants.LAUNCH) * 3);
        assertThat(strong.get(RainsEnchants.LAUNCH)).isGreaterThan(weak.get(RainsEnchants.LAUNCH) * 2);
    }

    @Test
    @DisplayName("legendary shards — Reinforced and Thor — come only from a warden or a wither, and rarer than rare ones")
    void legendary() {
        assertThat(ShardRules.isLegendary(RainsEnchants.REINFORCED)).isTrue();
        assertThat(ShardRules.isLegendary(RainsEnchants.THOR)).isTrue();
        assertThat(ShardRules.isLegendary(RainsEnchants.LAUNCH)).isFalse();
        assertThat(ShardRules.weight(RainsEnchants.REINFORCED)).isLessThan(ShardRules.weight(RainsEnchants.LAUNCH));

        Random random = new Random(5);
        Map<String, Integer> ordinary = new HashMap<>();
        Map<String, Integer> boss = new HashMap<>();
        for (int i = 0; i < 20_000; i++) {
            ordinary.merge(ShardRules.pick(random, 8, false), 1, Integer::sum);
            boss.merge(ShardRules.pick(random, 8, true), 1, Integer::sum);
        }
        assertThat(ordinary).doesNotContainKeys(RainsEnchants.REINFORCED, RainsEnchants.THOR);
        assertThat(boss.get(RainsEnchants.THOR)).isPositive();
        assertThat(boss.get(RainsEnchants.REINFORCED)).isPositive().isLessThan(boss.get(RainsEnchants.LAUNCH));
    }

    @Test
    @DisplayName("a switched-off enchantment drops no shards; with everything off there is nothing to drop")
    void switchedOff() {
        java.util.Set<String> off = java.util.Set.of(RainsEnchants.LAUNCH, RainsEnchants.THOR);
        Random random = new Random(9);
        for (int i = 0; i < 5_000; i++) {
            assertThat(ShardRules.pick(random, 8, true, off)).isNotIn(off).isNotNull();
        }
        java.util.Set<String> allOff = new java.util.HashSet<>(EnchantsDefinitions.all().stream().map(EnchantDef::key).toList());
        assertThat(ShardRules.pick(random, 8, true, allOff)).isNull();
    }
}
