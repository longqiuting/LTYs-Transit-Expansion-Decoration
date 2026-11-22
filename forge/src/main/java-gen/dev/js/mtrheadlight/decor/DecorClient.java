package dev.js.mtrheadlight.decor;

import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Layers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

public final class DecorClient implements ClientInit {
   public void onInitializeClient() {
      try {
         putCutout(DecorBlocks.marblePlatform());
         putCutout(DecorBlocks.marbleFloor());
      } catch (Throwable var5) {
      }

      try {
         for(Block block : RccPlatforms.all()) {
            putCutout(block);
         }
      } catch (Throwable var6) {
      }

   }

   private static void putCutout(Block block) {
      if (block != null) {
         Layers.put(block, RenderType.cutout());
      }

   }
}
