// LED 走字屏：文字先栅格化，再按点阵放大成「一颗颗灯珠」，最后注册成动态贴图。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * LED 屏贴图缓存：一个文字 → 一张点阵图。
 *
 * <h2>为什么先画小字再放大</h2>
 * <p>直接按大字号画出来是平滑字体，没有 LED 的颗粒感。这里先用 12px 等宽字体把文字画成
 * 1:1 的小位图，再把每个像素扩成一个 {@value #DOT}px 的方块（中间留缝），看起来就是
 * 一颗颗灯珠 —— 和 MTR4 滚动屏那种点阵观感一致。
 *
 * <p>颜色：暗绿（正文），底色透明（屏幕本身的深色由方块贴图提供）。
 */
public final class FuzhouLedTextures {

    /** 一颗灯珠占多少像素（含 1px 缝）。 */
    private static final int DOT = 3;
    /** 灯珠边长（DOT 里的实心部分）。 */
    private static final int DOT_FILL = 2;
    /** LED 文字颜色（暗绿）。 */
    private static final int LED_COLOR = 0x2FA34C;
    private static final int FONT_SIZE = 12;

    private static final Map<String, Entry> ENTRIES = new HashMap<>();

    private FuzhouLedTextures() {
    }

    /** 一张贴图 + 它在世界里的宽高比（宽 / 高）。 */
    public static final class Entry {
        public final Identifier id;
        public final float aspect;

        Entry(final Identifier id, final float aspect) {
            this.id = id;
            this.aspect = aspect;
        }
    }

    /** 取（或生成）一段文字的点阵贴图；失败返回 {@code null}（屏就留空）。 */
    public static Entry get(final String text) {
        final String key = text == null || text.isEmpty() ? FuzhouMachineBlockEntity.DEFAULT_TEXT : text;
        final Entry cached = ENTRIES.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            final Entry entry = build(key);
            ENTRIES.put(key, entry);
            return entry;
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] led texture unavailable", t);
            return null;
        }
    }

    private static Entry build(final String text) {
        // 1) 小字位图（1:1，白字黑底，方便当遮罩用）
        final Font font = new Font(Font.SANS_SERIF, Font.BOLD, FONT_SIZE); // 等宽字体字距太松，用常规字体
        final BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D pg = probe.createGraphics();
        pg.setFont(font);
        final int textWidth = Math.max(1, pg.getFontMetrics().stringWidth(text));
        final int textHeight = Math.max(1, pg.getFontMetrics().getHeight());
        pg.dispose();

        final BufferedImage mask = new BufferedImage(textWidth, textHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = mask.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        // 横向压一下：观感上字不要被拉宽（0.72 = 字宽缩到七成，字距跟着变紧）
        g.scale(0.72D, 1.0D);
        g.setFont(font);
        g.setColor(java.awt.Color.WHITE);
        g.drawString(text, 0, pg.getFontMetrics(font).getAscent());
        g.dispose();

        // 2) 每个像素 → 一颗灯珠
        final int outW = textWidth * DOT;
        final int outH = textHeight * DOT;
        // true = 清零：未点亮的像素必须是透明，否则是未初始化内存的花屏
        final NativeImage image = new NativeImage(NativeImage.Format.RGBA, outW, outH, true);
        final int r = (LED_COLOR >> 16) & 0xFF;
        final int gg = (LED_COLOR >> 8) & 0xFF;
        final int b = LED_COLOR & 0xFF;
        for (int y = 0; y < textHeight; y++) {
            for (int x = 0; x < textWidth; x++) {
                final boolean lit = (mask.getRGB(x, y) >>> 24) > 96;
                if (!lit) {
                    continue;
                }
                for (int dy = 0; dy < DOT_FILL; dy++) {
                    for (int dx = 0; dx < DOT_FILL; dx++) {
                        // NativeImage.setColor 要的是 ABGR，不是 ARGB
                        image.setColor(x * DOT + dx, y * DOT + dy, (0xFF << 24) | (b << 16) | (gg << 8) | r);
                    }
                }
            }
        }

        final Identifier id = new Identifier("mtrlte", "led/" + Integer.toHexString(text.hashCode()));
        // 必须用同一个 id 注册，否则渲染时按 id 找不到贴图（紫黑块）
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(image));
        return new Entry(id, (float) outW / (float) outH);
    }
}
