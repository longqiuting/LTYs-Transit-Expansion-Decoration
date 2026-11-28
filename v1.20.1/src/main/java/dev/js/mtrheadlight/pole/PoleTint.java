// 柱底凹槽色带的染色 provider（Fabric 侧）。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 柱底那条凹槽色带的取色器（模型里 color 分组那 16 个 element 的每个面都带 {@code tintindex: 0}）。
 *
 * <h2>颜色从哪来</h2>
 * <ol>
 *   <li>方块实体里已经有颜色（刷子刷的自定义色，或服务端解析出来并同步过来的车站色）——直接用它；</li>
 *   <li>方块实体里还是白的（服务端那边 {@code blockPosToStation} 可能是空的，解析不出来）——
 *       <b>客户端自己算一次</b>（{@link PoleColorsClient}，用面板/吊板同一份 ClientCache），按位置缓存。</li>
 * </ol>
 *
 * <p>两条都拿不到就是白色（=看起来没有色带），不会每帧刷日志。
 */
public final class PoleTint {

    /**
     * 客户端兜底算出来的颜色；{@code 0} = 算过但没结果。
     *
     * <p>key 是「维度 + 坐标」—— 只按 BlockPos 的话，主世界和地狱同一个坐标会互相串色；
     * 条目数也封了顶（超了就整表清空），跑长途线路不会一直涨。
     */
    private static final Map<String, Integer> CLIENT_COLORS = new ConcurrentHashMap<>();
    private static final int CLIENT_CACHE_LIMIT = 4096;

    private static final java.util.concurrent.atomic.AtomicInteger PROBES =
            new java.util.concurrent.atomic.AtomicInteger();

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
                final int rgb = colorAt(level, pos);
                if (PROBES.incrementAndGet() <= 3) {
                    LteLog.LOGGER.info("[lte] pole tint probe: pos={} rgb=#{}", pos, String.format("%06X", rgb));
                }
                return 0xFF000000 | rgb;
            }, block);
            LteLog.LOGGER.info("[lte] pole tint provider registered");
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole tint provider unavailable (the groove stays white)", t);
        }
    }

    /** {@code 0xRRGGBB}：方块实体优先，其次客户端自己解析，最后白色。 */
    private static int colorAt(final World world, final BlockPos pos) {
        try {
            final int fromEntity = AmtrPoleBottomBlock.effectiveTint(world, pos) & 0xFFFFFF;
            if (fromEntity != 0xFFFFFF) {
                return fromEntity;
            }
            if (CLIENT_COLORS.size() > CLIENT_CACHE_LIMIT) {
                CLIENT_COLORS.clear();
            }
            final String key = world.getRegistryKey().getValue() + "#" + pos.asLong();
            final int cached = CLIENT_COLORS.computeIfAbsent(key, k -> PoleColors.resolve(world, pos));
            if (PROBES.incrementAndGet() <= 3) {
                LteLog.LOGGER.info("[lte] pole tint probe: pos={} clientFallback=#{}{}", pos,
                        String.format("%06X", cached & 0xFFFFFF), cached == 0 ? " (no station colour)" : "");
            }
            return cached == 0 ? 0xFFFFFF : (cached & 0xFFFFFF);
        } catch (final Throwable t) {
            return 0xFFFFFF;
        }
    }
}
