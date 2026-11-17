package de.raindancer.modules.enchants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Every default spelled out: a positional record with swapped ints compiles perfectly. */
class EnchantsSettingsTest {

    @Test
    @DisplayName("the defaults are the ones the docs promise")
    void defaults() {
        EnchantsSettings defaults = EnchantsSettings.DEFAULTS;

        assertThat(defaults.flightSecondsPerLevel()).isEqualTo(30);
        assertThat(defaults.flightRechargeSeconds()).isEqualTo(30);
        assertThat(defaults.landingGraceSeconds()).isEqualTo(8);
        assertThat(defaults.doubleJumpPowerPercent()).isEqualTo(100);
        assertThat(defaults.boostRechargeSeconds()).isEqualTo(90);
        assertThat(defaults.boostSpeedPercent()).isEqualTo(150);
        assertThat(defaults.launchSpeed()).isEqualTo(200);
        assertThat(defaults.launchCooldownSeconds()).isEqualTo(600);
        assertThat(defaults.wisdomPercentPerLevel()).isEqualTo(50);
        assertThat(defaults.autopilotTopSpeedPercent()).isEqualTo(200);
        assertThat(defaults.reinforcedPercentPerLevel()).isEqualTo(25);
        assertThat(defaults.abilitiesEnabled()).isTrue();
    }

    @Test
    @DisplayName("shards are gone: no setting is named after them or the shard world")
    void noShardSettings() {
        assertThat(EnchantsSettings.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .noneMatch(name -> name.toLowerCase().contains("shard") || name.equals("portalKey")
                        || name.equals("crystalWardenPerMille"))
                .doesNotContain("abilitiesMoneyPerRank");
    }

    @Test
    @DisplayName("a stored file that still has the removed shard and ability-price keys loads, and keeps the rest")
    void oldKeysAreTolerated(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws java.io.IOException {
        java.nio.file.Path file = dir.resolve("enchants.yml");
        java.nio.file.Files.writeString(file, """
                flight-seconds-per-level: 45
                shard-world-enabled: true
                shard-world: shardlands
                shard-world-radius: 2500
                shard-world-monsters: 140
                portal-key: AMETHYST_SHARD
                shard-drop-percent: 100
                shard-world-thunder-percent: 70
                shard-world-haze: 12
                crystal-warden-per-mille: 2
                abilities.enabled: false
                abilities.shards-per-rank: "4, 8, 16"
                abilities.money-per-rank: 20k
                """);
        var store = new de.raindancer.core.data.settings.SettingsStore<>(
                de.raindancer.core.data.settings.SettingsSchema.of(EnchantsSettings.class, EnchantsSettings.DEFAULTS), file);

        store.load();

        assertThat(store.problems()).isEmpty();
        assertThat(store.current().flightSecondsPerLevel()).isEqualTo(45);
        assertThat(store.current().abilitiesEnabled()).isFalse();
    }

    @Test
    @DisplayName("Boost may be set to five times its default: up to 1000 percent of a rocket")
    void boostGoesHigh() throws Exception {
        var component = java.util.Arrays.stream(EnchantsSettings.class.getRecordComponents())
                .filter(each -> each.getName().equals("boostSpeedPercent")).findFirst().orElseThrow();
        de.raindancer.core.data.settings.Range range = component.getAnnotation(de.raindancer.core.data.settings.Range.class);

        assertThat(range.max()).isGreaterThanOrEqualTo(1000);
    }

    @Test
    @DisplayName("Launch starts switched off; /settings shows a button for every enchantment, listed meaning off")
    void switchedOff() throws Exception {
        assertThat(EnchantsSettings.DEFAULTS.switchedOff()).containsExactly(de.raindancer.core.content.enchant.RainsEnchants.LAUNCH);

        var component = java.util.Arrays.stream(EnchantsSettings.class.getRecordComponents())
                .filter(each -> each.getName().equals("switchedOff")).findFirst().orElseThrow();
        de.raindancer.core.data.settings.Choices choices = component.getAnnotation(de.raindancer.core.data.settings.Choices.class);
        assertThat(choices.listedMeansOff()).isTrue();
        assertThat(choices.value().getDeclaredConstructor().newInstance().get())
                .containsExactlyElementsOf(EnchantsDefinitions.all().stream()
                        .map(de.raindancer.core.content.enchant.EnchantDef::key).toList());
    }
}
