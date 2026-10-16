package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.text.DecimalFormat;

/**
 * {@code %avian_…%} placeholders, for the chat hover card (CarbonChat) and anything else that
 * reads PlaceholderAPI.
 *
 * <ul>
 *   <li>{@code balance} — money with its symbol: {@code $1,640}</li>
 *   <li>{@code tokens}, {@code gems} — the bare number: {@code 1,200}</li>
 *   <li>{@code rank} — the player's rank tag, their LuckPerms suffix (MiniMessage, see ranks.lp)</li>
 *   <li>{@code name} — the account name, for click-to-message</li>
 *   <li>{@code ice_fish} — catches in icy waters (the Ice Fisher quest)</li>
 *   <li>{@code hp}, {@code max_hp} — health and max health (skills included), whole, .5 rounding up</li>
 * </ul>
 *
 * <p>Every value comes from memory (the balance cache, LuckPerms' loaded user), never the
 * database: chat formats these on every message.
 */
public final class AvianPlaceholders extends PlaceholderExpansion {

    private final Economy economy;

    public AvianPlaceholders(Economy economy) {
        this.economy = economy;
    }

    @Override
    public String getIdentifier() {
        return "avian";
    }

    @Override
    public String getAuthor() {
        return "elirtf";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public boolean persist() {
        return true;   // survive /papi reload: we are registered by our plugin, not loaded from a jar
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        return switch (params.toLowerCase(java.util.Locale.ROOT)) {
            case "balance" -> economy.format(Currency.MONEY, economy.balance(player.getUniqueId(), Currency.MONEY));
            case "tokens" -> number(economy.balance(player.getUniqueId(), Currency.TOKENS));
            case "gems" -> number(economy.balance(player.getUniqueId(), Currency.GEMS));
            case "rank" -> rank(player);
            case "name" -> player.getName() == null ? "" : player.getName();
            case "level" -> player.getPlayer() == null ? "" : String.valueOf(player.getPlayer().getLevel());   // XP level (the Enchanter)
            case "hp" -> player.getPlayer() == null ? "" : whole(player.getPlayer().getHealth());   // the HUD (AuraSkills action bar)
            case "max_hp" -> player.getPlayer() == null ? "" : whole(maxHealth(player.getPlayer()));
            case "ice_fish" -> player.getPlayer() == null ? "0" : String.valueOf(IceFishing.count(player.getPlayer()));   // Ice Fisher quest
            default -> null;   // unknown: PlaceholderAPI leaves the text as typed
        };
    }

    /** Health as players see it: a whole number, .5 and up rounding up (owner, 2026-10-16). */
    static String whole(double health) {
        return String.valueOf(Math.round(health));
    }

    private static double maxHealth(org.bukkit.entity.Player player) {
        var attribute = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        return attribute == null ? 20 : attribute.getValue();
    }

    static String number(long amount) {
        return new DecimalFormat("#,##0").format(amount);
    }

    private static String rank(OfflinePlayer player) {
        var chat = Bukkit.getServicesManager().load(Chat.class);   // LuckPerms provides it through Vault
        if (chat == null) {
            return "";
        }
        var suffix = chat.getPlayerSuffix(null, player);
        return suffix == null ? "" : suffix.strip();
    }
}
