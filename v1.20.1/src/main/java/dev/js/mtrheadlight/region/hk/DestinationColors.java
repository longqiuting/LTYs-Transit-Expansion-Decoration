// 「这个站台正在进站的车开往哪个终点站」→ 灯带的颜色。纯客户端算，不存任何方块状态。
package dev.js.mtrheadlight.region.hk;

import dev.js.mtrheadlight.LteLog;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.Station;
import mtr.data.TrainClient;
import mtr.path.PathData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 终点站颜色解析：灯带颜色 = <b>该站台正在进站列车所开往的终点站的车站色</b>，没有车就是灭的（{@code 0}）。
 *
 * <h2>为什么在客户端算</h2>
 * MTR 的列车只在客户端模拟（{@code ClientData.TRAINS}，服务端没有 {@code TrainServer}），所以这件事在
 * 服务端根本拿不到。好处是：整条灯带没有任何服务端状态，不用同步、不会因为车来了就刷方块更新。
 *
 * <h2>「正在进站」怎么判</h2>
 * <ol>
 *   <li>灯带贴在哪个站台：先查 {@code blockPosToPlatformId}，查不到就找最近的站台（灯带通常比站台方块高两三格）；</li>
 *   <li>哪趟车：{@code TRAINS} 里「下一站 == 这个站台所属车站」且 {@code getDistanceToNextStop()} 在一个范围内，
 *       取最近的那趟；</li>
 *   <li>终点站：从该车 {@code path} 末尾往前找第一条 {@code dwellTime > 0} 的停站，它的 {@code savedRailBaseId}
 *       是终点站台，再由 {@code platformIdToStation} 拿到终点站；</li>
 *   <li>颜色：终点站的 {@code Station.color}（车站专属色），取不到就退到该车线路色，再取不到就是灭。</li>
 * </ol>
 *
 * <p>结果按方块位置缓存 {@value #TTL_MS}ms —— 一整个站台几百段灯带每帧都问一次，但真正重算的只有
 * 「缓存过期」的那一下。
 */
public final class DestinationColors {

    /** 进站判定的最远距离（格）：再远就不算「正在进站」，灯还是灭的。 */
    private static final double APPROACH_DISTANCE = 400.0D;
    /** 灯带找不到自己站台时，往周围找站台方块的半径（格）。 */
    private static final int PLATFORM_SEARCH = 8;
    /** 灯带通常比站台方块高 2~4 格，往下找得更深一点。 */
    private static final int PLATFORM_SEARCH_DOWN = 8;
    /** 每个方块位置的颜色缓存时长（毫秒）。 */
    private static final long TTL_MS = 400L;
    /** 缓存上限，超了整表清空（灯带很多也不会一直涨）。 */
    private static final int LIMIT = 1024;

    /** 时刻表提前量：下一班到站还差这么久就先亮（毫秒）。 */
    private static final long SCHEDULE_LEAD_MS = 60_000L;
    /** 时刻表尾巴：到点之后还能亮这么久（车已经停稳时由列车本身接管）。 */
    private static final long SCHEDULE_TAIL_MS = 20_000L;

    /** {@code -Dlte.debug.destlight=true} 才打解析日志（默认关）。 */
    private static final boolean DEBUG = Boolean.getBoolean("lte.debug.destlight");
    private static final Map<Long, Long> DEBUG_AT = new HashMap<>();

    private static final Map<Long, Stamp> CACHE = new HashMap<>();

    private DestinationColors() {
    }

    /** @return {@code 0xRRGGBB}；{@code 0} = 灭（没有进站车 / 拿不到终点站色）。 */
    public static int colorAt(final World world, final BlockPos pos) {
        final long key = pos.asLong();
        final long now = System.currentTimeMillis();
        final Stamp cached = CACHE.get(key);
        if (cached != null && now - cached.at < TTL_MS) {
            return cached.rgb;
        }
        int rgb = 0;
        try {
            rgb = resolve(world, pos);
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

    private static int resolve(final World world, final BlockPos pos) {
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

        // ① 有车在「进站途中」：用它的终点站色（车还远也亮，靠 APPROACH_DISTANCE 控制提前量）
        final TrainClient train = approachingTrain(here.id);
        if (train != null) {
            final int destination = destinationColor(train);
            if (destination != 0) {
                debug(pos, "platform " + platformId + " train " + train.trainId + " -> #"
                        + String.format("%06X", destination));
                return destination;
            }
            final mtr.data.Route route = train.getThisRoute();
            if (route != null && route.color != 0) {
                debug(pos, "platform " + platformId + " train " + train.trainId + " route colour");
                return route.color & 0xFFFFFF;
            }
        }

        // ② 车还没出现（时刻表里已经在来的路上）：按时刻表提前亮，颜色用那条线路的线路色
        final int scheduled = scheduledColor(platformId);
        debug(pos, "platform " + platformId + " station " + here.id + " scheduled="
                + (scheduled == 0 ? "none" : String.format("#%06X", scheduled)));
        return scheduled;
    }

    /**
     * 时刻表提前量：下一班到站还差 {@value #SCHEDULE_LEAD_MS}ms 以内就先亮起来（颜色取线路色）。
     *
     * <p>列车在客户端要等它真的被创建出来才会出现在 {@code TRAINS} 里，而玩家希望的观感是
     * 「车还没到、灯先亮」—— 这段时间只能靠时刻表。
     */
    private static int scheduledColor(final long platformId) {
        final java.util.Set<mtr.data.ScheduleEntry> entries =
                ClientData.SCHEDULES_FOR_PLATFORM.get(platformId);
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        final long now = System.currentTimeMillis();
        for (final mtr.data.ScheduleEntry entry : entries) {
            final long delta = entry.arrivalMillis - now;
            if (delta < -SCHEDULE_TAIL_MS || delta > SCHEDULE_LEAD_MS) {
                continue;
            }
            for (final mtr.data.Route route : ClientData.ROUTES) {
                if (route.id == entry.routeId && route.color != 0) {
                    return route.color & 0xFFFFFF;
                }
            }
        }
        return 0;
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

    /**
     * 灯带所在位置对应哪个站台。
     *
     * <p>灯带贴在屏蔽门顶上，坐标本身通常不是站台方块，所以先按 {@code blockPosToPlatformId} 在周围
     * 一小片区域里找（横向 ±{@value #PLATFORM_SEARCH}、纵向 ±3），找到最近的那块就够了 —— 一只灯带
     * 一辈子只算一次（结果进缓存）。
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

    /** 下一站是这个车站、且还没有停稳的车里最近的那趟。 */
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
            if (distance <= 0.0D || distance > APPROACH_DISTANCE) {
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

    /** 一个位置的颜色 + 算出来的时刻。 */
    private record Stamp(int rgb, long at) {
    }
}
