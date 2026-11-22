package dev.js.mtrheadlight.clock;

public final class ClockGlobalCache {
   private static volatile ClockGlobalConfig current;

   private ClockGlobalCache() {
   }

   public static ClockGlobalConfig get() {
      ClockGlobalConfig cfg = current;
      return cfg == null ? new ClockGlobalConfig() : cfg.copy();
   }

   public static boolean enabled() {
      ClockGlobalConfig cfg = current;
      return cfg != null && cfg.enabled;
   }

   public static void set(ClockGlobalConfig cfg) {
      current = cfg == null ? null : cfg.copy();
   }

   public static void clear() {
      current = null;
   }

   public static ClockConfig effectiveOr(ClockConfig own) {
      ClockGlobalConfig cfg = current;
      return cfg != null && cfg.enabled ? cfg.config.copy() : own;
   }
}
