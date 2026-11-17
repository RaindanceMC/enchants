package de.raindancer.modules.enchants.service;

import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.tree.SkillView;
import de.raindancer.core.ui.tree.TreeNode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;
import java.util.UUID;

/**
 * The abilities tree as one entry in Core's skills registry, listed in /skills beside the other skills. It has no
 * level of its own: the "level" is how many ranks are unlocked.
 */
public final class AbilitiesSkillView implements SkillView {

    /** After the skills of the land and fishing. */
    static final int ORDER = 40;

    private final AbilityService service;
    private final BiConsumer<Player, Menu> open;

    public AbilitiesSkillView(AbilityService service, BiConsumer<Player, Menu> open) {
        this.service = service;
        this.open = open;
    }

    @Override
    public String id() {
        return "abilities";
    }

    @Override
    public String title() {
        return "Abilities";
    }

    @Override
    public Material icon() {
        return Material.ENCHANTED_BOOK;
    }

    @Override
    public int order() {
        return ORDER;
    }

    private int owned(UUID player) {
        Player online = Bukkit.getPlayer(player);
        return online == null ? 0 : service.ranks(online).values().stream().mapToInt(Integer::intValue).sum();
    }

    private int total() {
        return service.tree().nodes().stream().mapToInt(TreeNode::ranks).sum();
    }

    @Override
    public int level(UUID player) {
        return owned(player);
    }

    @Override
    public double progress(UUID player) {
        int total = total();
        return total == 0 ? 0 : Math.min(1.0, (double) owned(player) / total);
    }

    @Override
    public int pointsLeft(UUID player) {
        Player online = Bukkit.getPlayer(player);
        return online == null ? 0 : service.pointsLeft(online);
    }

    @Override
    public int branches() {
        return 0;
    }

    @Override
    public int mastered(UUID player) {
        return 0;
    }

    @Override
    public void open(Player viewer, Menu parent) {
        open.accept(viewer, parent);
    }
}
