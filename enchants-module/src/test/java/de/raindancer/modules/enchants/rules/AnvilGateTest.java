package de.raindancer.modules.enchants.rules;

import de.raindancer.core.content.enchant.RainsEnchants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AnvilGateTest {

    private static final Set<String> OFF = Set.of(RainsEnchants.LAUNCH);

    @Test
    @DisplayName("an anvil may not put on, or raise, a switched-off enchantment")
    void notPutOn() {
        assertThat(AnvilGate.blocks(Map.of(), Map.of(RainsEnchants.LAUNCH, 1), OFF::contains)).isTrue();
        assertThat(AnvilGate.blocks(Map.of(RainsEnchants.LAUNCH, 1), Map.of(RainsEnchants.LAUNCH, 2), OFF::contains)).isTrue();
    }

    @Test
    @DisplayName("an item that already has it can still be repaired or renamed, and others go on as ever")
    void otherwiseAsEver() {
        assertThat(AnvilGate.blocks(Map.of(RainsEnchants.LAUNCH, 3), Map.of(RainsEnchants.LAUNCH, 3), OFF::contains)).isFalse();
        assertThat(AnvilGate.blocks(Map.of(), Map.of(RainsEnchants.BOOST, 2), OFF::contains)).isFalse();
        assertThat(AnvilGate.blocks(Map.of(), Map.of(), OFF::contains)).isFalse();
    }
}
