package de.raindancer.modules.enchants.service;

import de.raindancer.core.RainsCore;
import de.raindancer.core.content.enchant.EnchantDef;
import de.raindancer.core.content.pack.JarPack;
import de.raindancer.core.platform.log.Log;
import de.raindancer.modules.enchants.EnchantsDefinitions;
import de.raindancer.modules.enchants.rules.WeaponLook;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Every enchantment's shard with a model of its own (Blockbench sources in {@code enchants-module/art/shards}): copied
 * out of the jar and offered to Core, which builds and sends the pack. The echo shard picks the model by the key
 * {@code EnchantShards} puts in the first custom_model_data string, so any other echo shard stays one. A mace with Thor
 * is Mjölnir ({@code enchants-module/art/mjolnir.bbmodel}).
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
        for (WeaponLook look : WeaponLook.LOOKS) {
            files.add("assets/rainsenchants/items/" + look.name() + ".json");
            for (String model : look.models()) {
                files.add("assets/rainsenchants/models/item/" + model + ".json");
            }
            for (String texture : look.textures()) {
                files.add("assets/rainsenchants/textures/item/" + texture + ".png");
            }
            for (String texture : look.animated()) {
                files.add("assets/rainsenchants/textures/item/" + texture + ".png");
                files.add("assets/rainsenchants/textures/item/" + texture + ".png.mcmeta");
            }
        }
        return files;
    }

    /** Offers the pack through Core's {@code JarPack}. */
    public static void contribute(Plugin plugin, RainsCore core) {
        try {
            JarPack.offer(plugin, core.resourcePacks(), EnchantsPack.class, ROOT, files(), OWNER, NAME,
                    "A look of its own for every shard, and Mjölnir");
        } catch (IOException failure) {
            Log.of("enchants").warn("The shards' looks could not be written ({}); they work, but look like echo shards.",
                    failure.getMessage());
        }
    }

    public static void withdraw(RainsCore core) {
        core.resourcePacks().withdraw(OWNER.toLowerCase(java.util.Locale.ROOT) + ":" + NAME);
    }
}
