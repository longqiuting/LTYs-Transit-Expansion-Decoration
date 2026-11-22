package dev.js.mtrheadlight.xiamen.machine;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Renderers;

public final class XiamenMachinesClient implements ClientInit {
   public void onInitializeClient() {
      try {
         Renderers.ber(XiamenMachines.balanceEntityType(), XiamenBalanceMachineRenderer::new);
         LteLog.LOGGER.info("[lte] balance machine renderer registered: {} (rawmodels)", XiamenBalanceMachineRenderer.class.getSimpleName());
      } catch (Throwable var2) {
         LteLog.LOGGER.error("[lte] could not register the balance machine renderer", var2);
      }

   }
}
