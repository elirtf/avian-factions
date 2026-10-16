package club.avian.factions.ftop;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Weekly payouts fall due Sundays 21:00 Central; each place's prize splits equally between the members. */
class PayoutsTest {

    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");

    private static Instant at(String local) {
        return java.time.LocalDateTime.parse(local).atZone(CHICAGO).toInstant();
    }

    @Test
    void theLastDuePayoutIsThisSundayOnceItsTimeHasPassed() {
        // 2026-10-18 is a Sunday.
        assertEquals(at("2026-10-11T21:00"), Payouts.lastDue(at("2026-10-18T20:59"), DayOfWeek.SUNDAY, LocalTime.of(21, 0), CHICAGO));
        assertEquals(at("2026-10-18T21:00"), Payouts.lastDue(at("2026-10-18T21:00"), DayOfWeek.SUNDAY, LocalTime.of(21, 0), CHICAGO));
        assertEquals(at("2026-10-18T21:00"), Payouts.lastDue(at("2026-10-21T08:00"), DayOfWeek.SUNDAY, LocalTime.of(21, 0), CHICAGO));
        assertEquals("2026-10-18", Payouts.period(at("2026-10-18T21:00"), CHICAGO));
    }

    private static Payouts.Member member(String name) {
        return new Payouts.Member(UUID.randomUUID(), name);
    }

    private static Ranking.Entry entry(int id, String tag, long total) {
        return new Ranking.Entry(id, new Ranking.Owner(id, tag), total, total, 0, Map.of());
    }

    private final List<SeasonConfig.Prize> prizes = List.of(
            new SeasonConfig.Prize(1_000_000, 2_500, List.of("key {player}")),
            new SeasonConfig.Prize(500_000, 1_500, List.of()),
            new SeasonConfig.Prize(250_000, 750, List.of()));

    @Test
    void eachPlaceSplitsItsPrizeBetweenItsMembers() {
        var ranking = new Ranking(List.of(entry(1, "Ravens", 9_000_000), entry(2, "Hawks", 5_000_000),
                entry(3, "Owls", 1_000_000), entry(4, "Crows", 500_000)), Instant.EPOCH);
        var members = Map.of(1, List.of(member("a"), member("b"), member("c"), member("d")),
                2, List.of(member("e")), 3, List.of(member("f"), member("g")), 4, List.of(member("h")));
        var awards = Payouts.plan(ranking, prizes, members::get);

        assertEquals(3, awards.size(), "only the top 3 are paid");
        assertEquals(250_000, awards.get(0).moneyEach(), "$1M split four ways");
        assertEquals(625, awards.get(0).tokensEach());
        assertEquals(500_000, awards.get(1).moneyEach(), "a solo faction gets it all");
        assertEquals(125_000, awards.get(2).moneyEach());
        assertEquals("Owls", awards.get(2).faction().tag());
    }

    @Test
    void factionsWithNoMembersOrNoValueAreSkipped() {
        var ranking = new Ranking(List.of(entry(1, "Ghosts", 9_000_000), entry(2, "Hawks", 5_000_000),
                entry(3, "Empty", 0)), Instant.EPOCH);
        var members = Map.of(1, List.<Payouts.Member>of(), 2, List.of(member("e")), 3, List.of(member("x")));
        var awards = Payouts.plan(ranking, prizes, members::get);
        assertEquals(1, awards.size());
        assertEquals("Hawks", awards.get(0).faction().tag());
        assertEquals(1, awards.get(0).place(), "Hawks takes #1's prize");
    }
}
