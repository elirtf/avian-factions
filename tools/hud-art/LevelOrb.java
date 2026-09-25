import java.io.File; import javax.imageio.ImageIO; import java.awt.image.BufferedImage;

/**
 * The XP level orb, drawn to sit with the heart and food orbs: a beveled gold rim, emerald glass with
 * an inner glow, a crescent glint top-left and a small specular dot. 40 texels = 30 GUI px at 0.75.
 */
public class LevelOrb {
  public static void main(String[] a) throws Exception {
    int N = 40; double c = N / 2.0, R = c - 0.5;
    var o = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
      double dx = x + 0.5 - c, dy = y + 0.5 - c, d = Math.hypot(dx, dy);
      if (d > R) continue;
      int col;
      if (d > R - 1.2) col = 0x0B0910;                                   // outline
      else if (d > R - 3.6) {                                            // beveled gold rim, lit from top-left
        double light = (-dx - dy) / (Math.sqrt(2) * d);                  // -1..1
        int[] ramp = {0x5A3A14, 0x8A5E22, 0xB8893A, 0xE0B458, 0xFFE08A};
        col = ramp[(int) Math.round((light + 1) / 2 * (ramp.length - 1))];
        if (d > R - 1.9 && d <= R - 1.2) col = mix(col, 0x0B0910, 0.35);  // soft outer edge of the rim
      }
      else if (d > R - 4.6) col = 0x140C08;                               // inner lip
      else {                                                             // emerald glass
        double t = d / (R - 4.6);
        col = mix(0x08170D, 0x163A20, t);                                 // darker centre
        col = mix(col, 0x04100A, Math.max(0, dy / (R - 4.6)) * 0.45);     // shadow toward the bottom
        if (t > 0.72) col = mix(col, 0x3FCF5A, (t - 0.72) / 0.28 * 0.55); // inner glow at the edge
        double ang = Math.toDegrees(Math.atan2(dy, dx));
        if (t > 0.55 && t < 0.86 && ang > -165 && ang < -105) col = mix(col, 0xB8F5C2, 0.55); // crescent glint
      }
      o.setRGB(x, y, 0xFF000000 | col);
    }
    // specular dot
    int sx = (int) (c - 7), sy = (int) (c - 9);
    o.setRGB(sx, sy, 0xFFEFFFF2); o.setRGB(sx + 1, sy, 0xFFB8F5C2); o.setRGB(sx, sy + 1, 0xFFB8F5C2);
    ImageIO.write(o, "png", new File(a[0], "avian_orb_level.png"));
  }
  static int mix(int a, int b, double t) {
    t = Math.max(0, Math.min(1, t));
    int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t), bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
    return r << 16 | g << 8 | bl;
  }
}
