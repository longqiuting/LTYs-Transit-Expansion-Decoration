// 厦门地铁柱子「车站色判定链」的离线检查：不需要世界、不需要客户端，直接喂合成出来的
// DataCache 把 PoleColors 的三条规则跑一遍。
//
//   javac 到临时目录（classpath = v1.20.1/build/classes/java/main + named MC + MTR named jar）
//   java  -cp 同上一份 PoleColorCheck
//
// 覆盖：
//   ① blockPosToStation 精确命中
//   ② 取不到时 ±24 格内最近的
//   ③ 超出 ±24 → null（不瞎猜）
//   ④ 车站 → 线路：线路 id 升序，跳过 color == 0，只认「站台属于该站」的线路
//   ⑤ 一条都没有 → 0（调用方用白色）
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.js.mtrheadlight.pole.PoleColors;
import mtr.data.DataCache;
import mtr.data.Route;
import mtr.data.Station;
import mtr.data.TransportMode;
import net.minecraft.util.math.BlockPos;

public final class PoleColorCheck {

    private static int failures;

    public static void main(final String[] args) {
        // ---- 合成一份数据：车站 100 在北边，车站 200 在南边（离得很远）
        final Station north = new Station(100L);
        north.name = "North";
        final Station south = new Station(200L);
        south.name = "South";

        final Set<Station> stations = new HashSet<>();
        stations.add(north);
        stations.add(south);

        // 平台 11 / 12 属于北站，平台 21 属于南站
        final DataCache cache = new DataCache(stations, new HashSet<>(), new HashSet<>(),
                new HashSet<>(), new HashSet<>(), new HashSet<>());
        cache.platformIdToStation.put(Long.valueOf(11L), north);
        cache.platformIdToStation.put(Long.valueOf(12L), north);
        cache.platformIdToStation.put(Long.valueOf(21L), south);
        cache.blockPosToStation.put(new BlockPos(0, 64, 0), north);
        cache.blockPosToStation.put(new BlockPos(500, 64, 500), south);

        // ---- ① 精确命中
        expectStation("精确命中", cache, stations, new BlockPos(0, 70, 0), north);
        // ---- ② ±24 内最近的
        expectStation("就近（20 格）", cache, stations, new BlockPos(18, 70, 0), north);
        expectStation("就近（24 格边界）", cache, stations, new BlockPos(0, 64, 24), north);
        // ---- ③ 超出范围 → null
        expectStation("超出 ±24 → null", cache, stations, new BlockPos(40, 70, 0), null);
        expectStation("几百格外 → null", cache, stations, new BlockPos(250, 70, 250), null);

        // ---- ④ 线路色：id 小的先算，color == 0 跳过，只有「站台属于该站」的才算
        final Route noColor = route(1L, 0x000000, 11L);            // 没设色 → 跳过
        final Route otherStation = route(2L, 0x00FF00, 21L);       // 绿色，但服务南站 → 不算
        final Route metro = route(3L, 0x1E88E5, 11L);              // 蓝色，北站 → 命中
        final Route metro2 = route(4L, 0xE53935, 12L);             // 红色，北站（但 id 更大）

        final List<Route> routes = new ArrayList<>(List.of(noColor, otherStation, metro2, metro));
        expectColor("线路色（跳过 0 / 非本站 / 取 id 最小）", PoleColors.routeColor(routes, cache, north),
                0x1E88E5, 1);
        expectColor("线路色（南站只看 21）", PoleColors.routeColor(routes, cache, south), 0x00FF00, 2);
        expectColor("没有线路 → 0", PoleColors.routeColor(new ArrayList<>(), cache, north), 0, 3);
        expectColor("全是未设色 → 0",
                PoleColors.routeColor(List.of(route(9L, 0, 11L)), cache, north), 0, 4);

        if (failures == 0) {
            System.out.println("PASS 车站色判定链（5 条定位规则 + 4 条线路色规则）");
            return;
        }
        System.out.println("FAIL " + failures + " 项");
        System.exit(1);
    }

    /** 造一条线路：id + 颜色 + 一个站台。 */
    private static Route route(final long id, final int color, final long platformId) {
        final Route route = new Route(id, TransportMode.TRAIN);
        route.color = color;
        route.platformIds.add(new Route.RoutePlatform(platformId));
        return route;
    }

    private static void expectStation(final String what, final DataCache cache, final Set<Station> stations,
                                      final BlockPos pos, final Station expected) {
        final Station actual = PoleColors.findStation(cache, pos, stations);
        final long actualId = actual == null ? -1L : actual.id;
        final long expectedId = expected == null ? -1L : expected.id;
        if (actualId != expectedId) {
            failures++;
            System.out.println("FAIL " + what + "：期望站 " + expectedId + "，实际 " + actualId);
            return;
        }
        System.out.println("ok   " + what + " -> station=" + actualId);
    }

    private static void expectColor(final String what, final int actual, final int expected, final int index) {
        if (actual != expected) {
            failures++;
            System.out.printf("FAIL %s：期望 #%06X，实际 #%06X%n", what, expected, actual);
            return;
        }
        System.out.printf("ok   %s -> #%06X (case %d)%n", what, actual, index);
    }
}
