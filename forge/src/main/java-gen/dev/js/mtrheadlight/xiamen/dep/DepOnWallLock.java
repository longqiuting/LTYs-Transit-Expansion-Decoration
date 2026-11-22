package dev.js.mtrheadlight.xiamen.dep;

import mtr.data.ScheduleEntry;

final class DepOnWallLock {
   static final int OVERTIME_SECONDS = 5;
   private final long platformId;
   private final long arrivalMillis;
   private final long routeId;
   private final int dwellHalf;
   private boolean started;
   private boolean overdue;
   private boolean done;
   private int lastRemain;

   private DepOnWallLock(long platformId, ScheduleEntry entry, int dwellHalf) {
      this.platformId = platformId;
      this.arrivalMillis = entry.arrivalMillis;
      this.routeId = entry.routeId;
      this.dwellHalf = Math.max(0, dwellHalf);
   }

   static DepOnWallLock of(long platformId, ScheduleEntry entry, int dwellHalf) {
      return new DepOnWallLock(platformId, entry, dwellHalf);
   }

   boolean matches(ScheduleEntry entry) {
      return entry != null && entry.arrivalMillis == this.arrivalMillis && entry.routeId == this.routeId;
   }

   boolean samePlatform(long platformId) {
      return platformId == this.platformId;
   }

   long arrivalMillis() {
      return this.arrivalMillis;
   }

   int dwellHalf() {
      return this.dwellHalf;
   }

   boolean started() {
      return this.started;
   }

   boolean done() {
      return this.done;
   }

   boolean finished(long now) {
      return finished(this.arrivalMillis, this.dwellHalf, now);
   }

   static boolean finished(long arrivalMillis, int dwellHalf, long now) {
      long sec = (arrivalMillis - now) / 1000L;
      return sec <= 0L && Math.abs(sec) > (long)Math.max(0, dwellHalf) + 5L;
   }

   DepOnWallLock.Reading tick(long now) {
      if (this.done) {
         return null;
      } else {
         long secRaw = (this.arrivalMillis - now) / 1000L;
         if (!this.started) {
            if (secRaw > 0L) {
               return null;
            }

            this.started = true;
            long abs = Math.abs(secRaw);
            this.overdue = abs >= (long)this.dwellHalf;
            this.lastRemain = this.remain(abs);
         } else {
            long sec = Math.min(0L, secRaw);
            long abs = Math.abs(sec);
            if (!this.overdue) {
               if (abs >= (long)this.dwellHalf) {
                  this.overdue = true;
                  this.lastRemain = 0;
               } else {
                  this.lastRemain = Math.min(this.remain(abs), this.lastRemain);
               }
            } else {
               this.lastRemain = Math.max(this.remain(abs), this.lastRemain);
            }
         }

         if (!this.overdue) {
            return new DepOnWallLock.Reading(this.lastRemain, false);
         } else if (this.lastRemain > 5) {
            this.done = true;
            return null;
         } else {
            return new DepOnWallLock.Reading(this.lastRemain, true);
         }
      }
   }

   private int remain(long absSec) {
      long remain = Math.abs((long)this.dwellHalf - absSec);
      return (int)Math.min(2147483647L, remain);
   }

   static record Reading(int remain, boolean overdue) {
   }
}
