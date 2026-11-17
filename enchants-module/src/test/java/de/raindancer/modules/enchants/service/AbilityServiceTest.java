package de.raindancer.modules.enchants.service;

import de.raindancer.core.content.enchant.RainsEnchants;
import de.raindancer.core.ui.tree.SkillTree;
import de.raindancer.core.ui.tree.TreeNode;
import de.raindancer.modules.enchants.EnchantsSettings;
import de.raindancer.modules.enchants.store.AbilityStore;
import de.raindancer.core.ui.tree.SkillPoints;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
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

    /** The adventurer's pool, kept in two fields: a level and the points the skills' own trees have taken. */
    private static final class Pool implements SkillPoints.Pool {
        int level = 30;
        int total = 30;
        int spentBySkills;

        public int level(UUID player) {
            return level;
        }

        public int total(UUID player) {
            return total;
        }

        public int spent(UUID player) {
            return spentBySkills;
        }
    }

    private final Map<NamespacedKey, Object> data = new HashMap<>();
    private final Pool pool = new Pool();
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
        SkillPoints.provide(null, pool);
        service = new AbilityService(AbilityService.loadTree(), new AbilityStore(), () -> settings,
                AbilityService.livePoints());
    }

    @AfterEach
    void clear() {
        SkillPoints.clear();
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
    @DisplayName("Double Jump costs 3 points a rank, and needs Adventurer level 10, 15 and 20")
    void costsAndLevels() {
        SkillTree tree = AbilityService.loadTree();
        TreeNode jump = tree.node("double_jump").orElseThrow();
        assertThat(jump.cost()).isEqualTo(3);
        assertThat(jump.levelFor(1)).isEqualTo(10);
        assertThat(jump.levelFor(2)).isEqualTo(15);
        assertThat(jump.levelFor(3)).isEqualTo(20);
        assertThat(tree.node("swiftness").orElseThrow().cost()).isEqualTo(2);
        assertThat(tree.node("night_vision").orElseThrow().cost()).isEqualTo(2);
    }

    @Test
    @DisplayName("a rank is paid from the shared pool and kept")
    void firstRank() {
        assertThat(service.learn(player, "double_jump")).isEmpty();

        assertThat(rank("double_jump")).isEqualTo(1);
        assertThat(service.spent(player)).isEqualTo(3);
        assertThat(SkillPoints.spentElsewhere(player.getUniqueId())).as("not registered here yet").isZero();
        assertThat(service.unlocked(player, RainsEnchants.DOUBLE_JUMP)).isEqualTo(1);
        assertThat(service.unlocked(player, RainsEnchants.SWIFTNESS)).isZero();
    }

    @Test
    @DisplayName("registered as a spender, the ranks come off the points skills shows as left")
    void spenderCounts() {
        SkillPoints.spender(null, "enchants", id -> service.spent(player));
        service.learn(player, "double_jump");
        service.learn(player, "swiftness");

        assertThat(SkillPoints.left(player.getUniqueId())).isEqualTo(30 - 3 - 2);
        pool.spentBySkills = 20;
        assertThat(SkillPoints.left(player.getUniqueId())).isEqualTo(5);
    }

    @Test
    @DisplayName("later ranks wait for their level; Double Jump II is out of reach at level 12")
    void laterRanks() {
        pool.level = 12;
        assertThat(service.learn(player, "double_jump")).isEmpty();
        assertThat(service.learn(player, "double_jump")).contains(SkillTree.Refusal.LEVEL);
        pool.level = 20;
        assertThat(service.learn(player, "double_jump")).isEmpty();
        assertThat(service.learn(player, "double_jump")).isEmpty();
        assertThat(service.learn(player, "double_jump")).contains(SkillTree.Refusal.MAXED);
        assertThat(service.spent(player)).isEqualTo(9);
    }

    @Test
    @DisplayName("too few points left: refused, and nothing changes")
    void tooFewPoints() {
        SkillPoints.spender(null, "enchants", id -> service.spent(player));
        pool.spentBySkills = 28;

        assertThat(service.learn(player, "double_jump")).contains(SkillTree.Refusal.NO_POINTS);
        assertThat(rank("double_jump")).isZero();
        assertThat(service.learn(player, "swiftness")).isEmpty();
        assertThat(service.learn(player, "night_vision")).contains(SkillTree.Refusal.NO_POINTS);
    }

    @Test
    @DisplayName("a point spent in abilities cannot be spent again in the skills' trees")
    void notTwice() {
        SkillPoints.spender(null, "enchants", id -> service.spent(player));
        pool.total = 5;
        service.learn(player, "double_jump");

        assertThat(SkillPoints.left(player.getUniqueId())).isEqualTo(2);
        assertThat(service.learn(player, "double_jump")).isPresent();
        assertThat(service.learn(player, "swiftness")).isEmpty();
        assertThat(SkillPoints.left(player.getUniqueId())).isZero();
    }

    @Test
    @DisplayName("without the skills plugin there are no points: nothing is bought, and the reason is a refusal")
    void noSkillsPlugin() {
        SkillPoints.clear();

        assertThat(service.pointsAvailable()).isFalse();
        assertThat(service.learn(player, "night_vision")).contains(SkillTree.Refusal.NO_POINTS);
        assertThat(rank("night_vision")).isZero();
    }

    @Test
    @DisplayName("with abilities switched off nothing is bought, but what is unlocked keeps working")
    void switchedOff() {
        service.learn(player, "night_vision");
        settings = withEnabled(false);

        assertThat(service.learn(player, "swiftness")).contains(SkillTree.Refusal.NO_POINTS);
        assertThat(rank("swiftness")).isZero();
        assertThat(service.unlocked(player, RainsEnchants.NIGHT_VISION)).isEqualTo(1);
    }

    @Test
    @DisplayName("the effective level is the better of the gear and the unlock")
    void effective() {
        service.learn(player, "swiftness");
        pool.level = 30;
        service.learn(player, "swiftness");

        assertThat(service.effective(player, null, RainsEnchants.SWIFTNESS)).isEqualTo(2);
        assertThat(service.effective(player, null, RainsEnchants.DOUBLE_JUMP)).isZero();
    }

    private static EnchantsSettings withEnabled(boolean on) {
        EnchantsSettings d = EnchantsSettings.DEFAULTS;
        return new EnchantsSettings(d.flightSecondsPerLevel(), d.flightRechargeSeconds(), d.landingGraceSeconds(),
                d.doubleJumpPowerPercent(), d.boostRechargeSeconds(), d.boostSpeedPercent(), d.launchSpeed(),
                d.launchCooldownSeconds(), d.wisdomPercentPerLevel(),
                d.autopilotTopSpeedPercent(), d.reinforcedPercentPerLevel(), d.switchedOff(), on);
    }

    @Test
    @DisplayName("the pool is told what is remembered, so no other thread reads the player's data")
    void rememberedSpent() {
        assertThat(service.spentFor(player.getUniqueId())).as("nothing remembered yet").isZero();
        assertThat(service.learn(player, "double_jump")).isEmpty();
        assertThat(service.spentFor(player.getUniqueId())).as("remembered by learning").isEqualTo(3);

        service.forget(player.getUniqueId());
        assertThat(service.spentFor(player.getUniqueId())).isZero();
        service.remember(player);
        assertThat(service.spentFor(player.getUniqueId())).as("read again on joining").isEqualTo(3);
    }
}
