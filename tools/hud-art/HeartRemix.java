import java.io.File; import javax.imageio.ImageIO; import java.awt.image.BufferedImage; import java.util.Map;

/** Remixes pxlpunkt's "Pulsing Heart" orb (commercial use + modification allowed): glass recoloured. */
public class HeartRemix {
  public static void main(String[] a) throws Exception {
    Map<Integer,Integer> plum = Map.ofEntries(Map.entry(0x070A0C, 0x0A060C), Map.entry(0x0E1418, 0x150E18), Map.entry(0x040607, 0x060408)); plum = merge(plum, Map.of(0x080A13, 0x0E0812, 0x1B202A, 0x1F1726, 0x30394B, 0x372A42, 0x202A32, 0x281C30,
        0x394B5A, 0x45355A, 0x7296B4, 0x9A7FB8, 0xB8CAD9, 0xD6C8E8));
    Map<Integer,Integer> alarm = Map.ofEntries(Map.entry(0x070A0C, 0x0E0206), Map.entry(0x0E1418, 0x1E060C), Map.entry(0x040607, 0x0A0104)); alarm = merge(alarm, Map.of(0x080A13, 0x14040A, 0x1B202A, 0x2E0A14, 0x30394B, 0x4E1222, 0x202A32, 0x3A0C18,
        0x394B5A, 0x6A1A2C, 0x7296B4, 0xFF6B7A, 0xB8CAD9, 0xFFC2C8));
    for (int i = 0; i < 12; i++) {
      var f = ImageIO.read(new File(a[0] + "/s" + i + ".png"));
      write(recolor(f, plum), a[1], String.format("avian_heart_%02d", i));
      write(recolor(f, alarm), a[1], String.format("avian_heart_alarm_%02d", i));
    }
  }
  static Map<Integer,Integer> merge(Map<Integer,Integer> a, Map<Integer,Integer> b) { var m = new java.util.HashMap<>(a); m.putAll(b); return m; }
  static BufferedImage recolor(BufferedImage f, Map<Integer,Integer> m) {
    var o = new BufferedImage(28, 28, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < 28; y++) for (int x = 0; x < 28; x++) { int c = f.getRGB(x, y); if ((c >>> 24) == 0) continue;
      o.setRGB(x, y, 0xFF000000 | m.getOrDefault(c & 0xFFFFFF, c & 0xFFFFFF)); }
    return o;
  }
  static void write(BufferedImage im, String dir, String n) throws Exception { ImageIO.write(im, "png", new File(dir, n + ".png")); }
}
