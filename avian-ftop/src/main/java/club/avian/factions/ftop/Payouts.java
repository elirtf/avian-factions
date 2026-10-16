package club.avian.factions.ftop;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

/**
 * F-Top payouts' arithmetic, with no Bukkit in it: when the last weekly payout fell due, and how each placed faction's
 * prize splits between its members (owner, 2026-10-16: equally, online or not).
 */
final class Payouts {

    private Payouts() {
    }

    /** A member to pay: their id for money and tokens, their name for commands. */
    record Member(UUID id, String name) {}

    /** What one placed faction gets: each member's share of the money and tokens, and the commands for each. */
    record Award(int place, Ranking.Owner faction, long value, List<Member> members, long moneyEach, long tokensEach,
                 List<String> commands) {}

    /** The most recent weekly payout time at or before {@code now}. */
    static Instant lastDue(Instant now, DayOfWeek day, LocalTime time, ZoneId zone) {
        ZonedDateTime local = now.atZone(zone);
        ZonedDateTime candidate = local.with(TemporalAdjusters.previousOrSame(day)).with(time);
        if (candidate.isAfter(local)) {
            candidate = candidate.minusWeeks(1);
        }
        return candidate.toInstant();
    }

    /** The payout's period key: its date in the payout time zone, e.g. 2026-10-18. */
    static String period(Instant due, ZoneId zone) {
        return due.atZone(zone).toLocalDate().toString();
    }

    /** The awards for the top places, skipping factions with no members (nothing to split). */
    static List<Award> plan(Ranking ranking, List<SeasonConfig.Prize> prizes, IntFunction<List<Member>> membersOf) {
        var awards = new ArrayList<Award>();
        int place = 0;
        for (var entry : ranking.entries()) {
            if (place >= prizes.size()) {
                break;
            }
            var members = membersOf.apply(entry.owner().id());
            if (members.isEmpty() || entry.total() <= 0) {
                continue;
            }
            var prize = prizes.get(place);
            place++;
            awards.add(new Award(place, entry.owner(), entry.total(), members, prize.money() / members.size(),
                    prize.tokens() / members.size(), prize.commands()));
        }
        return awards;
    }
}
