// 所有售票机通用的配置面板：模式 / 颜色数据源 / LED 文字，不支持的项置灰。
package dev.js.mtrheadlight.machine;

import dev.js.mtrheadlight.ColorPickerScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 售票机配置面板（所有款式共用）。
 *
 * <p>三行：<b>售票机模式</b>（方速 / 原版 / 关机）、<b>面板颜色数据源</b>（车站色 / 线路色 / 自选色，
 * 自选色点开取色面板、线路色点色块）、<b>LED 文字</b>（输入框 + 滚动/固定）。
 *
 * <p>款式不支持的行整行不出现、按钮全部置灰 —— 厦门与港铁那两台老售票机只有模式一行。
 */
public final class MachineConfigScreen extends Screen {

    private static final int W = 240;

    private final BlockPos pos;
    private final List<Integer> lineColors;
    private MachineConfig config;

    private final List<ButtonWidget> modeButtons = new ArrayList<>();
    private final List<ButtonWidget> sourceButtons = new ArrayList<>();
    private final List<ButtonWidget> lineButtons = new ArrayList<>();
    private ButtonWidget customButton;
    private ButtonWidget scrollButton;

    public MachineConfigScreen(final BlockPos pos, final MachineConfig config,
                               final List<Integer> lineColors) {
        super(Text.literal("售票机设置"));
        this.pos = pos;
        this.config = config;
        this.lineColors = lineColors;
    }

    @Override
    protected void init() {
        final int left = this.width / 2 - W / 2;
        int y = this.height / 2 - 84;

        // ── 售票机模式 ──
        final String[] modeNames = {"方速", "原版", "关机"};
        this.modeButtons.clear();
        for (int i = 0; i < 3; i++) {
            final int value = i;
            final ButtonWidget button = ButtonWidget.builder(Text.literal(modeNames[i]), b -> {
                this.config = this.config.with(value, this.config.colorSource(), this.config.custom(),
                        this.config.line(), this.config.text(), this.config.scroll());
                refresh();
            }).dimensions(left + i * 80, y, 76, 20).build();
            button.active = this.config.supports(MachineConfig.CAP_MODE);
            this.modeButtons.add(button);
            this.addDrawableChild(button);
        }
        y += 28;

        // ── 面板颜色数据源 ──
        if (this.config.supports(MachineConfig.CAP_COLOR)) {
            final String[] sourceNames = {"车站色", "线路色", "自选色"};
            this.sourceButtons.clear();
            for (int i = 0; i < 3; i++) {
                final int value = i;
                final ButtonWidget button = ButtonWidget.builder(Text.literal(sourceNames[i]), b -> {
                    this.config = this.config.with(this.config.mode(), value, this.config.custom(),
                            this.config.line(), this.config.text(), this.config.scroll());
                    refresh();
                }).dimensions(left + i * 80, y, 76, 20).build();
                this.sourceButtons.add(button);
                this.addDrawableChild(button);
            }
            y += 28;

            this.customButton = ButtonWidget.builder(Text.literal("自选颜色…"), b ->
                    this.client.setScreen(new ColorPickerScreen(this, "面板颜色", this.config.custom(), rgb -> {
                        this.config = this.config.with(this.config.mode(), this.config.colorSource(),
                                rgb & 0xFFFFFF, this.config.line(), this.config.text(), this.config.scroll());
                        this.client.setScreen(this);
                    }))).dimensions(left, y, 116, 20).build();
            this.addDrawableChild(this.customButton);

            this.lineButtons.clear();
            int x = left + 124;
            for (int i = 0; i < Math.min(5, this.lineColors.size()); i++) {
                final int rgb = this.lineColors.get(i);
                final ButtonWidget button = ButtonWidget.builder(Text.literal("■"), b -> {
                    this.config = this.config.with(this.config.mode(), this.config.colorSource(),
                            this.config.custom(), rgb, this.config.text(), this.config.scroll());
                    refresh();
                }).dimensions(x, y, 20, 20).build();
                this.lineButtons.add(button);
                this.addDrawableChild(button);
                x += 22;
            }
            y += 28;
        }

        // ── LED 文字 ──
        if (this.config.supports(MachineConfig.CAP_LED)) {
            final TextFieldWidget field = new TextFieldWidget(this.textRenderer, left, y, 170, 20,
                    Text.literal("LED 文字"));
            field.setMaxLength(64);
            field.setText(this.config.text());
            field.setChangedListener(s -> this.config = this.config.with(this.config.mode(),
                    this.config.colorSource(), this.config.custom(), this.config.line(), s,
                    this.config.scroll()));
            this.addDrawableChild(field);

            this.scrollButton = ButtonWidget.builder(scrollLabel(), b -> {
                this.config = this.config.with(this.config.mode(), this.config.colorSource(),
                        this.config.custom(), this.config.line(), this.config.text(),
                        !this.config.scroll());
                b.setMessage(scrollLabel());
            }).dimensions(left + 178, y, 62, 20).build();
            this.addDrawableChild(this.scrollButton);
            y += 32;
        }

        // ── 完成 / 取消 ──
        this.addDrawableChild(ButtonWidget.builder(Text.literal("完成"), b -> {
            final MachineConfig finalConfig = this.config.normalized();
            final PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(this.pos);
            buf.writeVarInt(finalConfig.mode());
            buf.writeVarInt(finalConfig.colorSource());
            buf.writeInt(finalConfig.custom());
            buf.writeInt(finalConfig.line());
            buf.writeString(finalConfig.text(), 128);
            buf.writeBoolean(finalConfig.scroll());
            ClientPlayNetworking.send(MachineConfigNet.CHANNEL_SAVE, buf);
            this.close();
        }).dimensions(left, y, 116, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("取消"), b -> this.close())
                .dimensions(left + 124, y, 116, 20).build());

        refresh();
    }

    private Text scrollLabel() {
        return Text.literal(this.config.scroll() ? "滚动" : "固定");
    }

    /** 选中项前面加 ▶，没选中的保持原样；不支持的项保持置灰。 */
    private void refresh() {
        final String[] modeNames = {"方速", "原版", "关机"};
        for (int i = 0; i < this.modeButtons.size(); i++) {
            this.modeButtons.get(i).setMessage(Text.literal((this.config.mode() == i ? "▶ " : "") + modeNames[i]));
        }
        final String[] sourceNames = {"车站色", "线路色", "自选色"};
        for (int i = 0; i < this.sourceButtons.size(); i++) {
            this.sourceButtons.get(i).setMessage(Text.literal(
                    (this.config.colorSource() == i ? "▶ " : "") + sourceNames[i]));
        }
        if (this.customButton != null) {
            this.customButton.active = this.config.colorSource() == 2;
            this.customButton.setMessage(Text.literal(String.format("自选颜色：#%06X", this.config.custom())));
        }
        for (final ButtonWidget button : this.lineButtons) {
            button.active = this.config.colorSource() == 1;
        }
        if (this.scrollButton != null) {
            this.scrollButton.setMessage(scrollLabel());
        }
    }

    @Override
    public void render(final DrawContext context, final int mouseX, final int mouseY, final float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2,
                this.height / 2 - 104, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
