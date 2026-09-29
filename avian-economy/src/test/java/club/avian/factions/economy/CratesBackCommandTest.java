package club.avian.factions.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A crate preview's Menu button: book rolls go back to the Enchanter, everything else to /crates. */
class CratesBackCommandTest {

    @Test
    void bookRollsGoBackToTheEnchanter() {
        for (var crate : new String[] {"EnchantCommon", "EnchantRare", "EnchantLegendary", "EnchantMythic"}) {
            assertEquals("enchanter", CratesBackCommand.menuFor(crate));
        }
    }

    @Test
    void otherCratesGoToTheCratesMenu() {
        for (var crate : new String[] {"Vote", "Common", "Rare", "Epic", "Legendary", "Mythic"}) {
            assertEquals("crates", CratesBackCommand.menuFor(crate));
        }
    }
}
