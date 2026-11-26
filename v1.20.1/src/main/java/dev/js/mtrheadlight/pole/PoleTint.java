// 柱子那一圈的染色 provider（Fabric 侧）。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;

/**
 * 柱子那一圈的染色 provider。
 *
 * <p>模型 {@code amtr_pole_bottom.json} 里 {@code color} 分组的那 8 个 element，每个面都带
 * {@code "tintindex": 0}；MC 画这些面时会来这里问颜色。这里注册的是
 * {@code ColorProviderRegistry.BLOCK}（Forge 侧对应 {@code RegisterColorHandlersEvent.Block}，
 * 见 {@code forge/.../pole/PoleTint.java}）。
 *
 * <p>取色只读方块实体里缓存好的字段（{@link AmtrPoleBottomBlockEntity#effectiveColor}）——
 * 这个方法是<b>每帧每面</b>都调的，绝不能在里面查 MTR 数据。
 */
public final class PoleTint {

    private PoleTint() {
    }

    public static void register() {
        try {
            final AmtrPoleBottomBlock block = PoleBlocks.bottom();
            if (block == null) {
                return;
            }
            ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) ->
                    tintIndex == 0 ? AmtrPoleBottomBlock.effectiveTint(world, pos) : PoleColors.WHITE,
                    block);
            LteLog.LOGGER.info("[lte] pole tint provider registered");
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole tint provider unavailable (the colour band stays white)", t);
        }
    }
}
