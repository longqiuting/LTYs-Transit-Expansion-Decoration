package dev.js.mtrheadlight.railing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public final class RailingGlassClient implements ClientModInitializer {
   public void onInitializeClient() {
      try {
         for(RailingWallBlock block : RailingGlassBlocks.blocks()) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.translucent());
         }
      } catch (Throwable var3) {
      }

   }
}
