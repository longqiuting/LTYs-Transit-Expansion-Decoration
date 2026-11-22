package dev.js.mtrheadlight.logo;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

public final class LogoClient implements ClientModInitializer {
   public void onInitializeClient() {
      put(LogoObjects.lteLogo(), "lte logo");
      put(LogoObjects.jiaomeiLogo(), "jiaomei logo");
   }

   private static void put(Block block, String what) {
      if (block != null) {
         try {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout());
         } catch (Throwable var3) {
            LteLog.LOGGER.warn("[lte] could not set the cutout layer for the {} block", what, var3);
         }

      }
   }
}
