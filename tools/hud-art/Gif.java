import javax.imageio.*; import javax.imageio.stream.*; import java.io.File; import java.awt.image.BufferedImage;
/** Splits PulsingHeart.gif into its frames (f0.png…), printing each frame's delay. */
public class Gif { public static void main(String[] a) throws Exception {
  var r = ImageIO.getImageReadersByFormatName("gif").next(); r.setInput(ImageIO.createImageInputStream(new File(a[0])));
  int n = r.getNumImages(true); System.out.println("frames " + n);
  for (int i = 0; i < n; i++) { var im = r.read(i); var meta = r.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
    String delay = ""; var kids = meta.getChildNodes(); for (int k = 0; k < kids.getLength(); k++) if (kids.item(k).getNodeName().equals("GraphicControlExtension")) delay = ((javax.imageio.metadata.IIOMetadataNode) kids.item(k)).getAttribute("delayTime");
    System.out.println(i + " " + im.getWidth() + "x" + im.getHeight() + " delay " + delay + "cs"); ImageIO.write(im, "png", new File(a[1] + "/f" + i + ".png")); } } }
