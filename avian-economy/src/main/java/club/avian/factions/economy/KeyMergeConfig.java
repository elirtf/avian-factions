package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

/**
 * {@code keymerge.conf}: merging crate keys up a tier (owner, 2026-10-16). Each step trades {@code cost} keys of one
 * crate for one of the next. The ratios cost about 1.5-1.7x the token-shop value of what they make (Common 200,
 * Rare 600, Epic 1,500, Mythic 10,000 tokens), so merging is a use for spare keys, never a cheaper route.
 */
@ConfigSerializable
public final class KeyMergeConfig {

    public static final ConfigSpec<KeyMergeConfig> SPEC = ConfigSpec.of("keymerge.conf", KeyMergeConfig.class)
            .version(1)
            .validate(KeyMergeConfig::validate)
            .build();

    @Comment("""
            The steps, in menu order: the first three on the top row, the rest on the row below. Crate names
            are CrazyCrates' (plugins/CrazyCrates/crates/<name>.yml). Only virtual keys merge (every key
            this server gives is virtual).""")
    private List<Step> steps = List.of(
            new Step("Common", "Rare", 5), new Step("Rare", "Epic", 4), new Step("Epic", "Mythic", 10),
            new Step("EnchantCommon", "EnchantRare", 5), new Step("EnchantRare", "EnchantLegendary", 4),
            new Step("EnchantLegendary", "EnchantMythic", 10));

    public List<Step> steps() {
        return steps;
    }

    static void validate(KeyMergeConfig cfg, ConfigErrors e) {
        e.check(cfg.steps.size() <= 6, "steps", "at most 6 fit the menu");
        for (int i = 0; i < cfg.steps.size(); i++) {
            var s = cfg.steps.get(i);
            e.check(s.cost >= 2, "steps[" + i + "].cost", "must be at least 2");
            e.check(s.from.matches("[A-Za-z0-9_-]+") && s.to.matches("[A-Za-z0-9_-]+"), "steps[" + i + "]",
                    "crate names are letters, digits, _ and -");
        }
    }

    /** {@code cost} keys of {@code from} make one key of {@code to}. */
    @ConfigSerializable
    public static final class Step {

        private String from = "Common";
        private String to = "Rare";
        private int cost = 5;

        public Step() {
        }

        Step(String from, String to, int cost) {
            this.from = from;
            this.to = to;
            this.cost = cost;
        }

        public String from() {
            return from;
        }

        public String to() {
            return to;
        }

        public int cost() {
            return cost;
        }
    }
}
