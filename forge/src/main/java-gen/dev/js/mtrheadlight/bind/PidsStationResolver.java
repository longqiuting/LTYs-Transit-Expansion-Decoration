package dev.js.mtrheadlight.bind;

import dev.js.mtrheadlight.LteLog;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import mtr.data.DataCache;
import mtr.data.RailwayData;
import mtr.data.Station;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

public final class PidsStationResolver {
   public static final String SRC_BE = "be";
   public static final String SRC_MTR = "mtr";
   private static final Map<String, String> LAST_LINE = new HashMap();
   private static final int LOG_LIMIT = 512;
   private static ClientLevel lastWorld;

   private PidsStationResolver() {
   }

   public static PidsStationResolver.Result resolve(Set<Station> stations, DataCache dataCache, BlockPos pos) {
      lte$resetIfWorldChanged();
      if (pos == null) {
         return new PidsStationResolver.Result((Station)null, "mtr", Collections.emptySet());
      } else {
         Station inside = RailwayData.getStation(stations, dataCache, pos);
         Set<Long> ids = lte$idsAt(pos);
         if (ids.isEmpty()) {
            return new PidsStationResolver.Result(inside, "mtr", ids);
         } else {
            Station bound = PidsStationIndex.firstResolvable(dataCache, ids);
            if (bound == null) {
               return new PidsStationResolver.Result(inside, "mtr", ids);
            } else if (inside == null) {
               return new PidsStationResolver.Result(bound, "be", ids);
            } else if (bound.id == inside.id) {
               return new PidsStationResolver.Result(inside, "be", ids);
            } else {
               return lte$anyBelongsTo(dataCache, ids, inside) ? new PidsStationResolver.Result(inside, "be", ids) : new PidsStationResolver.Result(bound, "be", ids);
            }
         }
      }
   }

   private static boolean lte$anyBelongsTo(DataCache dataCache, Set<Long> ids, Station station) {
      for(Long id : ids) {
         if (id != null) {
            Station owner = PidsStationIndex.stationOf(dataCache, id);
            if (owner != null && owner.id == station.id) {
               return true;
            }
         }
      }

      return false;
   }

   private static Set<Long> lte$idsAt(BlockPos pos) {
      try {
         ClientLevel world = Minecraft.getInstance().level;
         if (world == null) {
            return Collections.emptySet();
         }

         Object tile = world.getBlockEntity(pos);
         if (tile == null) {
            return Collections.emptySet();
         }

         if (MtrTiles.isProjector(tile)) {
            return new LinkedHashSet(MtrTiles.projectorPlatformIds(tile));
         }

         if (MtrTiles.isPids(tile)) {
            return new LinkedHashSet(MtrTiles.pidsPlatformIds(tile));
         }
      } catch (Throwable var3) {
      }

      return Collections.emptySet();
   }

   private static void lte$resetIfWorldChanged() {
      try {
         ClientLevel world = Minecraft.getInstance().level;
         if (world != lastWorld) {
            lastWorld = world;
            PidsStationIndex.reset();
            LAST_LINE.clear();
         }
      } catch (Throwable var1) {
      }

   }

   public static void log(String tag, BlockPos pos, PidsStationResolver.Result result) {
      Set<Long> ids = result.platformIds();
      String idsText = ids.isEmpty() ? "[]" : (new TreeSet(ids)).toString();
      Station station = result.station();
      String stationText = station == null ? "null" : station.id + "/" + station.name;
      String line = "pos=" + String.valueOf(pos) + " be=" + idsText + " -> station=" + stationText + " src=" + result.src();
      String key = tag + "@" + String.valueOf(pos);
      if (!line.equals(LAST_LINE.get(key))) {
         if (LAST_LINE.size() > 512) {
            LAST_LINE.clear();
         }

         LAST_LINE.put(key, line);
         LteLog.LOGGER.info("[lte] pids station: {}", line);
      }
   }

   public static record Result(Station station, String src, Set<Long> platformIds) {
   }
}
