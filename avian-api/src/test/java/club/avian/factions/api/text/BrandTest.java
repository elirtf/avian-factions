package club.avian.factions.api.text;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The brand tags resolve to colours and never leak as text. */
class BrandTest {

    @Test
    void brandTagsBecomeColoursNotText() {
        var line = Brand.mm("<cane>+64 cane</cane>");
        assertEquals("+64 cane", PlainTextComponentSerializer.plainText().serialize(line));
        assertEquals(TextColor.fromHexString("#7CFF4F"), line.children().isEmpty() ? line.color() : line.children().getFirst().color());
    }

    @Test
    void neverItalic() {
        assertEquals(TextDecoration.State.FALSE, Brand.mm("<soft>lore</soft>").decoration(TextDecoration.ITALIC));
    }

    @Test
    void barShowsLevelOutOfMax() {
        assertEquals("■■■■■", PlainTextComponentSerializer.plainText().serialize(Brand.mm(Brand.bar(2, 5))));
        assertEquals("", PlainTextComponentSerializer.plainText().serialize(Brand.mm(Brand.bar(0, 0))));
    }
}
