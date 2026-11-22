package dev.js.mtrheadlight.xiamen.charger;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public final class ChargerClient implements ClientModInitializer {
   public void onInitializeClient() {
      try {
         BlockEntityRendererRegistry.register(ChargerObjects.blockEntityType(), ChargerRenderer::new);
         LteLog.LOGGER.info("[lte] charger renderer registered: {} (rawmodels)", ChargerRenderer.class.getSimpleName());
      } catch (Throwable var3) {
         LteLog.LOGGER.error("[lte] could not register the charger renderer", var3);
      }

      try {
         EntityRendererRegistry.register(ChargerObjects.projectileType(), ThrownItemRenderer::new);
      } catch (Throwable var2) {
         LteLog.LOGGER.error("[lte] could not register the power bank projectile renderer", var2);
      }

   }
}
