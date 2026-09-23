package club.avian.factions.factions;

import dev.kitteh.factions.Universe;
import dev.kitteh.factions.upgrade.Upgrade;
import dev.kitteh.factions.upgrade.UpgradeRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Applies {@code factions.conf}'s upgrade list to FactionsUUID, which offers no command for it:
 * listed upgrades are on, every other one is off. Runs at enable and on reload.
 */
final class UpgradeSwitch {

    private UpgradeSwitch() {
    }

    static void apply(List<String> enabled, Logger logger) {
        var wanted = new HashSet<String>();
        enabled.forEach(name -> wanted.add(name.toLowerCase(Locale.ROOT)));
        var on = new ArrayList<String>();
        for (Upgrade upgrade : UpgradeRegistry.getUpgrades()) {
            boolean enable = wanted.remove(upgrade.name().toLowerCase(Locale.ROOT));
            Universe.universe().upgradeEnabled(upgrade, enable);
            if (enable) {
                on.add(upgrade.name());
            }
        }
        wanted.forEach(name -> logger.warning("factions.conf: enabled-upgrades lists unknown upgrade '" + name + "'"));
        logger.info("Faction upgrades on: " + String.join(", ", on));
    }
}
