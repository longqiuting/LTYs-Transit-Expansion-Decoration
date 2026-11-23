import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * 纹理体检：统计目录下所有 PNG 的尺寸分布 / 体积 / 显存估算，并可按上限断言。
 *
 * <p>用法：
 * <pre>
 *   java tools/TextureAudit.java &lt;dir&gt;              # 只报告
 *   java tools/TextureAudit.java &lt;dir&gt; 512          # 断言：任何 PNG 最大边 &lt;= 512，否则退出码 1
 * </pre>
 *
 * <p>显存估算 = Σ(w * h * 4 字节)，即 RGBA8 未压缩、未 mipmap。
 */
public final class TextureAudit {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("usage: TextureAudit <dir> [assertMaxEdge]");
            return;
        }
        final Path root = Paths.get(args[0]);
        final int assertCap = args.length > 1 ? Integer.parseInt(args[1]) : -1;

        final List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .filter(Files::isRegularFile)
                    .forEach(files::add);
        }
        Collections.sort(files);

        long bytes = 0, vram = 0;
        int over512 = 0, over1024 = 0, maxEdge = 0, bad = 0;
        final Path worst = null;
        String worstPath = "-";
        final Map<String, Integer> buckets = new TreeMap<>();

        for (Path p : files) {
            bytes += Files.size(p);
            BufferedImage img;
            try {
                img = ImageIO.read(p.toFile());
            } catch (IOException e) {
                img = null;
            }
            if (img == null) {
                bad++;
                System.out.println("  !! 读不出: " + root.relativize(p));
                continue;
            }
            final int w = img.getWidth(), h = img.getHeight();
            final int me = Math.max(w, h);
            vram += (long) w * h * 4L;
            if (me > maxEdge) {
                maxEdge = me;
                worstPath = root.relativize(p).toString();
            }
            if (me > 512) over512++;
            if (me > 1024) over1024++;
            final String b = me <= 128 ? "<=128" : me <= 256 ? "<=256" : me <= 512 ? "<=512"
                    : me <= 1024 ? "<=1024" : ">1024";
            buckets.merge(b, 1, Integer::sum);
        }

        System.out.printf("目录 %s%n", root);
        System.out.printf("  PNG %d 张 | %.1f MB | 显存估算 %d MB | 不可读 %d%n",
                files.size(), bytes / 1048576.0, vram / 1048576L, bad);
        System.out.printf("  最大边 %d（%s）| >512: %d 张 | >1024: %d 张%n", maxEdge, worstPath, over512, over1024);
        System.out.print("  分布:");
        buckets.forEach((k, v) -> System.out.printf("  %s=%d", k, v));
        System.out.println();

        if (assertCap > 0) {
            if (maxEdge > assertCap) {
                System.out.printf("  ✗ 失败：最大边 %d > 上限 %d%n", maxEdge, assertCap);
                System.exit(1);
            }
            System.out.printf("  ✓ 通过：全部 PNG 最大边 <= %d%n", assertCap);
        }
    }
}
