package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.decor.DecorConfig;
import dev.js.mtrheadlight.xiamen.XiamenRouteStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mtr.client.ClientData;
import mtr.data.Route;

public final class PsdLcdStrip {
   private static final int WHITE = -1;
   private static boolean warned;
   private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
   private static long cachedPlatform = Long.MIN_VALUE;
   private static int[] cachedColors;
   private static long cachedAt;
   private static final long CACHE_MS = 500L;

   private PsdLcdStrip() {
   }

   public static int[] colors(long platformId) {
      long now = System.currentTimeMillis();
      int[] cached = cachedColors;
      if (cached != null && cachedPlatform == platformId && now - cachedAt < 500L) {
         return cached;
      } else {
         int[] out = compute(platformId);
         cachedPlatform = platformId;
         cachedColors = out;
         cachedAt = now;
         return out;
      }
   }

   private static int[] compute(long platformId) {
      List<Integer> colors = resolve(platformId);
      if (colors.isEmpty()) {
         if (!warned) {
            warned = true;
            LteLog.LOGGER.warn("[lte] psd lcd: no route colour for platform {} (no route contains it / MTR data not synced yet) \u2014 drawing the line strip white", platformId);
         }

         return new int[]{-1};
      } else {
         int[] out = new int[colors.size()];

         for(int i = 0; i < out.length; ++i) {
            out[i] = colors.get(i);
         }

         logOnce(platformId, out);
         return out;
      }
   }

   private static List<Integer> resolve(long platformId) {
      List<Integer> primary = new ArrayList();
      List<Integer> terminating = new ArrayList();
      if (platformId == 0L) {
         return primary;
      } else {
         try {
            List<Route> routes = new ArrayList();

            for(Route route : ClientData.ROUTES) {
               if (route != null && !route.isHidden && route.containsPlatformId(platformId)) {
                  routes.add(route);
               }
            }

            routes.sort((a, b) -> {
               int ca = a.color & 16777215;
               int cb = b.color & 16777215;
               if (ca != cb) {
                  return Integer.compare(ca, cb);
               } else {
                  String na = a.name == null ? "" : a.name;
                  String nb = b.name == null ? "" : b.name;
                  return na.compareTo(nb);
               }
            });

            for(Route route : routes) {
               int rgb = rgbOf(route);
               if (rgb != 0) {
                  int index = route.getPlatformIdIndex(platformId);
                  boolean terminatesHere = index < 0 || index >= route.platformIds.size() - 1;
                  List<Integer> target = terminatesHere ? terminating : primary;
                  if (!target.contains(rgb)) {
                     target.add(rgb);
                  }
               }
            }

            if (primary.isEmpty()) {
               primary.addAll(terminating);
            }
         } catch (Throwable var11) {
            LteLog.LOGGER.debug("[lte] psd lcd: could not resolve the line colours", var11);
         }

         return primary;
      }
   }

   private static int rgbOf(Route route) {
      int rgb;
      try {
         rgb = XiamenRouteStyle.lineColor(DecorConfig.get(), route, route.name) & 16777215;
      } catch (Throwable var3) {
         return (route.color & 16777215) == 0 ? 0 : -16777216 | route.color & 16777215;
      }

      return -16777216 | rgb;
   }

   private static void logOnce(long platformId, int[] colors) {
      StringBuilder key = (new StringBuilder()).append(platformId);
      StringBuilder text = new StringBuilder();

      for(int c : colors) {
         key.append('|').append(Integer.toHexString(c));
         if (text.length() > 0) {
            text.append(',');
         }

         text.append('#').append(String.format("%06X", c & 16777215));
      }

      if (LOGGED.size() <= 256 && LOGGED.add(key.toString())) {
         LteLog.LOGGER.info("[lte] psd lcd: strip platform={} colors={}", platformId, text);
      }
   }
}
