package de.raindancer.modules.enchants.service;

import de.raindancer.core.content.enchant.CustomEnchantments;
import de.raindancer.modules.enchants.rules.WeaponLook;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Puts {@link WeaponLook} on a stack, or takes it off again once the enchantment is gone. */
public final class WeaponLooks {

    private WeaponLooks() {
    }

    /** @return whether the stack was changed */
    public static boolean dress(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        Set<String> enchantments = new HashSet<>();
        for (String enchantment : WeaponLook.enchantments()) {
            if (CustomEnchantments.levelOn(stack, enchantment) > 0) {
                enchantments.add(enchantment);
            }
        }
        Optional<WeaponLook> look = WeaponLook.of(stack.getType().getKey().getKey(), enchantments);
        Key current = stack.getData(DataComponentTypes.ITEM_MODEL);
        if (look.isPresent()) {
            Key model = Key.key(look.get().model());
            // Another plugin's own model on this item wins.
            if (current != null && !current.equals(model) && !current.namespace().equals("rainsenchants")
                    && !current.equals(stack.getType().getKey())) {
                return false;
            }
            if (model.equals(current)) {
                return false;
            }
            stack.setData(DataComponentTypes.ITEM_MODEL, model);
            return true;
        }
        if (current != null && WeaponLook.isOurs(current.asString())) {
            stack.resetData(DataComponentTypes.ITEM_MODEL);
            return true;
        }
        return false;
    }

    /** A copy for the air: the first custom_model_data flag picks the fully charged model. */
    public static ItemStack flying(ItemStack likeness) {
        ItemStack copy = likeness.clone();
        CustomModelData data = copy.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        CustomModelData.Builder builder = CustomModelData.customModelData().addFlags(List.of(true));
        if (data != null) {
            builder.addFlags(data.flags().stream().skip(1).toList())
                    .addFloats(data.floats()).addStrings(data.strings()).addColors(data.colors());
        }
        copy.setData(DataComponentTypes.CUSTOM_MODEL_DATA, builder.build());
        return copy;
    }
}
