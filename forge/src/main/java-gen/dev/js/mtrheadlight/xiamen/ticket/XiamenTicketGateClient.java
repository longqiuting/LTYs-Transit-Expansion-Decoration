package dev.js.mtrheadlight.xiamen.ticket;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.Renderers;

public final class XiamenTicketGateClient {
   private XiamenTicketGateClient() {
   }

   public static void register() {
      try {
         Renderers.ber(XiamenTicketGates.entityType(), XiamenTicketGateRenderer::new);
         LteLog.LOGGER.info("[lte] ticket gate renderer registered: {} ({} gates + {} side shells, rawmodels)", new Object[]{XiamenTicketGateRenderer.class.getSimpleName(), XiamenTicketGates.gates().size(), XiamenTicketGates.sides().size()});
      } catch (Throwable var1) {
         LteLog.LOGGER.error("[lte] could not register the ticket gate renderer", var1);
      }

   }
}
