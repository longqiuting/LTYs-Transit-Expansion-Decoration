package dev.js.mtrheadlight.xiamen.ticket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.js.mtrheadlight.client.RenderDistance;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import mtr.block.BlockTicketBarrier;
import mtr.data.TicketSystem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public class XiamenTicketGateRenderer implements BlockEntityRenderer<XiamenTicketGateBlockEntity> {
   public boolean rendersOutsideBoundingBox(XiamenTicketGateBlockEntity entity) {
      return true;
   }

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(XiamenTicketGateBlockEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public XiamenTicketGateRenderer(BlockEntityRendererProvider.Context context) {
   }

   public void render(XiamenTicketGateBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Level world = entity.getLevel();
      BlockState state = world != null ? world.getBlockState(entity.getBlockPos()) : entity.getBlockState();
      Block consumer = state.getBlock();
      String modelName;
      boolean side;
      if (consumer instanceof XiamenTicketGate gate) {
         boolean open = state.hasProperty(BlockTicketBarrier.OPEN) && ((TicketSystem.EnumTicketBarrierOpen)state.getValue(BlockTicketBarrier.OPEN)).isOpen();
         modelName = gate.rawModel() + (open ? "_open" : "_closed");
         side = false;
      } else {
         consumer = state.getBlock();
         if (!(consumer instanceof XiamenTicketGateSide)) {
            return;
         }

         XiamenTicketGateSide sideBlock = (XiamenTicketGateSide)consumer;
         modelName = sideBlock.rawModel();
         side = true;
      }

      RawMesh mesh = RawModelJson.get(RawModelJson.id(modelName));
      if (mesh != null) {
         VertexConsumer consumerLted = vertexConsumers.getBuffer(RawModelRenderer.cutoutLayer(mesh));
         matrices.pushPose();
         matrices.translate(0.5D, 0.0D, 0.5D);
         matrices.mulPose(Axis.YP.rotationDegrees((float)(-rotationOf(state))));
         matrices.translate(-0.5D, 0.0D, -0.5D);
         if (side) {
            matrices.translate(1.0D, 0.0D, 0.0D);
         }

         RawModelRenderer.render(matrices, consumerLted, mesh, light, overlay);
         matrices.popPose();
      }
   }

   private static int rotationOf(BlockState state) {
      Property<Direction> facing = HorizontalDirectionalBlock.FACING;
      return !state.hasProperty(facing) ? 0 : (int)((Direction)state.getValue(facing)).getOpposite().toYRot();
   }
}
