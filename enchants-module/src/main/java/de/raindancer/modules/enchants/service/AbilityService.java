package de.raindancer.modules.enchants.service;

import de.raindancer.core.content.enchant.CustomEnchantments;
import de.raindancer.core.content.enchant.RainsEnchants;
import de.raindancer.core.ui.tree.SkillPoints;
import de.raindancer.core.ui.tree.SkillTree;
import de.raindancer.core.ui.tree.TreeNode;
import de.raindancer.modules.enchants.EnchantsModule;
import de.raindancer.modules.enchants.EnchantsSettings;
import de.raindancer.modules.enchants.rules.AbilityRule;
import de.raindancer.modules.enchants.store.AbilityStore;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The abilities tree: Double Jump, Swiftness and Night Vision unlocked for good instead of enchanted onto gear. A
 * rank costs the node's skill points, from the pool the skills plugin provides in Core ({@link SkillPoints}); what a
 * player has spent here is reported back there, so a point is never spent twice. What is unlocked counts as the
 * enchantment's level wherever it is read, next to what the gear carries.
 */
public final class AbilityService implements AbilityLevels {

    /** The enchantments the tree may carry; a node's effect id has to be one of these. */
    private static final Set<String> KEYS = Set.of(RainsEnchants.DOUBLE_JUMP, RainsEnchants.SWIFTNESS,
            RainsEnchants.NIGHT_VISION);

    /** The id this tree is known by as a spender of skill points. */
    public static final String SPENDER = "enchants";

    /** Where the points come from. Live: Core's {@link SkillPoints}; an older Core has none. */
    public interface Points {

        boolean available();

        int level(Player player);

        int left(Player player);
    }

    private final SkillTree tree;
    private final AbilityStore store;
    private final Supplier<EnchantsSettings> settings;
    private final Points points;

    public AbilityService(SkillTree tree, AbilityStore store, Supplier<EnchantsSettings> settings, Points points) {
        this.tree = tree;
        this.store = store;
        this.settings = settings;
        this.points = points;
    }

    public static SkillTree loadTree() {
        return SkillTree.resource(EnchantsModule.class, "abilities.yml", KEYS::contains);
    }

    public static Points livePoints() {
        return new Points() {
            @Override
            public boolean available() {
                try {
                    return SkillPoints.available();
                } catch (LinkageError olderCore) {
                    return false;
                }
            }

            @Override
            public int level(Player player) {
                try {
                    return SkillPoints.level(player.getUniqueId());
                } catch (LinkageError olderCore) {
                    return 0;
                }
            }

            @Override
            public int left(Player player) {
                try {
                    return SkillPoints.left(player.getUniqueId());
                } catch (LinkageError olderCore) {
                    return 0;
                }
            }
        };
    }

    public SkillTree tree() {
        return tree;
    }

    public boolean enabled() {
        return settings.get().abilitiesEnabled();
    }

    /** Whether a skills plugin is there to give the points. */
    public boolean pointsAvailable() {
        return points.available();
    }

    public int level(Player player) {
        return points.level(player);
    }

    public int pointsLeft(Player player) {
        return points.left(player);
    }

    /** Node id to unlocked rank, for the nodes the player has anything in. */
    public Map<String, Integer> ranks(Player player) {
        Map<String, Integer> ranks = new LinkedHashMap<>();
        for (TreeNode node : tree.nodes()) {
            int rank = store.rank(player, node.id());
            if (rank > 0) {
                ranks.put(node.id(), rank);
            }
        }
        return ranks;
    }

    /** The skill points the player has put into this tree. */
    public int spent(Player player) {
        return tree.spent(ranks(player));
    }

    /** The level of this enchantment the player has unlocked; 0 for none. */
    public int unlocked(Player player, String enchantment) {
        return tree.effects(ranks(player)).getOrDefault(enchantment, 0.0).intValue();
    }

    @Override
    public int effective(Player player, ItemStack gear, String enchantment) {
        return AbilityRule.effective(CustomEnchantments.levelOn(gear, enchantment), unlocked(player, enchantment));
    }

    /** Why the next rank may not be taken, if it may not. */
    public Optional<SkillTree.Refusal> refusal(Player player, String nodeId) {
        Optional<TreeNode> node = tree.node(nodeId);
        if (node.isEmpty()) {
            return Optional.of(SkillTree.Refusal.NEEDS);
        }
        if (!enabled() || !points.available()) {
            return Optional.of(SkillTree.Refusal.NO_POINTS);
        }
        return tree.refusal(ranks(player), node.get(), points.level(player), points.left(player));
    }

    /**
     * Takes the next rank. The points are not moved anywhere: what is spent is what the ranks add up to.
     *
     * @return why not, if not
     */
    public Optional<SkillTree.Refusal> learn(Player player, String nodeId) {
        Optional<SkillTree.Refusal> refused = refusal(player, nodeId);
        if (refused.isPresent()) {
            return refused;
        }
        store.set(player, nodeId, store.rank(player, nodeId) + 1);
        return Optional.empty();
    }
}
