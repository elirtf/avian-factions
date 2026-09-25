package club.avian.factions.economy;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import org.bukkit.entity.Player;

/**
 * Farming XP through AuraSkills. Its own class so the AuraSkills API is only loaded when the plugin
 * is installed; {@code addSkillXp} applies AuraSkills' own XP multipliers.
 */
final class AuraSkillsXp {

    private AuraSkillsXp() {
    }

    static void addFarmingXp(Player player, double xp) {
        var user = AuraSkillsApi.get().getUser(player.getUniqueId());
        if (user != null) {
            user.addSkillXp(Skills.FARMING, xp);
        }
    }
}
