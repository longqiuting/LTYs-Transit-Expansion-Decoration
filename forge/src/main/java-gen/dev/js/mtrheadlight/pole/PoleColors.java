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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 车站色解析（Forge / mojmap 版）。判定链与 Fabric 侧 {@code PoleColors} 逐条一致：
 * <ol>
 *   <li>{@code dataCache.blockPosToStation.get(pos)}（这一格正好是车站登记坐标）；</li>
 *   <li>{@code blockPosToStation} 里 ±{@value #SCAN_RADIUS} 格内最近的车站；</li>
 *   <li>{@code RailwayData.getStation(stations, dataCache, pos)}（MTR 自己的「在车站区域内」判定）；</li>
 *   <li>都没有 → {@code 0}（调用方用白色）。</li>
 * </ol>
 * 车站 → 线路色：线路的 {@code platformIds} 里有站台属于该车站，取线路 id 升序的第一条颜色非 0。
 */
public final class PoleColors {

    /** 默认色（白）。<b>必须带不透明 alpha</b>：这个 int 被当作 ARGB 乘进顶点色，alpha=0 会让面全透明。 */
    public static final int WHITE = 0xFFFFFFFF;
    public static final int SCAN_RADIUS = 24;

    private static boolean loggedFailure;

    private PoleColors() {
    }

    public static int resolve(final Level level, final BlockPos pos) {
        if (level == null || pos == null) {
            return 0;
        }
        try {
            if (level.isClientSide) {
                return PoleColorsClient.resolve(pos);
            }
            final RailwayData data = RailwayData.getInstance(level);
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

    public static Station findStation(final DataCache cache, final BlockPos pos, final Set<Station> stations) {
        if (cache == null || pos == null) {
            return null;
        }
        final Station exact = cache.blockPosToStation.get(pos);
        if (exact != null) {
            return exact;
        }
        final Station near = nearest(cache.blockPosToStation, pos);
        if (near != null) {
            return near;
        }
        try {
            return RailwayData.getStation(stations, cache, pos);
        } catch (final Throwable ignored) {
            return null;
        }
    }

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
            final double distance = other.distSqr(pos);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = station;
            }
        }
        return best;
    }

    private static int routeColor(final java.util.Collection<Route> routes, final DataCache cache,
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
