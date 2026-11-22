package dev.js.mtrheadlight.xiamen.eyecandy;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public final class EyecandyClient {
   private EyecandyClient() {
   }

   public static void register() {
      try {
         BlockEntityRendererRegistry.register(EyecandyBlocks.entityType(), EyecandyRenderer::new);
         LteLog.LOGGER.info("[lte] eyecandy renderer registered: {} ({} blocks, obj)", EyecandyRenderer.class.getSimpleName(), EyecandyBlocks.blocks().size());
      } catch (Throwable var1) {
         LteLog.LOGGER.error("[lte] could not register the eyecandy renderer", var1);
      }

   }
}
