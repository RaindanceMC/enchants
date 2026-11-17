package de.raindancer.modules.enchants.rules;

import de.raindancer.core.social.economy.Money;

import java.util.ArrayList;
import java.util.List;

/** What the abilities tree charges and what an unlocked rank is worth against an enchanted piece of gear. */
public final class AbilityRule {

    private static final List<Integer> DEFAULT_SHARDS = List.of(4, 8, 16);
    private static final int MOST_SHARDS = 1_000_000;

    private AbilityRule() {
    }

    /** The level that counts: the gear's own or the unlocked rank, whichever is better. */
    public static int effective(int onGear, int unlocked) {
        return Math.max(onGear, unlocked);
    }

    /** "4, 8, 16" as numbers; entries that are not a positive number are skipped, none left gives 4, 8, 16. */
    public static List<Integer> parseShards(String written) {
        List<Integer> shards = new ArrayList<>();
        if (written != null) {
            for (String part : written.split(",")) {
                try {
                    int amount = Integer.parseInt(part.strip());
                    if (amount > 0) {
                        shards.add(amount);
                    }
                } catch (NumberFormatException skipped) {
                    // not a number: left out
                }
            }
        }
        return shards.isEmpty() ? DEFAULT_SHARDS : shards;
    }

    /** Shards for rank {@code rank} (from 1): the list's entry, then double the one before. */
    public static int shards(List<Integer> perRank, int rank) {
        if (rank <= perRank.size()) {
            return perRank.get(rank - 1);
        }
        long amount = perRank.getLast();
        for (int more = rank - perRank.size(); more > 0 && amount < MOST_SHARDS; more--) {
            amount *= 2;
        }
        return (int) Math.min(MOST_SHARDS, amount);
    }

    public static Money money(Money perRankNumber, int rank) {
        return perRankNumber.times(rank);
    }
}
