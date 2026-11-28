// 厦门地铁柱子的车站色解析：柱子「跟随车站色」时，这一格到底该用哪个颜色。
package dev.js.mtrheadlight.pole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.js.mtrheadlight.LteLog;
import mtr.data.DataCache;
import mtr.data.RailwayData;
import mtr.data.Route;
import mtr.data.Station;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 「这个柱子属于哪个车站、那个车站的线路是什么颜色」的<b>唯一判定</b>（两端共用同一套规则）。
 *
 * <h2>判定链（按顺序，碰到第一个能用的就停）</h2>
 * <ol>
 *   <li>{@code dataCache.blockPosToStation.get(pos)} —— 这一格<b>正好</b>是车站登记过的坐标；</li>
 *   <li>在 {@code blockPosToStation} 里扫 <b>±{@value #SCAN_RADIUS}</b> 格，取<b>最近的</b>那个车站
 *       （柱子一般立在站台中间，离车站节点不会太远；用最近而不是任意一个，是为了不在两站之间乱猜）；</li>
 *   <li>{@code RailwayData.getStation(stations, dataCache, pos)} —— MTR 自己的「这一格在不在某个车站
 *       的区域内」判定（{@code Station#inArea}）。柱子在车站区域内、但附近没有车站节点的坐标时靠它，
 *       这是 MTR 算总站名 / 到站屏用的同一个函数；</li>
 *   <li>都没有 → 返回 {@code 0}（= 没解析出来，调用方用白色）。</li>
 * </ol>
 *
 * <h2>车站 → 线路颜色</h2>
 * <p>「服务这个车站的线路」= 线路的 {@code platformIds} 里至少有一个站台，其
 * {@code dataCache.platformIdToStation} 就是该车站。多条线路共站时取<b>线路 id 升序的第一条
 * 颜色非 0</b> 的（{@code color & 0xFFFFFF == 0} 在 MTR 里表示「没设色」，跳过）。排序是为了
 * 同一个存档每次解析出同一个颜色 —— 不排序的话 {@code Set} 的遍历顺序换个版本就可能变。
 *
 * <h2>为什么分两半</h2>
 * <p>服务端只有 {@link RailwayData}，客户端只有 {@code mtr.client.ClientData.DATA_CACHE}
 * （{@code ClientCache}，里面才有 {@code stationIdToRoutes}）。服务端那一半在本类，
 * 客户端那一半在 {@link PoleColorsClient} —— 专用服务端永远不会加载那一个类。
 *
 * <p><b>调用频率</b>：只在「解析一次」的时候调（方块实体把结果缓存起来），不是每帧调 ——
 * 方块染色 provider 是每帧每面都问的，那里只许读缓存字段。
 */
public final class PoleColors {

    /**
     * 默认色（白）。<b>必须带不透明 alpha</b>：Fabric 的 {@code ColorProviderRegistry} 与 Forge 的
     * {@code RegisterColorHandlersEvent.Block} 都把这个 int 当 <b>ARGB</b> 用，顶点颜色乘上去，
     * alpha 为 0 会让整片面全透明 —— 柱子「完全看不见」就是这么来的。
     */
    public static final int WHITE = 0xFFFFFFFF;

    /** 就近扫车站的半径（格）。 */
    public static final int SCAN_RADIUS = 24;

    /** 解析失败只打一次日志，别把日志刷爆。 */
    private static boolean loggedFailure;

    private PoleColors() {
    }

    /**
     * 这一格该用的车站色。
     *
     * @return {@code 0xRRGGBB}；{@code 0} 表示<b>没解析出来</b>（不是黑色 —— 黑色是 {@code 0x000000}）
     */
    public static int resolve(final World world, final BlockPos pos) {
        if (world == null || pos == null) {
            return 0;
        }
        try {
            if (world.isClient) {
                return PoleColorsClient.resolve(pos);
            }
            final RailwayData data = RailwayData.getInstance(world);
            if (data == null || data.dataCache == null) {
                return 0;
            }
            final Station station = findStation(data.dataCache, pos, data.stations);
            if (station == null) {
                return 0;
            }
            // MTR 每个车站自己有专属色（Station 继承 NameColorDataBase.color），优先用车站色；
            // 车站色为 0 时才退回「服务该站的第一条线路色」。
            final int stationRgb = station.color & 0xFFFFFF;
            if (stationRgb != 0) {
                return stationRgb;
            }
            return routeColor(data.routes, data.dataCache, station);
        } catch (final Throwable t) {
            if (!loggedFailure) {
                loggedFailure = true;
                LteLog.LOGGER.warn("[lte] pole: could not resolve the station colour at {}", pos, t);
            }
            return 0;
        }
    }

    /**
     * 「这一格属于哪个车站」—— 上面判定链的第 1–3 步。两端共用（客户端传的是 {@code ClientCache}，
     * 它继承 {@link DataCache}，字段名一样）。
     */
    public static Station findStation(final DataCache cache, final BlockPos pos, final Set<Station> stations) {
        if (cache == null || pos == null) {
            return null;
        }
        // ① 正好在这一格。
        final Station exact = cache.blockPosToStation.get(pos);
        if (exact != null) {
            return exact;
        }
        // ② ±24 格里最近的。
        final Station near = nearest(cache.blockPosToStation, pos);
        if (near != null) {
            return near;
        }
        // ③ MTR 自己的「这一格在不在车站区域内」。
        try {
            return RailwayData.getStation(stations, cache, pos);
        } catch (final Throwable ignored) {
            return null;
        }
    }

    /** {@code blockPosToStation} 里离 {@code pos} 最近、且水平距离不超过 ±{@link #SCAN_RADIUS} 的车站。 */
    private static Station nearest(final Map<BlockPos, Station> map, final BlockPos pos) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Station best = null;
        double bestDistance = Double.MAX_VALUE;
        for (final Map.Entry<BlockPos, Station> entry : map.entrySet()) {
            final BlockPos other = entry.getKey();
            final Station station = entry.getValue();
            if (other == null || station == null) {
                continue;
            }
            if (Math.abs(other.getX() - pos.getX()) > SCAN_RADIUS
                    || Math.abs(other.getY() - pos.getY()) > SCAN_RADIUS
                    || Math.abs(other.getZ() - pos.getZ()) > SCAN_RADIUS) {
                continue;
            }
            final double distance = other.getSquaredDistance(pos);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = station;
            }
        }
        return best;
    }

    /**
     * 服务这个车站的线路里，第一条「颜色非 0」的颜色（线路 id 升序，保证确定性）。
     *
     * <p>参数拆成 {@code routes + cache} 而不是整个 {@link RailwayData}：这样这条判定可以在
     * 没有世界的情况下被 {@code tools/PoleColorCheck.java} 直接跑一遍（RailwayData 需要一个
     * 真实的 {@code World}）。
     */
    public static int routeColor(final java.util.Collection<Route> routes, final DataCache cache,
                                 final Station station) {
        if (routes == null || routes.isEmpty() || station == null) {
            return 0;
        }
        final List<Route> sorted = new ArrayList<>(routes);
        sorted.sort(Comparator.comparingLong(route -> route == null ? Long.MAX_VALUE : route.id));
        for (final Route route : sorted) {
            if (route == null) {
                continue;
            }
            final int rgb = route.color & 0xFFFFFF;
            if (rgb == 0) {
                continue;
            }
            if (servesStation(cache, route, station)) {
                return rgb;
            }
        }
        return 0;
    }

    /** 这条线路的站台里，有没有哪个站台属于 {@code station}。 */
    private static boolean servesStation(final DataCache cache, final Route route, final Station station) {
        if (route.platformIds == null || route.platformIds.isEmpty() || cache == null) {
            return false;
        }
        for (final Route.RoutePlatform platform : route.platformIds) {
            if (platform == null) {
                continue;
            }
            final Station owner = cache.platformIdToStation.get(Long.valueOf(platform.platformId));
            if (owner != null && owner.id == station.id) {
                return true;
            }
        }
        return false;
    }
}
