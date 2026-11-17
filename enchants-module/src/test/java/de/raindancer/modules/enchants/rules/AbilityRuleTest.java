package de.raindancer.modules.enchants.rules;

import de.raindancer.core.social.economy.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbilityRuleTest {

    @Test
    @DisplayName("the effective level is the better of the gear's and the unlocked rank")
    void effective() {
        assertThat(AbilityRule.effective(0, 0)).isZero();
        assertThat(AbilityRule.effective(2, 0)).isEqualTo(2);
        assertThat(AbilityRule.effective(0, 3)).isEqualTo(3);
        assertThat(AbilityRule.effective(3, 1)).isEqualTo(3);
        assertThat(AbilityRule.effective(1, 2)).isEqualTo(2);
    }

    @Test
    @DisplayName("the shard list reads leniently and falls back to the default when nothing in it is usable")
    void parsing() {
        assertThat(AbilityRule.parseShards("4, 8, 16")).containsExactly(4, 8, 16);
        assertThat(AbilityRule.parseShards(" 3,x, 0 ,-2, 5 ")).containsExactly(3, 5);
        assertThat(AbilityRule.parseShards("")).containsExactly(4, 8, 16);
        assertThat(AbilityRule.parseShards(null)).containsExactly(4, 8, 16);
        assertThat(AbilityRule.parseShards("none")).containsExactly(4, 8, 16);
    }

    @Test
    @DisplayName("a rank takes its own entry of the list; past the last, each rank doubles the one before")
    void shardsPerRank() {
        List<Integer> list = List.of(4, 8, 16);
        assertThat(AbilityRule.shards(list, 1)).isEqualTo(4);
        assertThat(AbilityRule.shards(list, 3)).isEqualTo(16);
        assertThat(AbilityRule.shards(list, 4)).isEqualTo(32);
        assertThat(AbilityRule.shards(list, 5)).isEqualTo(64);
        assertThat(AbilityRule.shards(List.of(5), 3)).isEqualTo(20);
    }

    @Test
    @DisplayName("doubling never overflows")
    void noOverflow() {
        assertThat(AbilityRule.shards(List.of(4), 200)).isPositive();
    }

    @Test
    @DisplayName("the money is the written amount times the rank number")
    void money() {
        Money base = Money.of(2_000_000);
        assertThat(AbilityRule.money(base, 1)).isEqualTo(base);
        assertThat(AbilityRule.money(base, 3)).isEqualTo(Money.of(6_000_000));
    }
}
