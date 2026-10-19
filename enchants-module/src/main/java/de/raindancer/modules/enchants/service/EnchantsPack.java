package de.raindancer.modules.enchants.service;

import de.raindancer.core.RainsCore;
import de.raindancer.core.content.enchant.EnchantDef;
import de.raindancer.core.content.pack.PackContribution;
import de.raindancer.core.platform.log.Log;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.modules.enchants.EnchantsDefinitions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Every enchantment's shard with a model of its own (Blockbench sources in {@code enchants-module/art/shards}): copied
 * out of the jar and offered to Core, which builds and sends the pack. The echo shard picks the model by the key
 * {@code EnchantShards} puts in the first custom_model_data string, so any other echo shard stays one.
 */
public final class EnchantsPack {

    static final String ROOT = "/de/raindancer/modules/enchants/pack/";
    static final String SHARD_ITEM = "assets/minecraft/items/echo_shard.json";
    private static final String OWNER = "Enchants";
    private static final String NAME = "shards";

    private EnchantsPack() {
    }

    static List<String> files() {
        List<String> files = new ArrayList<>(List.of("pack.mcmeta", SHARD_ITEM));
        for (EnchantDef def : EnchantsDefinitions.all()) {
            files.add("assets/rainsenchants/models/item/shard/" + def.path() + ".json");
            files.add("assets/rainsenchants/textures/item/shard/" + def.path() + ".png");
        }
        return files;
    }

    /** Offers the pack; a server that has already built its pack builds again, so nobody waits for a restart. */
    public static void contribute(Plugin plugin, RainsCore core) {
        Path folder = plugin.getDataFolder().toPath().resolve("pack").resolve(NAME);
        try {
            for (String file : files()) {
                Path target = folder.resolve(file);
                Files.createDirectories(target.getParent());
                try (InputStream in = EnchantsPack.class.getResourceAsStream(ROOT + file)) {
                    if (in == null) {
                        throw new IOException(file + " is missing from the jar");
                    }
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException failure) {
            Log.of("enchants").warn("The shards' looks could not be written ({}); they work, but look like echo shards.",
                    failure.getMessage());
            return;
        }
        var packs = core.resourcePacks();
        packs.contribute(PackContribution.of(OWNER, NAME, folder).describedAs("A look of its own for every shard"));
        if (packs.current().isPresent()) {
            Scheduling.async(plugin, () -> packs.rebuild().ifPresent(built -> packs.sendToAll(
                    plugin.getServer().getOnlinePlayers().stream().map(Player::getUniqueId).toList())));
        }
    }

    public static void withdraw(RainsCore core) {
        core.resourcePacks().withdraw(OWNER.toLowerCase(java.util.Locale.ROOT) + ":" + NAME);
    }
}
