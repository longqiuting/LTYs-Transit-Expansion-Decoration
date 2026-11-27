// 柱底凹槽色带的染色 provider（Forge 侧）。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 柱底凹槽色带的取色器（Forge 侧，和 Fabric 侧 {@code PoleTint} 同构）。
 *
 * <p>方块是普通方块（没有方块实体），颜色按位置缓存，缓存每 100 tick 清一次；
 * 取不到车站/线路色就回白色。
 */
public final class PoleTint {

    private static final int CACHE_TICKS = 100;

    private static final Map<BlockPos, Integer> CACHE = new ConcurrentHashMap<>();
    private static long cachedTick = Long.MIN_VALUE;

    private PoleTint() {
    }

    public static void register() {
        // Forge 侧走 RegisterColorHandlersEvent.Block（见 PoleForgeClient）。
    }

    /** {@code RegisterColorHandlersEvent.Block} 回调。 */
    public static void onRegisterBlockColours(final BlockColors colors) {
        try {
            final AmtrPoleBottomBlock block = PoleBlocks.bottom();
            if (block == null) {
                return;
            }
            colors.register((state, level, pos, tintIndex) -> {
                if (tintIndex != 0 || level == null || pos == null) {
                    return 0xFFFFFFFF;
                }
                return 0xFF000000 | colorAt(level, pos);
            }, block);
            LteLog.LOGGER.info("[lte] pole tint provider registered");
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole tint provider unavailable (the groove stays white)", t);
        }
    }

    private static int colorAt(final Level level, final BlockPos pos) {
        try {
            if (level.getGameTime() - cachedTick > CACHE_TICKS) {
                CACHE.clear();
                cachedTick = level.getGameTime();
            }
            final int rgb = CACHE.computeIfAbsent(pos.immutable(), p -> PoleColors.resolve(level, p));
            return rgb == 0 ? 0xFFFFFF : (rgb & 0xFFFFFF);
        } catch (final Throwable t) {
            return 0xFFFFFF;
        }
    }
}
