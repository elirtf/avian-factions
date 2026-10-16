package club.avian.factions.ftop;

import club.avian.factions.api.database.Database;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Seasons, payout runs, payouts and frozen rankings (V202610161600__ftop_seasons.sql). */
final class SeasonRepository {

    /** A season: its number, when it started and ends, and whether it's still running. */
    record Season(int id, Instant startedAt, Instant endsAt, boolean active) {}

    private final Database db;

    SeasonRepository(Database db) {
        this.db = db;
    }

    /** The newest season, or null if none was ever started. Boot only. */
    Season latestDuringBoot() {
        return db.queryDuringBoot(c -> {
            try (var ps = c.prepareStatement("SELECT id, started_at, ends_at, status FROM seasons ORDER BY id DESC LIMIT 1");
                 var rs = ps.executeQuery()) {
                return rs.next() ? new Season(rs.getInt(1), rs.getTimestamp(2).toInstant(), rs.getTimestamp(3).toInstant(),
                        "ACTIVE".equals(rs.getString(4))) : null;
            }
        });
    }

    CompletableFuture<Void> start(Season season) {
        return db.executeInOrder("seasons", c -> {
            try (var ps = c.prepareStatement("INSERT INTO seasons (id, started_at, ends_at, status) VALUES (?, ?, ?, 'ACTIVE')")) {
                ps.setInt(1, season.id());
                ps.setTimestamp(2, Timestamp.from(season.startedAt()));
                ps.setTimestamp(3, Timestamp.from(season.endsAt()));
                ps.executeUpdate();
            }
        });
    }

    CompletableFuture<Void> end(int season, Instant at, List<Ranking.Entry> finalRanking) {
        return db.executeInOrder("seasons", c -> {
            try (var ps = c.prepareStatement("UPDATE seasons SET status = 'ENDED', ended_at = ? WHERE id = ?")) {
                ps.setTimestamp(1, Timestamp.from(at));
                ps.setInt(2, season);
                ps.executeUpdate();
            }
            try (var ps = c.prepareStatement(
                    "INSERT INTO season_rankings (season_id, place, faction_id, faction_tag, value) VALUES (?, ?, ?, ?, ?)")) {
                int place = 0;
                for (var e : finalRanking) {
                    ps.setInt(1, season);
                    ps.setInt(2, ++place);
                    ps.setInt(3, e.owner().id());
                    ps.setString(4, e.owner().tag());
                    ps.setLong(5, e.total());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        });
    }

    /** Claims a payout run; false if it already ran (or is running). Only a true claim may pay. */
    CompletableFuture<Boolean> claim(int season, String kind, String period, Instant at) {
        return db.query(c -> {
            try (var ps = c.prepareStatement("INSERT INTO ftop_payout_runs (season_id, kind, period, run_at) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, season);
                ps.setString(2, kind);
                ps.setString(3, period);
                ps.setTimestamp(4, Timestamp.from(at));
                ps.executeUpdate();
                return true;
            } catch (SQLIntegrityConstraintViolationException duplicate) {
                return false;
            }
        });
    }

    CompletableFuture<Void> record(int season, String kind, String period, Payouts.Award a, Instant at) {
        return db.executeInOrder("payouts", c -> {
            try (var ps = c.prepareStatement("""
                    INSERT INTO ftop_payouts (season_id, kind, period, place, faction_id, faction_tag, value, members,
                        money_each, tokens_each, paid_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")) {
                ps.setInt(1, season);
                ps.setString(2, kind);
                ps.setString(3, period);
                ps.setInt(4, a.place());
                ps.setInt(5, a.faction().id());
                ps.setString(6, a.faction().tag());
                ps.setLong(7, a.value());
                ps.setInt(8, a.members().size());
                ps.setLong(9, a.moneyEach());
                ps.setLong(10, a.tokensEach());
                ps.setTimestamp(11, Timestamp.from(at));
                ps.executeUpdate();
            }
        });
    }
}
