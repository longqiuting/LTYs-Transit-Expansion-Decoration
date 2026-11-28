// 「这个站台现在该亮什么颜色」——照 DEP（发车倒数器）那套：getClosePlatformId 找站台 + 锁住一段状态。
package dev.js.mtrheadlight.region.hk;

import dev.js.mtrheadlight.LteLog;
import mtr.client.ClientData;
import mtr.data.RailwayData;
import mtr.data.Route;
import mtr.data.ScheduleEntry;
import mtr.data.Station;
import mtr.data.TrainClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 终点站颜色解析。
 *
 * <h2>找站台：和 DEP 用同一个 API</h2>
 * {@code RailwayData.getClosePlatformId(PLATFORMS, DATA_CACHE, pos, radius, horizontalMargin,
 * verticalMargin)} —— DEP 的挂墙牌用 5/3/3、挂顶牌用 7/4/5。灯带贴在屏蔽门顶上、沿站台一整排，
 * 所以取一档更大的范围（{@value #SEARCH_RADIUS}/{@value #SEARCH_HORIZONTAL}/{@value #SEARCH_VERTICAL}），
 * 保证整条站台的每一段都能认到同一个站台 —— 之前手写的小范围扫描只有靠近 PIDS 的几块能认出来。
 *
 * <h2>状态锁：和 DEP 的 depLock 一个思路</h2>
 * DEP 不会每帧重新判断「该不该显示」，而是<b>锁住一个（站台 + 车头 + 停站时长）</b>直到它结束。
 * 这里同样：每次算出「该亮」就写一把锁 {@code until = 门关后 10 秒}，锁没到期之前
 * <b>不再理会调度条目翻页、列车状态抖动</b>。这解决了两类不稳定：
 * <ul>
 *   <li>进站途中偶发灭灯（时刻表翻页 / 车早到 / 车晚点）；</li>
 *   <li>出站瞬间颜色跳成线路色（兜底色被换掉）。</li>
 * </ul>
 *
 * <h2>颜色从哪来</h2>
 * 站台最近一条调度条目 → {@code routeId + currentStationIndex} 判断行车方向 → 该方向最后一个
 * 站台的 {@code Station.color}。取不到就沿用这把锁上次的颜色，<b>绝不退回线路色</b>。
 */
public final class DestinationColors {

    /** 找站台的搜索范围（DEP 的挂墙 5/3/3、挂顶 7/4/5 都太小，灯带是一整排）。 */
    private static final int SEARCH_RADIUS = 12;
    private static final int SEARCH_HORIZONTAL = 6;
    private static final int SEARCH_VERTICAL = 6;

    /** 提前多久亮（毫秒）。 */
    private static final long WINDOW_BEFORE_MS = 15_000L;
    /** 到站时刻之后还算在窗口里的时长（毫秒）：车晚点也不至于提前关窗。 */
    private static final long WINDOW_AFTER_MS = 300_000L;
    /** 车门关闭后多久熄灯（毫秒）。 */
    private static final long DOOR_CLOSE_HOLD_MS = 10_000L;

    /** 每个方块位置的颜色缓存时长（毫秒）。 */
    private static final long TTL_MS = 400L;
    private static final int LIMIT = 1024;

    private static final boolean DEBUG = Boolean.getBoolean("lte.debug.destlight");
    private static final Map<Long, Long> DEBUG_AT = new HashMap<>();

    private static final Map<Long, Stamp> CACHE = new HashMap<>();
    /** 每趟车「车门刚刚关上」的时刻。 */
    private static final Map<String, DoorState> DOORS = new HashMap<>();
    /** 每个站台的状态锁（DEP 的 depLock 同思路）。 */
    private static final Map<Long, Lock> LOCKS = new HashMap<>();

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

    /** 断线 / 换存档：整表清空。 */
    public static void clear() {
        CACHE.clear();
        DOORS.clear();
        LOCKS.clear();
    }

    private static int resolve(final BlockPos pos, final long now) {
        long platformId = 0L;
        try {
            platformId = RailwayData.getClosePlatformId(ClientData.PLATFORMS, ClientData.DATA_CACHE,
                    pos, SEARCH_RADIUS, SEARCH_HORIZONTAL, SEARCH_VERTICAL);
        } catch (final Throwable t) {
            return 0;
        }
        if (platformId == 0L) {
            debug(pos, "no platform near");
            return 0;
        }

        final Station here = ClientData.DATA_CACHE.platformIdToStation.get(platformId);
        final Lock lock = LOCKS.computeIfAbsent(platformId, id -> new Lock());

        // ① 调度条目给的终点站色（唯一颜色来源）
        final ScheduleEntry entry = nearestEntry(platformId, now);
        if (entry != null) {
            final int scheduled = colorOfEntry(entry, platformId);
            if (scheduled != 0) {
                lock.rgb = scheduled;
            }
        }

        // ② 有车停站：门开着就一直续锁，门关了就锁到关门后 DOOR_CLOSE_HOLD_MS
        final TrainClient stopping = here == null ? null : trainAtPlatform(here.id);
        if (stopping != null) {
            final boolean justClosed = doorJustClosed(stopping, now);
            final long closedAt = closedAt(stopping);
            if (stopping.isDoorOpening() || justClosed) {
                lock.until = now + DOOR_CLOSE_HOLD_MS;
            } else if (closedAt != 0L) {
                lock.until = Math.max(lock.until, closedAt + DOOR_CLOSE_HOLD_MS);
            } else {
                // 车停稳了但门还没开：保持亮，别在这一段闪
                lock.until = Math.max(lock.until, now + 1_000L);
            }
        } else if (entry != null) {
            // 调度窗口内、车还没到：锁一小段，避免条目翻页瞬间灭
            lock.until = Math.max(lock.until, now + 2_000L);
        }

        if (lock.until > now && lock.rgb != 0) {
            debug(pos, "platform " + platformId + " on " + hex(lock.rgb)
                    + (stopping != null ? " (train)" : " (schedule)"));
            return lock.rgb;
        }

        debug(pos, "platform " + platformId + " off");
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

    /** 调度条目 → 终点站色：按 currentStationIndex 与本站台索引比较判断行车方向。 */
    private static int colorOfEntry(final ScheduleEntry entry, final long platformId) {
        final Route route = routeById(entry.routeId);
        if (route == null) {
            return 0;
        }
        final int hereIndex = route.getPlatformIdIndex(platformId);
        if (hereIndex < 0 || route.platformIds.isEmpty()) {
            return 0;
        }
        final boolean forward = entry.currentStationIndex <= hereIndex;
        final long terminusId = forward
                ? route.platformIds.get(route.platformIds.size() - 1).platformId
                : route.platformIds.get(0).platformId;
        final Station terminus = ClientData.DATA_CACHE.platformIdToStation.get(terminusId);
        return terminus == null ? 0 : terminus.color & 0xFFFFFF;
    }

    private static Route routeById(final long routeId) {
        for (final Route route : ClientData.ROUTES) {
            if (route.id == routeId) {
                return route;
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

    /** 站台状态锁：亮到什么时候 + 亮什么色。 */
    private static final class Lock {
        private long until;
        private int rgb;
    }

    private record Stamp(int rgb, long at) {
    }
}
