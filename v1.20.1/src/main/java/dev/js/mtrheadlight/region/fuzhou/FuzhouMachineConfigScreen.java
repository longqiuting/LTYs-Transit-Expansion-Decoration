// 售票机配置面板：模式 / 面板颜色数据源 / 自选色 / 线路色 / LED 文字。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.ColorPickerScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 福州地铁售票机的配置面板（刷子右键打开）。
 *
 * <h2>包含什么</h2>
 * <ul>
 *   <li><b>售票机模式</b>：方速 / 原版 / 关机 —— 和现有售票机一致，只影响行为与开屏；</li>
 *   <li><b>面板颜色数据源</b>：车站色 / 线路色 / 玩家自选；选「自选」时下面出现颜色块，
 *       点一下打开取色面板；选「线路色」时列出该站线路的颜色，点一下就选它；</li>
 *   <li><b>LED 文字</b>：输入框 + 滚动/固定切换。</li>
 * </ul>
 *
 * <p>「完成」把整份配置发回服务端（服务端写进方块实体并同步），「取消」直接关掉不改。
 */
public final class FuzhouMachineConfigScreen extends Screen {

    private static final int W = 240;

    private final BlockPos pos;
    private final List<Integer> lineColors;
    private int mode;
    private int source;
    private int customColor;
    private int lineColor;
    private boolean scroll;
    private String ledText;

    private TextFieldWidget textField;
    private final List<ButtonWidget> modeButtons = new ArrayList<>();
    private final List<ButtonWidget> sourceButtons = new ArrayList<>();
    private ButtonWidget customButton;
    private final List<ButtonWidget> lineButtons = new ArrayList<>();
    private ButtonWidget scrollButton;

    public FuzhouMachineConfigScreen(final BlockPos pos, final int mode, final int source,
                                     final int customColor, final int lineColor,
                                     final String ledText, final boolean scroll,
                                     final List<Integer> lineColors) {
        super(Text.literal("售票机设置"));
        this.pos = pos;
        this.mode = mode;
        this.source = source;
        this.customColor = customColor;
        this.lineColor = lineColor;
        this.ledText = ledText;
        this.scroll = scroll;
        this.lineColors = lineColors;
    }

    @Override
    protected void init() {
        final int left = this.width / 2 - W / 2;
        int y = this.height / 2 - 92;

        // 模式
        final String[] modeNames = {"方速", "原版", "关机"};
        this.modeButtons.clear();
        for (int i = 0; i < 3; i++) {
            final int value = i;
            final ButtonWidget button = ButtonWidget.builder(Text.literal(modeNames[i]), b -> {
                this.mode = value;
                refresh();
            }).dimensions(left + i * 80, y, 76, 20).build();
            this.modeButtons.add(button);
            this.addDrawableChild(button);
        }
        y += 28;

        // 数据源
        final String[] sourceNames = {"车站色", "线路色", "自选色"};
        this.sourceButtons.clear();
        for (int i = 0; i < 3; i++) {
            final int value = i;
            final ButtonWidget button = ButtonWidget.builder(Text.literal(sourceNames[i]), b -> {
                this.source = value;
                refresh();
            }).dimensions(left + i * 80, y, 76, 20).build();
            this.sourceButtons.add(button);
            this.addDrawableChild(button);
        }
        y += 28;

        // 自选色
        this.customButton = ButtonWidget.builder(Text.literal("自选颜色…"), b ->
                this.client.setScreen(new ColorPickerScreen(this, "面板颜色", this.customColor, rgb -> {
                    this.customColor = rgb & 0xFFFFFF;
                    this.client.setScreen(this);
                }))).dimensions(left, y, 116, 20).build();
        this.addDrawableChild(this.customButton);

        // 线路色色块
        this.lineButtons.clear();
        int x = left + 124;
        for (int i = 0; i < Math.min(5, this.lineColors.size()); i++) {
            final int rgb = this.lineColors.get(i);
            final ButtonWidget button = ButtonWidget.builder(Text.literal("■"), b -> {
                this.lineColor = rgb;
                refresh();
            }).dimensions(x, y, 20, 20).build();
            this.lineButtons.add(button);
            this.addDrawableChild(button);
            x += 22;
        }
        y += 28;

        // LED 文字
        this.textField = new TextFieldWidget(this.textRenderer, left, y, 170, 20, Text.literal("LED 文字"));
        this.textField.setMaxLength(64);
        this.textField.setText(this.ledText);
        this.textField.setChangedListener(s -> this.ledText = s);
        this.addDrawableChild(this.textField);

        this.scrollButton = ButtonWidget.builder(scrollLabel(), b -> {
            this.scroll = !this.scroll;
            b.setMessage(scrollLabel());
        }).dimensions(left + 178, y, 62, 20).build();
        this.addDrawableChild(this.scrollButton);
        y += 32;

        // 完成 / 取消
        this.addDrawableChild(ButtonWidget.builder(Text.literal("完成"), b -> {
            final PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(this.pos);
            buf.writeVarInt(this.mode);
            buf.writeVarInt(this.source);
            buf.writeInt(this.customColor);
            buf.writeInt(this.lineColor);
            buf.writeString(this.ledText, 128);
            buf.writeBoolean(this.scroll);
            ClientPlayNetworking.send(FuzhouMachineNet.CHANNEL_SAVE, buf);
            this.close();
        }).dimensions(left, y, 116, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("取消"), b -> this.close())
                .dimensions(left + 124, y, 116, 20).build());

        refresh();
    }

    private Text scrollLabel() {
        return Text.literal(this.scroll ? "滚动" : "固定");
    }

    /** 按当前模式/数据源把按钮的可用状态刷一遍（不支持的项置灰）。 */
    private void refresh() {
        for (int i = 0; i < this.modeButtons.size(); i++) {
            this.modeButtons.get(i).setMessage(Text.literal((this.mode == i ? "▶ " : "") +
                    new String[]{"方速", "原版", "关机"}[i]));
        }
        for (int i = 0; i < this.sourceButtons.size(); i++) {
            this.sourceButtons.get(i).setMessage(Text.literal((this.source == i ? "▶ " : "") +
                    new String[]{"车站色", "线路色", "自选色"}[i]));
        }
        if (this.customButton != null) {
            this.customButton.active = this.source == FuzhouMachineBlockEntity.COLOR_CUSTOM;
            this.customButton.setMessage(Text.literal("自选颜色：#" + String.format("%06X", this.customColor)));
        }
        for (int i = 0; i < this.lineButtons.size(); i++) {
            this.lineButtons.get(i).active = this.source == FuzhouMachineBlockEntity.COLOR_LINE;
        }
        if (this.scrollButton != null) {
            this.scrollButton.setMessage(scrollLabel());
        }
    }

    @Override
    public void render(final DrawContext context, final int mouseX, final int mouseY, final float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2,
                this.height / 2 - 112, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
