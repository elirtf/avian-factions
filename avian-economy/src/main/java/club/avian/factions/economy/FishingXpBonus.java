package club.avian.factions.economy;

/** The fishing skill-XP bonus for custom-enchant catches (see {@link FishingEnchantXp}); no AuraSkills types. */
final class FishingXpBonus {

    private FishingXpBonus() {
    }

    /**
     * Extra XP on top of what AuraSkills paid for the catch: a doubled catch counts twice when
     * {@code doubleCatchCounts}, and Seasoned Angler adds {@code anglerPerLevel} per level; the two stack.
     */
    static double of(double paid, boolean doubled, int anglerLevel, boolean doubleCatchCounts, double anglerPerLevel) {
        double factor = (doubled && doubleCatchCounts ? 2 : 1) * (1 + anglerPerLevel * anglerLevel);
        return paid * (factor - 1);
    }
}
