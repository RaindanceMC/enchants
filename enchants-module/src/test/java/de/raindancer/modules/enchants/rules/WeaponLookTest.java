package de.raindancer.modules.enchants.rules;

import de.raindancer.core.content.enchant.RainsEnchants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WeaponLookTest {

    @Test
    @DisplayName("a mace with Thor is Mjölnir, whatever else is on it")
    void mjolnir() {
        assertThat(WeaponLook.of("mace", Set.of(RainsEnchants.THOR))).get().extracting(WeaponLook::model).isEqualTo("rainsenchants:mjolnir");
        assertThat(WeaponLook.of("mace", Set.of(RainsEnchants.THOR, RainsEnchants.VOLLEY))).isPresent();
    }

    @Test
    @DisplayName("a mace without Thor, and Thor on an axe or a trident, keep vanilla's look")
    void nothingToShow() {
        assertThat(WeaponLook.of("mace", Set.of())).isEmpty();
        assertThat(WeaponLook.of("mace", Set.of(RainsEnchants.VOLLEY))).isEmpty();
        assertThat(WeaponLook.of("netherite_axe", Set.of(RainsEnchants.THOR))).isEmpty();
        assertThat(WeaponLook.of("trident", Set.of(RainsEnchants.THOR))).isEmpty();
    }

    @Test
    @DisplayName("only its own models are ever taken off again")
    void ours() {
        assertThat(WeaponLook.isOurs("rainsenchants:mjolnir")).isTrue();
        assertThat(WeaponLook.isOurs("rainsenchants:look/iron_helmet")).isFalse();
        assertThat(WeaponLook.isOurs("someplugin:mjolnir")).isFalse();
    }
}
