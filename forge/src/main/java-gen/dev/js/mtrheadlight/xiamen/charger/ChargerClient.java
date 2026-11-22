package dev.js.mtrheadlight.xiamen.charger;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Renderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public final class ChargerClient implements ClientInit {
   public void onInitializeClient() {
      try {
         Renderers.ber(ChargerObjects.blockEntityType(), ChargerRenderer::new);
         LteLog.LOGGER.info("[lte] charger renderer registered: {} (rawmodels)", ChargerRenderer.class.getSimpleName());
      } catch (Throwable var3) {
         LteLog.LOGGER.error("[lte] could not register the charger renderer", var3);
      }

      try {
         Renderers.entity(ChargerObjects.projectileType(), ThrownItemRenderer::new);
      } catch (Throwable var2) {
         LteLog.LOGGER.error("[lte] could not register the power bank projectile renderer", var2);
      }

   }
}
