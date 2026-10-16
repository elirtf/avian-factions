package club.avian.factions.economy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Gen blocks: fill air, pass over their own block, stop at anything else, at the edge, at full length or broke. */
class GenRunTest {

    /** A line of spots; placing turns AIR into SAME. */
    private static final class Line implements GenRun.World {
        final List<GenRun.Spot> spots;
        long money;
        int protectedFrom = Integer.MAX_VALUE;

        Line(long money, GenRun.Spot... spots) {
            this.money = money;
            this.spots = new ArrayList<>(List.of(spots));
        }

        public GenRun.Spot spot(int step) {
            return step < spots.size() ? spots.get(step) : GenRun.Spot.OUTSIDE;
        }

        public boolean place(int step) {
            if (step >= protectedFrom) {
                return false;
            }
            spots.set(step, GenRun.Spot.SAME);
            return true;
        }

        public long balance() {
            return money;
        }
    }

    private static final GenRun.Spot A = GenRun.Spot.AIR, S = GenRun.Spot.SAME, O = GenRun.Spot.OTHER;

    private static GenRun runAll(GenRun run, Line line) {
        while (run.advance(line, 1) == null) { }
        return run;
    }

    @Test
    void fillsAirUntilAnotherBlock() {
        var run = runAll(new GenRun(6, 100), new Line(1000, A, A, A, O, A));
        assertEquals(GenRun.End.BLOCKED, run.end());
        assertEquals(3, run.placed());
        assertEquals(18, run.owed());
    }

    @Test
    void passesOverItsOwnBlockForFree() {
        var run = runAll(new GenRun(6, 100), new Line(1000, A, S, S, A, O));
        assertEquals(GenRun.End.BLOCKED, run.end());
        assertEquals(2, run.placed(), "the two sand blocks already there cost nothing");
    }

    @Test
    void stopsWhenTheMoneyRunsOut() {
        var run = runAll(new GenRun(32, 100), new Line(100, A, A, A, A, A));
        assertEquals(GenRun.End.MONEY, run.end());
        assertEquals(3, run.placed(), "$100 buys three $32 obsidian");
    }

    @Test
    void stopsAtFullLengthAndAtTheEdge() {
        assertEquals(GenRun.End.LENGTH, runAll(new GenRun(1, 3), new Line(1000, A, A, A, A)).end());
        assertEquals(GenRun.End.EDGE, runAll(new GenRun(1, 100), new Line(1000, A, A)).end());
    }

    @Test
    void stopsWhereThePlayerMayNotBuild() {
        var line = new Line(1000, A, A, A, A);
        line.protectedFrom = 2;
        var run = runAll(new GenRun(1, 100), line);
        assertEquals(GenRun.End.PROTECTED, run.end());
        assertEquals(2, run.placed());
    }

    @Test
    void owedMoneyIsTakenOnce() {
        var run = runAll(new GenRun(6, 100), new Line(1000, A, A, O));
        assertEquals(12, run.takeOwed());
        assertEquals(0, run.takeOwed());
    }
}
