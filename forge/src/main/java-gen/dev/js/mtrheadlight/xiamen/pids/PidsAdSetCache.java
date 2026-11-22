package dev.js.mtrheadlight.xiamen.pids;

import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PidsAdSetCache {
   private static volatile PidsAdSet current;
   private static volatile PidsAdSet resolvedFor;
   private static volatile List<PidsXiamenAds.Ad> resolved;

   private PidsAdSetCache() {
   }

   public static PidsAdSet get() {
      PidsAdSet set = current;
      return set == null ? new PidsAdSet() : set.copy();
   }

   public static void set(PidsAdSet set) {
      current = set == null ? null : set.copy();
   }

   public static void clear() {
      current = null;
   }

   public static void save(PidsAdSet set) {
      PidsAdSet copy = set == null ? new PidsAdSet() : set.copy();
      current = copy;

      try {
         if (ClientPlayNetworking.canSend(PidsAdSetNet.ID)) {
            ClientPlayNetworking.send(PidsAdSetNet.ID, PidsAdSetNet.savePacket(copy));
         }
      } catch (Throwable var3) {
      }

   }

   public static List<PidsXiamenAds.Ad> activeAds() {
      PidsAdSet set = current;
      if (set != null && set.enabled()) {
         if (set == resolvedFor) {
            return resolved;
         } else {
            List<PidsXiamenAds.Ad> out;
            try {
               out = set.resolve();
            } catch (Throwable var3) {
               out = null;
            }

            resolvedFor = set;
            resolved = out;
            return out;
         }
      } else {
         return null;
      }
   }

   public static void invalidate() {
      resolvedFor = null;
      resolved = null;
   }
}
