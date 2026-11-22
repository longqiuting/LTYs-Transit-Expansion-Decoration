package dev.js.mtrheadlight.xiamen.fangsu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.js.mtrheadlight.client.RenderDistance;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FangsuPsdGlassRenderer implements BlockEntityRenderer<FangsuPsdGlassBlock.FangsuPsdGlassEntity> {
   public boolean rendersOutsideBoundingBox(FangsuPsdGlassBlock.FangsuPsdGlassEntity entity) {
      return true;
   }

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(FangsuPsdGlassBlock.FangsuPsdGlassEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public FangsuPsdGlassRenderer(BlockEntityRendererProvider.Context context) {
   }

   public void render(FangsuPsdGlassBlock.FangsuPsdGlassEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Level world = entity.getLevel();
      BlockState state = world != null ? world.getBlockState(entity.getBlockPos()) : entity.getBlockState();
      Block def = state.getBlock();
      if (def instanceof FangsuPsdGlassBlock block) {
         FangsuPsdGlassDef defLted = block.def();
         Map<String, List<RawMesh>> meshes = FangsuPsdBlocks.meshes(defLted);
         List left = (List)meshes.get(defLted.leftForm(block.leftFormOf(state)).group());
         List right = (List)meshes.get(defLted.rightForm(block.rightFormOf(state)).group());
         if (left != null || right != null) {
            Direction facing = FangsuPsdDraw.facingOf(state);
            matrices.pushPose();

            try {
               if (facing != Direction.NORTH) {
                  matrices.translate(0.5F, 0.0F, 0.5F);
                  matrices.mulPose(Axis.YN.rotationDegrees(degreesOf(facing)));
                  matrices.translate(-0.5F, 0.0F, -0.5F);
               }

               FangsuPsdDraw.drawGroup(matrices, vertexConsumers, left, light, overlay);
               FangsuPsdDraw.drawGroup(matrices, vertexConsumers, right, light, overlay);
            } finally {
               matrices.popPose();
            }

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
