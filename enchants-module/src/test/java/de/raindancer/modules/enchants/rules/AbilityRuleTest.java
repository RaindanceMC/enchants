package de.raindancer.modules.enchants.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
