package club.avian.factions.economy;

/**
 * One running gen block's rules, with no Bukkit in them (#gen blocks, owner 2026-10-16). Each step looks at the
 * next spot in the line: air gets the block (if the player can pay and may build there); a block of the gen's own
 * kind is passed over for free, so a line joins up with blocks placed earlier; anything else ends the run, as do
 * the length limit, the world's edge and an empty wallet.
 */
final class GenRun {

    /** What is at the next spot. */
    enum Spot { AIR, SAME, OTHER, OUTSIDE }

    /** Why a run ended. */
    enum End {
        BLOCKED("it reached a block"),
        EDGE("it reached the edge of the world"),
        LENGTH("it reached its full length"),
        MONEY("you ran out of money"),
        PROTECTED("you can't build there"),
        GONE("you left");

        final String why;

        End(String why) {
            this.why = why;
        }
    }

    /** The world, as far as a run needs it. */
    interface World {
        Spot spot(int step);

        /** Lays the block at that step; false if the player may not build there (nothing is left behind). */
        boolean place(int step);

        long balance();
    }

    private final long cost;
    private final int maxLength;
    private int step;
    private int placed;
    private long owed;
    private End end;

    GenRun(long cost, int maxLength) {
        this.cost = cost;
        this.maxLength = maxLength;
    }

    /** Advances up to {@code budget} spots; returns the end once there is one. */
    End advance(World world, int budget) {
        for (int i = 0; i < budget && end == null; i++) {
            if (step >= maxLength) {
                return end = End.LENGTH;
            }
            switch (world.spot(step)) {
                case SAME -> step++;
                case OTHER -> end = End.BLOCKED;
                case OUTSIDE -> end = End.EDGE;
                case AIR -> {
                    if (world.balance() - owed < cost) {
                        end = End.MONEY;
                    } else if (!world.place(step)) {
                        end = End.PROTECTED;
                    } else {
                        owed += cost;
                        placed++;
                        step++;
                    }
                }
            }
        }
        return end;
    }

    void stop(End why) {
        if (end == null) {
            end = why;
        }
    }

    End end() {
        return end;
    }

    int placed() {
        return placed;
    }

    /** Money laid but not yet withdrawn; taking it resets it. */
    long takeOwed() {
        long o = owed;
        owed = 0;
        return o;
    }

    long owed() {
        return owed;
    }
}
