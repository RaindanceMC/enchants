package de.raindancer.modules.enchants.rules;

import de.raindancer.core.content.enchant.RainsEnchants;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What a weapon wearing a visible enchantment looks like: a whole model of its own instead of vanilla's
 * ({@code rainsenchants:<model>}, Blockbench source in {@code enchants-module/art}). So far a mace with Thor is
 * Mjölnir: ready, a quiet lightning cage crackles round the head; after a throw, while it is on cooldown, it
 * recharges in four livelier stages (the pack reads the cooldown itself); and it flies crackling. The lightning
 * textures come from {@code art/mjolnir/lightning.py}.
 *
 * @param models   every model the item definition switches between
 * @param animated the textures that come with a {@code .png.mcmeta} animation
 */
public record WeaponLook(String item, String enchantment, String model, List<String> models,
                         List<String> textures, List<String> animated) {

    public static final List<WeaponLook> LOOKS = List.of(
            new WeaponLook("mace", RainsEnchants.THOR, "rainsenchants:mjolnir",
                    List.of("mjolnir", "mjolnir_charge_1", "mjolnir_charge_2", "mjolnir_charge_3",
                            "mjolnir_charge_4"),
                    List.of("mjolnir"),
                    List.of("mjolnir_cage", "mjolnir_cage_outer", "mjolnir_lightning_1", "mjolnir_lightning_2",
                            "mjolnir_lightning_3", "mjolnir_lightning_4")));

    /** The look of {@code item} (a vanilla item key's path) carrying {@code enchantments}, if it has one. */
    public static Optional<WeaponLook> of(String item, Set<String> enchantments) {
        return LOOKS.stream()
                .filter(look -> look.item().equals(item) && enchantments.contains(look.enchantment()))
                .findFirst();
    }

    /** Whether {@code model} is one of these looks — the only item models taken off again once the enchantment is gone. */
    public static boolean isOurs(String model) {
        return LOOKS.stream().anyMatch(look -> look.model().equals(model));
    }

    public static List<String> enchantments() {
        return LOOKS.stream().map(WeaponLook::enchantment).distinct().toList();
    }

    /** The item definition's name: {@code rainsenchants:mjolnir} → {@code mjolnir}. */
    public String name() {
        return model.substring(model.indexOf(':') + 1);
    }
}
