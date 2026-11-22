package dev.js.mtrheadlight.xiamen.fangsu;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Renderers;

public final class FangsuPsdClient implements ClientInit {
   public void onInitializeClient() {
      if (FangsuPsdBlocks.entityType() != null) {
         try {
            Renderers.ber(FangsuPsdBlocks.entityType(), (context) -> new FangsuPsdRenderer(context));
            LteLog.LOGGER.info("[lte] fangsu psd door renderer registered: {}", FangsuPsdRenderer.class.getName());
         } catch (Throwable var4) {
            LteLog.LOGGER.error("[lte] could not register the fangsu psd door renderer", var4);
         }
      } else {
         LteLog.LOGGER.warn("[lte] fangsu psd sliding doors were not registered, no renderer");
      }

      if (FangsuPsdBlocks.endDoorEntityType() != null) {
         try {
            Renderers.ber(FangsuPsdBlocks.endDoorEntityType(), (context) -> new FangsuPsdEndDoorRenderer(context));
            LteLog.LOGGER.info("[lte] fangsu psd end door renderer registered: {}", FangsuPsdEndDoorRenderer.class.getName());
         } catch (Throwable var3) {
            LteLog.LOGGER.error("[lte] could not register the fangsu psd end door renderer", var3);
         }
      } else {
         LteLog.LOGGER.warn("[lte] fangsu psd end doors were not registered, no renderer");
      }

      if (FangsuPsdBlocks.glassEntityType() != null) {
         try {
            Renderers.ber(FangsuPsdBlocks.glassEntityType(), (context) -> new FangsuPsdGlassRenderer(context));
            LteLog.LOGGER.info("[lte] fangsu psd glass renderer registered: {}", FangsuPsdGlassRenderer.class.getName());
         } catch (Throwable var2) {
            LteLog.LOGGER.error("[lte] could not register the fangsu psd glass renderer", var2);
         }
      } else {
         LteLog.LOGGER.warn("[lte] fangsu psd glass panels were not registered, no renderer");
      }

   }
}
