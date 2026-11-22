package dev.js.mtrheadlight.xiamen.dep;

import dev.js.mtrheadlight.LteLog;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

public final class DepOnWallDebug {
   private static final long INTERVAL_MILLIS = 1000L;
   private static final int MAX_POSITIONS = 256;
   private static volatile boolean enabled;
   private static final Map<BlockPos, Long> LAST = new ConcurrentHashMap();

   private DepOnWallDebug() {
   }

   public static boolean enabled() {
      return enabled;
   }

   public static void set(boolean on) {
      enabled = on;
      LAST.clear();
      LteLog.LOGGER.info("[lte] dep debug = {}", on ? "on" : "off");
   }

   public static String statusLine() {
      return "LTE\uff1a\u5012\u6570\u5668\u53d6\u8bc1 = " + (enabled ? "on" : "off") + (enabled ? "\uff08\u6bcf\u5757\u724c\u5b50\u6bcf\u79d2\u4e00\u884c [lte] dep: \u2026\uff0c\u5e26\u4e0a platform/entries/firstArrival/sec/dwell/phase\uff09" : "\uff08\u9ed8\u8ba4\u5173\uff09") + " \u2014 /mtr_lte dep debug on|off";
   }

   static void log(BlockPos pos, long now, String message) {
      if (enabled) {
         try {
            BlockPos key = pos.immutable();
            Long last = (Long)LAST.get(key);
            if (last != null && now >= last && now - last < 1000L) {
               return;
            }

            LAST.put(key, now);
            if (LAST.size() > 256) {
               LAST.clear();
            }

            LteLog.LOGGER.info(message);
         } catch (Throwable var7) {
            Throwable t = var7;

            try {
               LteLog.LOGGER.warn("[lte] dep debug: could not log", t);
            } catch (Throwable var6) {
            }
         }

      }
   }
}
