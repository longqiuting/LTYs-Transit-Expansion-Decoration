package dev.js.mtrheadlight.decor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

public final class DecorClient implements ClientModInitializer {
   public void onInitializeClient() {
      try {
         putCutout(DecorBlocks.marblePlatform());
         putCutout(DecorBlocks.marbleFloor());
      } catch (Throwable var5) {
      }

      try {
         for(Block block : RccPlatforms.all()) {
            putCutout(block);
         }
      } catch (Throwable var6) {
      }

   }

   private static void putCutout(Block block) {
      if (block != null) {
         BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout());
      }

   }
}
