package dev.js.mtrheadlight;

import net.minecraft.client.Minecraft;

public final class McCompat {
   private McCompat() {
   }

   public static int viewDistanceChunks() {
      try {
         return Minecraft.getInstance().options.renderDistance().get();
      } catch (RuntimeException var1) {
         return 0;
      }
   }
}
