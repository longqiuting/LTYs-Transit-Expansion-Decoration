package dev.js.mtrheadlight.xiamen.sign;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Layers;
import dev.js.mtrheadlight.forge.platform.Renderers;
import net.minecraft.client.renderer.RenderType;

public final class WallSignClient implements ClientInit {
   public void onInitializeClient() {
      try {
         Renderers.ber(WallSignObjects.entityType(), (context) -> new WallSignRenderer(context.getBlockEntityRenderDispatcher()));
         LteLog.LOGGER.info("[lte] wall sign renderer registered: {} (MTR's RenderStationNameTiled)", WallSignRenderer.class.getName());
      } catch (Throwable var5) {
         LteLog.LOGGER.error("[lte] could not register the wall sign renderer", var5);
      }

      for(WallSignObjects.Entry entry : WallSignObjects.entries()) {
         try {
            Layers.put(entry.block(), RenderType.cutout());
         } catch (Throwable var4) {
            LteLog.LOGGER.warn("[lte] could not set the cutout layer for the wall sign {}", entry.id(), var4);
         }
      }

   }
}
