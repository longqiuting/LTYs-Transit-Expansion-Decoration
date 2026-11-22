package dev.js.mtrheadlight.xiamen.ticket;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public final class XiamenTicketGateClient {
   private XiamenTicketGateClient() {
   }

   public static void register() {
      try {
         BlockEntityRendererRegistry.register(XiamenTicketGates.entityType(), XiamenTicketGateRenderer::new);
         LteLog.LOGGER.info("[lte] ticket gate renderer registered: {} ({} gates + {} side shells, rawmodels)", new Object[]{XiamenTicketGateRenderer.class.getSimpleName(), XiamenTicketGates.gates().size(), XiamenTicketGates.sides().size()});
      } catch (Throwable var1) {
         LteLog.LOGGER.error("[lte] could not register the ticket gate renderer", var1);
      }

   }
}
