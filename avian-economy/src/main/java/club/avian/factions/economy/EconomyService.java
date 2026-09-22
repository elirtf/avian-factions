package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.economy.TransactionResult;

import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Balances: a read cache in memory, writes through the database (spec §12).
 *
 * <p>The cache exists so a scoreboard or a shop GUI can read a balance on the main thread without
 * touching the database. It is never the authority — every write is decided by the database's
 * conditional update, and the cache is corrected from the result, so a stale cache can make a
 * balance display briefly wrong but can never let a player spend money they do not have.
 */
public final class EconomyService implements Economy {

    private final Map<UUID, Map<Currency, Long>> cache = new ConcurrentHashMap<>();
    private final EconomyRepository repository;
    private final ConfigHandle<EconomyConfig> config;
    private final int seasonId;

    public EconomyService(EconomyRepository repository, ConfigHandle<EconomyConfig> config, int seasonId) {
        this.repository = repository;
        this.config = config;
        this.seasonId = seasonId;
    }

    /** Loads the Season's balances. Boot-time; see {@code Database#queryDuringBoot}. */
    public int load() {
        cache.putAll(repository.loadSeason(seasonId));
        return cache.size();
    }

    @Override
    public long balance(UUID player, Currency currency) {
        var balances = cache.get(player);
        if (balances == null) {
            return 0L;
        }
        return balances.getOrDefault(currency, 0L);
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(UUID player, Currency currency, long amount, String reason) {
        var invalid = rejectBadAmount(player, currency, amount);
        if (invalid != null) {
            return CompletableFuture.completedFuture(invalid);
        }
        return repository.apply(player, currency, amount, "DEPOSIT", reason, null, seasonId)
                .thenApply(result -> remember(player, currency, result));
    }

    @Override
    public CompletableFuture<TransactionResult> withdraw(UUID player, Currency currency, long amount, String reason) {
        var invalid = rejectBadAmount(player, currency, amount);
        if (invalid != null) {
            return CompletableFuture.completedFuture(invalid);
        }
        return repository.apply(player, currency, -amount, "WITHDRAW", reason, null, seasonId)
                .thenApply(result -> remember(player, currency, result));
    }

    @Override
    public CompletableFuture<TransactionResult> transfer(UUID from, UUID to, Currency currency,
                                                         long amount, String reason) {
        var invalid = rejectBadAmount(from, currency, amount);
        if (invalid != null) {
            return CompletableFuture.completedFuture(invalid);
        }
        if (from.equals(to)) {
            return CompletableFuture.completedFuture(
                    TransactionResult.failure(TransactionResult.Status.INVALID_AMOUNT, balance(from, currency)));
        }
        return repository.transfer(from, to, currency, amount, reason, seasonId)
                .thenApply(result -> {
                    if (result.ok()) {
                        adjust(from, currency, -amount);
                        adjust(to, currency, amount);
                    }
                    return result;
                });
    }

    /** Gives a new player their starting balances (spec §12). */
    public CompletableFuture<Void> grantStartingBalances(UUID player) {
        var cfg = config.get();
        var money = cfg.startingMoney();
        CompletableFuture<?> all = CompletableFuture.completedFuture(null);
        if (money > 0) {
            all = deposit(player, Currency.MONEY, money, "starting-balance");
        }
        if (cfg.startingTokens() > 0) {
            all = all.thenCompose(ignored -> deposit(player, Currency.TOKENS, cfg.startingTokens(), "starting-balance"));
        }
        if (cfg.startingGems() > 0) {
            all = all.thenCompose(ignored -> deposit(player, Currency.GEMS, cfg.startingGems(), "starting-balance"));
        }
        return all.thenApply(ignored -> null);
    }

    @Override
    public String format(Currency currency, long amount) {
        var formatted = new DecimalFormat("#,##0").format(amount);
        // Money leads with its symbol ($1,640); tokens and gems trail with their name.
        return currency == Currency.MONEY
                ? currency.label() + formatted
                : formatted + " " + currency.label();
    }

    /** Amount validation lives here so every path refuses the same things (#4, spec §74). */
    private TransactionResult rejectBadAmount(UUID player, Currency currency, long amount) {
        if (amount <= 0) {
            return TransactionResult.failure(TransactionResult.Status.INVALID_AMOUNT, balance(player, currency));
        }
        return null;
    }

    private TransactionResult remember(UUID player, Currency currency, TransactionResult result) {
        if (result.ok()) {
            cache.computeIfAbsent(player, p -> new EnumMap<>(Currency.class))
                    .put(currency, result.balanceAfter());
        }
        return result;
    }

    private void adjust(UUID player, Currency currency, long delta) {
        cache.computeIfAbsent(player, p -> new EnumMap<>(Currency.class))
                .merge(currency, delta, Long::sum);
    }

    /** True when this player already has balances; false means they have never been paid anything. */
    public boolean hasAccount(UUID player) {
        return cache.containsKey(player);
    }

    public int trackedPlayers() {
        return cache.size();
    }
}
