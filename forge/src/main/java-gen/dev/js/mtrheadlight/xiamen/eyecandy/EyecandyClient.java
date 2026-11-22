package dev.js.mtrheadlight.xiamen.eyecandy;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.Renderers;

public final class EyecandyClient {
   private EyecandyClient() {
   }

   public static void register() {
      try {
         Renderers.ber(EyecandyBlocks.entityType(), EyecandyRenderer::new);
         LteLog.LOGGER.info("[lte] eyecandy renderer registered: {} ({} blocks, obj)", EyecandyRenderer.class.getSimpleName(), EyecandyBlocks.blocks().size());
      } catch (Throwable var1) {
         LteLog.LOGGER.error("[lte] could not register the eyecandy renderer", var1);
      }

   }
}
