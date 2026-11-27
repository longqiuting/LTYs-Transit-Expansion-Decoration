// 柱底凹槽色带的染色 provider（Fabric 侧）。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 柱底那条<b>凹槽色带</b>的取色器（模型里那 16 个 element 的每个面都带 {@code tintindex: 0}）。
 *
 * <p>方块本身是普通方块（没有方块实体、没有 BER），所以颜色不能存在方块实体里；这里按
 * <b>位置</b>缓存一次「这个位置属于哪个车站 / 线路色」——和吊板取线路色用的是同一份
 * {@link PoleColors#resolve(World, BlockPos)}。缓存每 {@value #CACHE_TICKS} tick 清一次，
 * 改了线路色最多几秒后跟上。
 *
 * <p>取不到（不在车站范围、数据还没同步）就回白色，也就是「没有色带」的样子，不会刷日志。
 */
public final class PoleTint {

    /** 缓存存活时间（tick）。 */
    private static final int CACHE_TICKS = 100;

    private static final Map<BlockPos, Integer> CACHE = new ConcurrentHashMap<>();
    private static long cachedTick = Long.MIN_VALUE;

    private PoleTint() {
    }

    public static void register() {
        try {
            final AmtrPoleBottomBlock block = PoleBlocks.bottom();
            if (block == null) {
                return;
            }
            ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
                if (tintIndex != 0 || !(world instanceof World level) || pos == null) {
                    return 0xFFFFFFFF;
                }
                return 0xFF000000 | colorAt(level, pos);
            }, block);
            LteLog.LOGGER.info("[lte] pole tint provider registered");
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole tint provider unavailable (the groove stays white)", t);
        }
    }

    /** 位置 → 线路色（{@code 0xRRGGBB}），取不到回白色。 */
    private static int colorAt(final World world, final BlockPos pos) {
        try {
            if (world.getTime() - cachedTick > CACHE_TICKS) {
                CACHE.clear();
                cachedTick = world.getTime();
            }
            final int rgb = CACHE.computeIfAbsent(pos.toImmutable(), p -> PoleColors.resolve(world, p));
            return rgb == 0 ? 0xFFFFFF : (rgb & 0xFFFFFF);
        } catch (final Throwable t) {
            return 0xFFFFFF;
        }
    }
}
