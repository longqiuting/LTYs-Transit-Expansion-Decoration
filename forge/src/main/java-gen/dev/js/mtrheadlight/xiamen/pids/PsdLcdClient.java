package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Layers;
import dev.js.mtrheadlight.forge.platform.Renderers;
import net.minecraft.client.renderer.RenderType;

public final class PsdLcdClient implements ClientInit {
   public void onInitializeClient() {
      try {
         Renderers.ber(PsdLcdObjects.entityType(), PsdLcdRenderer::new);
         LteLog.LOGGER.info("[lte] psd lcd renderer registered: {}", PsdLcdRenderer.class.getName());
      } catch (Throwable var3) {
         LteLog.LOGGER.error("[lte] could not register the psd lcd renderer", var3);
      }

      try {
         Layers.put(PsdLcdObjects.block(), RenderType.cutout());
      } catch (Throwable var2) {
         LteLog.LOGGER.warn("[lte] could not set the cutout layer for the psd lcd block", var2);
      }

   }
}
