package dev.js.mtrheadlight.xiamen.fangsu;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.js.mtrheadlight.client.RenderDistance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FangsuPsdEndDoorRenderer implements BlockEntityRenderer<FangsuPsdEndDoorBlock.FangsuPsdEndDoorEntity> {
   public boolean rendersOutsideBoundingBox(FangsuPsdEndDoorBlock.FangsuPsdEndDoorEntity entity) {
      return true;
   }

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(FangsuPsdEndDoorBlock.FangsuPsdEndDoorEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public FangsuPsdEndDoorRenderer(BlockEntityRendererProvider.Context context) {
   }

   public void render(FangsuPsdEndDoorBlock.FangsuPsdEndDoorEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Level world = entity.getLevel();
      BlockState state = world != null ? world.getBlockState(entity.getBlockPos()) : entity.getBlockState();
      Block progress = state.getBlock();
      if (progress instanceof FangsuPsdEndDoorBlock block) {
         if (!FangsuPsdDraw.isUpperHalf(state)) {
            float progressLted = entity.progress(state, world == null ? 0L : world.getGameTime(), tickDelta);
            FangsuPsdDraw.draw(block.def(), progressLted, FangsuPsdDraw.facingOf(state), matrices, vertexConsumers, light, overlay);
         }
      }
   }
}
