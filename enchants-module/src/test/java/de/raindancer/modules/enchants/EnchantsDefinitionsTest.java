package de.raindancer.modules.enchants;

import de.raindancer.core.content.enchant.EnchantDef;
import de.raindancer.core.content.enchant.Gear;
import de.raindancer.core.content.enchant.RainsEnchants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnchantsDefinitionsTest {

    private static EnchantDef def(String key) {
        return EnchantsDefinitions.all().stream().filter(each -> each.key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("the enchantments are exactly the keys Core publishes for others to ask about")
    void keysMatchCore() {
        assertThat(EnchantsDefinitions.all()).extracting(EnchantDef::key).containsExactlyInAnyOrder(
                RainsEnchants.VEINMINER, RainsEnchants.NIGHT_VISION, RainsEnchants.FLIGHT, RainsEnchants.DOUBLE_JUMP,
                RainsEnchants.AUTO_SELL, RainsEnchants.BOOST, RainsEnchants.LAUNCH, RainsEnchants.SWIFTNESS,
                RainsEnchants.HASTE, RainsEnchants.SOULBOUND, RainsEnchants.WISDOM, RainsEnchants.EXCAVATOR,
                RainsEnchants.HARVESTER, RainsEnchants.SMELTING, RainsEnchants.TELEKINESIS, RainsEnchants.CHEST_LINK,
                RainsEnchants.HEAT_SEEKING, RainsEnchants.THOR, RainsEnchants.VOLLEY, RainsEnchants.LIFESTEAL,
                RainsEnchants.EXECUTIONER, RainsEnchants.BLEED, RainsEnchants.VENOM, RainsEnchants.CLEAVE,
                RainsEnchants.ARMOR_PIERCE, RainsEnchants.BERSERK, RainsEnchants.OVERPOWER, RainsEnchants.UNDYING,
                RainsEnchants.FUSION, RainsEnchants.AUTOPILOT, RainsEnchants.REINFORCED, RainsEnchants.XP_SIPHON);
        assertThat(def(RainsEnchants.CLEAVE).gear()).isEqualTo(Gear.AXES);
        assertThat(def(RainsEnchants.AUTOPILOT).maxLevel()).isEqualTo(3);
        assertThat(def(RainsEnchants.OVERPOWER).maxLevel()).isEqualTo(5);
        assertThat(def(RainsEnchants.UNDYING).gear()).isEqualTo(Gear.CHESTPLATE);
        assertThat(def(RainsEnchants.VOLLEY).gear()).as("tridents, and Thor's hammers").isEqualTo(Gear.THUNDER);
        assertThat(def(RainsEnchants.THOR).gear()).isEqualTo(Gear.THUNDER);
        assertThat(def(RainsEnchants.HEAT_SEEKING).gear()).isEqualTo(Gear.SEEKING);
        assertThat(def(RainsEnchants.HEAT_SEEKING).maxLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("the new ones go where they work, and the pairs that fight each other are kept apart")
    void newOnes() {
        assertThat(def(RainsEnchants.BOOST).gear()).isEqualTo(Gear.ELYTRA);
        assertThat(def(RainsEnchants.LAUNCH).gear()).isEqualTo(Gear.ELYTRA);
        assertThat(def(RainsEnchants.SWIFTNESS).gear()).isEqualTo(Gear.LEGGINGS);
        assertThat(def(RainsEnchants.HARVESTER).gear()).isEqualTo(Gear.HOE);
        assertThat(def(RainsEnchants.EXCAVATOR).gear()).isEqualTo(Gear.EXCAVATING_TOOLS);
        assertThat(def(RainsEnchants.SOULBOUND).gear()).isEqualTo(Gear.ANY_GEAR);
        assertThat(def(RainsEnchants.AUTO_SELL).gear()).isEqualTo(Gear.HAND_TOOLS);

        assertThat(def(RainsEnchants.EXCAVATOR).exclusiveWith()).contains(RainsEnchants.VEINMINER);
        assertThat(def(RainsEnchants.SMELTING).exclusiveWith()).contains("minecraft:silk_touch");
        assertThat(def(RainsEnchants.TELEKINESIS).exclusiveWith()).contains(RainsEnchants.CHEST_LINK);
    }

    @Test
    @DisplayName("each goes on the gear it is about, and only Veinminer and Night Vision stop at one level")
    void gearAndLevels() {
        assertThat(def(RainsEnchants.VEINMINER).gear()).isEqualTo(Gear.VEIN_TOOLS);
        assertThat(def(RainsEnchants.NIGHT_VISION).gear()).isEqualTo(Gear.HELMET);
        assertThat(def(RainsEnchants.FLIGHT).gear()).isEqualTo(Gear.CHESTPLATE);
        assertThat(def(RainsEnchants.DOUBLE_JUMP).gear()).isEqualTo(Gear.BOOTS);

        assertThat(def(RainsEnchants.VEINMINER).maxLevel()).isEqualTo(1);
        assertThat(def(RainsEnchants.NIGHT_VISION).maxLevel()).isEqualTo(1);
        assertThat(def(RainsEnchants.FLIGHT).maxLevel()).isGreaterThan(1);
        assertThat(def(RainsEnchants.DOUBLE_JUMP).maxLevel()).isGreaterThan(1);
    }

    @Test
    @DisplayName("Reinforced goes on any gear and sits beside Unbreaking; XP Siphon goes on hand tools beside Telekinesis")
    void durabilityAndExperience() {
        EnchantDef reinforced = def(RainsEnchants.REINFORCED);
        assertThat(reinforced.gear()).isEqualTo(Gear.ANY_GEAR);
        assertThat(reinforced.maxLevel()).isEqualTo(3);
        assertThat(reinforced.exclusiveWith()).as("stacks with Unbreaking and Mending").isEmpty();

        EnchantDef siphon = def(RainsEnchants.XP_SIPHON);
        assertThat(siphon.gear()).isEqualTo(Gear.HAND_TOOLS);
        assertThat(siphon.maxLevel()).isEqualTo(1);
        assertThat(siphon.exclusiveWith()).as("an enchantment of its own, not a Telekinesis variant").isEmpty();
        assertThat(def(RainsEnchants.TELEKINESIS).exclusiveWith()).doesNotContain(RainsEnchants.XP_SIPHON);
    }

    @Test
    @DisplayName("each goes only on its own gear: a chestplate enchantment never fits a pickaxe, a sword one never fits boots")
    void onlyItsGear() {
        assertThat(def(RainsEnchants.LIFESTEAL).gear().matches("diamond_pickaxe")).isFalse();
        assertThat(def(RainsEnchants.FLIGHT).gear().matches("diamond_sword")).isFalse();
        assertThat(def(RainsEnchants.DOUBLE_JUMP).gear().matches("diamond_helmet")).isFalse();
        assertThat(def(RainsEnchants.XP_SIPHON).gear().matches("diamond_chestplate")).isFalse();
        assertThat(def(RainsEnchants.XP_SIPHON).gear().matches("netherite_sword")).isTrue();
        assertThat(def(RainsEnchants.REINFORCED).gear().matches("elytra")).isTrue();
        assertThat(def(RainsEnchants.REINFORCED).gear().matches("diamond")).isFalse();
    }
}
