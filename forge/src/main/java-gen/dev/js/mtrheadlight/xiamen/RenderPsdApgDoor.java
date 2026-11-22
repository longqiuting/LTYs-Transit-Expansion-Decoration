package dev.js.mtrheadlight.xiamen;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.js.mtrheadlight.client.RenderDistance;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.render.RenderPSDAPGDoor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class RenderPsdApgDoor extends RenderPSDAPGDoor<BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase> implements IXiamenTextureNamespace {
   private static final int TYPE = 2;

   public boolean rendersOutsideBoundingBox(BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase entity) {
      return true;
   }

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public RenderPsdApgDoor(BlockEntityRendererProvider.Context context) {
      super(context.getBlockEntityRenderDispatcher(), 2);
   }

   public String lte$textureNamespace() {
      return "mtrlte";
   }

   public void render(BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      String variant = XiamenObjects.textureVariantOf(entity.getBlockState().getBlock());
      XiamenTextures.withVariant(variant, XiamenDoorLights.colorFor(entity.getBlockState().getBlock(), entity.getBlockPos()), () -> this.drawInMtr(entity, tickDelta, matrices, vertexConsumers, light, overlay));
   }

   private void drawInMtr(BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);
   }
}
