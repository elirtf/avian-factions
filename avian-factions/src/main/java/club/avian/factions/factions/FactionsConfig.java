package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

/**
 * {@code factions.conf}. Factions, claims, power and protection belong to FactionsUUID (ADR-0007)
 * and are configured in its own {@code config/main.conf}; this file holds only what we add on top.
 */
@ConfigSerializable
public final class FactionsConfig {

    public static final ConfigSpec<FactionsConfig> SPEC = ConfigSpec.of("factions.conf", FactionsConfig.class)
            .version(1)
            .validate(FactionsConfig::validate)
            .build();

    @Comment("Flat power every new faction gets on top of its members' power, so a solo player can\n"
            + "hold a real base. With FactionsUUID one power is one chunk: base 5 + a full player's 20\n"
            + "is 25, a 5x5 square. Applied once, when the faction is created; changing it does not\n"
            + "touch existing factions (use /f admin power boost for those). Not lost on death.")
    private double factionBasePower = 5.0;

    @Comment("FactionsUUID upgrades factions can buy, by name. Every other upgrade is switched off.\n"
            + "Costs and levels are FactionsUUID's own, in its data/universe.json.\n"
            + "Launch set: claim_boost (ours), spawner_rate, crop_yield + growth (farming), mob_exp, tnt_bank.")
    private List<String> enabledUpgrades = List.of("claim_boost", "spawner_rate", "crop_yield", "growth", "mob_exp", "tnt_bank");

    public double factionBasePower() {
        return factionBasePower;
    }

    public List<String> enabledUpgrades() {
        return enabledUpgrades;
    }

    static void validate(FactionsConfig cfg, ConfigErrors e) {
        e.check(Double.isFinite(cfg.factionBasePower) && cfg.factionBasePower >= 0, "faction-base-power",
                "must be a finite number >= 0 (got %s)", cfg.factionBasePower);
    }
}
