// 「这个站台现在该亮什么颜色」——按 MZYC 的路子：站台的调度条目 + 线路终点站，而不是逐趟车去猜。
package dev.js.mtrheadlight.region.hk;

import dev.js.mtrheadlight.LteLog;
import mtr.client.ClientData;
import mtr.data.Route;
import mtr.data.ScheduleEntry;
import mtr.data.Station;
import mtr.data.TrainClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 终点站颜色解析（参考 MZYC 的取数方式）。
 *
 * <h2>数据从哪来</h2>
 * MZYC 的进站广播不是逐趟列车去猜的：它按 <b>站台的调度条目</b>
 * （{@code ClientData.SCHEDULES_FOR_PLATFORM}）+ <b>线路的终点站</b>
 * （{@code Route.platformIds} / {@code getFormattedRouteDestination}）算出来。这里照同一套：
 * <ol>
 *   <li>灯带位置 → 站台 id；</li>
 *   <li>该站台最近的一条调度条目（{@code arrivalMillis} 距离现在最近的那条）；</li>
 *   <li>条目给 {@code routeId + currentStationIndex}：按索引和本站台在线路里的位置判断行车方向，
 *       方向的最后一个站台就是<b>终点站</b>；</li>
 *   <li>终点站的 {@code Station.color} 就是灯的颜色（取不到退线路色）。</li>
 * </ol>
 * 全程不碰列车对象，所以不会出现「同一时刻两趟车各说各色」的颜色乱跳，也不会把通过车算进来。
 *
 * <h2>什么时候亮 / 灭</h2>
 * <ul>
 *   <li><b>亮</b>：最近一条调度条目的到站时刻在 {@value #WINDOW_BEFORE}ms 之后到
 *       {@value #WINDOW_AFTER}ms 之前（也就是快要进站、正在停站、刚发车这一整段）；</li>
 *   <li><b>灭</b>：该站台有车停靠时，<b>车门关闭后 {@value #DOOR_CLOSE_HOLD_MS}ms</b> 淡出
 *       —— 不再看车的位置；没有车停靠时由调度窗口决定。</li>
 * </ul>
 *
 * <p>结果按方块位置缓存 {@value #TTL_MS}ms。
 */
public final class DestinationColors {

    /** 提前多久亮（毫秒）：到站时刻前这段时间开始亮。 */
    private static final long WINDOW_BEFORE_MS = 15_000L;
    /** 到站时刻之后还能亮多久（毫秒）：覆盖停站 + 开关门。 */
    private static final long WINDOW_AFTER_MS = 300_000L;
    /** 车门关闭后多久淡出（毫秒）。 */
    private static final long DOOR_CLOSE_HOLD_MS = 10_000L;
    /** 调度窗口没覆盖到时，按「真要停本站台的车已进入这么多格」补亮（格）。 */
    private static final double BRIDGE_RADIUS = 40.0D;

    /** 找站台的横向半径（格）—— 比 PIDS 自己的检测范围宽一格。 */
    private static final int PLATFORM_SEARCH = 4;
    /** 往下找的深度（格）：灯带在门顶，比站台方块高 2~4 格。 */
    private static final int PLATFORM_SEARCH_DOWN = 5;

    /** 每个方块位置的颜色缓存时长（毫秒）。 */
    private static final long TTL_MS = 400L;
    private static final int LIMIT = 1024;

    /** {@code -Dlte.debug.destlight=true} 才打解析日志（默认关）。 */
    private static final boolean DEBUG = Boolean.getBoolean("lte.debug.destlight");
    private static final Map<Long, Long> DEBUG_AT = new HashMap<>();

    private static final Map<Long, Stamp> CACHE = new HashMap<>();
    /** 每趟车「车门刚刚关上」的时刻（毫秒），用来做关灯延迟。 */
    private static final Map<String, DoorState> DOORS = new HashMap<>();
    /** 每个站台「上一次算出来的终点站色」—— 停站/延时期间沿用，不退回线路色。 */
    private static final Map<Long, Integer> LAST_COLOR = new HashMap<>();

    private DestinationColors() {
    }

    /** @return {@code 0xRRGGBB}；{@code 0} = 灭。 */
    public static int colorAt(final World world, final BlockPos pos) {
        final long key = pos.asLong();
        final long now = System.currentTimeMillis();
        final Stamp cached = CACHE.get(key);
        if (cached != null && now - cached.at < TTL_MS) {
            return cached.rgb;
        }
        int rgb = 0;
        try {
            rgb = resolve(pos, now);
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
        DOORS.clear();
        LAST_COLOR.clear();
    }

    private static int resolve(final BlockPos pos, final long now) {
        final long platformId = platformAt(pos);
        if (platformId == 0L) {
            debug(pos, "no platform within " + PLATFORM_SEARCH);
            return 0;
        }
        final Station here = ClientData.DATA_CACHE.platformIdToStation.get(platformId);

        // ① 停靠中的车：门开着 → 亮；门关了 → 再撑 DOOR_CLOSE_HOLD_MS 就灭
        final TrainClient stopping = here == null ? null : trainAtPlatform(here.id);
        if (stopping != null) {
            final boolean justClosed = doorJustClosed(stopping, now);
            final long closedAt = closedAt(stopping);
            // 车还在站台、门还一次没开过（进站减速/刚停稳）也算亮 —— 否则会出现
            // 「广播响了灯亮 → 车快停时又灭 → 开门才亮」这种闪断
            final boolean notYetOpened = closedAt == 0L;
            final boolean holding = stopping.isDoorOpening() || notYetOpened
                    || (closedAt != 0L && now - closedAt <= DOOR_CLOSE_HOLD_MS);
            if (holding || justClosed) {
                final int rgb = colorOfPlatform(platformId, stopping, now);
                debug(pos, "platform " + platformId + " doors "
                        + (stopping.isDoorOpening() ? "open" : "closing/hold") + " -> " + hex(rgb));
                return rgb;
            }
            debug(pos, "platform " + platformId + " train left the doors, off");
            return 0;
        }

        // ② 没有车停靠：看调度窗口（快到站 / 正在来的路上）
        final int rgb = colorOfPlatform(platformId, null, now);
        if (rgb != 0) {
            debug(pos, "platform " + platformId + " scheduled -> " + hex(rgb));
            return rgb;
        }
        // 车早到 / 时刻表还没翻到这一班：光靠调度窗口会漏亮，这里用「确实要停本站台的车」
        // 补位 —— 只看 path 里的下一停靠站台，通过车不会被算进来；颜色仍沿用上次的终点站色。
        if (nextStopTrain(platformId) != null) {
            final Integer last = LAST_COLOR.get(platformId);
            if (last != null) {
                debug(pos, "platform " + platformId + " train closing in -> " + hex(last));
                return last;
            }
        }
        return 0;
    }

    /**
     * 该站台当前应该显示的颜色。
     *
     * <p>优先用调度条目的终点站色；调度窗口内没有条目时（比如刚发车、时刻表已经翻页），
     * 用停靠车的线路色兜底。
     */
    private static int colorOfPlatform(final long platformId, final TrainClient stopping,
                                       final long now) {
        final ScheduleEntry entry = nearestEntry(platformId, now);
        if (entry != null) {
            final int rgb = colorOfEntry(entry, platformId);
            if (rgb != 0) {
                LAST_COLOR.put(platformId, rgb);
                return rgb;
            }
        }
        // 调度条目翻页 / 车晚点：沿用这个站台上一次的终点站色。
        // 绝不退回线路色 —— 那样出站瞬间颜色会从终点站色跳成线路色。
        if (stopping != null) {
            final Integer last = LAST_COLOR.get(platformId);
            if (last != null) {
                return last;
            }
        }
        return 0;
    }

    /** 该站台里离现在最近、且落在窗口内的调度条目。 */
    private static ScheduleEntry nearestEntry(final long platformId, final long now) {
        final java.util.Set<ScheduleEntry> entries = ClientData.SCHEDULES_FOR_PLATFORM.get(platformId);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        ScheduleEntry best = null;
        long bestDelta = Long.MAX_VALUE;
        for (final ScheduleEntry entry : entries) {
            final long delta = entry.arrivalMillis - now;
            if (delta > WINDOW_BEFORE_MS || delta < -WINDOW_AFTER_MS) {
                continue;
            }
            final long abs = Math.abs(delta);
            if (abs < bestDelta) {
                bestDelta = abs;
                best = entry;
            }
        }
        return best;
    }

    /**
     * 调度条目 → 终点站色。
     *
     * <p>方向判定：条目里的 {@code currentStationIndex} 是这趟车在线路上走到第几个站台；
     * 和本站台的索引比较，往哪个方向走，那个方向的最后一个站台就是终点站。
     */
    private static int colorOfEntry(final ScheduleEntry entry, final long platformId) {
        final Route route = routeById(entry.routeId);
        if (route == null) {
            return 0;
        }
        final int hereIndex = route.getPlatformIdIndex(platformId);
        if (hereIndex < 0 || route.platformIds.isEmpty()) {
            return route.color & 0xFFFFFF;
        }
        final boolean forward = entry.currentStationIndex <= hereIndex;
        final long terminusId = forward
                ? route.platformIds.get(route.platformIds.size() - 1).platformId
                : route.platformIds.get(0).platformId;
        final Station terminus = ClientData.DATA_CACHE.platformIdToStation.get(terminusId);
        if (terminus != null && terminus.color != 0) {
            return terminus.color & 0xFFFFFF;
        }
        return route.color & 0xFFFFFF;
    }

    private static Route routeById(final long routeId) {
        for (final Route route : ClientData.ROUTES) {
            if (route.id == routeId) {
                return route;
            }
        }
        return null;
    }

    /** 灯带所在位置对应哪个站台（先精确查，再在很小的范围内找）。 */
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
                        continue;
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

    /** 下一停靠站台就是本站台、且已经进入 {@value #BRIDGE_RADIUS} 格的车（补调度窗口的漏）。 */
    private static TrainClient nextStopTrain(final long platformId) {
        for (final TrainClient train : ClientData.TRAINS) {
            if (train == null || train.isRemoved || train.path == null) {
                continue;
            }
            final int from = Math.max(0, Math.min(train.path.size() - 1, train.getCurrentStationIndex()));
            for (int i = from; i < train.path.size(); i++) {
                final mtr.path.PathData stop = train.path.get(i);
                if (stop == null || stop.dwellTime <= 0 || stop.savedRailBaseId == 0L) {
                    continue;
                }
                if (stop.savedRailBaseId == platformId
                        && train.getDistanceToNextStop() > 0.0D
                        && train.getDistanceToNextStop() <= BRIDGE_RADIUS) {
                    return train;
                }
                break;
            }
        }
        return null;
    }

    /** 正停靠在本站台所属车站的车。 */
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

    /** 记录车门「由开变关」的那一刻；返回这次调用是否刚刚关上。 */
    private static boolean doorJustClosed(final TrainClient train, final long now) {
        final DoorState state = DOORS.computeIfAbsent(train.trainId, id -> new DoorState());
        final boolean open = train.isDoorOpening();
        final boolean justClosed = state.open && !open;
        if (justClosed) {
            state.closedAt = now;
        }
        if (open) {
            state.closedAt = 0L;
        }
        state.open = open;
        if (DOORS.size() > 512) {
            DOORS.clear();
        }
        return justClosed;
    }

    private static long closedAt(final TrainClient train) {
        final DoorState state = DOORS.get(train.trainId);
        return state == null ? 0L : state.closedAt;
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

    private static final class DoorState {
        private boolean open;
        private long closedAt;
    }

    private record Stamp(int rgb, long at) {
    }
}
