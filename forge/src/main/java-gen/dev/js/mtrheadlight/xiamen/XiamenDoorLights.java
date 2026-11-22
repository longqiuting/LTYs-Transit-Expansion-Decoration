package dev.js.mtrheadlight.xiamen;

import dev.js.mtrheadlight.decor.DecorConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mtr.client.ClientCache;
import mtr.client.ClientData;
import mtr.data.RailwayData;
import mtr.data.Route;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

public final class XiamenDoorLights {
   private static final Map<Long, String> LINE_BY_POS = new HashMap();
   private static long lastRefresh;

   private XiamenDoorLights() {
   }

   public static String colorFor(Block block, BlockPos pos) {
      try {
         XiamenLine line = XiamenObjects.lineOf(block);
         DecorConfig cfg = DecorConfig.get();
         if (cfg == null) {
            return line.doorLight();
         } else {
            String byLine = lookup(cfg.doorLightByLine, line.number);
            if (byLine != null) {
               return DecorConfig.normalizeDoorLightColor(byLine);
            } else {
               String global = DecorConfig.normalizeDoorLightColor(cfg.doorLightColor);
               return !"orange".equals(global) ? global : line.doorLight();
            }
         }
      } catch (Throwable var6) {
         return "orange";
      }
   }

   public static String colorFor(BlockPos pos) {
      try {
         DecorConfig cfg = DecorConfig.get();
         if (cfg == null) {
            return "orange";
         } else {
            return cfg.doorLightByLine != null && !cfg.doorLightByLine.isEmpty() ? XiamenRouteStyle.doorLightColor(cfg, lineNumberAt(pos)) : DecorConfig.normalizeDoorLightColor(cfg.doorLightColor);
         }
      } catch (Throwable var2) {
         return "orange";
      }
   }

   private static String lookup(Map<String, String> map, String number) {
      if (map != null && !map.isEmpty() && number != null && !number.isEmpty()) {
         String value = (String)map.get(number);
         if (value == null) {
            value = (String)map.get(number + "\u53f7\u7ebf");
         }

         if (value == null) {
            String lower = number.toLowerCase(Locale.ROOT);

            for(Map.Entry<String, String> entry : map.entrySet()) {
               String key = entry.getKey() == null ? "" : ((String)entry.getKey()).trim().toLowerCase(Locale.ROOT);
               if (key.equals(lower) || key.equals(lower + "\u53f7\u7ebf")) {
                  value = (String)entry.getValue();
                  break;
               }
            }
         }

         return value;
      } else {
         return null;
      }
   }

   public static String lineNumberAt(BlockPos pos) {
      if (pos == null) {
         return "";
      } else {
         String cached = cachedLine(pos);
         if (cached != null) {
            return cached;
         } else {
            String number = "";

            try {
               long platformId = RailwayData.getClosePlatformId(ClientData.PLATFORMS, ClientData.DATA_CACHE, pos);
               if (platformId != 0L) {
                  List<ClientCache.PlatformRouteDetails> routes = ClientData.DATA_CACHE.requestPlatformIdToRoutes(platformId);
                  if (routes != null) {
                     for(ClientCache.PlatformRouteDetails details : routes) {
                        if (details != null) {
                           Route route = XiamenRouteStyle.findRoute(platformId, details.routeName);
                           number = XiamenRouteStyle.lineNumber(route, details.routeName);
                           if (!number.isEmpty()) {
                              break;
                           }
                        }
                     }
                  }
               }
            } catch (Throwable var11) {
               number = "";
            }

            synchronized(LINE_BY_POS) {
               if (LINE_BY_POS.size() > 4096) {
                  LINE_BY_POS.clear();
               }

               LINE_BY_POS.put(pos.asLong(), number);
               return number;
            }
         }
      }
   }

   private static String cachedLine(BlockPos pos) {
      try {
         if (ClientData.DATA_CACHE.needsRefresh(lastRefresh)) {
            synchronized(LINE_BY_POS) {
               LINE_BY_POS.clear();
            }

            lastRefresh = System.currentTimeMillis();
         }
      } catch (Throwable var6) {
         return null;
      }

      synchronized(LINE_BY_POS) {
         return (String)LINE_BY_POS.get(pos.asLong());
      }
   }
}
