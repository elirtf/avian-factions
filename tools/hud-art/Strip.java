import java.awt.image.BufferedImage; import java.io.File; import javax.imageio.ImageIO;
/**
 * The GIF stores changed pixels only, at 10x: composites each frame onto the last and samples it back
 * to real pixels (s0.png…, 90x28), plus a strip of the 28x28 orb from every frame for eyeballing.
 */
public class Strip { public static void main(String[] a) throws Exception {
  int n = 12, w = 28, h = 28; var acc = new BufferedImage(90, 28, BufferedImage.TYPE_INT_ARGB); var strip = new BufferedImage(w * n + (n - 1), h, BufferedImage.TYPE_INT_ARGB);
  for (int i = 0; i < n; i++) { var f = ImageIO.read(new File(a[0] + "/f" + i + ".png"));
    var small = new BufferedImage(90, 28, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < 28; y++) for (int x = 0; x < 90; x++) { int c = f.getRGB(x * 10 + 5, y * 10 + 5); if ((c >>> 24) != 0) acc.setRGB(x, y, c); small.setRGB(x, y, acc.getRGB(x, y)); }
    ImageIO.write(small, "png", new File(a[0] + "/s" + i + ".png"));
    for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) strip.setRGB(i * (w + 1) + x, y, small.getRGB(x, y)); }
  ImageIO.write(strip, "png", new File(a[1])); } }
