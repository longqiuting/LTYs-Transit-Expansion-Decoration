package dev.js.mtrheadlight.xiamen.dep;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;

public final class DepOnWallClient implements ClientModInitializer {
   public void onInitializeClient() {
      try {
         BlockEntityRendererRegistry.register(DepOnWallObjects.entityType(), DepOnWallRenderer::new);
         LteLog.LOGGER.info("[lte] dep on wall renderer registered: {}", DepOnWallRenderer.class.getName());
      } catch (Throwable var6) {
         LteLog.LOGGER.error("[lte] could not register the dep on wall renderer", var6);
      }

      try {
         BlockEntityRendererRegistry.register(DepOnRoofObjects.entityType(), DepOnRoofRenderer::new);
         LteLog.LOGGER.info("[lte] dep on roof renderer registered: {}", DepOnRoofRenderer.class.getName());
      } catch (Throwable var5) {
         LteLog.LOGGER.error("[lte] could not register the dep on roof renderer", var5);
      }

      try {
         BlockEntityRendererRegistry.register(DepPoleObjects.entityType(), DepPoleRenderer::new);
         LteLog.LOGGER.info("[lte] dep pole renderer registered: {}", DepPoleRenderer.class.getName());
      } catch (Throwable var4) {
         LteLog.LOGGER.error("[lte] could not register the dep pole renderer", var4);
      }

      try {
         BlockRenderLayerMap.INSTANCE.putBlock(DepOnWallObjects.block(), RenderType.cutout());
      } catch (Throwable var3) {
         LteLog.LOGGER.warn("[lte] could not set the cutout layer for the dep on wall block", var3);
      }

      try {
         BlockRenderLayerMap.INSTANCE.putBlock(DepOnRoofObjects.block(), RenderType.cutout());
      } catch (Throwable var2) {
         LteLog.LOGGER.warn("[lte] could not set the cutout layer for the dep on roof block", var2);
      }

   }
}
