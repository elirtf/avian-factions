import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * The XP level art, drawn to sit with the heart and food orbs (all shown at BetterHud scale 0.75):
 *  - avian_orb_level_<tier>: 40 texels (30 px): beveled gold rim, tier-coloured glass, glint + specular.
 *  - avian_ornament_<tier>: 56x48 (42x36 px): gold filigree scrolls either side and a set gem on top,
 *    drawn behind the orb (the orb sits at texel 8,4 of it).
 *  - avian_levelup_NN: 16 frames of 64x64 (48 px): an expanding gold ring and sparkles, for the popup.
 * Tiers: emerald 0-29, sapphire 30-59, amethyst 60-99, ruby 100+.
 */
public class LevelArt {
    record Tier(String name, int deep, int mid, int glow, int glint, int gem) {}
    static final Tier[] TIERS = {
        new Tier("emerald", 0x08170D, 0x163A20, 0x3FCF5A, 0xB8F5C2, 0x3FCF5A),
        new Tier("sapphire", 0x07122A, 0x12305A, 0x3FA9F5, 0xBFE3FF, 0x4FB4FF),
        new Tier("amethyst", 0x150A26, 0x2E1650, 0xA86CFF, 0xE2CCFF, 0xB47CFF),
        new Tier("ruby", 0x24060C, 0x4A0E1A, 0xFF3A55, 0xFFC4CC, 0xFF4A62),
    };
    static final int OUTLINE = 0x0B0910;
    static final int[] GOLD = {0x5A3A14, 0x8A5E22, 0xB8893A, 0xE0B458, 0xFFE08A};

    public static void main(String[] a) throws Exception {
        for (var t : TIERS) {
            write(orb(t), a[0], "avian_orb_level_" + t.name());
            write(ornament(t), a[0], "avian_ornament_" + t.name());
        }
        for (int i = 0; i < 16; i++) write(burst(i / 15.0), a[0], String.format("avian_levelup_%02d", i));
    }

    static BufferedImage orb(Tier t) {
        int n = 40; double c = n / 2.0, r = c - 0.5, glass = r - 4.6;
        var o = img(n, n);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            double dx = x + 0.5 - c, dy = y + 0.5 - c, d = Math.hypot(dx, dy);
            if (d > r) continue;
            int col;
            if (d > r - 1.2) col = OUTLINE;
            else if (d > r - 3.6) {
                double light = (-dx - dy) / (Math.sqrt(2) * d);
                col = GOLD[(int) Math.round((light + 1) / 2 * (GOLD.length - 1))];
                if (d > r - 1.9) col = mix(col, OUTLINE, 0.35);
            } else if (d > glass) col = 0x140C08;
            else {
                double k = d / glass;
                col = mix(t.deep(), t.mid(), k);
                col = mix(col, mix(t.deep(), 0, 0.5), Math.max(0, dy / glass) * 0.45);
                if (k > 0.72) col = mix(col, t.glow(), (k - 0.72) / 0.28 * 0.55);
                double ang = Math.toDegrees(Math.atan2(dy, dx));
                if (k > 0.55 && k < 0.86 && ang > -165 && ang < -105) col = mix(col, t.glint(), 0.55);
            }
            o.setRGB(x, y, 0xFF000000 | col);
        }
        int sx = (int) (c - 7), sy = (int) (c - 9);
        o.setRGB(sx, sy, 0xFFFFFFFF); o.setRGB(sx + 1, sy, 0xFF000000 | t.glint()); o.setRGB(sx, sy + 1, 0xFF000000 | t.glint());
        return o;
    }

    /** Filigree: gold scrolls either side and a gem crest on top; mirrored so both sides match. */
    static BufferedImage ornament(Tier t) {
        int w = 56, h = 48;
        var shape = img(w, h);
        var g = shape.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // left side (orb centre at 28,24, radius 20): an upper and a lower scroll curling from the rim
        g.draw(new QuadCurve2D.Double(9, 20, 1, 18, 3, 11));
        g.draw(new QuadCurve2D.Double(9, 28, 1, 30, 3, 37));
        g.setStroke(new BasicStroke(1f));
        g.draw(new QuadCurve2D.Double(3, 11, 6, 9, 7, 13));       // curls
        g.draw(new QuadCurve2D.Double(3, 37, 6, 39, 7, 35));
        g.draw(new QuadCurve2D.Double(9, 24, 3, 24, 1, 24));      // centre spur
        // crest horns on the top, left half
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(19, 6, 21, 1, 24, 2));
        g.dispose();
        mirrorLeftToRight(shape);
        var o = img(w, h);
        // gold, lit from the top-left, then a dark outline around everything
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if ((shape.getRGB(x, y) >>> 24) == 0) continue;
            boolean top = y == 0 || (shape.getRGB(x, y - 1) >>> 24) == 0;
            boolean bottom = y == h - 1 || (shape.getRGB(x, y + 1) >>> 24) == 0;
            o.setRGB(x, y, 0xFF000000 | (top ? GOLD[4] : bottom ? GOLD[1] : GOLD[3]));
        }
        // tier gems: the scroll-end curls and the crest
        gem(o, 7, 12, t, 1); gem(o, w - 8, 12, t, 1); gem(o, 7, 36, t, 1); gem(o, w - 8, 36, t, 1);
        gem(o, 28, 3, t, 2);
        outline(o);
        return o;
    }

    static void gem(BufferedImage o, int cx, int cy, Tier t, int r) {
        for (int y = -r - 1; y <= r + 1; y++) for (int x = -r - 1; x <= r + 1; x++) {
            int d = Math.abs(x) + Math.abs(y);
            if (d > r + 1) continue;
            int col = d == r + 1 ? GOLD[2] : (x < 0 && y < 0 || d == 0 && r == 0) ? t.glint() : d <= r - 1 ? t.gem() : mix(t.gem(), 0, 0.3);
            set(o, cx + x, cy + y, 0xFF000000 | col);
        }
        if (r >= 1) set(o, cx - 1, cy - 1, 0xFFFFFFFF);
    }

    /** One burst frame, p = 0..1: a gold ring expanding from the orb's rim and fading, with sparkles. */
    static BufferedImage burst(double p) {
        int n = 64; double c = n / 2.0;
        var o = img(n, n);
        double ring = 20 + p * 11, fade = 1 - p;
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            double d = Math.hypot(x + 0.5 - c, y + 0.5 - c), off = Math.abs(d - ring);
            if (off < 1.6) {
                int a = (int) (255 * fade * (off < 0.8 ? 1 : 0.5));
                if (a > 0) o.setRGB(x, y, a << 24 | (off < 0.8 ? 0xFFE58A : 0xFFB84A));
            }
        }
        for (int s = 0; s < 8; s++) {
            double ang = Math.PI / 4 * s + Math.PI / 8, dist = 18 + p * 13;
            int sx = (int) Math.round(c + Math.cos(ang) * dist), sy = (int) Math.round(c + Math.sin(ang) * dist);
            int a = (int) (255 * Math.max(0, 1 - p * 1.1));
            if (a <= 0) continue;
            set(o, sx, sy, a << 24 | 0xFFFFFF);
            if (p < 0.6) { set(o, sx + 1, sy, a << 24 | 0xFFE58A); set(o, sx - 1, sy, a << 24 | 0xFFE58A); set(o, sx, sy + 1, a << 24 | 0xFFE58A); set(o, sx, sy - 1, a << 24 | 0xFFE58A); }
        }
        return o;
    }

    static void mirrorLeftToRight(BufferedImage im) {
        int w = im.getWidth();
        for (int y = 0; y < im.getHeight(); y++) for (int x = 0; x < w / 2; x++) if ((im.getRGB(x, y) >>> 24) != 0) im.setRGB(w - 1 - x, y, im.getRGB(x, y));
    }
    static void outline(BufferedImage im) {
        var src = img(im.getWidth(), im.getHeight()); src.getGraphics().drawImage(im, 0, 0, null);
        for (int y = 0; y < im.getHeight(); y++) for (int x = 0; x < im.getWidth(); x++) {
            if ((src.getRGB(x, y) >>> 24) != 0) continue;
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = x + d[0], ny = y + d[1];
                if (nx >= 0 && ny >= 0 && nx < im.getWidth() && ny < im.getHeight() && (src.getRGB(nx, ny) >>> 24) != 0) { im.setRGB(x, y, 0xFF000000 | OUTLINE); break; }
            }
        }
    }
    static void set(BufferedImage im, int x, int y, int argb) { if (x >= 0 && y >= 0 && x < im.getWidth() && y < im.getHeight()) im.setRGB(x, y, argb); }
    static BufferedImage img(int w, int h) { return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); }
    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t), bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }
    static void write(BufferedImage im, String dir, String n) throws Exception { ImageIO.write(im, "png", new File(dir, n + ".png")); }
}
