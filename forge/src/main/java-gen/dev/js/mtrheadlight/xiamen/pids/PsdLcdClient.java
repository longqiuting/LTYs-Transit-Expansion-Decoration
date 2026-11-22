package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;

public final class PsdLcdClient implements ClientModInitializer {
   public void onInitializeClient() {
      try {
         BlockEntityRendererRegistry.register(PsdLcdObjects.entityType(), PsdLcdRenderer::new);
         LteLog.LOGGER.info("[lte] psd lcd renderer registered: {}", PsdLcdRenderer.class.getName());
      } catch (Throwable var3) {
         LteLog.LOGGER.error("[lte] could not register the psd lcd renderer", var3);
      }

      try {
         BlockRenderLayerMap.INSTANCE.putBlock(PsdLcdObjects.block(), RenderType.cutout());
      } catch (Throwable var2) {
         LteLog.LOGGER.warn("[lte] could not set the cutout layer for the psd lcd block", var2);
      }

   }
}
