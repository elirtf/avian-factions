import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Avian sword re-skins, 16x16, original pixel art (style after fantasy RPG icon sets: tinted outlines,
 * 3-4 step ramps lit from the top-left, chunky readable shapes). One template of shade roles, a palette
 * per material, and a few per-material touches (chipped stone, patina, runes). Writes
 * <material>_sword.png — the vanilla names, so the pack replaces them.
 *
 *   java tools/item-art/Swords.java <out dir>
 */
public class Swords {
    static final int NONE = 0, EDGE = 1, LIGHT = 2, MID = 3, DARK = 4, FULLER = 5,
            GUARD_HI = 6, GUARD = 7, GUARD_LO = 8, GRIP_A = 9, GRIP_B = 10, GEM = 11, GEM_HI = 12, JEWEL = 13, OUTLINE = 14;

    record Material(String name, int outline, int edge, int light, int mid, int dark, int fuller,
                    int guardHi, int guard, int guardLo, int gripA, int gripB, int gem, int gemHi, boolean jewel) {}

    static final Material[] MATERIALS = {
        new Material("wooden",    0x2A1A10, 0xF6D6A0, 0xE0AE6E, 0xB8864A, 0x7A5230, 0x9C6E3C, 0x9A6A3A, 0x7A4E28, 0x4E3018, 0x5A3420, 0x7E4E2E, 0x8A5A30, 0xB8844C, false),
        new Material("stone",     0x1E1D22, 0xEEEEE6, 0xC8C8C0, 0x9A9A94, 0x5E5E5A, 0x84847E, 0xA87A48, 0x7E5630, 0x52361C, 0x5A3A22, 0x7A5232, 0x76766F, 0xB4B4AC, false),
        new Material("copper",    0x2A140E, 0xFFE0C8, 0xFFB488, 0xDA8250, 0x9A4E2C, 0xB8663A, 0x7FE0C4, 0x3FAE92, 0x2A7A66, 0x5A2E1E, 0x7E4430, 0x3FAE92, 0xA8F5E0, false),
        new Material("iron",      0x1C1A26, 0xFFFFFF, 0xE6EAF0, 0xBCC2CE, 0x7A8298, 0x969EB0, 0xB8C0CE, 0x8A92A6, 0x5A6076, 0x6A2A26, 0x8E3C34, 0x8A92A6, 0xDCE2EA, false),
        new Material("golden",    0x3A2208, 0xFFFBE0, 0xFFEE90, 0xFFCE3A, 0xC08018, 0xD89A20, 0xFFE58A, 0xE0A830, 0x9A6A14, 0x6E1A22, 0x9A2A30, 0xE8303F, 0xFF9AA4, true),
        new Material("diamond",   0x0C2230, 0xFFFFFF, 0xC8FFF8, 0x6FE3D8, 0x2A9AA6, 0x4CC4C0, 0xFFE58A, 0xE0A830, 0x9A6A14, 0x1E2A6A, 0x2E3E94, 0x4F8CFF, 0xC4DCFF, true),
        new Material("netherite", 0x0E0A10, 0xB8AEB8, 0x847884, 0x544854, 0x2C242C, 0xC070FF, 0xE0B458, 0xB8893A, 0x6E4A1A, 0x2A1E2E, 0x3E2E44, 0xA050F0, 0xE0B8FF, true),
    };

    public static void main(String[] a) throws Exception {
        for (var m : MATERIALS) {
            int[][] t = template(m.name());
            var im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int role = t[y][x];
                if (role == JEWEL && !m.jewel()) role = GUARD;
                int c = switch (role) {
                    case EDGE -> m.edge(); case LIGHT -> m.light(); case MID -> m.mid(); case DARK -> m.dark(); case FULLER -> m.fuller();
                    case GUARD_HI -> m.guardHi(); case GUARD -> m.guard(); case GUARD_LO -> m.guardLo();
                    case GRIP_A -> m.gripA(); case GRIP_B -> m.gripB(); case GEM, JEWEL -> m.gem(); case GEM_HI -> m.gemHi();
                    case OUTLINE -> m.outline(); default -> -1;
                };
                if (c >= 0) im.setRGB(x, y, 0xFF000000 | c);
            }
            ImageIO.write(im, "png", new File(a[0], m.name() + "_sword.png"));
        }
    }

    static int[][] t;
    static void put(int x, int y, int role) { if (x >= 0 && y >= 0 && x < 16 && y < 16) t[y][x] = role; }

    /** Handle bottom-left, tip top-right. */
    static int[][] template(String material) {
        t = new int[16][16];
        // blade along x + y = 15, rows 1..9: light edge top-left, dark edge bottom-right, fuller in the middle
        for (int y = 2; y <= 9; y++) {
            int c = 15 - y;
            put(c - 1, y, y % 2 == 0 ? EDGE : LIGHT);
            put(c, y, y >= 4 && y <= 8 ? FULLER : MID);
            put(c + 1, y, DARK);
        }
        put(13, 1, LIGHT); put(14, 1, EDGE);                               // the tip, with a glint
        // crossguard: five cells across the blade's base (perpendicular to it), jewel in the middle,
        // tips curling toward the tip of the blade
        put(3, 8, GUARD_HI); put(4, 9, GUARD_HI); put(5, 10, JEWEL); put(6, 11, GUARD); put(7, 12, GUARD_LO);
        put(3, 7, GUARD_HI); put(8, 12, GUARD_LO);
        // wrapped grip, then a 2x2 pommel gem
        put(4, 11, GRIP_A); put(3, 12, GRIP_B);
        put(1, 13, GEM_HI); put(2, 13, GEM); put(1, 14, GEM); put(2, 14, GEM);
        switch (material) {
            case "stone" -> { put(12, 4, NONE); put(9, 7, NONE); }          // chipped edge
            case "copper" -> { put(12, 4, GUARD); put(8, 8, GUARD_LO); }    // patina
            case "wooden" -> { put(10, 5, DARK); put(8, 7, DARK); }         // knots
            case "diamond" -> { for (int y = 3; y <= 9; y += 2) put(15 - y - 1, y, EDGE); put(15 - 5, 5, LIGHT); put(15 - 7, 7, LIGHT); }  // facets
            case "netherite" -> { put(14, 4, DARK); put(12, 6, DARK); put(10, 8, DARK); }  // serrated spikes (runes are the fuller)
            default -> { }
        }
        // outline: every empty pixel touching a drawn one
        int[][] o = new int[16][16];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            o[y][x] = t[y][x];
            if (t[y][x] != NONE) continue;
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = x + d[0], ny = y + d[1];
                if (nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && t[ny][nx] != NONE) { o[y][x] = OUTLINE; break; }
            }
        }
        return o;
    }
}
