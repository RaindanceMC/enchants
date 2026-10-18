package de.raindancer.modules.enchants.rules;

import java.util.Map;
import java.util.function.Predicate;

/** What an anvil may not do while an enchantment is switched off: put it on, or raise it. */
public final class AnvilGate {

    private AnvilGate() {
    }

    /**
     * @param before the enchantments on the item going in, key to level
     * @param after  those on the result
     */
    public static boolean blocks(Map<String, Integer> before, Map<String, Integer> after, Predicate<String> switchedOff) {
        return after.entrySet().stream().anyMatch(each -> switchedOff.test(each.getKey())
                && each.getValue() > before.getOrDefault(each.getKey(), 0));
    }
}
