package dev.js.mtrheadlight.xiamen.machine;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public final class XiamenMachinesClient implements ClientModInitializer {
   public void onInitializeClient() {
      try {
         BlockEntityRendererRegistry.register(XiamenMachines.balanceEntityType(), XiamenBalanceMachineRenderer::new);
         LteLog.LOGGER.info("[lte] balance machine renderer registered: {} (rawmodels)", XiamenBalanceMachineRenderer.class.getSimpleName());
      } catch (Throwable var2) {
         LteLog.LOGGER.error("[lte] could not register the balance machine renderer", var2);
      }

   }
}
