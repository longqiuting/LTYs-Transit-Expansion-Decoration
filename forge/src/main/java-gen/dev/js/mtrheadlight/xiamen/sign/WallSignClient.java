package dev.js.mtrheadlight.xiamen.sign;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;

public final class WallSignClient implements ClientModInitializer {
   public void onInitializeClient() {
      try {
         BlockEntityRendererRegistry.register(WallSignObjects.entityType(), (context) -> new WallSignRenderer(context.getBlockEntityRenderDispatcher()));
         LteLog.LOGGER.info("[lte] wall sign renderer registered: {} (MTR's RenderStationNameTiled)", WallSignRenderer.class.getName());
      } catch (Throwable var5) {
         LteLog.LOGGER.error("[lte] could not register the wall sign renderer", var5);
      }

      for(WallSignObjects.Entry entry : WallSignObjects.entries()) {
         try {
            BlockRenderLayerMap.INSTANCE.putBlock(entry.block(), RenderType.cutout());
         } catch (Throwable var4) {
            LteLog.LOGGER.warn("[lte] could not set the cutout layer for the wall sign {}", entry.id(), var4);
         }
      }

   }
}
