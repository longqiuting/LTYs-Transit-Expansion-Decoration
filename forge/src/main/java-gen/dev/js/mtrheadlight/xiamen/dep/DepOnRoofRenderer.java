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
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class DepOnRoofRenderer extends BlockEntityRendererMapper<DepOnRoofBlockEntity> {
   private static final double PLATE_X = 8.0D;
   private static final double PLATE_Y = 5.0D;
   private static final double PLATE_Z = 9.34D;
   static final float SCREEN_CENTER_X;
   static final float SCREEN_CENTER_Y;
   static final float SCREEN_CENTER_Z;
   static final Vec3 SCREEN_NORMAL;
   private static boolean warned;
   private static final ResourceLocation MODEL;
   static final float MODEL_YAW_FIX_DEGREES = 90.0F;

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(DepOnRoofBlockEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public DepOnRoofRenderer(BlockEntityRendererProvider.Context context) {
      super(context.getBlockEntityRenderDispatcher());
   }

   public void render(DepOnRoofBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      try {
         this.renderSafe(entity, matrices, vertexConsumers, light, overlay);
      } catch (Throwable var8) {
         if (!warned) {
            warned = true;
            LteLog.LOGGER.error("[lte] dep on roof render failed", var8);
         }
      }

   }

   private void renderSafe(DepOnRoofBlockEntity entity, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Level world = entity.getLevel();
      if (world != null) {
         BlockPos pos = entity.getBlockPos();
         BlockState state = world.getBlockState(pos);
         if (state.getBlock() instanceof DepOnRoofBlock) {
            Direction facing = (Direction)state.getOptionalValue(HorizontalDirectionalBlock.FACING).orElse(Direction.NORTH);
            drawModel(matrices, vertexConsumers, facing, light, overlay);
            DepCountdown.Display display = DepCountdown.display(entity, pos);
            if (display != null) {
               matrices.pushPose();

               try {
                  applyScreenTransform(matrices, facing);
                  matrices.pushPose();

                  try {
                     matrices.translate(0.0F, 0.0F, -1.5F);
                     DepOnWallRenderer.drawDigits(matrices, vertexConsumers, display);
                  } finally {
                     matrices.popPose();
                  }
               } finally {
                  matrices.popPose();
               }

            }
         }
      }
   }

   private static void drawModel(PoseStack matrices, MultiBufferSource vertexConsumers, Direction facing, int light, int overlay) {
      RawMesh mesh = RawModelJson.get(MODEL);
      if (mesh != null) {
         VertexConsumer consumer = vertexConsumers.getBuffer(RawModelRenderer.cutoutLayer(mesh));
         double pivotX = 0.5D;
         double pivotZ = 0.446875D;
         matrices.pushPose();

         try {
            matrices.translate(0.5D, 0.0D, 0.5D);
            matrices.mulPose(Axis.YN.rotationDegrees((float)DepOnRoofBlock.blockRotation(facing)));
            matrices.translate(-0.5D, 0.0D, -0.5D);
            matrices.translate(0.5D, 0.0D, 0.446875D);
            matrices.mulPose(Axis.YP.rotationDegrees(90.0F));
            matrices.translate(-0.5D, 0.0D, -0.446875D);
            RawModelRenderer.render(matrices, consumer, mesh, light, overlay);
         } finally {
            matrices.popPose();
         }

      }
   }

   private static void applyScreenTransform(PoseStack matrices, Direction facing) {
      matrices.translate(0.5F, 0.5F, 0.5F);
      matrices.mulPose(Axis.YN.rotationDegrees((float)DepOnRoofBlock.blockRotation(facing)));
      matrices.translate(SCREEN_CENTER_X - 0.5F, SCREEN_CENTER_Y - 0.5F, SCREEN_CENTER_Z - 0.5F);
      Matrix4f basis = screenBasis();
      PoseStack.Pose entry = matrices.last();
      entry.pose().mul(basis);
      entry.normal().mul(new Matrix3f(basis));
      float scale = 0.0078125F;
      matrices.scale(0.0078125F, 0.0078125F, 0.0078125F);
   }

   private static Matrix4f screenBasis() {
      Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
      Vec3 out = SCREEN_NORMAL;
      Vec3 right = up.cross(out);
      Vec3 down = up.scale(-1.0D);
      Vec3 into = out.scale(-1.0D);
      return new Matrix4f((float)right.x, (float)right.y, (float)right.z, 0.0F, (float)down.x, (float)down.y, (float)down.z, 0.0F, (float)into.x, (float)into.y, (float)into.z, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
   }

   public boolean rendersOutsideBoundingBox(DepOnRoofBlockEntity entity) {
      return true;
   }

   static {
      double rad = Math.toRadians(-45.0D);
      double cos = Math.cos(rad);
      double sin = Math.sin(rad);
      double dx = 0.0D;
      double dz = 2.1899999999999995D;
      SCREEN_CENTER_X = (float)((8.0D + 0.0D * cos - 2.1899999999999995D * sin) / 16.0D);
      SCREEN_CENTER_Y = 0.3125F;
      SCREEN_CENTER_Z = (float)((7.15D + 0.0D * sin + 2.1899999999999995D * cos) / 16.0D);
      SCREEN_NORMAL = new Vec3(-sin, 0.0D, cos);
      MODEL = RawModelJson.id("dep_on_roof");
   }
}
