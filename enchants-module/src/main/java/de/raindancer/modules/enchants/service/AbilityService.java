package de.raindancer.modules.enchants.service;

import de.raindancer.core.content.enchant.CustomEnchantments;
import de.raindancer.core.content.enchant.EnchantShards;
import de.raindancer.core.content.enchant.RainsEnchants;
import de.raindancer.core.social.economy.Economies;
import de.raindancer.core.social.economy.Economy;
import de.raindancer.core.social.economy.EconomyResult;
import de.raindancer.core.social.economy.Fees;
import de.raindancer.core.social.economy.Money;
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
 * rank costs shards of the ability's own enchantment and money, taken together or not at all. What is unlocked
 * counts as the enchantment's level wherever it is read, next to what the gear carries.
 */
public final class AbilityService implements AbilityLevels {

    /** The enchantments the tree may carry; a node's effect id has to be one of these. */
    private static final Set<String> KEYS = Set.of(RainsEnchants.DOUBLE_JUMP, RainsEnchants.SWIFTNESS,
            RainsEnchants.NIGHT_VISION);

    static final String SOURCE = "enchant-abilities";

    /** What a rank costs, and what a player pays it with. Live: shards in the inventory, Core's economy. */
    public interface Purse {

        int shards(Player player, String enchantment);

        boolean takeShards(Player player, String enchantment, int amount);

        /** What taking {@code written} would really cost now. */
        Money quote(Money written);

        Money balance(Player player);

        /** Takes the money; empty when it could not. */
        Optional<Money> charge(Player player, Money written);

        void refund(Player player, Money taken);
    }

    /** What the next rank still lacks. */
    public record Shortfall(int shards, Money money) {
        public boolean any() {
            return shards > 0 || money.isPositive();
        }
    }

    private final SkillTree tree;
    private final AbilityStore store;
    private final Supplier<EnchantsSettings> settings;
    private final Purse purse;

    public AbilityService(SkillTree tree, AbilityStore store, Supplier<EnchantsSettings> settings, Purse purse) {
        this.tree = tree;
        this.store = store;
        this.settings = settings;
        this.purse = purse;
    }

    public static SkillTree loadTree() {
        return SkillTree.resource(EnchantsModule.class, "abilities.yml", KEYS::contains);
    }

    public static Purse livePurse() {
        return new Purse() {
            @Override
            public int shards(Player player, String enchantment) {
                return EnchantShards.count(player.getInventory(), enchantment);
            }

            @Override
            public boolean takeShards(Player player, String enchantment, int amount) {
                return EnchantShards.take(player.getInventory(), enchantment, amount);
            }

            @Override
            public Money quote(Money written) {
                return Fees.quote(SOURCE, written);
            }

            @Override
            public Money balance(Player player) {
                return Economies.current().map(bank -> bank.balance(player.getUniqueId())).orElse(Money.ZERO);
            }

            @Override
            public Optional<Money> charge(Player player, Money written) {
                EconomyResult result = Fees.charge(player.getUniqueId(), written, "Enchantment ability", SOURCE);
                return result.succeeded() ? Optional.of(result.amount()) : Optional.empty();
            }

            @Override
            public void refund(Player player, Money taken) {
                Fees.refund(player.getUniqueId(), taken, "Enchantment ability (not completed)", SOURCE);
            }
        };
    }

    public SkillTree tree() {
        return tree;
    }

    public boolean enabled() {
        return settings.get().abilitiesEnabled();
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

    /** The level of this enchantment the player has unlocked; 0 for none. */
    public int unlocked(Player player, String enchantment) {
        return tree.effects(ranks(player)).getOrDefault(enchantment, 0.0).intValue();
    }

    @Override
    public int effective(Player player, ItemStack gear, String enchantment) {
        return AbilityRule.effective(CustomEnchantments.levelOn(gear, enchantment), unlocked(player, enchantment));
    }

    /** The enchantment whose shards a node is paid in. */
    public Optional<String> enchantmentOf(TreeNode node) {
        return node.effects().keySet().stream().findFirst();
    }

    /** What the next rank costs, for a player: "4 shards, $20k". */
    public String priceText(int rank) {
        int shards = shardsFor(rank);
        Money money = purse.quote(moneyFor(rank));
        String shardText = shards + " shard" + (shards == 1 ? "" : "s");
        return money.isPositive() ? shardText + ", " + Fees.format(money) : shardText;
    }

    public Money moneyFor(int rank) {
        return AbilityRule.money(Fees.amount(settings.get().abilitiesMoneyPerRank()), rank);
    }

    public int shardsFor(int rank) {
        return AbilityRule.shards(AbilityRule.parseShards(settings.get().abilitiesShardsPerRank()), rank);
    }

    public Shortfall shortfall(Player player, String nodeId, int rank) {
        Optional<String> enchantment = tree.node(nodeId).flatMap(this::enchantmentOf);
        if (enchantment.isEmpty()) {
            return new Shortfall(0, Money.ZERO);
        }
        int missingShards = Math.max(0, shardsFor(rank) - purse.shards(player, enchantment.get()));
        Money price = purse.quote(moneyFor(rank));
        Money balance = purse.balance(player);
        Money missingMoney = price.isPositive() && !balance.isAtLeast(price) ? price.minus(balance) : Money.ZERO;
        return new Shortfall(missingShards, missingMoney);
    }

    /** Why the next rank may not be taken, if it may not. */
    public Optional<SkillTree.Refusal> refusal(Player player, String nodeId) {
        Optional<TreeNode> node = tree.node(nodeId);
        if (node.isEmpty()) {
            return Optional.of(SkillTree.Refusal.NEEDS);
        }
        Map<String, Integer> ranks = ranks(player);
        Optional<SkillTree.Refusal> refused = tree.refusal(ranks, node.get(), 0, 0);
        if (refused.isPresent()) {
            return refused;
        }
        int next = ranks.getOrDefault(nodeId, 0) + 1;
        return !enabled() || shortfall(player, nodeId, next).any() ? Optional.of(SkillTree.Refusal.NO_POINTS)
                : Optional.empty();
    }

    /**
     * Takes the next rank: money first, then the shards, and if either fails whatever was taken goes back.
     *
     * @return why not, if not
     */
    public Optional<SkillTree.Refusal> learn(Player player, String nodeId) {
        Optional<SkillTree.Refusal> refused = refusal(player, nodeId);
        if (refused.isPresent()) {
            return refused;
        }
        TreeNode node = tree.node(nodeId).orElseThrow();
        String enchantment = enchantmentOf(node).orElseThrow();
        int next = store.rank(player, nodeId) + 1;
        Optional<Money> paid = purse.charge(player, moneyFor(next));
        if (paid.isEmpty()) {
            return Optional.of(SkillTree.Refusal.NO_POINTS);
        }
        if (!purse.takeShards(player, enchantment, shardsFor(next))) {
            if (paid.get().isPositive()) {
                purse.refund(player, paid.get());
            }
            return Optional.of(SkillTree.Refusal.NO_POINTS);
        }
        store.set(player, nodeId, next);
        return Optional.empty();
    }
}
