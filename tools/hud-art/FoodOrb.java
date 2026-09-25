import java.io.File; import javax.imageio.ImageIO; import java.awt.image.BufferedImage; import java.util.*;

/**
 * The food orb: the remixed heart orb with the heart removed. The glass under it is rebuilt ring by
 * ring — each missing pixel takes the most common glass colour at the same distance from the centre —
 * so the shading matches the heart orb's; then a shaded drumstick is drawn in. Mirrored for the right.
 */
public class FoodOrb {
  static final String[] STICK = {
    "......8888....",
    "....88455488..",
    "...8455544338.",
    "..845544333328",
    "..854443333228",
    "..843333332218",
    "..833333222118",
    "..833322221118",
    "...83222111188",
    "..888221118...",
    ".8678888......",
    "8676..........",
    "86678.........",
    ".8888.........",
  };
  static final double CX = 13, CY = 14;
  public static void main(String[] a) throws Exception {
    var f = ImageIO.read(new File(a[0], "avian_heart_00.png"));
    Set<Integer> rim = Set.of(0x000000, 0x7F708A, 0x694F62, 0x3E3546);
    Set<Integer> glass = Set.of(0x0E0812, 0x1F1726, 0x372A42, 0x281C30, 0x45355A);
    Set<Integer> glint = Set.of(0x9A7FB8, 0xD6C8E8);
    // glass colours by ring (half-texel radius buckets)
    Map<Integer, Map<Integer, Integer>> rings = new HashMap<>();
    for (int y = 0; y < 28; y++) for (int x = 0; x < 28; x++) {
      int c = f.getRGB(x, y) & 0xFFFFFF; if ((f.getRGB(x, y) >>> 24) < 255 || !glass.contains(c)) continue;
      rings.computeIfAbsent(ring(x, y), k -> new HashMap<>()).merge(c, 1, Integer::sum);
    }
    var o = new BufferedImage(28, 28, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < 28; y++) for (int x = 0; x < 28; x++) {
      int argb = f.getRGB(x, y); if ((argb >>> 24) < 255) continue;
      int c = argb & 0xFFFFFF;
      if (!rim.contains(c) && !glass.contains(c) && !glint.contains(c)) c = glassAt(rings, ring(x, y));
      o.setRGB(x, y, 0xFF000000 | c);
    }
    int[] pal = {0x5A2E14, 0x8E4C22, 0xB86A30, 0xDB8E4E, 0xF5B878, 0xF2E6D0, 0xC0B29A, 0x1A0C06};
    for (int y = 0; y < STICK.length; y++) for (int x = 0; x < STICK[y].length(); x++) {
      char ch = STICK[y].charAt(x); if (ch < '1' || ch > '8') continue;
      o.setRGB(6 + x, 7 + y, 0xFF000000 | pal[ch - '1']);
    }
    var m = new BufferedImage(28, 28, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < 28; y++) for (int x = 0; x < 28; x++) m.setRGB(27 - x, y, o.getRGB(x, y));
    ImageIO.write(m, "png", new File(a[0], "avian_orb_food.png"));
  }
  static int ring(int x, int y) { return (int) Math.round(Math.hypot(x + 0.5 - CX, y + 0.5 - CY) * 2); }
  static int glassAt(Map<Integer, Map<Integer, Integer>> rings, int r) {
    for (int d = 0; d < 8; d++) for (int s : new int[] {r - d, r + d}) {
      var m = rings.get(s); if (m != null) return Collections.max(m.entrySet(), Map.Entry.comparingByValue()).getKey();
    }
    return 0x1F1726;
  }
}
