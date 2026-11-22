package dev.js.mtrheadlight.xiamen.drinks;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public final class DrinksMachineClient implements ClientModInitializer {
   public void onInitializeClient() {
      try {
         BlockEntityRendererRegistry.register(DrinksMachineObjects.blockEntityType(), DrinksMachineRenderer::new);
         LteLog.LOGGER.info("[lte] drinks machine renderer registered: {} (rawmodels)", DrinksMachineRenderer.class.getSimpleName());
      } catch (Throwable var2) {
         LteLog.LOGGER.error("[lte] could not register the drinks machine renderer", var2);
      }

   }
}
