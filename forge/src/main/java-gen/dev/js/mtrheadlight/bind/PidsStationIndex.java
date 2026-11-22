package dev.js.mtrheadlight.bind;

import java.util.HashMap;
import java.util.Map;
import mtr.client.ClientData;
import mtr.data.DataCache;
import mtr.data.Platform;
import mtr.data.Station;
import net.minecraft.core.BlockPos;

public final class PidsStationIndex {
   public static final String SRC_MTR = "mtr";
   public static final String SRC_OURS = "ours";
   private static final Map<Long, Station> STATIONS = new HashMap();
   private static final Map<Long, String> SOURCES = new HashMap();
   private static final Map<Long, Long> RETRY_AT = new HashMap();
   private static final long RETRY_MS = 500L;
   private static int misses;

   private PidsStationIndex() {
   }

   public static Station stationOf(DataCache dataCache, long platformId) {
      if (platformId == 0L) {
         return null;
      } else {
         Station known = (Station)STATIONS.get(platformId);
         if (known != null) {
            return known;
         } else {
            long now = System.currentTimeMillis();
            Long retryAt = (Long)RETRY_AT.get(platformId);
            if (retryAt != null && now < retryAt) {
               return null;
            } else {
               Station found = resolve(dataCache, platformId);
               if (found == null) {
                  RETRY_AT.put(platformId, now + 500L);
                  ++misses;
                  return null;
               } else {
                  STATIONS.put(platformId, found);
                  RETRY_AT.remove(platformId);
                  return found;
               }
            }
         }
      }
   }

   public static String sourceOf(long platformId) {
      return (String)SOURCES.get(platformId);
   }

   public static int size() {
      return STATIONS.size();
   }

   public static int misses() {
      return misses;
   }

   public static void reset() {
      STATIONS.clear();
      SOURCES.clear();
      RETRY_AT.clear();
      misses = 0;
   }

   private static Station resolve(DataCache dataCache, long platformId) {
      if (dataCache == null) {
         return null;
      } else {
         try {
            Station byCache = (Station)dataCache.platformIdToStation.get(platformId);
            if (byCache != null) {
               SOURCES.put(platformId, "mtr");
               return byCache;
            }
         } catch (Throwable var7) {
         }

         try {
            Platform platform = (Platform)dataCache.platformIdMap.get(platformId);
            if (platform == null) {
               return null;
            }

            BlockPos mid = platform.getMidPos();
            if (mid == null) {
               return null;
            }

            Station byArea = stationContaining(mid.getX(), mid.getZ());
            if (byArea != null) {
               SOURCES.put(platformId, "ours");
               return byArea;
            }
         } catch (Throwable var6) {
         }

         return null;
      }
   }

   private static Station stationContaining(int x, int z) {
      try {
         for(Station station : ClientData.STATIONS) {
            if (station != null && station.inArea(x, z)) {
               return station;
            }
         }
      } catch (Throwable var4) {
      }

      return null;
   }

   public static Station firstResolvable(DataCache dataCache, Iterable<Long> platformIds) {
      if (platformIds == null) {
         return null;
      } else {
         for(Long id : platformIds) {
            if (id != null) {
               Station station = stationOf(dataCache, id);
               if (station != null) {
                  return station;
               }
            }
         }

         return null;
      }
   }
}
