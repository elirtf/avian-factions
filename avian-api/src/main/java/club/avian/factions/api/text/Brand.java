package club.avian.factions.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;


/**
 * The look of everything custom: bright, colourful, readable. Menus, item names, lore and messages
 * are written in MiniMessage with these colours, so every feature shares one palette.
 *
 * <p>Use the tags inline, e.g. {@code Brand.mm("<cane>+64 cane</cane>  <token>+1 token</token>")}.
 */
public final class Brand {

    /** Named colours, usable as MiniMessage tags: {@code <gold>…</gold>} is ours, not vanilla's. */
    private static final MiniMessage MM = MiniMessage.builder()
            .editTags(tags -> tags
                    .resolver(color("sun", "#FFD23F"))        // headline gold
                    .resolver(color("cane", "#7CFF4F"))       // sugar cane green
                    .resolver(color("money", "#4CE08A"))      // money
                    .resolver(color("token", "#FFE66D"))      // tokens
                    .resolver(color("xp", "#5FF3E0"))         // skill XP
                    .resolver(color("gem", "#C58CFF"))        // gems, rare things
                    .resolver(color("hot", "#FF7A45"))        // attention, costs
                    .resolver(color("bad", "#FF5C5C"))        // refusals, off
                    .resolver(color("soft", "#B8B8C8"))       // body text
                    .resolver(color("dim", "#6E6E80")))       // hints
            .build();

    /** Fills for progress bars. */
    public static final String BAR_ON = "<cane>■</cane>";
    public static final String BAR_OFF = "<dim>■</dim>";

    private Brand() {
    }

    private static net.kyori.adventure.text.minimessage.tag.resolver.TagResolver color(String name, String hex) {
        return net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.resolver(name,
                net.kyori.adventure.text.minimessage.tag.Tag.styling(net.kyori.adventure.text.format.TextColor.fromHexString(hex)));
    }

    /** MiniMessage with the brand colours, never italic (item lore is italic by default). */
    public static Component mm(String miniMessage) {
        return MM.deserialize(miniMessage).decoration(TextDecoration.ITALIC, false);
    }

    /** A level bar: {@code ■■■□□} in cane green and dim grey. */
    public static String bar(int level, int max) {
        return BAR_ON.repeat(Math.max(0, level)) + BAR_OFF.repeat(Math.max(0, max - level));
    }

    /** Title-style gradient text, e.g. menu titles and item names. */
    public static Component title(String text) {
        return mm("<bold><gradient:#FFD23F:#FF7A45>" + text + "</gradient></bold>");
    }
}
