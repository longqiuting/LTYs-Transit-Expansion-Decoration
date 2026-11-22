package dev.js.mtrheadlight.xiamen.drinks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.js.mtrheadlight.client.RenderDistance;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DrinksMachineRenderer implements BlockEntityRenderer<DrinksMachineBlockEntity> {
   public static final float MODEL_X_SCALE = 1.0F;

   public boolean rendersOutsideBoundingBox(DrinksMachineBlockEntity entity) {
      return true;
   }

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(DrinksMachineBlockEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public DrinksMachineRenderer(BlockEntityRendererProvider.Context context) {
   }

   public void render(DrinksMachineBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Level world = entity.getLevel();
      BlockState state = world != null ? world.getBlockState(entity.getBlockPos()) : entity.getBlockState();
      Block mesh = state.getBlock();
      if (mesh instanceof DrinksMachineBlock block) {
         RawMesh mesh = RawModelJson.get(RawModelJson.id(DrinksMachineBlock.rawModel(state)));
         if (mesh != null) {
            VertexConsumer consumer = vertexConsumers.getBuffer(RawModelRenderer.cutoutLayer(mesh));
            matrices.pushPose();
            matrices.translate(0.5D, 0.0D, 0.5D);
            matrices.mulPose(Axis.YP.rotationDegrees(-((Direction)state.getValue(HorizontalDirectionalBlock.FACING)).toYRot()));
            matrices.translate(-0.5D, 0.0D, -0.5D);
            float dx = block.modelDx();
            if (dx != 0.0F) {
               matrices.translate((double)dx / 16.0D, 0.0D, 0.0D);
            }

            RawModelRenderer.render(matrices, consumer, mesh, light, overlay);
            matrices.popPose();
         }
      }
   }
}
