package dev.js.mtrheadlight.xiamen.sign;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.render.RenderStationNameTiled;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class WallSignRenderer extends RenderStationNameTiled<WallSignBlockEntity> {
   public WallSignRenderer(BlockEntityRenderDispatcher dispatcher) {
      super(dispatcher, true);
   }

   public void render(WallSignBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      WallSignLogos.push(brandOf(entity));

      try {
         super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);
      } finally {
         WallSignLogos.pop();
      }

   }

   private static String brandOf(WallSignBlockEntity entity) {
      if (entity == null) {
         return null;
      } else {
         try {
            BlockState state = entity.getBlockState();
            if (state != null) {
               Block var3 = state.getBlock();
               if (var3 instanceof WallSignBlock) {
                  WallSignBlock sign = (WallSignBlock)var3;
                  return sign.logo();
               }
            }

            return null;
         } catch (Throwable var4) {
            return null;
         }
      }
   }
}
