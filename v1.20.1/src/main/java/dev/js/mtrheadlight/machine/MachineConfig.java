// 所有售票机共用的一份配置：模式、面板颜色数据源、LED 文字。
package dev.js.mtrheadlight.machine;

/**
 * 售票机的统一配置。
 *
 * <p>{@code caps} 是这台款式支持的项（位标记）：面板上不支持的按钮就置灰，和以前一样。
 *
 * @param mode        0 = 方速界面，1 = 原版界面，2 = 关机
 * @param colorSource 0 = 车站色，1 = 线路色，2 = 玩家自选
 * @param custom      自选色（0xRRGGBB）
 * @param line        选中的线路色（0xRRGGBB）
 * @param text        LED 文字
 * @param scroll      LED 是否滚动
 * @param caps        这台机器支持的配置项（{@link #CAP_MODE} 等按位或）
 */
public record MachineConfig(int mode, int colorSource, int custom, int line,
                            String text, boolean scroll, int caps) {

    /** 支持「售票机模式」这一行。 */
    public static final int CAP_MODE = 1;
    /** 支持「面板颜色数据源」这一行（含自选色 / 线路色）。 */
    public static final int CAP_COLOR = 2;
    /** 支持「LED 文字」这一行。 */
    public static final int CAP_LED = 4;

    public static final String DEFAULT_TEXT = "正常服务";

    /** 这台机器支持这一行吗。 */
    public boolean supports(final int cap) {
        return (this.caps & cap) != 0;
    }

    /** 换一份内容，保留 caps。 */
    public MachineConfig with(final int newMode, final int newSource, final int newCustom,
                              final int newLine, final String newText, final boolean newScroll) {
        return new MachineConfig(newMode, newSource, newCustom, newLine, newText, newScroll, this.caps);
    }

    /** 归一化：越界值收敛到合法范围，文字为空时回落到默认文字。 */
    public MachineConfig normalized() {
        final String safeText = this.text == null || this.text.isBlank() ? DEFAULT_TEXT : this.text;
        return with(clamp(this.mode, 0, 2), clamp(this.colorSource, 0, 2),
                this.custom & 0xFFFFFF, this.line & 0xFFFFFF, safeText, this.scroll);
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}
