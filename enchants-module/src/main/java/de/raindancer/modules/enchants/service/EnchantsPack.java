package de.raindancer.modules.enchants.service;

import de.raindancer.core.RainsCore;
import de.raindancer.core.content.pack.JarPack;
import de.raindancer.core.platform.log.Log;
import de.raindancer.modules.enchants.rules.WeaponLook;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The resource pack of the enchantments: a mace with Thor is Mjölnir ({@code enchants-module/art/mjolnir.bbmodel}).
 * Copied out of the jar and offered to Core, which builds and sends the pack.
 */
public final class EnchantsPack {

    static final String ROOT = "/de/raindancer/modules/enchants/pack/";
    private static final String OWNER = "Enchants";
    private static final String NAME = "looks";

    private EnchantsPack() {
    }

    static List<String> files() {
        List<String> files = new ArrayList<>(List.of("pack.mcmeta"));
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
                    "Mjölnir, the mace of Thor");
        } catch (IOException failure) {
            Log.of("enchants").warn("The enchantments' looks could not be written ({}); they work, but look plain.",
                    failure.getMessage());
        }
    }

    public static void withdraw(RainsCore core) {
        core.resourcePacks().withdraw(OWNER.toLowerCase(java.util.Locale.ROOT) + ":" + NAME);
    }
}
