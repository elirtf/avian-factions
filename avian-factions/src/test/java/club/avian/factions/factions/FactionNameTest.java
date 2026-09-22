package club.avian.factions.factions;

import org.junit.jupiter.api.Test;

import static club.avian.factions.factions.FactionName.Problem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Spec §7: names are validated for length, characters, reserved words and duplicates. */
class FactionNameTest {

    final FactionsConfig.Names rules = new FactionsConfig().names();   // defaults: 3-16, reserved list

    private Problem check(String name) {
        return FactionName.validate(name, rules);
    }

    @Test
    void acceptsOrdinaryNames() {
        assertNull(check("Ravens"));
        assertNull(check("the_hawks"));
        assertNull(check("Clan99"));
        assertNull(check("abc"));
        assertNull(check("0123456789abcdef"));   // exactly 16
    }

    @Test
    void rejectsByLength() {
        assertEquals(Problem.TOO_SHORT, check("ab"));
        assertEquals(Problem.TOO_SHORT, check(""));
        assertEquals(Problem.TOO_LONG, check("0123456789abcdefg"));
    }

    @Test
    void rejectsIllegalCharacters() {
        assertEquals(Problem.ILLEGAL_CHARACTERS, check("Ravens!"));
        assertEquals(Problem.ILLEGAL_CHARACTERS, check("two words"));
        assertEquals(Problem.ILLEGAL_CHARACTERS, check("clan-x"));
        assertEquals(Problem.ILLEGAL_CHARACTERS, check("ráven"));
        // Colour codes are characters like any other; they must not slip through.
        assertEquals(Problem.ILLEGAL_CHARACTERS, check("&cRed"));
    }

    @Test
    void rejectsReservedNamesRegardlessOfCase() {
        assertEquals(Problem.RESERVED, check("wilderness"));
        assertEquals(Problem.RESERVED, check("WarZone"));
        assertEquals(Problem.RESERVED, check("SAFEZONE"));
        assertEquals(Problem.RESERVED, check("Avian"));
    }

    @Test
    void lengthIsCheckedBeforeCharacters() {
        // A too-long name full of illegal characters reports the length, the more useful message.
        assertEquals(Problem.TOO_LONG, check("!!!!!!!!!!!!!!!!!!!!"));
    }

    @Test
    void keyIsCaseInsensitiveAndDrivesDuplicateDetection() {
        assertEquals(FactionName.key("Ravens"), FactionName.key("RAVENS"));
        assertEquals("ravens", FactionName.key("Ravens"));
    }
}
