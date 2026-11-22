package dev.js.mtrheadlight.railing;

import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Layers;
import net.minecraft.client.renderer.RenderType;

public final class RailingGlassClient implements ClientInit {
   public void onInitializeClient() {
      try {
         for(RailingWallBlock block : RailingGlassBlocks.blocks()) {
            Layers.put(block, RenderType.translucent());
         }
      } catch (Throwable var3) {
      }

   }
}
