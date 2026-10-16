package club.avian.factions.ftop;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.text.Brand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.plugin.Plugin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Seasons and F-Top payouts (spec §50-52; owner, 2026-10-16). Inert until {@code /season start}: with no season
 * running, nothing is ever paid (the dev server). While one runs, a check every minute pays the weekly payout once it
 * falls due, and ends the season when its time is up: a last F-Top, the season-end payout, the final ranking frozen.
 * Every payout is claimed in the database before anything is paid, so a restart or a second tick can't pay twice.
 */
final class SeasonService {

    static final String WEEKLY = "WEEKLY";
    static final String SEASON = "SEASON";

    private final Plugin plugin;
    private final ConfigHandle<SeasonConfig> config;
    private final SeasonRepository repository;
    private final Supplier<Ranking> recalculate;
    private final IntFunction<List<Payouts.Member>> membersOf;
    private final Economy economy;
    private final Clock clock;
    private final Logger log;
    private final Set<String> inFlight = new HashSet<>();
    private SeasonRepository.Season season;

    SeasonService(Plugin plugin, ConfigHandle<SeasonConfig> config, SeasonRepository repository, Supplier<Ranking> recalculate,
                  IntFunction<List<Payouts.Member>> membersOf, Economy economy, Clock clock, Logger log) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.recalculate = recalculate;
        this.membersOf = membersOf;
        this.economy = economy;
        this.clock = clock;
        this.log = log;
    }

    void load() {
        season = repository.latestDuringBoot();
        log.info(season == null ? "No season has started yet: F-Top payouts are off (/season start)"
                : season.active() ? "Season " + season.id() + " runs until " + season.endsAt()
                : "Season " + season.id() + " has ended; /season start begins the next");
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 60, 20L * 60);
    }

    SeasonRepository.Season season() {
        return season;
    }

    /** Starts season {@code id} now, running for the configured weeks. */
    SeasonRepository.Season start(int id) {
        var now = clock.instant();
        season = new SeasonRepository.Season(id, now, now.plus(Duration.ofDays(7L * config.get().lengthWeeks())), true);
        repository.start(season).exceptionally(e -> {
            log.severe("Could not record season " + id + " starting: " + e);
            return null;
        });
        broadcast("<sun>🏆</sun> <bold><gradient:#FFD23F:#FF8A3D>Season " + id + " has begun!</gradient></bold> "
                + "<soft>The top 3 factions on</soft> <white>/ftop</white> <soft>are paid every week, and far more at the end.</soft>");
        return season;
    }

    /** Ends the running season now (or when its time is up): final payout, ranking frozen. */
    void end() {
        if (season == null || !season.active()) {
            return;
        }
        var s = season;
        season = new SeasonRepository.Season(s.id(), s.startedAt(), s.endsAt(), false);
        payout(s.id(), SEASON, "final", config.get().seasonEnd(), ranking -> repository.end(s.id(), clock.instant(), ranking.entries())
                .exceptionally(e -> {
                    log.severe("Could not archive season " + s.id() + "'s final ranking: " + e);
                    return null;
                }));
    }

    void tick() {
        var s = season;
        if (s == null || !s.active()) {
            return;
        }
        var cfg = config.get();
        var now = clock.instant();
        var due = Payouts.lastDue(now, cfg.weeklyDay(), cfg.weeklyTime(), cfg.timeZone());
        if (!due.isBefore(s.startedAt()) && due.isBefore(s.endsAt())) {
            payout(s.id(), WEEKLY, Payouts.period(due, cfg.timeZone()), cfg.weekly(), r -> { });
        }
        if (!now.isBefore(s.endsAt())) {
            end();
        }
    }

    /** What the next weekly payout would pay with today's F-Top: for /season preview. Pays nothing. */
    List<Payouts.Award> preview() {
        return Payouts.plan(recalculate.get(), config.get().weekly(), membersOf);
    }

    private void payout(int seasonId, String kind, String period, List<SeasonConfig.Prize> prizes,
                        java.util.function.Consumer<Ranking> afterRanking) {
        var key = seasonId + "/" + kind + "/" + period;
        if (!inFlight.add(key)) {
            return;
        }
        repository.claim(seasonId, kind, period, clock.instant()).whenComplete((claimed, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (error != null) {
                        inFlight.remove(key);   // try again next minute
                        log.warning("F-Top payout " + key + ": could not claim it: " + error);
                        return;
                    }
                    if (!claimed) {
                        return;   // already paid; stays in inFlight so it isn't asked again
                    }
                    var ranking = recalculate.get();
                    afterRanking.accept(ranking);
                    pay(seasonId, kind, period, Payouts.plan(ranking, prizes, membersOf));
                }));
    }

    private void pay(int seasonId, String kind, String period, List<Payouts.Award> awards) {
        var now = clock.instant();
        String title = SEASON.equals(kind) ? "Season " + seasonId + " is over! Final F-Top payout"
                : "Weekly F-Top payout";
        broadcast("<sun>🏆</sun> <bold><gradient:#FFD23F:#FF8A3D>" + title + "</gradient></bold>");
        if (awards.isEmpty()) {
            broadcast("<soft>No faction is on the F-Top yet: nothing to pay.</soft>");
        }
        for (var a : awards) {
            for (var m : a.members()) {
                if (a.moneyEach() > 0) {
                    economy.deposit(m.id(), Currency.MONEY, a.moneyEach(), "ftop:" + kind.toLowerCase() + ":" + period);
                }
                if (a.tokensEach() > 0) {
                    economy.deposit(m.id(), Currency.TOKENS, a.tokensEach(), "ftop:" + kind.toLowerCase() + ":" + period);
                }
                for (var command : a.commands()) {
                    var line = command.replace("{player}", m.name()).replace("{season}", String.valueOf(seasonId));
                    if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line)) {
                        log.warning("F-Top payout: command failed: " + line);
                    }
                }
                var online = Bukkit.getPlayer(m.id());
                if (online != null) {
                    online.sendMessage(Brand.mm("<sun>🏆</sun> <soft>Your faction placed</soft> <sun><bold>#" + a.place()
                            + "</bold></sun><soft>! Your share:</soft> <money>" + economy.format(Currency.MONEY, a.moneyEach())
                            + "</money> <soft>+</soft> <token>" + economy.format(Currency.TOKENS, a.tokensEach()) + "</token>"
                            + (a.commands().isEmpty() ? "" : " <soft>+ keys (/keys)</soft>")));
                    online.playSound(online.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1f);
                }
            }
            broadcast("  <sun>#" + a.place() + "</sun> <white><bold>" + MiniMessage.miniMessage().escapeTags(a.faction().tag())
                    + "</bold></white> <soft>(" + economy.format(Currency.MONEY, a.value()) + " F-Top): each of its "
                    + a.members().size() + " members gets</soft> <money>" + economy.format(Currency.MONEY, a.moneyEach())
                    + "</money> <soft>and</soft> <token>" + economy.format(Currency.TOKENS, a.tokensEach()) + "</token>");
            repository.record(seasonId, kind, period, a, now).exceptionally(e -> {
                log.warning("F-Top payout record failed: " + e);
                return null;
            });
            log.info("F-Top payout " + seasonId + "/" + kind + "/" + period + " #" + a.place() + " " + a.faction().tag()
                    + ": " + a.members().size() + " members, $" + a.moneyEach() + " + " + a.tokensEach() + " tokens each");
        }
    }

    private static void broadcast(String mm) {
        Bukkit.broadcast(Brand.mm(mm));
    }
}
