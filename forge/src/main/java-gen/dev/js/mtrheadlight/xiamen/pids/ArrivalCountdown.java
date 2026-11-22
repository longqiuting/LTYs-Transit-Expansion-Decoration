package dev.js.mtrheadlight.xiamen.pids;

public final class ArrivalCountdown {
   public static final long ARRIVING_SECONDS = 60L;
   public static final String KEY_ARRIVED_PIDS = "gui.mtrlte.pids.arrived";
   public static final String KEY_ARRIVED_PSD_LCD = "gui.mtrlte.psd_lcd.arrived";
   public static final String KEY_ARRIVING = "gui.mtrlte.pids.arriving";
   public static final String KEY_MINUTE = "gui.mtrlte.pids.minute";
   public static final String KEY_SECOND = "gui.mtrlte.pids.second";

   private ArrivalCountdown() {
   }

   public static ArrivalCountdown.State state(long remainingSeconds, boolean arriving) {
      if (remainingSeconds <= 0L) {
         return ArrivalCountdown.State.ARRIVED;
      } else if (arriving && remainingSeconds <= 60L) {
         return ArrivalCountdown.State.ARRIVING;
      } else {
         return remainingSeconds > 60L ? ArrivalCountdown.State.MINUTES : ArrivalCountdown.State.SECONDS;
      }
   }

   public static String digits(ArrivalCountdown.State state, long remainingSeconds) {
      String var10000;
      switch (state.ordinal()) {
         case 0:
         case 1:
            var10000 = "";
            break;
         case 2:
            var10000 = String.valueOf(Math.max(1L, remainingSeconds / 60L));
            break;
         case 3:
            var10000 = String.valueOf(Math.max(1L, remainingSeconds));
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static String unitKey(ArrivalCountdown.State state) {
      String var10000;
      switch (state.ordinal()) {
         case 0:
         case 1:
            var10000 = null;
            break;
         case 2:
            var10000 = "gui.mtrlte.pids.minute";
            break;
         case 3:
            var10000 = "gui.mtrlte.pids.second";
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static String stateKey(ArrivalCountdown.State state, boolean psdLcd) {
      String var10000;
      switch (state.ordinal()) {
         case 0:
            var10000 = psdLcd ? "gui.mtrlte.psd_lcd.arrived" : "gui.mtrlte.pids.arrived";
            break;
         case 1:
            var10000 = "gui.mtrlte.pids.arriving";
            break;
         case 2:
         case 3:
            var10000 = null;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static enum State {
      ARRIVED,
      ARRIVING,
      MINUTES,
      SECONDS;
   }
}
