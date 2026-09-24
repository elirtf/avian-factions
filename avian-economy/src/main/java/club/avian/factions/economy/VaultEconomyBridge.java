package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Exposes our money as Vault's economy, so EssentialsX and anything else Vault-aware reads and
 * writes the same balance instead of keeping a second one (resolves the "EssentialsX overlap"
 * question for money).
 *
 * <p>Vault's API is synchronous and callers expect a result immediately, so writes block briefly
 * on the database future. That is Vault's design, not ours; our own modules use {@link Economy}
 * directly and never block.
 */
/*
 * VaultAPI deprecated its whole name-keyed surface in favour of OfflinePlayer-keyed methods, but
 * AbstractEconomy still declares them abstract, so a provider has no choice but to implement them.
 * The suppression is for that, and stops here: nothing else in Avian calls a deprecated Vault API.
 */
@SuppressWarnings("deprecation")
public final class VaultEconomyBridge extends AbstractEconomy {

    private static final long WRITE_TIMEOUT_SECONDS = 5;

    private final Economy economy;
    private final String serverName;

    public VaultEconomyBridge(Economy economy, String serverName) {
        this.economy = economy;
        this.serverName = serverName;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return serverName + " Economy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;   // faction banks are ours, not Vault's
    }

    @Override
    public int fractionalDigits() {
        return 0;   // whole dollars, no cents
    }

    @Override
    public String format(double amount) {
        return economy.format(Currency.MONEY, toWhole(amount));
    }

    @Override
    public String currencyNamePlural() {
        return "dollars";
    }

    @Override
    public String currencyNameSingular() {
        return "dollar";
    }

    // --- keyed by OfflinePlayer (UUID) -----------------------------------------------------------
    //
    // AbstractEconomy turns these into the by-name methods below, which only know players the server
    // has seen. FactionsUUID keeps every faction's bank as its own account and asks about it by
    // OfflinePlayer; the by-name lookup found no such player, reported "no account", and FactionsUUID
    // then looped (checkStatus -> setBalance -> getBalance -> checkStatus …) into a StackOverflowError.
    // That crashed /f claim, since claiming charges the bank. Our balances are keyed by UUID anyway,
    // so answer these directly: every UUID has an account, and a missing row is a balance of zero.

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return toDouble(economy.balance(player.getUniqueId(), Currency.MONEY));
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (toWhole(amount) == 0) {
            return noChange(player);   // plugins zero accounts this way; nothing to write
        }
        return await(economy.withdraw(player.getUniqueId(), Currency.MONEY, toWhole(amount), "vault"),
                amount, player.getUniqueId());
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (toWhole(amount) == 0) {
            return noChange(player);
        }
        return await(economy.deposit(player.getUniqueId(), Currency.MONEY, toWhole(amount), "vault"),
                amount, player.getUniqueId());
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;   // accounts are implicit
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    // --- keyed by name (the deprecated half of Vault; still used by some plugins) ---------------

    @Override
    public boolean hasAccount(String playerName) {
        return resolve(playerName) != null;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public double getBalance(String playerName) {
        var uuid = resolve(playerName);
        return uuid == null ? 0 : toDouble(economy.balance(uuid, Currency.MONEY));
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        var uuid = resolve(playerName);
        if (uuid == null) {
            return failure(0, "Unknown player " + playerName);
        }
        return await(economy.withdraw(uuid, Currency.MONEY, toWhole(amount), "vault"), amount, uuid);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        var uuid = resolve(playerName);
        if (uuid == null) {
            return failure(0, "Unknown player " + playerName);
        }
        return await(economy.deposit(uuid, Currency.MONEY, toWhole(amount), "vault"), amount, uuid);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;   // accounts are implicit: a missing row reads as zero
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    // --- banks: unsupported ------------------------------------------------------------------

    @Override
    public EconomyResponse createBank(String name, String player) {
        return unsupported();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return unsupported();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return unsupported();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return unsupported();
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    // --- helpers ------------------------------------------------------------------------------

    /** Vault speaks doubles; our money is whole dollars, so rounding is the whole conversion. */
    private static long toWhole(double amount) {
        return Math.round(amount);
    }

    private static double toDouble(long minor) {
        return minor;
    }

    private static UUID resolve(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(playerName);
        return player == null ? null : player.getUniqueId();
    }

    private EconomyResponse await(java.util.concurrent.CompletableFuture<club.avian.factions.api.economy.TransactionResult> future,
                                  double amount, UUID player) {
        try {
            var result = future.get(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            double balance = toDouble(result.balanceAfter());
            if (result.ok()) {
                return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, null);
            }
            return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, result.status().name());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // only for a real interruption
            return failure(toDouble(economy.balance(player, Currency.MONEY)), "interrupted");
        } catch (Exception e) {
            // Timeout or a failed write. Never interrupt the calling thread for these: Vault is
            // called on the main server thread, and marking it interrupted breaks unrelated code.
            return failure(toDouble(economy.balance(player, Currency.MONEY)), String.valueOf(e.getMessage()));
        }
    }

    private EconomyResponse noChange(OfflinePlayer player) {
        return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
    }

    private static EconomyResponse failure(double balance, String message) {
        return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, message);
    }

    private static EconomyResponse unsupported() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks are not Vault-backed");
    }
}
