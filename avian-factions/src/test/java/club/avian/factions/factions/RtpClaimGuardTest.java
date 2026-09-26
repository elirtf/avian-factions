package club.avian.factions.factions;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** /rtp candidates inside faction land are refused, so BetterRTP rolls again (#45). */
class RtpClaimGuardTest {

    static final class Candidate implements Cancellable {
        boolean cancelled;
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void setCancelled(boolean cancel) { cancelled = cancel; }
    }

    final RtpClaimGuard guard = new RtpClaimGuard(location -> location.getX() < 0);   // "claimed" west of x=0

    @Test
    void aSpotInsideAClaimIsRefused() {
        var event = new Candidate();
        assertTrue(guard.veto(new Location(null, -5000, 64, 0), event));
        assertTrue(event.isCancelled());
    }

    @Test
    void aWildernessSpotIsKept() {
        var event = new Candidate();
        assertFalse(guard.veto(new Location(null, 5000, 64, 0), event));
        assertFalse(event.isCancelled());
    }

    @Test
    void noCandidateIsLeftAlone() {
        var event = new Candidate();
        assertFalse(guard.veto(null, event));
    }
}
