package de.raindancer.modules.enchants.screen;

import de.raindancer.core.ui.chat.Brand;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.tree.LearningMenu;
import de.raindancer.core.ui.tree.SkillTree;
import de.raindancer.core.ui.tree.TreeNode;
import de.raindancer.modules.enchants.service.AbilityService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The abilities tree as Core's {@link LearningMenu}, answered for one player: skill points, no reset. */
public final class AbilitiesScreen {

    private final AbilityService service;
    private final Messages messages;
    private final Brand brand;

    public AbilitiesScreen(AbilityService service, Messages messages, Brand brand) {
        this.service = service;
        this.messages = messages;
        this.brand = brand;
    }

    /** Opens the tree for {@code viewer}, with {@code parent} to go back to (null for none). */
    public void open(Player viewer, Menu parent) {
        if (!service.enabled()) {
            messages.send(viewer, "enchants.abilities-off");
            return;
        }
        if (!service.pointsAvailable()) {
            messages.send(viewer, "enchants.abilities-need-skills");
            return;
        }
        new LearningMenu(viewer, brand, parent, new Answers(viewer)).open();
    }

    private final class Answers implements LearningMenu.Learner {

        private final Player viewer;

        Answers(Player viewer) {
            this.viewer = viewer;
        }

        @Override
        public SkillTree tree() {
            return service.tree();
        }

        @Override
        public String title() {
            return "Abilities";
        }

        @Override
        public String blurb() {
            return "Unlock what an enchantment does, for good, with skill points.";
        }

        @Override
        public Material icon() {
            return Material.ENCHANTED_BOOK;
        }

        @Override
        public Map<String, Integer> ranks() {
            return service.ranks(viewer);
        }

        @Override
        public int level() {
            return service.level(viewer);
        }

        @Override
        public String levelWord() {
            return "Adventurer level";
        }

        @Override
        public int points() {
            return service.pointsLeft(viewer) + service.spent(viewer);
        }

        @Override
        public int pointsLeft() {
            return service.pointsLeft(viewer);
        }

        /** Unlocked for good: the ranks are not given back. */
        @Override
        public boolean resettable() {
            return false;
        }

        @Override
        public String describe(String effect, double value) {
            return "Counts as " + name(effect) + " level " + (int) value;
        }

        @Override
        public Optional<SkillTree.Refusal> refusal(String node) {
            return service.refusal(viewer, node);
        }

        @Override
        public Optional<SkillTree.Refusal> learn(String node) {
            return service.learn(viewer, node);
        }

        @Override
        public long resetAgainAt() {
            return 0;
        }

        @Override
        public boolean reset() {
            return false;
        }

        @Override
        public List<String> extra(TreeNode node) {
            return List.of("<dark_gray>The " + node.title() + " enchantment on gear still works.");
        }

        private String name(String effect) {
            return service.tree().nodes().stream().filter(node -> node.effects().containsKey(effect))
                    .map(TreeNode::title).findFirst().orElse(effect);
        }
    }
}
