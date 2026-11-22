package dev.js.mtrheadlight.xiamen.fangsu;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.js.mtrheadlight.client.RenderDistance;
import dev.js.mtrheadlight.decor.DecorConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FangsuPsdRenderer implements BlockEntityRenderer<FangsuPsdDoorBlock.FangsuPsdDoorEntity> {
   public boolean rendersOutsideBoundingBox(FangsuPsdDoorBlock.FangsuPsdDoorEntity entity) {
      return true;
   }

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(FangsuPsdDoorBlock.FangsuPsdDoorEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public FangsuPsdRenderer(BlockEntityRendererProvider.Context context) {
   }

   public void render(FangsuPsdDoorBlock.FangsuPsdDoorEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Level world = entity.getLevel();
      BlockState state = world != null ? world.getBlockState(entity.getBlockPos()) : entity.getBlockState();
      Block progress = state.getBlock();
      if (progress instanceof FangsuPsdDoorBlock block) {
         if (!FangsuPsdDraw.isUpperHalf(state)) {
            FangsuPsdSoundsClient.play(entity.getBlockPos(), entity.tickDoorSound(world, entity.getBlockPos(), DecorConfig.get().fangsuDoorSound));
            float progressLted = entity.progress(world == null ? 0L : world.getGameTime(), tickDelta);
            FangsuPsdDraw.draw(block.def(), progressLted, FangsuPsdDraw.facingOf(state), matrices, vertexConsumers, light, overlay);
         }
      }
   }
}
