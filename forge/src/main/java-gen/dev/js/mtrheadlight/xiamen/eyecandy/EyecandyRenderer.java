package dev.js.mtrheadlight.xiamen.eyecandy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.js.mtrheadlight.client.RenderDistance;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EyecandyRenderer implements BlockEntityRenderer<EyecandyBlockEntity> {
   public boolean rendersOutsideBoundingBox(EyecandyBlockEntity entity) {
      return true;
   }

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(EyecandyBlockEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public EyecandyRenderer(BlockEntityRendererProvider.Context context) {
   }

   public void render(EyecandyBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Level world = entity.getLevel();
      BlockState state = world != null ? world.getBlockState(entity.getBlockPos()) : entity.getBlockState();
      Block entry = state.getBlock();
      if (entry instanceof EyecandyBlock block) {
         EyecandyEntry entryLted = block.entry();
         if (entryLted.bakedModel() == null) {
            boolean warningLight = block.hasWarningLight();
            RawMesh base = warningLight ? EyecandyBlocks.baseMesh(entryLted) : EyecandyBlocks.mesh(entryLted);
            if (base != null) {
               boolean lit = warningLight && entity.isLit();
               RawMesh lightPart = warningLight ? EyecandyBlocks.lightMesh(entryLted, lit) : null;
               RenderType layer = RawModelRenderer.cutoutLayer(base);
               VertexConsumer consumer = vertexConsumers.getBuffer(layer);
               Direction facing = state.hasProperty(EyecandyBlock.FACING) ? (Direction)state.getValue(EyecandyBlock.FACING) : Direction.NORTH;
               int degrees = EyecandyBlock.blockRotation(facing);
               if (degrees != 0) {
                  matrices.pushPose();
                  matrices.translate(0.5F, 0.0F, 0.5F);
                  matrices.mulPose(Axis.YN.rotationDegrees((float)degrees));
                  matrices.translate(-0.5F, 0.0F, -0.5F);
               }

               try {
                  RawModelRenderer.render(matrices, consumer, base, light, overlay);
                  if (lightPart != null) {
                     RawModelRenderer.render(matrices, consumer, lightPart, light, overlay, lit);
                  }
               } finally {
                  if (degrees != 0) {
                     matrices.popPose();
                  }

               }

            }
         }
      }
   }
}
