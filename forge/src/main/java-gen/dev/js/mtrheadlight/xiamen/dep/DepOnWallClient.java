package dev.js.mtrheadlight.xiamen.dep;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Layers;
import dev.js.mtrheadlight.forge.platform.Renderers;
import net.minecraft.client.renderer.RenderType;

public final class DepOnWallClient implements ClientInit {
   public void onInitializeClient() {
      try {
         Renderers.ber(DepOnWallObjects.entityType(), DepOnWallRenderer::new);
         LteLog.LOGGER.info("[lte] dep on wall renderer registered: {}", DepOnWallRenderer.class.getName());
      } catch (Throwable var6) {
         LteLog.LOGGER.error("[lte] could not register the dep on wall renderer", var6);
      }

      try {
         Renderers.ber(DepOnRoofObjects.entityType(), DepOnRoofRenderer::new);
         LteLog.LOGGER.info("[lte] dep on roof renderer registered: {}", DepOnRoofRenderer.class.getName());
      } catch (Throwable var5) {
         LteLog.LOGGER.error("[lte] could not register the dep on roof renderer", var5);
      }

      try {
         Renderers.ber(DepPoleObjects.entityType(), DepPoleRenderer::new);
         LteLog.LOGGER.info("[lte] dep pole renderer registered: {}", DepPoleRenderer.class.getName());
      } catch (Throwable var4) {
         LteLog.LOGGER.error("[lte] could not register the dep pole renderer", var4);
      }

      try {
         Layers.put(DepOnWallObjects.block(), RenderType.cutout());
      } catch (Throwable var3) {
         LteLog.LOGGER.warn("[lte] could not set the cutout layer for the dep on wall block", var3);
      }

      try {
         Layers.put(DepOnRoofObjects.block(), RenderType.cutout());
      } catch (Throwable var2) {
         LteLog.LOGGER.warn("[lte] could not set the cutout layer for the dep on roof block", var2);
      }

   }
}
