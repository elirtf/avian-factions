package club.avian.factions.combat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatTagsTest {

    long now = 1_000_000;
    final CombatTags tags = new CombatTags(() -> now);
    final UUID player = UUID.randomUUID();

    @Test
    void aTagLastsItsSecondsThenRunsOut() {
        assertTrue(tags.tag(player, 20));
        now += 19_999;
        assertTrue(tags.isTagged(player));
        now += 1;
        assertFalse(tags.isTagged(player));
        assertEquals(List.of(player), tags.expire());
        assertEquals(List.of(), tags.expire());
    }

    @Test
    void anotherHitRestartsTheTagAtFullLength() {
        tags.tag(player, 20);
        now += 15_000;
        assertFalse(tags.tag(player, 20), "already in combat");
        assertEquals(20_000, tags.remainingMillis(player));
    }

    @Test
    void clearingEndsItAtOnce() {
        tags.tag(player, 20);
        tags.clear(player);
        assertFalse(tags.isTagged(player));
        assertEquals(List.of(), tags.tagged());
    }
}
