package de.raindancer.modules.enchants.rules;

/** What an unlocked rank of the abilities tree is worth against an enchanted piece of gear. */
public final class AbilityRule {

    private AbilityRule() {
    }

    /** The level that counts: the gear's own or the unlocked rank, whichever is better. */
    public static int effective(int onGear, int unlocked) {
        return Math.max(onGear, unlocked);
    }
}
