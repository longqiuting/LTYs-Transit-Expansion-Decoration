package dev.js.mtrheadlight.xiamen.fangsu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

final class FangsuPsdDraw {
   private FangsuPsdDraw() {
   }

   static boolean isUpperHalf(BlockState state) {
      return state != null && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
   }

   static Direction facingOf(BlockState state) {
      return state != null && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? (Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
   }

   static void draw(FangsuPsdDef def, float progress, Direction facing, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Map<String, List<RawMesh>> meshes = FangsuPsdBlocks.meshes(def);
      if (meshes != null && !meshes.isEmpty()) {
         matrices.pushPose();

         try {
            if (facing != Direction.NORTH) {
               matrices.translate(0.5F, 0.0F, 0.5F);
               matrices.mulPose(Axis.YN.rotationDegrees(degreesOf(facing)));
               matrices.translate(-0.5F, 0.0F, -0.5F);
            }

            if (def.staticGroup() != null) {
               drawGroup(matrices, vertexConsumers, (List)meshes.get(def.staticGroup()), light, overlay);
            }

            for(FangsuPsdDef.Motion motion : def.motions()) {
               matrices.pushPose();

               try {
                  if (motion.rotating()) {
                     float[] pivot = motion.pivot();
                     matrices.translate(pivot[0] / 16.0F, pivot[1] / 16.0F, pivot[2] / 16.0F);
                     matrices.mulPose(Axis.YP.rotation(motion.angle() * progress));
                     matrices.translate(-pivot[0] / 16.0F, -pivot[1] / 16.0F, -pivot[2] / 16.0F);
                  } else if (motion.step() != 0.0F) {
                     matrices.translate(motion.step() * progress, 0.0F, 0.0F);
                  }

                  drawGroup(matrices, vertexConsumers, (List)meshes.get(motion.group()), light, overlay);
               } finally {
                  matrices.popPose();
               }
            }
         } finally {
            matrices.popPose();
         }

      }
   }

   static void drawGroup(PoseStack matrices, MultiBufferSource vertexConsumers, List<RawMesh> meshes, int light, int overlay) {
      if (meshes != null) {
         for(RawMesh mesh : meshes) {
            RenderType layer = RawModelRenderer.cutoutLayer(mesh);
            VertexConsumer consumer = vertexConsumers.getBuffer(layer);
            RawModelRenderer.render(matrices, consumer, mesh, light, overlay);
         }

      }
   }

   private static float degreesOf(Direction facing) {
      float var10000;
      switch (facing) {
         case EAST:
            var10000 = 90.0F;
            break;
         case SOUTH:
            var10000 = 180.0F;
            break;
         case WEST:
            var10000 = 270.0F;
            break;
         default:
            var10000 = 0.0F;
      }

      return var10000;
   }
}
