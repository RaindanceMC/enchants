package de.raindancer.modules.enchants.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeaponLooksTest {

    @Test
    @DisplayName("an enchantment's look goes over the vanilla model, our own looks and a cosmetic weapon skin")
    void replaces() {
        assertThat(WeaponLooks.mayReplace(null, "minecraft:mace")).isTrue();
        assertThat(WeaponLooks.mayReplace("minecraft:mace", "minecraft:mace")).isTrue();
        assertThat(WeaponLooks.mayReplace("rainsenchants:mjolnir", "minecraft:mace")).isTrue();
        assertThat(WeaponLooks.mayReplace("rainsskins:real-mace/mace", "minecraft:mace"))
                .as("a skin says less about the weapon than its enchantment").isTrue();
    }

    @Test
    @DisplayName("another plugin's own model stays")
    void leavesOthers() {
        assertThat(WeaponLooks.mayReplace("someplugin:special_hammer", "minecraft:mace")).isFalse();
    }
}
