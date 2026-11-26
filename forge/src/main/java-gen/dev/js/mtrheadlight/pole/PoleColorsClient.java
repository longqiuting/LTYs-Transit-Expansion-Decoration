package dev.js.mtrheadlight.pole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mtr.client.ClientCache;
import mtr.client.ClientData;
import mtr.data.Station;
import net.minecraft.core.BlockPos;

/** 车站色解析的客户端那一半（Forge / mojmap 版）：读 {@code ClientData.DATA_CACHE}。 */
final class PoleColorsClient {

    private PoleColorsClient() {
    }

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
