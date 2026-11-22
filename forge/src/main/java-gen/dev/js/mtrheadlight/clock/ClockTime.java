package dev.js.mtrheadlight.clock;

import java.time.ZonedDateTime;
import java.util.Locale;
import net.minecraft.world.level.Level;

public final class ClockTime {
   public static final double DAY_SECONDS = 86400.0D;

   private ClockTime() {
   }

   public static double wrap(double seconds) {
      double s = seconds % 86400.0D;
      if (s < 0.0D) {
         s += 86400.0D;
      }

      return s;
   }

   public static double secondsOfDay(Level world, ClockConfig.TimeSource source, double tickDelta) {
      if (source == ClockConfig.TimeSource.REAL) {
         ZonedDateTime now = ZonedDateTime.now();
         return (double)now.getHour() * 3600.0D + (double)now.getMinute() * 60.0D + (double)now.getSecond() + (double)now.getNano() / 1.0E9D + tickDelta * 0.05D;
      } else if (world == null) {
         return 0.0D;
      } else {
         long tod = world.getDayTime() % 24000L;
         return wrap((double)tod / 24000.0D * 86400.0D + 21600.0D + tickDelta * 3.6D);
      }
   }

   public static double displayTarget(Level world, ClockConfig config, double tickDelta) {
      return wrap(secondsOfDay(world, config.timeSource, tickDelta) + config.offsetMinutes * 60.0D);
   }

   public static int hour(double secondsOfDay) {
      return (int)(wrap(secondsOfDay) / 3600.0D);
   }

   public static int minute(double secondsOfDay) {
      return (int)(wrap(secondsOfDay) % 3600.0D / 60.0D);
   }

   public static double second(double secondsOfDay) {
      return wrap(secondsOfDay) % 60.0D;
   }

   public static int hour12(double secondsOfDay) {
      int h = hour(secondsOfDay) % 12;
      return h == 0 ? 12 : h;
   }

   public static String formatHhMm(double secondsOfDay) {
      return String.format(Locale.ROOT, "%02d:%02d", hour(secondsOfDay), minute(secondsOfDay));
   }

   public static double hourFraction(double secondsOfDay) {
      double s = wrap(secondsOfDay);
      double h12 = s / 3600.0D % 12.0D;
      return h12 / 12.0D;
   }

   public static double minuteFraction(double secondsOfDay) {
      double s = wrap(secondsOfDay);
      return s % 3600.0D / 3600.0D;
   }

   public static double secondFraction(double secondsOfDay) {
      double s = wrap(secondsOfDay);
      return s % 60.0D / 60.0D;
   }
}
