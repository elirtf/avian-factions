package club.avian.factions.economy;

/** The fishing skill-XP bonus for multi-item catches and Seasoned Angler (see {@link FishingEnchantXp}); no AuraSkills types. */
final class FishingXpBonus {

    private FishingXpBonus() {
    }

    /**
     * Extra XP on top of what was paid for the catch: a catch of {@code amount} items counts that many
     * times when {@code multipleCount} (Double Catch, AuraSkills' Fisher, anything that doubles the
     * stack), and Seasoned Angler adds {@code anglerPerLevel} per level; the two stack.
     */
    static double of(double paid, int amount, int anglerLevel, boolean multipleCount, double anglerPerLevel) {
        double factor = (multipleCount ? Math.max(1, amount) : 1) * (1 + anglerPerLevel * anglerLevel);
        return paid * (factor - 1);
    }
}
