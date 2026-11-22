package dev.js.mtrheadlight.xiamen.dep;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.client.RenderDistance;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import mtr.mappings.BlockEntityRendererMapper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class DepOnWallRenderer extends BlockEntityRendererMapper<DepOnWallBlockEntity> {
   public static final ResourceLocation TEXTURE = new ResourceLocation("mtrlte", "textures/block/dep_on_wall.png");
   public static final ResourceLocation LED_TEXTURE = new ResourceLocation("mtrlte", "textures/block/dep_on_wall_led.png");
   private static boolean warned;
   private static final ResourceLocation MODEL = RawModelJson.id("dep_on_wall");
   private static final Vec3 MODEL_SCREEN_NORMAL = new Vec3(0.0D, 0.0D, -1.0D);

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(DepOnWallBlockEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public DepOnWallRenderer(BlockEntityRendererProvider.Context context) {
      super(context.getBlockEntityRenderDispatcher());
   }

   public void render(DepOnWallBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      try {
         this.renderSafe(entity, matrices, vertexConsumers, light, overlay);
      } catch (Throwable var8) {
         if (!warned) {
            warned = true;
            LteLog.LOGGER.error("[lte] dep on wall render failed", var8);
         }
      }

   }

   private void renderSafe(DepOnWallBlockEntity entity, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Level world = entity.getLevel();
      if (world != null) {
         BlockPos pos = entity.getBlockPos();
         BlockState state = world.getBlockState(pos);
         if (state.getBlock() instanceof DepOnWallBlock) {
            Direction facing = (Direction)state.getOptionalValue(HorizontalDirectionalBlock.FACING).orElse(Direction.NORTH);
            drawModel(matrices, vertexConsumers, facing, light, overlay);
            DepCountdown.Display display = DepCountdown.display(entity, pos);
            matrices.pushPose();

            try {
               applyScreenTransform(matrices, facing);
               if (display != null) {
                  matrices.pushPose();

                  try {
                     matrices.translate(0.0F, 0.0F, -1.5F);
                     DepOnWallOrient.mode().apply(matrices);
                     drawDigits(matrices, vertexConsumers, display);
                  } finally {
                     matrices.popPose();
                  }
               }
            } finally {
               matrices.popPose();
            }

         }
      }
   }

   private static void drawModel(PoseStack matrices, MultiBufferSource vertexConsumers, Direction facing, int light, int overlay) {
      RawMesh mesh = RawModelJson.get(MODEL);
      if (mesh != null) {
         VertexConsumer consumer = vertexConsumers.getBuffer(RawModelRenderer.cutoutLayer(mesh));
         matrices.pushPose();

         try {
            matrices.translate(0.5D, 0.0D, 0.5D);
            matrices.mulPose(Axis.YN.rotationDegrees((float)DepOnWallBlock.blockRotation(facing)));
            matrices.translate(-0.5D, 0.0D, -0.5D);
            RawModelRenderer.render(matrices, consumer, mesh, light, overlay);
         } finally {
            matrices.popPose();
         }

      }
   }

   private static void applyScreenTransform(PoseStack matrices, Direction facing) {
      matrices.translate(0.5F, 0.5F, 0.5F);
      matrices.mulPose(Axis.YN.rotationDegrees((float)DepOnWallBlock.blockRotation(facing)));
      matrices.translate(0.0F, 0.0F, 0.2475F);
      Matrix4f basis = screenBasis();
      PoseStack.Pose entry = matrices.last();
      entry.pose().mul(basis);
      entry.normal().mul(new Matrix3f(basis));
      float scale = 0.0078125F;
      matrices.scale(0.0078125F, 0.0078125F, 0.0078125F);
   }

   private static Matrix4f screenBasis() {
      Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
      Vec3 out = MODEL_SCREEN_NORMAL.scale(-1.0D);
      Vec3 right = up.cross(out);
      Vec3 down = up.scale(-1.0D);
      Vec3 into = out.scale(-1.0D);
      return new Matrix4f((float)right.x, (float)right.y, (float)right.z, 0.0F, (float)down.x, (float)down.y, (float)down.z, 0.0F, (float)into.x, (float)into.y, (float)into.z, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
   }

   private static void drawScreen(PoseStack matrices, MultiBufferSource vertexConsumers) {
      VertexConsumer buffer = vertexConsumers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
      PoseStack.Pose entry = matrices.last();
      Matrix4f position = entry.pose();
      Matrix3f normal = entry.normal();
      float halfWidth = 48.0F;
      float halfHeight = 32.0F;
      vertex(buffer, position, normal, -48.0F, -32.0F, 0.1875F, 0.0F);
      vertex(buffer, position, normal, -48.0F, 32.0F, 0.1875F, 0.125F);
      vertex(buffer, position, normal, 48.0F, 32.0F, 0.375F, 0.125F);
      vertex(buffer, position, normal, 48.0F, -32.0F, 0.375F, 0.0F);
   }

   private static void vertex(VertexConsumer buffer, Matrix4f position, Matrix3f normal, float x, float y, float u, float v) {
      buffer.vertex(position, x, y, 0.0F).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
   }

   static void drawDigits(PoseStack matrices, MultiBufferSource vertexConsumers, DepCountdown.Display display) {
      float unitWidth = DepOnWallDigits.width(display.text(), 1.0F);
      if (!(unitWidth <= 0.0F)) {
         float height = Math.min(54.0F, 88.0F / unitWidth);
         float width = unitWidth * height;
         float left = -width * 0.5F;
         float top = -height * 0.5F;
         VertexConsumer buffer = vertexConsumers.getBuffer(RenderType.entityTranslucentEmissive(LED_TEXTURE));
         PoseStack.Pose entry = matrices.last();
         DepOnWallDigits.draw(buffer, entry.pose(), entry.normal(), display.text(), left, top, height, display.overdue() ? -65536 : -16718218);
      }
   }

   public boolean rendersOutsideBoundingBox(DepOnWallBlockEntity entity) {
      return true;
   }
}
