package club.avian.factions.factions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The Aerie's rules (#21), with no Bukkit in them: a fixed zone any faction can capture at any hour. A faction
 * standing in it alone for {@code captureSeconds} takes it; anyone else inside pauses the capture; the capturing
 * faction leaving drops it. The holder keeps it, and its rewards keep coming, until someone else captures it.
 *
 * <p>Ticked once a second with the teams standing inside: a faction id ({@link Integer}) or, for a player in no
 * faction, their UUID. A factionless player can't capture, but does contest.
 */
final class Aerie {

    sealed interface Event {}

    /** A faction began capturing. */
    record CaptureStarted(int faction) implements Event {}

    /** Someone else stepped in: the capture is paused (once per attempt). */
    record Contested(int faction) implements Event {}

    /** Half the capture time done. */
    record Halfway(int faction) implements Event {}

    /** One minute to go. */
    record OneMinuteLeft(int faction) implements Event {}

    /** The capturing faction left before finishing. */
    record CaptureFailed(int faction) implements Event {}

    /** Taken; {@code previous} is the faction that lost it, or null. */
    record Captured(int faction, Integer previous) implements Event {}

    /** Reward {@code index} of the holder's timed rewards is due. */
    record RewardDue(int faction, int index) implements Event {}

    /** The holder has held it {@code milestone} long without losing it (once per hold). */
    record Milestone(int faction) implements Event {}

    /** The holder stopped existing (disbanded) and the Aerie is free again. */
    record Released(int faction) implements Event {}

    /** What survives a restart: who holds it, since when, and whether this hold earned its milestone yet. */
    record Hold(int faction, long since, boolean milestoneDone) {}

    private final int captureSeconds;
    private final long[] rewardEveryMillis;
    private final long milestoneMillis;

    private Hold hold;
    private final long[] nextDue;

    private Integer capturer;
    private int progress;
    private boolean contested;

    Aerie(int captureSeconds, long[] rewardEveryMillis, long milestoneMillis, Hold restored, long now) {
        this.captureSeconds = captureSeconds;
        this.rewardEveryMillis = rewardEveryMillis.clone();
        this.milestoneMillis = milestoneMillis;
        this.nextDue = new long[rewardEveryMillis.length];
        if (restored != null) {
            hold = restored;
            // Payouts missed while the server was down are skipped; the next one falls where it would have.
            for (int i = 0; i < nextDue.length; i++) {
                long every = this.rewardEveryMillis[i];
                long periods = Math.max(1, (now - restored.since()) / every + 1);
                nextDue[i] = restored.since() + periods * every;
            }
        }
    }

    Integer holder() {
        return hold == null ? null : hold.faction();
    }

    Hold hold() {
        return hold;
    }

    Integer capturer() {
        return capturer;
    }

    int progress() {
        return progress;
    }

    int captureSeconds() {
        return captureSeconds;
    }

    boolean contested() {
        return contested;
    }

    /** One second passes with {@code teams} standing inside. */
    List<Event> tick(Set<Object> teams, long now) {
        var events = new ArrayList<Event>();
        Integer holder = holder();
        Object sole = teams.size() == 1 ? teams.iterator().next() : null;

        if (sole instanceof Integer faction && !faction.equals(holder)) {
            if (!faction.equals(capturer)) {
                if (capturer != null) {
                    events.add(new CaptureFailed(capturer));
                }
                capturer = faction;
                progress = 0;
                events.add(new CaptureStarted(faction));
            }
            contested = false;
            progress++;
            if (progress == captureSeconds / 2 && captureSeconds >= 120) {
                events.add(new Halfway(faction));
            }
            if (captureSeconds - progress == 60) {
                events.add(new OneMinuteLeft(faction));
            }
            if (progress >= captureSeconds) {
                capture(faction, now, events);
            }
        } else if (capturer != null && teams.contains(capturer)) {
            if (!contested) {
                contested = true;
                events.add(new Contested(capturer));
            }
        } else if (capturer != null) {
            events.add(new CaptureFailed(capturer));
            capturer = null;
            progress = 0;
            contested = false;
        }

        if (hold != null) {
            for (int i = 0; i < nextDue.length; i++) {
                if (now >= nextDue[i]) {
                    events.add(new RewardDue(hold.faction(), i));
                    nextDue[i] += rewardEveryMillis[i];
                }
            }
            if (!hold.milestoneDone() && milestoneMillis > 0 && now - hold.since() >= milestoneMillis) {
                hold = new Hold(hold.faction(), hold.since(), true);
                events.add(new Milestone(hold.faction()));
            }
        }
        return events;
    }

    /** An admin hands it to {@code faction} (or tests). */
    List<Event> forceCapture(int faction, long now) {
        var events = new ArrayList<Event>();
        capture(faction, now, events);
        return events;
    }

    /** Frees the Aerie: its holder disbanded, or an admin reset it (a new season). */
    List<Event> release() {
        var events = new ArrayList<Event>();
        if (hold != null) {
            events.add(new Released(hold.faction()));
        }
        hold = null;
        capturer = null;
        progress = 0;
        contested = false;
        return events;
    }

    private void capture(int faction, long now, List<Event> events) {
        Integer previous = holder();
        hold = new Hold(faction, now, false);
        for (int i = 0; i < nextDue.length; i++) {
            nextDue[i] = now + rewardEveryMillis[i];
        }
        capturer = null;
        progress = 0;
        contested = false;
        events.add(new Captured(faction, previous));
    }
}
