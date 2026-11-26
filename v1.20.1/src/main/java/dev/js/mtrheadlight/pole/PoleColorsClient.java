// 车站色解析的客户端那一半：用客户端的 MTR 数据缓存（ClientCache）算出同一个答案。
package dev.js.mtrheadlight.pole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mtr.client.ClientCache;
import mtr.client.ClientData;
import mtr.data.Station;
import net.minecraft.util.math.BlockPos;

/**
 * {@link PoleColors} 的客户端版本 —— <b>只在物理客户端加载</b>。
 *
 * <h2>为什么客户端也要自己算一遍</h2>
 * <p>服务端在<b>放下</b>柱子的那一刻就会算好并且写进存档 NBT；客户端平时读到的是服务端同步过来的
 * 那个值，不需要重算。但有两种情况客户端的缓存里 <b>没有</b>那个值：
 * <ul>
 *   <li>柱子是在 MTR 数据到客户端之前放下的（进服第一秒、数据还在同步）；</li>
 *   <li>存档里的柱子是旧版本 / 别的工具写的，NBT 里没有那个字段。</li>
 * </ul>
 * 这两种情况都只在「还没解析出来」时发生一次（方块实体解析成功就把结果缓存住，见
 * {@code AmtrPoleBottomBlockEntity#effectiveColor()}），所以这里不存在每帧扫数据的问题。
 *
 * <h2>数据来源</h2>
 * <ul>
 *   <li>车站：{@link PoleColors#findStation}（同一个判定链，{@code ClientCache} 继承
 *       {@code DataCache}，字段名一样）；</li>
 *   <li>车站 → 线路色：{@code ClientData.DATA_CACHE.stationIdToRoutes}（key = 线路色，
 *       value = {@code ColorNameTuple}）。这就是 MTR 自己画换乘环用的那张表，
 *       {@code XiamenRouteStrip} 也读它。</li>
 * </ul>
 */
final class PoleColorsClient {

    private PoleColorsClient() {
    }

    /** @return {@code 0xRRGGBB}；{@code 0} = 没解析出来 */
    static int resolve(final BlockPos pos) {
        if (pos == null) {
            return 0;
        }
        final ClientCache cache = ClientData.DATA_CACHE;
        if (cache == null) {
            return 0;
        }
        final Station station = PoleColors.findStation(cache, pos, ClientData.STATIONS);
        if (station == null) {
            return 0;
        }
        final Map<Integer, ClientCache.ColorNameTuple> all =
                cache.stationIdToRoutes.get(Long.valueOf(station.id));
        if (all == null || all.isEmpty()) {
            return 0;
        }
        // 与 {@link PoleColors} 的判定保持一致：线路色升序，取第一条非 0。
        final List<Integer> colours = new ArrayList<>(all.keySet());
        colours.sort(Integer::compareTo);
        for (final Integer colour : colours) {
            if (colour != null && (colour.intValue() & 0xFFFFFF) != 0) {
                return colour.intValue() & 0xFFFFFF;
            }
        }
        return 0;
    }
}
