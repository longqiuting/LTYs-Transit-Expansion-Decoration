package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Block;

/**
 * 柱子那一圈的染色 provider（Forge 侧）。
 *
 * <p>Fabric 侧对应物是 {@code v1.20.1/.../pole/PoleTint.java}（{@code ColorProviderRegistry.BLOCK}），
 * 这里换成 Forge 的 {@code RegisterColorHandlersEvent.Block}：两者都是「给某个方块挂一个
 * {@code (state, world, pos, tintIndex) -> rgb} 的取色器」，模型里 {@code tintindex: 0} 的面每帧都会问。
 *
 * <p>由 {@code ClientBootstrap} 在 mod 事件总线上注册 {@link #onRegisterBlockColours}。
 */
public final class PoleTint {

    private PoleTint() {
    }

    /** {@code RegisterColorHandlersEvent.Block} 的监听体。 */
    public static void onRegisterBlockColours(final BlockColors colours) {
        try {
            final Block block = PoleBlocks.bottom();
            if (block == null) {
                return;
            }
            colours.register((state, level, pos, tintIndex) ->
                    tintIndex == 0 ? AmtrPoleBottomBlock.effectiveTint(level, pos) : PoleColors.WHITE,
                    block);
            LteLog.LOGGER.info("[lte] pole tint provider registered");
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] pole tint provider unavailable (the colour band stays white)", t);
        }
    }
}
