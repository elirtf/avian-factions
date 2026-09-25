import java.io.File; import javax.imageio.ImageIO;
/**
 * BetterHud trims transparent edges and then sizes an image from its trimmed height, so pieces of one
 * bar end up at different scales. A near-invisible pixel (alpha 1/255) in two opposite corners keeps
 * every image at its full canvas, so all of them share one scale.
 */
public class Pin { public static void main(String[] a) throws Exception {
  for (var f : new File(a[0]).listFiles((d, n) -> n.startsWith("avian_") && n.endsWith(".png"))) {
    var im = ImageIO.read(f); int w = im.getWidth(), h = im.getHeight();
    if ((im.getRGB(0, 0) >>> 24) == 0) im.setRGB(0, 0, 0x01000000);
    if ((im.getRGB(w - 1, h - 1) >>> 24) == 0) im.setRGB(w - 1, h - 1, 0x01000000);
    ImageIO.write(im, "png", f); } } }
