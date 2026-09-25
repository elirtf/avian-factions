import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.IntUnaryOperator;
import javax.imageio.ImageIO;

/**
 * Remixes pxlpunkt's "Pulsing Heart" bars (commercial use + modification allowed) into the Avian HUD:
 * bars lengthened by repeating their textured middle, fills recoloured per status, right side mirrored.
 * Texel geometry (shown at BetterHud scale 0.6): orb 28x28 at 0,0; bar A rows 7..15, bar B rows 17..25.
 */
public class BarRemix {
    static String OUT;
    // source columns: left cap + body, a repeat of the body, right cap
    static final int[] COLS = cols();
    static int[] cols() {
        var l = new java.util.ArrayList<Integer>();
        for (int x = 27; x <= 84; x++) l.add(x);
        for (int x = 60; x <= 76; x++) l.add(x);
        for (int x = 85; x <= 89; x++) l.add(x);
        return l.stream().mapToInt(i -> i).toArray();
    }
    static final int X0 = 28, W = X0 + COLS.length;   // 108 texels = 81 GUI px at scale 0.75; X0 = 21 px, bars 60 px

    public static void main(String[] a) throws Exception {
        OUT = a[1];
        var empty = ImageIO.read(new File(a[0], "Health Bar Empty.png"));
        var full = ImageIO.read(new File(a[0], "Health Bar Full.png"));
        var casing = img(W, 28);
        var fillA = img(COLS.length, 28);
        var fillB = img(COLS.length, 28);
        for (int x = 27; x < X0; x++) for (int y = 0; y < 28; y++) { int e = empty.getRGB(x, y); if ((e >>> 24) != 0) casing.setRGB(x, y, e); }
        for (int i = 0; i < COLS.length; i++) {
            int sx = COLS[i];
            for (int y = 0; y < 28; y++) {
                int e = empty.getRGB(sx, y), f = full.getRGB(sx, y);
                if ((e >>> 24) != 0) casing.setRGB(X0 + i, y, e);
                if ((f >>> 24) != 0 && f != e) {
                    if (y >= 7 && y <= 15) fillA.setRGB(i, y, f);
                    else if (y >= 17 && y <= 25) fillB.setRGB(i, y - 10, f);   // both textures in the top-row position; row() moves them
                }
            }
        }
        // left side, and the mirrored right side
        casing = map(casing, c -> plum(c));
        write(casing, "avian_casing_l"); write(flip(casing), "avian_casing_r");
        var alarm = img(W, 28);   // only the health row (rows 7..15) flashes red
        for (int y = 0; y < 28; y++) for (int x = 0; x < W; x++) { int c = casing.getRGB(x, y); if ((c >>> 24) == 0) continue;
            alarm.setRGB(x, y, y >= 7 && y <= 15 ? 0xFF000000 | tint(c & 0xFFFFFF, 0xE0303F, 0.55) : c); }
        write(alarm, "avian_casing_alarm_l");

        // bar A is red (health-like), bar B blue (mana-like): recolour by hue/saturation, keep the shading
        pair(map(fillA, c -> c), "avian_fill_health", false);
        pair(map(fillA, c -> hue(c, 110, 1.0, 1.0)), "avian_fill_poison", false);
        pair(map(fillA, c -> hue(c, 285, 0.25, 0.55)), "avian_fill_wither", false);
        pair(map(fillB, c -> hue(c, 192, 0.55, 1.25)), "avian_fill_frozen", false);
        pair(map(fillA, c -> hue(c, 44, 1.0, 1.35)), "avian_fill_absorption", false);
        pair(map(fillA, c -> hue(c, 28, 1.0, 1.3)), "avian_fill_food", true);
        pair(map(fillA, c -> hue(c, 62, 0.55, 1.05)), "avian_fill_food_sick", true);
        pair(map(fillB, c -> hue(c, 215, 0.12, 1.25)), "avian_fill_armor", false);
        pair(map(fillB, c -> c), "avian_fill_mana", true);
        pair(map(fillB, c -> hue(c, 188, 0.8, 1.35)), "avian_fill_air", true);
    }

    /** Writes a fill: top row unless the name is a lower-row bar; mirrored for the right side. */
    static void pair(BufferedImage im, String name, boolean right) throws Exception {
        boolean lower = name.endsWith("armor") || name.endsWith("mana") || name.endsWith("air");
        if (lower) im = row(im, 10);
        write(right ? flip(im) : im, name);
    }
    static BufferedImage row(BufferedImage s, int dy) {
        var o = img(s.getWidth(), s.getHeight());
        for (int y = 0; y + dy < s.getHeight(); y++) for (int x = 0; x < s.getWidth(); x++) o.setRGB(x, y + dy, s.getRGB(x, y));
        return o;
    }

    static BufferedImage img(int w, int h) { return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); }
    static BufferedImage flip(BufferedImage s) {
        var o = img(s.getWidth(), s.getHeight());
        for (int y = 0; y < s.getHeight(); y++) for (int x = 0; x < s.getWidth(); x++) o.setRGB(s.getWidth() - 1 - x, y, s.getRGB(x, y));
        return o;
    }
    static BufferedImage map(BufferedImage s, IntUnaryOperator f) {
        var o = img(s.getWidth(), s.getHeight());
        for (int y = 0; y < s.getHeight(); y++) for (int x = 0; x < s.getWidth(); x++) {
            int c = s.getRGB(x, y);
            if ((c >>> 24) != 0) o.setRGB(x, y, 0xFF000000 | (f.applyAsInt(c & 0xFFFFFF) & 0xFFFFFF));
        }
        return o;
    }
    /** New hue (degrees), saturation scaled, brightness scaled — shading survives. */
    static int hue(int c, float deg, double sat, double bri) {
        float[] h = Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
        return Color.HSBtoRGB(deg / 360f, (float) Math.min(1, h[1] * sat), (float) Math.min(1, h[2] * bri));
    }
    /** Their blue-grey metal and glass, shifted to the plum of our orb glass. */
    static int plum(int c) {
        float[] h = Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
        return h[0] > 0.45f && h[0] < 0.72f ? Color.HSBtoRGB(0.80f, h[1] * 0.75f, h[2]) : c;
    }
    static int tint(int c, int t, double k) {
        int r = (int) (((c >> 16) & 255) * (1 - k) + ((t >> 16) & 255) * k), g = (int) (((c >> 8) & 255) * (1 - k) + ((t >> 8) & 255) * k), b = (int) ((c & 255) * (1 - k) + (t & 255) * k);
        return r << 16 | g << 8 | b;
    }
    static void write(BufferedImage im, String n) throws Exception { ImageIO.write(im, "png", new File(OUT, n + ".png")); }
}
