package dev.js.mtrheadlight.xiamen.dep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.ScheduleEntry;
import net.minecraft.core.BlockPos;

final class DepCountdown {
   private static final int SEARCH_RADIUS = 5;
   private static final int SEARCH_MARGIN = 3;
   static final DepCountdown.Search WALL_SEARCH = new DepCountdown.Search(5, 3, 3);
   static final DepCountdown.Search ROOF_SEARCH = new DepCountdown.Search(7, 4, 5);

   private DepCountdown() {
   }

   static DepCountdown.Display display(DepCountdown.Holder holder, BlockPos pos) {
      long now = System.currentTimeMillis();
      long platformId = 0L;
      int entries = 0;
      ScheduleEntry head = null;
      Long sec = null;
      int dwellRaw = 0;
      int dwellHalf = 0;
      Integer remain = null;
      String phase = "blank";
      Long lockArrival = null;
      DepCountdown.Display result = null;

      try {
         DepCountdown.Search search = holder.search();
         platformId = RailwayData.getClosePlatformId(ClientData.PLATFORMS, ClientData.DATA_CACHE, pos, search.radius(), search.horizontalMargin(), search.verticalMargin());
         if (platformId == 0L) {
            holder.setDepLock((DepOnWallLock)null);
         } else {
            Platform platform = (Platform)ClientData.DATA_CACHE.platformIdMap.get(platformId);
            dwellRaw = platform == null ? 0 : platform.getDwellTime();
            dwellHalf = dwellRaw / 2;
            List<ScheduleEntry> list = schedules(platformId);
            entries = list == null ? 0 : list.size();
            head = firstLive(list, dwellHalf, now);
            DepOnWallLock lock = holder.depLock();
            if (lock != null && (!lock.samePlatform(platformId) || lock.finished(now))) {
               lock = null;
            }

            if (lock != null && !lock.started() && head != null && !lock.matches(head)) {
               lock = null;
            }

            if (lock == null && head != null) {
               lock = DepOnWallLock.of(platformId, head, dwellHalf);
            }

            if (lock != null) {
               lockArrival = lock.arrivalMillis();
               sec = (lock.arrivalMillis() - now) / 1000L;
               DepOnWallLock.Reading reading = lock.tick(now);
               if (reading != null) {
                  remain = reading.remain();
                  phase = reading.overdue() ? "red" : "green";
                  result = new DepCountdown.Display(format((long)reading.remain()), reading.overdue());
               }

               if (lock.done()) {
                  lock = null;
               }
            }

            holder.setDepLock(lock);
         }
      } catch (Throwable var20) {
         result = null;
      }

      if (DepOnWallDebug.enabled()) {
         DepOnWallDebug.log(pos, now, "[lte] dep: pos=(" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ") platform=" + platformId + " entries=" + entries + " firstArrival=" + (head == null ? "none" : Long.toString(head.arrivalMillis)) + " now=" + now + " sec=" + (sec == null ? "none" : sec.toString()) + " dwell=" + dwellRaw + " dwellHalf=" + dwellHalf + " remain=" + (remain == null ? "none" : remain.toString()) + " phase=" + phase + " lockArrival=" + (lockArrival == null ? "none" : lockArrival.toString()));
      }

      return result;
   }

   private static List<ScheduleEntry> schedules(long platformId) {
      Set<ScheduleEntry> schedules = (Set)ClientData.SCHEDULES_FOR_PLATFORM.get(platformId);
      if (schedules == null) {
         return null;
      } else {
         List<ScheduleEntry> list = new ArrayList(schedules);
         Collections.sort(list);
         return list;
      }
   }

   private static ScheduleEntry firstLive(List<ScheduleEntry> list, int dwellHalf, long now) {
      if (list == null) {
         return null;
      } else {
         for(ScheduleEntry entry : list) {
            if (!DepOnWallLock.finished(entry.arrivalMillis, dwellHalf, now)) {
               return entry;
            }
         }

         return null;
      }
   }

   private static String format(long seconds) {
      long s = Math.max(0L, seconds);
      return String.format("%02d:%02d", s / 60L, s % 60L);
   }

   static record Display(String text, boolean overdue) {
   }

   interface Holder {
      DepOnWallLock depLock();

      void setDepLock(DepOnWallLock var1);

      default DepCountdown.Search search() {
         return DepCountdown.WALL_SEARCH;
      }
   }

   static record Search(int radius, int horizontalMargin, int verticalMargin) {
   }
}
