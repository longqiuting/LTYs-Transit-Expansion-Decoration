package dev.js.mtrheadlight.decor;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * LTED 仪表板（{@code mtrlte:lted_dashboard} 右键打开）—— 装饰侧自己的入口。
 *
 * <h2>为什么单独一个界面</h2>
 * 拆分前这三个入口塞在 LTE 仪表板的「其他」分区里（{@code ParameterScreen}），属于装饰内容却挂在
 * 优化侧：厦门 PIDS 设置、全局广告集、时钟全局配置。拆分后它们搬到这一页，LTE 的仪表板只剩
 * 光源 / 碰撞 / 粒子 / 伤害 / 其他（内置车型、重载）。
 *
 * <h2>分区（自上而下）</h2>
 * <ol>
 *   <li><b>显示屏</b> —— 厦门 PIDS 设置（{@code PidsXiamenSettingsScreen}）、全局广告集
 *       （{@code PidsXiamenAdSetScreen}）；PSD-LCD 本版没有全局面板参数，只留一行说明；</li>
 *   <li><b>时钟</b> —— 时钟全局设置（{@code ClockConfigScreen} 的全局模式）；</li>
 *   <li><b>设备</b> —— 安检机 / 售卖机 / 充电宝机本版都没有全局项（都是单块 NBT + 刷子配置），
 *       所以只留一行说明，不建空行；</li>
 *   <li><b>其他</b> —— 装饰配色（线路条带 / 站名位置 / 门灯颜色，写在 {@code config/mtrlte_decor.json}）
 *       与版本号。</li>
 * </ol>
 *
 * <h2>排版规则（与 LTE 仪表板一致）</h2>
 * 开关在上、滑块在下（这一页没有滑块 —— 装饰项的数值都是单块 NBT，用刷子改）；每个可改的行末尾
 * 一个方形 <b>↺</b>「复原」，写回 {@link DecorConfig#defaults()} 里同一个字段。
 *
 * <p>「进入子界面」的几行（PIDS / 广告集 / 时钟）写的是服务端 PersistentState，改一下立刻生效，
 * 所以没有 ↺ —— 复原在各自子界面里。本页<b>不写盘、不拍快照</b>，只有装饰配色那三行直接写
 * {@code mtrlte_decor.json}。
 */
public final class LtedDashboardScreen extends Screen {
    private static final Identifier OPTIONS_BACKGROUND =
            new Identifier("textures/gui/options_background.png");

    private static final int ROW_H = 20;
    /** 20px 按钮 + 4px 间隙。 */
    private static final int PITCH = 24;
    private static final int GAP = 8;
    /** 方形 ↺ 的边长 = 行高。 */
    private static final int RESET_W = 20;
    private static final int MIN_COLUMN = 176;
    private static final int MAX_COLUMN = 320;
    /** 分区标题占的高度（一行 9px 字体 + 间距）。 */
    private static final int HEADER_H = 16;
    /** 不可点击的说明行 / 版本行。 */
    private static final int NOTE_H = 12;

    private static final int CONTENT_TOP = 46;
    private static final int FOOTER_H = 36;

    private final Screen parent;

    private int colX;
    private int colW;
    /** 竖直滚动偏移（像素）。分区加起来会比 240px 窗口高，滚轮翻。 */
    private int scroll;
    private int contentHeight;

    public LtedDashboardScreen(final Screen parent) {
        super(tr("screen.mtrlte.lted_dashboard"));
        this.parent = parent;
    }

    private static MutableText tr(final String key, final Object... args) {
        return Text.translatable(key, args);
    }

    /**
     * 打开这一页（物品右键 / 将来的指令入口都走这里）。
     *
     * <p>{@code client.send}：物品的 {@code use} 可能不在渲染线程上，而建界面只能在客户端线程。
     */
    public static void open() {
        try {
            final MinecraftClient client = MinecraftClient.getInstance();
            client.send(() -> client.setScreen(new LtedDashboardScreen(client.currentScreen)));
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] could not open the LTED dashboard", t);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        this.headers.clear();
        this.notes.clear();
        // 上一次 init 算出的滚动上限：窗口变小时先把 scroll 夹回来，再按新偏移摆控件。
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, this.maxScroll)));

        final int maxCol = Math.max(MIN_COLUMN, this.width - 24);
        this.colW = Math.min(MAX_COLUMN, maxCol);
        this.colX = this.width / 2 - this.colW / 2;

        final int viewBottom = this.height - FOOTER_H;
        int y = CONTENT_TOP - this.scroll;

        // ---- 显示屏 ----
        y = header(y, "screen.mtrlte.lted.sec.display");
        y = actionRow(y, tr("screen.mtrlte.pids_xiamen"), b -> openPidsSettings());
        y = actionRow(y, tr("screen.mtrlte.pids_ad_set"), b -> openAdSet());
        y = note(y, "screen.mtrlte.lted.psd_lcd.none");

        // ---- 时钟 ----
        y = header(y, "screen.mtrlte.lted.sec.clock");
        y = actionRow(y, tr("screen.mtrlte.clock.global"), b -> openClockGlobal());

        // ---- 设备（本版没有全局项 → 不建行） ----
        y = header(y, "screen.mtrlte.lted.sec.device");
        y = note(y, "screen.mtrlte.lted.device.none");

        // ---- 其他：装饰配色 + 版本 ----
        y = header(y, "screen.mtrlte.lted.sec.other");
        y = toggleRow(y, "screen.mtrlte.lted.strip", cfg -> cfg.xiamenRouteStrip,
                (cfg, v) -> cfg.xiamenRouteStrip = v);
        y = toggleRow(y, "screen.mtrlte.lted.names_below", cfg -> cfg.routeStripNamesBelow,
                (cfg, v) -> cfg.routeStripNamesBelow = v);
        y = doorLightRow(y);
        // 方速屏蔽门的开关门音效总开关（拆分前在优化侧的配置界面里，现在只有这一处入口）。
        y = toggleRow(y, "screen.mtrlte.lted.door_sound", cfg -> cfg.fangsuDoorSound,
                (cfg, v) -> cfg.fangsuDoorSound = v);

        this.contentHeight = y - (CONTENT_TOP - this.scroll);

        // 版本行贴着内容末尾（跟着滚动）。
        this.versionY = y + 2;

        // 底部：关闭。
        final int footerY = this.height - 30;
        this.addDrawableChild(ButtonWidget.builder(tr("gui.done"),
                b -> close()).dimensions(this.colX, footerY, this.colW, ROW_H).build());

        // 滚动范围：内容 + 版本行不要盖到底栏。
        final int visible = (this.height - FOOTER_H) - CONTENT_TOP;
        this.maxScroll = Math.max(0, this.contentHeight + HEADER_H - visible);
        if (this.scroll > this.maxScroll) {
            this.scroll = this.maxScroll;
        }
        if (this.scroll < 0) {
            this.scroll = 0;
        }
    }

    private int maxScroll;
    private int versionY;

    private int header(final int yGuess, final String key) {
        // 标题是纯绘制（不是控件），位置记下来给 render() 用。
        this.headers.add(new Header(yGuess, key));
        return yGuess + HEADER_H;
    }

    private int note(final int yGuess, final String key) {
        this.notes.add(new Note(yGuess, key));
        return yGuess + NOTE_H;
    }

    /** 这一行是否落在可视区内（滚出上/下边界就不建控件，免得压到标题 / 底栏上）。 */
    private boolean inView(final int y) {
        return y + ROW_H > CONTENT_TOP && y < this.height - FOOTER_H;
    }

    private int actionRow(final int yGuess, final Text label, final ButtonWidget.PressAction action) {
        final int w = this.colW - RESET_W - GAP;
        if (inView(yGuess)) {
            this.addDrawableChild(ButtonWidget.builder(label, action)
                    .dimensions(this.colX, yGuess, w, ROW_H).build());
        }
        return yGuess + PITCH;
    }

    /** 开关行：左格是开关（开/关），右格是 ↺。 */
    private int toggleRow(final int yGuess, final String labelKey,
                          final java.util.function.Function<DecorConfig, Boolean> getter,
                          final BoolSetter setter) {
        if (!inView(yGuess)) {
            return yGuess + PITCH;
        }
        final int w = this.colW - RESET_W - GAP;
        final boolean[] value = {Boolean.TRUE.equals(getter.apply(DecorConfig.get()))};
        final ButtonWidget[] holder = new ButtonWidget[1];
        holder[0] = this.addDrawableChild(ButtonWidget.builder(toggleLabel(labelKey, value[0]), b -> {
            value[0] = !value[0];
            setter.set(DecorConfig.get(), value[0]);
            saveDecor();
            b.setMessage(toggleLabel(labelKey, value[0]));
        }).dimensions(this.colX, yGuess, w, ROW_H).build());
        this.addDrawableChild(ButtonWidget.builder(tr("btn.resetOne"), b -> {
            final DecorConfig def = DecorConfig.defaults();
            final boolean d = Boolean.TRUE.equals(getter.apply(def));
            value[0] = d;
            setter.set(DecorConfig.get(), d);
            saveDecor();
            holder[0].setMessage(toggleLabel(labelKey, d));
        }).dimensions(this.colX + w + GAP, yGuess, RESET_W, ROW_H).tooltip(
                net.minecraft.client.gui.tooltip.Tooltip.of(tr("screen.mtrlte.lted.reset"))).build());
        return yGuess + PITCH;
    }

    /** 门灯颜色：点一下在 orange → green → blue 里循环，末尾 ↺ 回默认橙。 */
    private int doorLightRow(final int yGuess) {
        if (!inView(yGuess)) {
            return yGuess + PITCH;
        }
        final int w = this.colW - RESET_W - GAP;
        final ButtonWidget[] holder = new ButtonWidget[1];
        holder[0] = this.addDrawableChild(ButtonWidget.builder(doorLightLabel(), b -> {
            final DecorConfig cfg = DecorConfig.get();
            final String next = switch (DecorConfig.normalizeDoorLightColor(cfg.doorLightColor)) {
                case DecorConfig.DOOR_LIGHT_ORANGE -> DecorConfig.DOOR_LIGHT_GREEN;
                case DecorConfig.DOOR_LIGHT_GREEN -> DecorConfig.DOOR_LIGHT_BLUE;
                default -> DecorConfig.DOOR_LIGHT_ORANGE;
            };
            cfg.doorLightColor = next;
            saveDecor();
            b.setMessage(doorLightLabel());
        }).dimensions(this.colX, yGuess, w, ROW_H).build());
        this.addDrawableChild(ButtonWidget.builder(tr("btn.resetOne"), b -> {
            DecorConfig.get().doorLightColor = DecorConfig.defaults().doorLightColor;
            saveDecor();
            holder[0].setMessage(doorLightLabel());
        }).dimensions(this.colX + w + GAP, yGuess, RESET_W, ROW_H).tooltip(
                net.minecraft.client.gui.tooltip.Tooltip.of(tr("screen.mtrlte.lted.reset"))).build());
        return yGuess + PITCH;
    }

    private Text doorLightLabel() {
        return tr("screen.mtrlte.lted.door_light",
                tr("screen.mtrlte.lted.door_light." + DecorConfig.normalizeDoorLightColor(
                        DecorConfig.get().doorLightColor)));
    }

    private static Text toggleLabel(final String labelKey, final boolean on) {
        return tr("screen.mtrlte.lted.toggle", tr(labelKey), tr(on ? "config.on" : "config.off"));
    }

    /** 三个装饰配色行改完直接落盘（不进主界面的完成/取消：它们没有快照）。 */
    private static void saveDecor() {
        try {
            DecorConfig.get().save();
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] could not save the decor config", t);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 子界面
    // ---------------------------------------------------------------------------------------------

    private void openPidsSettings() {
        final MinecraftClient client = this.client;
        if (client == null) {
            return;
        }
        client.setScreen(new dev.js.mtrheadlight.xiamen.pids.PidsXiamenSettingsScreen(this));
    }

    private void openAdSet() {
        final MinecraftClient client = this.client;
        if (client == null) {
            return;
        }
        client.setScreen(new dev.js.mtrheadlight.xiamen.pids.PidsXiamenAdSetScreen(this));
    }

    private void openClockGlobal() {
        final MinecraftClient client = this.client;
        if (client == null) {
            return;
        }
        dev.js.mtrheadlight.clock.ClockClient.openGlobalScreen(client);
    }

    // ---------------------------------------------------------------------------------------------
    // Render
    // ---------------------------------------------------------------------------------------------

    @Override
    public void render(final DrawContext context, final int mouseX, final int mouseY, final float delta) {
        drawVanillaBackground(context);
        super.render(context, mouseX, mouseY, delta);

        drawCentered(context, this.title, this.width / 2, 16, 0xFFFFFF);
        drawCentered(context, this.hint(), this.width / 2, 30, 0xB0B0B0);

        for (final Header h : this.headers) {
            if (h.y < CONTENT_TOP - HEADER_H || h.y > this.height - FOOTER_H) {
                continue;
            }
            context.drawTextWithShadow(this.textRenderer, tr(h.key), this.colX, h.y + 3, 0xFFD080);
        }
        for (final Note n : this.notes) {
            if (n.y < CONTENT_TOP - NOTE_H || n.y > this.height - FOOTER_H) {
                continue;
            }
            context.drawTextWithShadow(this.textRenderer, tr(n.key), this.colX, n.y + 1, 0x909090);
        }
        context.drawTextWithShadow(this.textRenderer, this.versionText(), this.colX, this.versionY, 0x808080);
    }

    private Text hint() {
        if (this.maxScroll <= 0) {
            return tr("screen.mtrlte.lted_dashboard.hint");
        }
        return tr("screen.mtrlte.lted_dashboard.hint.scroll");
    }

    private Text versionText() {
        String version = "?";
        try {
            version = FabricLoader.getInstance().getModContainer("mtrlte")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
        } catch (Throwable ignored) {
            // 拿不到就当 "?"，不影响界面。
        }
        return tr("screen.mtrlte.lted.version", version,
                FabricLoader.getInstance().getModContainer("minecraft")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?"));
    }

    private void drawVanillaBackground(final DrawContext context) {
        context.drawTexture(OPTIONS_BACKGROUND, 0, 0, 0, 0, this.width, this.height, 32, 32);
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    private void drawCentered(final DrawContext context, final Text text, final int cx,
                              final int y, final int color) {
        context.drawCenteredTextWithShadow(this.textRenderer, text, cx, y, color);
    }

    // ---------------------------------------------------------------------------------------------
    // Input
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double amount) {
        if (this.maxScroll > 0) {
            final int before = this.scroll;
            this.scroll = Math.max(0, Math.min(this.maxScroll,
                    this.scroll - (int) Math.signum(amount) * PITCH));
            if (this.scroll != before) {
                this.clearAndInit();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void close() {
        final MinecraftClient client = this.client;
        if (client != null) {
            client.setScreen(this.parent);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 绘制用的小记录
    // ---------------------------------------------------------------------------------------------

    private final java.util.List<Header> headers = new java.util.ArrayList<>();
    private final java.util.List<Note> notes = new java.util.ArrayList<>();

    /** 开关行的写回器（JDK 没有 {@code ObjBooleanConsumer}，自己写一个）。 */
    @FunctionalInterface
    private interface BoolSetter {
        void set(DecorConfig cfg, boolean value);
    }

    private record Header(int y, String key) {
    }

    private record Note(int y, String key) {
    }
}
