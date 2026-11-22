package dev.js.mtrheadlight.logo;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Layers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

public final class LogoClient implements ClientInit {
   public void onInitializeClient() {
      put(LogoObjects.lteLogo(), "lte logo");
      put(LogoObjects.jiaomeiLogo(), "jiaomei logo");
   }

   private static void put(Block block, String what) {
      if (block != null) {
         try {
            Layers.put(block, RenderType.cutout());
         } catch (Throwable var3) {
            LteLog.LOGGER.warn("[lte] could not set the cutout layer for the {} block", what, var3);
         }

      }
   }
}
