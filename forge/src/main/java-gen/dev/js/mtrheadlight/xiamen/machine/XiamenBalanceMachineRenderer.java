package dev.js.mtrheadlight.xiamen.machine;

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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

public class XiamenBalanceMachineRenderer implements BlockEntityRenderer<XiamenBalanceMachineBlockEntity> {
   private static final String MODEL_BASE = "balance_machine_";

   public boolean rendersOutsideBoundingBox(XiamenBalanceMachineBlockEntity entity) {
      return true;
   }

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(XiamenBalanceMachineBlockEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public XiamenBalanceMachineRenderer(BlockEntityRendererProvider.Context context) {
   }

   public void render(XiamenBalanceMachineBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Level world = entity.getLevel();
      BlockState state = world != null ? world.getBlockState(entity.getBlockPos()) : entity.getBlockState();
      if (state.getBlock() instanceof XiamenBalanceMachineBlock) {
         boolean upper = state.hasProperty(XiamenMachineBlock.HALF) && state.getValue(XiamenMachineBlock.HALF) == DoubleBlockHalf.UPPER;
         RawMesh mesh = RawModelJson.get(RawModelJson.id("balance_machine_" + (upper ? "top" : "bottom")));
         if (mesh != null) {
            VertexConsumer consumer = vertexConsumers.getBuffer(RawModelRenderer.cutoutLayer(mesh));
            matrices.pushPose();
            matrices.translate(0.5D, 0.0D, 0.5D);
            matrices.mulPose(Axis.YP.rotationDegrees(-((Direction)state.getValue(HorizontalDirectionalBlock.FACING)).toYRot()));
            matrices.translate(-0.5D, 0.0D, -0.5D);
            RawModelRenderer.render(matrices, consumer, mesh, light, overlay);
            matrices.popPose();
         }
      }
   }
}
