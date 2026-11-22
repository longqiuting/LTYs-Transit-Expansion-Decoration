package dev.js.mtrheadlight.xiamen.drinks;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Renderers;

public final class DrinksMachineClient implements ClientInit {
   public void onInitializeClient() {
      try {
         Renderers.ber(DrinksMachineObjects.blockEntityType(), DrinksMachineRenderer::new);
         LteLog.LOGGER.info("[lte] drinks machine renderer registered: {} (rawmodels)", DrinksMachineRenderer.class.getSimpleName());
      } catch (Throwable var2) {
         LteLog.LOGGER.error("[lte] could not register the drinks machine renderer", var2);
      }

   }
}
