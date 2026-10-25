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
        assertThat(defaults.shardWorldEnabled()).isTrue();
        assertThat(defaults.shardWorld()).isEqualTo("shardlands");
        assertThat(defaults.shardWorldRadius()).isEqualTo(2500);
        assertThat(defaults.shardWorldMonsters()).isEqualTo(140);
        assertThat(defaults.portalKey()).isEqualTo(org.bukkit.Material.AMETHYST_SHARD);
        assertThat(defaults.shardDropPercent()).isEqualTo(100);
        assertThat(defaults.crystalWardenPerMille()).isEqualTo(2);
        assertThat(defaults.autopilotTopSpeedPercent()).isEqualTo(200);
        assertThat(defaults.reinforcedPercentPerLevel()).isEqualTo(25);
    }

    @Test
    @DisplayName("no hourly shard limit: neither a setting nor a cap behind it")
    void noShardCap() {
        assertThat(EnchantsSettings.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName).doesNotContain("shardsPerHour");
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
