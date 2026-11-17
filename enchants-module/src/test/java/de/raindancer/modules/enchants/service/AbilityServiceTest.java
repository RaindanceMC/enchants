package de.raindancer.modules.enchants.service;

import de.raindancer.core.content.enchant.RainsEnchants;
import de.raindancer.core.social.economy.Money;
import de.raindancer.core.ui.tree.SkillTree;
import de.raindancer.modules.enchants.EnchantsSettings;
import de.raindancer.modules.enchants.store.AbilityStore;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbilityServiceTest {

    /** A purse that keeps its books in plain fields: one currency unit is 100, no price changes. */
    private static final class Purse implements AbilityService.Purse {
        int shards;
        long money;
        boolean shardsVanish;
        boolean bankRefuses;
        int taken;
        long charged;
        long refunded;

        public int shards(Player player, String enchantment) {
            return shards;
        }

        public boolean takeShards(Player player, String enchantment, int amount) {
            if (shardsVanish || amount > shards) {
                return false;
            }
            shards -= amount;
            taken += amount;
            return true;
        }

        public Money quote(Money written) {
            return written;
        }

        public Money balance(Player player) {
            return Money.of(money);
        }

        public Optional<Money> charge(Player player, Money written) {
            if (bankRefuses || money < written.minor()) {
                return Optional.empty();
            }
            money -= written.minor();
            charged += written.minor();
            return Optional.of(written);
        }

        public void refund(Player player, Money taken) {
            money += taken.minor();
            refunded += taken.minor();
        }
    }

    private final Map<NamespacedKey, Object> data = new HashMap<>();
    private final Purse purse = new Purse();
    private Player player;
    private AbilityService service;
    private EnchantsSettings settings = EnchantsSettings.DEFAULTS;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.getOrDefault(any(NamespacedKey.class), any(PersistentDataType.class), any()))
                .thenAnswer(call -> data.getOrDefault(call.<NamespacedKey>getArgument(0), call.getArgument(2)));
        org.mockito.Mockito.doAnswer(call -> data.put(call.<NamespacedKey>getArgument(0), call.getArgument(2)))
                .when(pdc).set(any(NamespacedKey.class), any(PersistentDataType.class), any());
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getPersistentDataContainer()).thenReturn(pdc);
        service = new AbilityService(AbilityService.loadTree(), new AbilityStore(), () -> settings, purse);
    }

    private int rank(String node) {
        return service.ranks(player).getOrDefault(node, 0);
    }

    @Test
    @DisplayName("the shipped tree has the three abilities, with the enchantments' own highest levels as ranks")
    void tree() {
        SkillTree tree = AbilityService.loadTree();
        assertThat(tree.node("double_jump")).hasValueSatisfying(node -> assertThat(node.ranks()).isEqualTo(3));
        assertThat(tree.node("swiftness")).hasValueSatisfying(node -> assertThat(node.ranks()).isEqualTo(2));
        assertThat(tree.node("night_vision")).hasValueSatisfying(node -> assertThat(node.ranks()).isEqualTo(1));
    }

    @Test
    @DisplayName("rank 1 costs 4 shards of the ability's enchantment and 20k, and is kept")
    void firstRank() {
        purse.shards = 10;
        purse.money = 5_000_000;

        assertThat(service.learn(player, "double_jump")).isEmpty();

        assertThat(rank("double_jump")).isEqualTo(1);
        assertThat(purse.taken).isEqualTo(4);
        assertThat(purse.charged).isEqualTo(2_000_000);
        assertThat(service.unlocked(player, RainsEnchants.DOUBLE_JUMP)).isEqualTo(1);
        assertThat(service.unlocked(player, RainsEnchants.SWIFTNESS)).isZero();
    }

    @Test
    @DisplayName("the second rank costs 8 shards and twice the money, the third 16 and three times")
    void laterRanks() {
        purse.shards = 100;
        purse.money = 100_000_000;
        service.learn(player, "double_jump");
        purse.taken = 0;
        purse.charged = 0;

        service.learn(player, "double_jump");
        assertThat(purse.taken).isEqualTo(8);
        assertThat(purse.charged).isEqualTo(4_000_000);

        purse.taken = 0;
        purse.charged = 0;
        service.learn(player, "double_jump");
        assertThat(purse.taken).isEqualTo(16);
        assertThat(purse.charged).isEqualTo(6_000_000);
        assertThat(service.learn(player, "double_jump")).contains(SkillTree.Refusal.MAXED);
    }

    @Test
    @DisplayName("too few shards: refused, and not a coin taken")
    void tooFewShards() {
        purse.shards = 3;
        purse.money = 5_000_000;

        assertThat(service.learn(player, "double_jump")).contains(SkillTree.Refusal.NO_POINTS);
        assertThat(purse.charged).isZero();
        assertThat(purse.taken).isZero();
        assertThat(rank("double_jump")).isZero();
    }

    @Test
    @DisplayName("too little money: refused, and not a shard taken")
    void tooLittleMoney() {
        purse.shards = 50;
        purse.money = 1_999_999;

        assertThat(service.learn(player, "double_jump")).contains(SkillTree.Refusal.NO_POINTS);
        assertThat(purse.taken).isZero();
        assertThat(purse.shards).isEqualTo(50);
        assertThat(rank("double_jump")).isZero();
    }

    @Test
    @DisplayName("the bank refusing after the check leaves everything as it was")
    void bankRefuses() {
        purse.shards = 50;
        purse.money = 5_000_000;
        purse.bankRefuses = true;

        assertThat(service.learn(player, "double_jump")).contains(SkillTree.Refusal.NO_POINTS);
        assertThat(purse.shards).isEqualTo(50);
        assertThat(rank("double_jump")).isZero();
    }

    @Test
    @DisplayName("shards gone after the money was taken: the money comes back and no rank is given")
    void shardsVanish() {
        purse.shards = 50;
        purse.money = 5_000_000;
        purse.shardsVanish = true;

        assertThat(service.learn(player, "double_jump")).contains(SkillTree.Refusal.NO_POINTS);
        assertThat(purse.money).isEqualTo(5_000_000);
        assertThat(purse.refunded).isEqualTo(2_000_000);
        assertThat(rank("double_jump")).isZero();
    }

    @Test
    @DisplayName("with abilities switched off nothing is bought, but what is unlocked keeps working")
    void switchedOff() {
        purse.shards = 50;
        purse.money = 5_000_000;
        service.learn(player, "night_vision");
        settings = withEnabled(false);
        purse.shards = 50;

        assertThat(service.learn(player, "swiftness")).contains(SkillTree.Refusal.NO_POINTS);
        assertThat(rank("swiftness")).isZero();
        assertThat(service.unlocked(player, RainsEnchants.NIGHT_VISION)).isEqualTo(1);
    }

    @Test
    @DisplayName("no money in the setting means shards only")
    void shardsOnly() {
        settings = withMoney("0");
        purse.shards = 4;

        assertThat(service.learn(player, "double_jump")).isEmpty();
        assertThat(purse.charged).isZero();
        assertThat(rank("double_jump")).isEqualTo(1);
    }

    @Test
    @DisplayName("the effective level is the better of the gear and the unlock")
    void effective() {
        purse.shards = 100;
        purse.money = 100_000_000;
        service.learn(player, "swiftness");
        service.learn(player, "swiftness");

        assertThat(service.effective(player, null, RainsEnchants.SWIFTNESS)).isEqualTo(2);
        assertThat(service.effective(player, null, RainsEnchants.DOUBLE_JUMP)).isZero();
    }

    @Test
    @DisplayName("what is missing is said as shards and money")
    void shortfall() {
        purse.shards = 1;
        purse.money = 500;

        AbilityService.Shortfall missing = service.shortfall(player, "double_jump", 1);
        assertThat(missing.shards()).isEqualTo(3);
        assertThat(missing.money()).isEqualTo(Money.of(2_000_000 - 500));
    }

    private static EnchantsSettings withEnabled(boolean on) {
        return copy(on, EnchantsSettings.DEFAULTS.abilitiesMoneyPerRank());
    }

    private static EnchantsSettings withMoney(String written) {
        return copy(true, written);
    }

    private static EnchantsSettings copy(boolean on, String money) {
        EnchantsSettings d = EnchantsSettings.DEFAULTS;
        return new EnchantsSettings(d.flightSecondsPerLevel(), d.flightRechargeSeconds(), d.landingGraceSeconds(),
                d.doubleJumpPowerPercent(), d.boostRechargeSeconds(), d.boostSpeedPercent(), d.launchSpeed(),
                d.launchCooldownSeconds(), d.wisdomPercentPerLevel(), d.shardWorldEnabled(), d.shardWorld(),
                d.shardWorldRadius(), d.shardWorldMonsters(), d.portalKey(), d.shardDropPercent(),
                d.shardWorldThunderPercent(), d.shardWorldHaze(), d.crystalWardenPerMille(),
                d.autopilotTopSpeedPercent(), d.reinforcedPercentPerLevel(), d.switchedOff(), on,
                d.abilitiesShardsPerRank(), money);
    }
}
