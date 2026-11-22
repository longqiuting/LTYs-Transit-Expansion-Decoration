package dev.js.mtrheadlight.xiamen.dep;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.js.mtrheadlight.client.RenderDistance;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DepPoleRenderer implements BlockEntityRenderer<DepPoleBlockEntity> {
   public boolean rendersOutsideBoundingBox(DepPoleBlockEntity entity) {
      return true;
   }

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(DepPoleBlockEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public DepPoleRenderer(BlockEntityRendererProvider.Context context) {
   }

   public void render(DepPoleBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Level world = entity.getLevel();
      BlockState state = world != null ? world.getBlockState(entity.getBlockPos()) : entity.getBlockState();
      if (state.getBlock() instanceof DepPoleBlock) {
         RawMesh mesh = RawModelJson.get(RawModelJson.id("dep_pole"));
         if (mesh != null) {
            RenderType layer = RawModelRenderer.cutoutLayer(mesh);
            VertexConsumer consumer = vertexConsumers.getBuffer(layer);
            Direction facing = state.hasProperty(DepPoleBlock.FACING) ? (Direction)state.getValue(DepPoleBlock.FACING) : Direction.NORTH;
            int degrees = DepPoleBlock.blockRotation(facing);
            if (degrees != 0) {
               matrices.pushPose();
               matrices.translate(0.5F, 0.0F, 0.5F);
               matrices.mulPose(Axis.YN.rotationDegrees((float)degrees));
               matrices.translate(-0.5F, 0.0F, -0.5F);
            }

            try {
               RawModelRenderer.render(matrices, consumer, mesh, light, overlay);
            } finally {
               if (degrees != 0) {
                  matrices.popPose();
               }

            }

         }
      }
   }
}
