// 「这个站台现在该显示什么颜色」——正在停靠/即将进站的车开往哪个终点站，灯带就什么色。
package dev.js.mtrheadlight.region.hk;

import dev.js.mtrheadlight.LteLog;
import mtr.client.ClientData;
import mtr.data.Station;
import mtr.data.TrainClient;
import mtr.path.PathData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 终点站颜色解析：灯带颜色 = <b>该站台正在停靠（或即将进站）列车所开往的终点站的车站色</b>，
 * 没有车就是 {@code 0}（灭）。
 *
 * <h2>什么时候开始亮</h2>
 * {@value #ARRIVE_RADIUS} 格 —— 和 MZYC（mzycBetterMTR）的进站广播同一个触发半径：它那套的
 * 默认值就是 {@code trainArriveAnnounceRoundXz = 10}（XZ 半径 10 格）。所以「广播响起的那一刻灯亮」
 * 在观感上是对齐的，而不是提前几十秒把灯点亮。
 *
 * <h2>什么时候灭</h2>
 * 停靠中的车 <b>整列解除站台占用</b>（{@code getThisStation()} 不再是本站）之后才灭。
 * 如果此时后方已经有车在 {@value #ARRIVE_RADIUS} 格内等着进站，直接换成后一趟车的终点站色，
 * <b>中间不灭灯</b>（渲染层的 alpha 会保持在 1）。
 *
 * <h2>为什么在客户端算</h2>
 * MTR 的列车只在客户端模拟（{@code ClientData.TRAINS}，服务端没有 TrainServer），所以这件事在
 * 服务端根本拿不到。好处是整条灯带没有任何服务端状态，不用同步、不会因为车来了就刷方块更新。
 *
 * <p>结果按方块位置缓存 {@value #TTL_MS}ms，几百段灯带每帧只算一次。
 */
public final class DestinationColors {

    /**
     * 开始亮灯的半径（格）。对齐 MZYC 的进站广播默认值 {@code trainArriveAnnounceRoundXz = 10}。
     */
    private static final double ARRIVE_RADIUS = 10.0D;
    /** 灯带找不到自己站台时，往周围找站台方块的半径（格）。 */
    private static final int PLATFORM_SEARCH = 8;
    /** 灯带通常比站台方块高 2~4 格，往下找得更深一点。 */
    private static final int PLATFORM_SEARCH_DOWN = 8;
    /** 每个方块位置的颜色缓存时长（毫秒）。 */
    private static final long TTL_MS = 400L;
    /** 缓存上限，超了整表清空。 */
    private static final int LIMIT = 1024;

    /** {@code -Dlte.debug.destlight=true} 才打解析日志（默认关）。 */
    private static final boolean DEBUG = Boolean.getBoolean("lte.debug.destlight");
    private static final Map<Long, Long> DEBUG_AT = new HashMap<>();

    private static final Map<Long, Stamp> CACHE = new HashMap<>();

    private DestinationColors() {
    }

    /** @return {@code 0xRRGGBB}；{@code 0} = 灭（没有车 / 拿不到终点站色）。 */
    public static int colorAt(final World world, final BlockPos pos) {
        final long key = pos.asLong();
        final long now = System.currentTimeMillis();
        final Stamp cached = CACHE.get(key);
        if (cached != null && now - cached.at < TTL_MS) {
            return cached.rgb;
        }
        int rgb = 0;
        try {
            rgb = resolve(pos);
        } catch (final Throwable t) {
            LteLog.LOGGER.debug("[lte] destination light: colour lookup failed", t);
        }
        if (CACHE.size() > LIMIT) {
            CACHE.clear();
        }
        CACHE.put(key, new Stamp(rgb, now));
        return rgb;
    }

    /** 断线 / 换存档：缓存整表丢掉。 */
    public static void clear() {
        CACHE.clear();
    }

    private static int resolve(final BlockPos pos) {
        final long platformId = platformAt(pos);
        if (platformId == 0L) {
            debug(pos, "no platform within " + PLATFORM_SEARCH);
            return 0;
        }
        final Station here = ClientData.DATA_CACHE.platformIdToStation.get(platformId);
        if (here == null) {
            debug(pos, "platform " + platformId + " has no station");
            return 0;
        }

        // ① 已经停靠在本站台的车优先：它没解除占用之前不换色、不灭灯
        final TrainClient stopping = trainAtPlatform(here.id);
        if (stopping != null) {
            final int rgb = colorOf(stopping);
            debug(pos, "platform " + platformId + " stopping " + stopping.trainId + " -> "
                    + hex(rgb));
            return rgb;
        }

        // ② 后方有车进入进站半径：用它（队列里的下一趟）的终点站色
        final TrainClient approaching = approachingTrain(here.id);
        if (approaching != null) {
            final int rgb = colorOf(approaching);
            debug(pos, "platform " + platformId + " approaching " + approaching.trainId + " -> "
                    + hex(rgb));
            return rgb;
        }

        debug(pos, "platform " + platformId + " station " + here.id + " idle");
        return 0;
    }

    /** 终点站色；取不到就退到该车的线路色。 */
    private static int colorOf(final TrainClient train) {
        final int destination = destinationColor(train);
        if (destination != 0) {
            return destination;
        }
        final mtr.data.Route route = train.getThisRoute();
        return route == null ? 0 : route.color & 0xFFFFFF;
    }

    /**
     * 灯带所在位置对应哪个站台。
     *
     * <p>灯带贴在屏蔽门顶上，坐标本身通常不是站台方块，所以先按 {@code blockPosToPlatformId} 在周围
     * 一小片区域里找（横向逐层外扩、往下找得深一点），找到就够 —— 每段灯带结果会进缓存。
     */
    private static long platformAt(final BlockPos pos) {
        final var map = ClientData.DATA_CACHE.blockPosToPlatformId;
        final long direct = map.get(pos.asLong());
        if (direct != 0L) {
            return direct;
        }
        for (int radius = 1; radius <= PLATFORM_SEARCH; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue; // 只看这一层的外圈
                    }
                    for (int dy = -PLATFORM_SEARCH_DOWN; dy <= 2; dy++) {
                        final long hit = map.get(new BlockPos(pos.getX() + dx, pos.getY() + dy,
                                pos.getZ() + dz).asLong());
                        if (hit != 0L) {
                            return hit;
                        }
                    }
                }
            }
        }
        return 0L;
    }

    /** 正停靠在本站台的车（还没解除占用）。 */
    private static TrainClient trainAtPlatform(final long stationId) {
        for (final TrainClient train : ClientData.TRAINS) {
            if (train == null || train.isRemoved) {
                continue;
            }
            final Station here = train.getThisStation();
            if (here != null && here.id == stationId) {
                return train;
            }
        }
        return null;
    }

    /** 下一站是本站台所属车站、且已经进入 {@value #ARRIVE_RADIUS} 格的车里最近的那趟。 */
    private static TrainClient approachingTrain(final long stationId) {
        TrainClient best = null;
        double bestDistance = Double.MAX_VALUE;
        for (final TrainClient train : ClientData.TRAINS) {
            if (train == null || train.isRemoved) {
                continue;
            }
            final Station next = train.getNextStation();
            if (next == null || next.id != stationId) {
                continue;
            }
            final double distance = train.getDistanceToNextStop();
            if (distance <= 0.0D || distance > ARRIVE_RADIUS) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = train;
            }
        }
        return best;
    }

    /** 终点站颜色：从线路末尾往前找最后一个真正停站的站台。 */
    private static int destinationColor(final TrainClient train) {
        if (train.path == null) {
            return 0;
        }
        for (int i = train.path.size() - 1; i >= 0; i--) {
            final PathData stop = train.path.get(i);
            if (stop == null || stop.dwellTime <= 0 || stop.savedRailBaseId == 0L) {
                continue;
            }
            final Station station = ClientData.DATA_CACHE.platformIdToStation.get(stop.savedRailBaseId);
            if (station != null && station.color != 0) {
                return station.color & 0xFFFFFF;
            }
        }
        return 0;
    }

    private static String hex(final int rgb) {
        return rgb == 0 ? "off" : String.format("#%06X", rgb);
    }

    /** 只在 {@code -Dlte.debug.destlight=true} 时打，且每个方块 5 秒最多一行。 */
    private static void debug(final BlockPos pos, final String message) {
        if (!DEBUG) {
            return;
        }
        final long now = System.currentTimeMillis();
        final long last = DEBUG_AT.getOrDefault(pos.asLong(), 0L);
        if (now - last < 5000L) {
            return;
        }
        DEBUG_AT.put(pos.asLong(), now);
        LteLog.LOGGER.info("[lte] destlight @{}: {}", pos, message);
    }

    /** 一个位置的颜色 + 算出来的时刻。 */
    private record Stamp(int rgb, long at) {
    }
}
