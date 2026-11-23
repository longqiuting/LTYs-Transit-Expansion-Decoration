import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * 把目录里的 PNG 按「最大边不超过 cap」等比缩小（只缩不放），其余原样复制。
 * 用于 LTED 纹理降分辨率（默认包）与生成高清资源包。
 *
 * 用法：java TextureDownscale.java <srcDir> <outDir> <cap>
 */
public final class TextureDownscale {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("usage: TextureDownscale <srcDir> <outDir> <cap>");
            return;
        }
        final Path src = Paths.get(args[0]);
        final Path out = Paths.get(args[1]);
        final int cap = Integer.parseInt(args[2]);

        final List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(src)) {
            stream.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .filter(Files::isRegularFile)
                    .forEach(files::add);
        }
        Collections.sort(files);

        long before = 0, after = 0;
        int resized = 0, copied = 0;
        final StringBuilder report = new StringBuilder();

        for (Path p : files) {
            final Path target = out.resolve(src.relativize(p).toString());
            Files.createDirectories(target.getParent());
            final long inBytes = Files.size(p);
            before += inBytes;

            BufferedImage img = null;
            try {
                img = ImageIO.read(p.toFile());
            } catch (IOException ignored) {
            }
            if (img == null) {
                Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                after += inBytes;
                copied++;
                continue;
            }

            final int w = img.getWidth(), h = img.getHeight();
            final int maxEdge = Math.max(w, h);
            if (maxEdge <= cap) {
                Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                after += inBytes;
                copied++;
                continue;
            }

            final double scale = (double) cap / maxEdge;
            final int nw = Math.max(1, (int) Math.round(w * scale));
            final int nh = Math.max(1, (int) Math.round(h * scale));

            final BufferedImage scaled = new BufferedImage(nw, nh,
                    img.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            final Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            ImageIO.write(scaled, "png", target.toFile());

            final long outBytes = Files.size(target);
            after += outBytes;
            resized++;
            report.append(String.format("  %dx%d -> %dx%d  %6.0fKB -> %5.0fKB  %s%n",
                    w, h, nw, nh, inBytes / 1024.0, outBytes / 1024.0, src.relativize(p)));
        }

        System.out.println(report);
        System.out.printf("cap=%d | 文件 %d（缩小 %d / 原样 %d）| %.1f MB -> %.1f MB（省 %.0f%%）%n",
                cap, files.size(), resized, copied, before / 1048576.0, after / 1048576.0,
                before == 0 ? 0 : (1.0 - (double) after / before) * 100);
    }
}
