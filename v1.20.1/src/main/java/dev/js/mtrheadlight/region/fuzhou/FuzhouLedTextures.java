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
import java.util.Iterator;
import java.util.Map;

/**
 * LED 屏贴图缓存：一个文字 → 一张点阵图。
 *
 * <h2>为什么先画小字再放大</h2>
 * <p>直接按大字号画出来是平滑字体，没有 LED 的颗粒感。这里先用 12px 无衬线字体把文字画成
 * 1:1 的小位图，再把每个像素扩成一个 {@value #DOT}px 的方块（中间留缝），看起来就是
 * 一颗颗灯珠 —— 和 MTR4 滚动屏那种点阵观感一致。
 *
 * <p>颜色：暗绿（正文），底色透明（屏幕本身的深色由方块贴图提供）。
 *
 * <h2>生命周期（别让它变成内存泄漏）</h2>
 * <ul>
 *   <li>贴图 key 是玩家写的文字 —— 如果不回收，每改一次文字就永久多一张 NativeImage + GL 贴图；</li>
 *   <li>所以每条都记 {@code lastUse}，超过 {@value #TTL_MS}ms 没被渲染到就
 *       {@code destroyTexture} 并 close 掉；</li>
 *   <li>断线 / 换存档时整表清空（{@link #clear()}，由客户端入口挂 DISCONNECT 调用）；</li>
 *   <li>文字长度在生成前截到 {@value #MAX_TEXT} 字，挡住存档里的超长串。</li>
 * </ul>
 */
public final class FuzhouLedTextures {

    /** 一颗灯珠占多少像素（含 1px 缝）。 */
    private static final int DOT = 3;
    /** 灯珠边长（DOT 里的实心部分）。 */
    private static final int DOT_FILL = 2;
    /** LED 文字颜色（暗绿）。 */
    private static final int LED_COLOR = 0x2FA34C;
    private static final int FONT_SIZE = 12;
    /** 文字最多这么长（和面板输入框上限一致）。 */
    private static final int MAX_TEXT = 64;
    /** 多久没被用到就回收（毫秒）。同时显示的多半是同一句，留 10 秒足够。 */
    private static final long TTL_MS = 10_000L;
    /** 清理的最短间隔，免得每帧都遍历一遍。 */
    private static final long SWEEP_INTERVAL_MS = 5_000L;

    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    private static long lastSweep;

    private FuzhouLedTextures() {
    }

    /** 一张贴图 + 它在世界里的宽高比（宽 / 高）。 */
    public static final class Entry {
        public final Identifier id;
        public final float aspect;

        private final NativeImageBackedTexture texture;
        private volatile long lastUse;

        Entry(final Identifier id, final float aspect, final NativeImageBackedTexture texture) {
            this.id = id;
            this.aspect = aspect;
            this.texture = texture;
            this.lastUse = System.currentTimeMillis();
        }
    }

    /** 取（或生成）一段文字的点阵贴图；失败返回 {@code null}（屏就留空）。 */
    public static Entry get(final String rawText) {
        final String text = truncate(rawText);
        final Entry cached = ENTRIES.get(text);
        if (cached != null) {
            cached.lastUse = System.currentTimeMillis();
            return cached;
        }
        try {
            final Entry entry = build(text);
            ENTRIES.put(text, entry);
            sweep();
            return entry;
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] led texture unavailable", t);
            return null;
        }
    }

    /** 把过期的条目丢掉（贴图和像素一起释放）。 */
    public static void sweep() {
        final long now = System.currentTimeMillis();
        if (now - lastSweep < SWEEP_INTERVAL_MS) {
            return;
        }
        lastSweep = now;
        final Iterator<Map.Entry<String, Entry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            final Entry entry = it.next().getValue();
            if (now - entry.lastUse > TTL_MS) {
                it.remove();
                destroy(entry);
            }
        }
    }

    /** 断线 / 换存档：整表清空。 */
    public static void clear() {
        for (final Entry entry : ENTRIES.values()) {
            destroy(entry);
        }
        ENTRIES.clear();
        lastSweep = 0L;
    }

    private static void destroy(final Entry entry) {
        try {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(entry.id);
        } catch (final Throwable t) {
            LteLog.LOGGER.debug("[lte] led texture could not be released", t);
        }
    }

    private static String truncate(final String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return FuzhouMachineBlockEntity.DEFAULT_TEXT;
        }
        return rawText.length() > MAX_TEXT ? rawText.substring(0, MAX_TEXT) : rawText;
    }

    /** 贴图 id：能读出来的部分 + 完整哈希 —— 只取哈希会撞（"Aa" 和 "BB" 同哈希），撞了就会串字。 */
    private static Identifier idOf(final String text) {
        final StringBuilder slug = new StringBuilder();
        for (int i = 0; i < text.length() && slug.length() < 16; i++) {
            final char c = text.charAt(i);
            slug.append(Character.isLetterOrDigit(c) && c < 128 ? c : '_');
        }
        return new Identifier("mtrlte", "led/" + slug + "_" + Integer.toHexString(text.hashCode()));
    }

    private static Entry build(final String text) {
        // 1) 小字位图（1:1，白字黑底，方便当遮罩用）
        final Font font = new Font(Font.SANS_SERIF, Font.BOLD, FONT_SIZE);
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
        try {
            final int r = (LED_COLOR >> 16) & 0xFF;
            final int gg = (LED_COLOR >> 8) & 0xFF;
            final int b = LED_COLOR & 0xFF;
            for (int y = 0; y < textHeight; y++) {
                for (int x = 0; x < textWidth; x++) {
                    if ((mask.getRGB(x, y) >>> 24) <= 96) {
                        continue;
                    }
                    for (int dy = 0; dy < DOT_FILL; dy++) {
                        for (int dx = 0; dx < DOT_FILL; dx++) {
                            // NativeImage.setColor 要的是 ABGR，不是 ARGB
                            image.setColor(x * DOT + dx, y * DOT + dy,
                                    (0xFF << 24) | (b << 16) | (gg << 8) | r);
                        }
                    }
                }
            }

            final Identifier id = idOf(text);
            final NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            // 必须用同一个 id 注册，否则渲染时按 id 找不到贴图（紫黑块）
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
            return new Entry(id, (float) outW / (float) outH, texture);
        } catch (final Throwable t) {
            // 注册没成功，像素的所有权还在我们手里 —— 必须自己关，否则堆外内存漏一次
            image.close();
            throw t;
        }
    }
}
