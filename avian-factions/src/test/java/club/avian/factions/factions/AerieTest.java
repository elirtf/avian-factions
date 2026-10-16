package club.avian.factions.factions;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Aerie's rules: 5 minutes alone to capture, anyone else pauses it, leaving drops it; hourly rewards; 8h milestone. */
class AerieTest {

    private static final long HOUR = 3_600_000L;
    private static final int RAVENS = 1;
    private static final int HAWKS = 2;
    private long now = 0;
    private final Aerie aerie = new Aerie(300, new long[] {HOUR, 2 * HOUR}, 8 * HOUR, null, 0);

    private List<Aerie.Event> run(int seconds, Set<Object> teams) {
        var all = new ArrayList<Aerie.Event>();
        for (int i = 0; i < seconds; i++) {
            now += 1000;
            all.addAll(aerie.tick(teams, now));
        }
        return all;
    }

    @Test
    void fiveMinutesAloneCaptures() {
        var events = run(299, Set.of(RAVENS));
        assertTrue(events.contains(new Aerie.CaptureStarted(RAVENS)));
        assertTrue(events.contains(new Aerie.Halfway(RAVENS)));
        assertTrue(events.contains(new Aerie.OneMinuteLeft(RAVENS)));
        assertNull(aerie.holder());
        assertEquals(List.of(new Aerie.Captured(RAVENS, null)), run(1, Set.of(RAVENS)));
        assertEquals(RAVENS, aerie.holder());
    }

    @Test
    void anEnemyPausesTheCaptureAndItResumes() {
        run(100, Set.of(RAVENS));
        var contested = run(30, Set.of(RAVENS, HAWKS));
        assertEquals(List.of(new Aerie.Contested(RAVENS)), contested, "announced once, not every second");
        assertEquals(100, aerie.progress(), "paused, not reset");
        run(200, Set.of(RAVENS));
        assertEquals(RAVENS, aerie.holder());
    }

    @Test
    void aFactionlessPlayerContestsButCantCapture() {
        UUID loner = UUID.randomUUID();
        run(50, Set.of(RAVENS));
        run(10, Set.of(RAVENS, loner));
        assertTrue(aerie.contested());
        assertTrue(run(400, Set.of(loner)).contains(new Aerie.CaptureFailed(RAVENS)));
        assertNull(aerie.holder());
        assertNull(aerie.capturer());
    }

    @Test
    void leavingDropsTheCapture() {
        run(250, Set.of(RAVENS));
        assertEquals(List.of(new Aerie.CaptureFailed(RAVENS)), run(1, Set.of()));
        assertEquals(0, aerie.progress());
    }

    @Test
    void theHolderStandingAloneDropsAnAttack() {
        aerie.forceCapture(RAVENS, now);
        run(100, Set.of(HAWKS));
        assertTrue(run(1, Set.of(RAVENS)).contains(new Aerie.CaptureFailed(HAWKS)));
        assertEquals(RAVENS, aerie.holder());
    }

    @Test
    void anotherFactionTakesItFromTheHolder() {
        aerie.forceCapture(RAVENS, now);
        var events = run(300, Set.of(HAWKS));
        assertTrue(events.contains(new Aerie.Captured(HAWKS, RAVENS)));
        assertEquals(HAWKS, aerie.holder());
    }

    @Test
    void rewardsComeEveryHourAndEveryTwoHours() {
        aerie.forceCapture(RAVENS, now);
        var events = run(2 * 3600, Set.of());
        assertEquals(2, events.stream().filter(e -> e.equals(new Aerie.RewardDue(RAVENS, 0))).count());
        assertEquals(1, events.stream().filter(e -> e.equals(new Aerie.RewardDue(RAVENS, 1))).count());
    }

    @Test
    void eightHoursHeldEarnsTheMilestoneOnce() {
        aerie.forceCapture(RAVENS, now);
        var events = run(9 * 3600, Set.of());
        assertEquals(1, events.stream().filter(e -> e.equals(new Aerie.Milestone(RAVENS))).count());
        assertTrue(aerie.hold().milestoneDone());
    }

    @Test
    void losingItStartsTheMilestoneClockOver() {
        aerie.forceCapture(RAVENS, now);
        run(7 * 3600, Set.of());
        aerie.forceCapture(HAWKS, now);
        aerie.forceCapture(RAVENS, now);
        assertFalse(run(7 * 3600, Set.of()).contains(new Aerie.Milestone(RAVENS)));
    }

    @Test
    void aRestoredHoldPaysOnItsOldScheduleWithoutBackPay() {
        // Held since t=0; the server comes back at 2.5 h: the next hourly payout is at 3 h, not a pile of missed ones.
        var restored = new Aerie(300, new long[] {HOUR}, 0, new Aerie.Hold(RAVENS, 0, false), (long) (2.5 * HOUR));
        assertTrue(restored.tick(Set.of(), (long) (2.9 * HOUR)).isEmpty());
        assertEquals(List.of(new Aerie.RewardDue(RAVENS, 0)), restored.tick(Set.of(), 3 * HOUR));
    }

    @Test
    void releaseFreesIt() {
        aerie.forceCapture(RAVENS, now);
        assertEquals(List.of(new Aerie.Released(RAVENS)), aerie.release());
        assertNull(aerie.holder());
    }
}
