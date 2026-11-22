package dev.js.mtrheadlight.clock;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class ClockRenderer implements BlockEntityRenderer<ClockBlockEntity> {
   public static final int RENDER_DISTANCE = 384;
   private static final ResourceLocation SOLID_TEXTURE = new ResourceLocation("mtrlte", "textures/block/white.png");
   private static final RenderType LIT_LAYER = RenderType.entityCutoutNoCull(SOLID_TEXTURE);
   private static final double BACK_PLANE = -0.484D;
   private static final double DISC_DEPTH = -0.484D;
   private static final double TICK_DEPTH = -0.482D;
   private static final double HOUR_DEPTH = -0.476D;
   private static final double MINUTE_DEPTH = -0.473D;
   private static final double SECOND_DEPTH = -0.47D;
   private static final double HUB_DEPTH = -0.46399999999999997D;
   private static final double TWO_PI = (Math.PI * 2D);
   private final Font textRenderer;
   private static final String[] ROMAN = new String[]{"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};

   public ClockRenderer(BlockEntityRendererProvider.Context context) {
      this.textRenderer = context.getFont();
   }

   public boolean rendersOutsideBoundingBox(ClockBlockEntity entity) {
      return true;
   }

   public int getViewDistance() {
      return 384;
   }

   public void render(ClockBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      Level world = entity.getLevel();
      BlockState state = world != null ? world.getBlockState(entity.getBlockPos()) : entity.getBlockState();
      if (state.getBlock() instanceof ClockBlock) {
         ClockConfig cfg = entity.effectiveConfig();
         Direction facing = (Direction)state.getValue(ClockBlock.FACING);
         double seconds = entity.displaySeconds(tickDelta);
         double faceRadius = (double)cfg.faceRadius;
         double tickRadius = (double)cfg.tickRadius;
         int segments = segmentsFor(Math.max(faceRadius, tickRadius));
         ClockRenderer.Frame frame = new ClockRenderer.Frame(facing);
         int lightCoord = lightFor(world, entity.getBlockPos(), cfg, light);
         frame.light = lightCoord;
         frame.normals = matrices.last().normal();
         int face = opaque(cfg.faceColor);
         int tick = opaque(cfg.tickColor);
         int hourHand = opaque(cfg.hourColor);
         int minuteHand = opaque(cfg.minuteColor);
         int secondHand = opaque(cfg.secondColor);
         int hub = opaque(cfg.hubColor);
         VertexConsumer vc = vertexConsumers.getBuffer(LIT_LAYER);
         Matrix4f matrix = matrices.last().pose();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

         try {
            if (cfg.showFace) {
               frame.depth = -0.484D;
               this.disc(vc, matrix, frame, faceRadius, face, segments);
            }

            if (cfg.showTicks) {
               frame.depth = -0.482D;
               this.ticks(vc, matrix, frame, cfg, tick, faceRadius, tickRadius, matrices, vertexConsumers);
            }

            if (cfg.showHour) {
               frame.depth = -0.476D;
               this.hand(vc, matrix, frame, ClockTime.hourFraction(seconds), (double)cfg.hourLength, Math.max(0.012D, faceRadius * 0.075D), hourHand);
            }

            if (cfg.showMinute) {
               frame.depth = -0.473D;
               this.hand(vc, matrix, frame, ClockTime.minuteFraction(seconds), (double)cfg.minuteLength, Math.max(0.01D, faceRadius * 0.055D), minuteHand);
            }

            if (cfg.showSecond) {
               frame.depth = -0.47D;
               this.hand(vc, matrix, frame, ClockTime.secondFraction(seconds), (double)cfg.secondLength, Math.max(0.006D, faceRadius * 0.028D), secondHand);
            }

            if (cfg.showHub) {
               frame.depth = -0.46399999999999997D;
               this.disc(vc, matrix, frame, Math.min(faceRadius, faceRadius * 0.04D + 0.03D), hub, Math.max(16, segments / 3));
            }
         } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

      }
   }

   private void disc(VertexConsumer vc, Matrix4f matrix, ClockRenderer.Frame frame, double radius, int color, int segments) {
      if (!(radius <= 0.0D)) {
         for(int i = 0; i < segments; ++i) {
            double t0 = (Math.PI * 2D) * (double)i / (double)segments;
            double t1 = (Math.PI * 2D) * (double)(i + 1) / (double)segments;
            double tm = (t0 + t1) * 0.5D;
            frame.vertex(vc, matrix, 0.0D, 0.0D, color);
            frame.vertex(vc, matrix, Math.sin(t0) * radius, Math.cos(t0) * radius, color);
            frame.vertex(vc, matrix, Math.sin(tm) * radius, Math.cos(tm) * radius, color);
            frame.vertex(vc, matrix, Math.sin(t1) * radius, Math.cos(t1) * radius, color);
         }

      }
   }

   private void hand(VertexConsumer vc, Matrix4f matrix, ClockRenderer.Frame frame, double fraction, double length, double width, int color) {
      double theta = fraction * (Math.PI * 2D);
      double dx = Math.sin(theta);
      double dy = Math.cos(theta);
      double px = Math.cos(theta);
      double py = -Math.sin(theta);
      double baseHalf = width * 0.5D;
      double tipHalf = width * 0.16D;
      double tail = length * 0.1D;
      frame.vertex(vc, matrix, -dx * tail + px * baseHalf, -dy * tail + py * baseHalf, color);
      frame.vertex(vc, matrix, dx * length + px * tipHalf, dy * length + py * tipHalf, color);
      frame.vertex(vc, matrix, dx * length - px * tipHalf, dy * length - py * tipHalf, color);
      frame.vertex(vc, matrix, -dx * tail - px * baseHalf, -dy * tail - py * baseHalf, color);
   }

   private void ticks(VertexConsumer vc, Matrix4f matrix, ClockRenderer.Frame frame, ClockConfig cfg, int color, double faceRadius, double tickRadius, PoseStack matrices, MultiBufferSource vertexConsumers) {
      if (cfg.tickStyle != ClockConfig.TickStyle.NONE) {
         double radius = Math.max(0.05D, faceRadius);
         double ring = tickRadius;

         for(int i = 0; i < 12; ++i) {
            double theta = (Math.PI * 2D) * (double)i / 12.0D;
            double dx = Math.sin(theta);
            double dy = Math.cos(theta);
            double px = Math.cos(theta);
            double py = -Math.sin(theta);
            double weight = i == 0 ? 1.35D : 1.0D;
            switch (cfg.tickStyle) {
               case BAR: {

                  double inner = ring - radius * 0.085D * weight;
                  double outer = ring + radius * 0.045D * weight;
                  double half = Math.max(0.004D, radius * 0.014D * weight);
                  frame.vertex(vc, matrix, dx * inner + px * half, dy * inner + py * half, color);
                  frame.vertex(vc, matrix, dx * outer + px * half, dy * outer + py * half, color);
                  frame.vertex(vc, matrix, dx * outer - px * half, dy * outer - py * half, color);
                  frame.vertex(vc, matrix, dx * inner - px * half, dy * inner - py * half, color);
                  break;
               }
               case DOT: {

                  double r = Math.max(0.006D, radius * 0.026D * weight);
                  double cx = dx * ring;
                  double cy = dy * ring;
                  frame.vertex(vc, matrix, cx, cy, color);
                  frame.vertex(vc, matrix, cx + px * r, cy + py * r, color);
                  frame.vertex(vc, matrix, cx + dx * r, cy + dy * r, color);
                  frame.vertex(vc, matrix, cx - px * r, cy - py * r, color);
                  break;
               }
               case SQUARE: {

                  double h = Math.max(0.006D, radius * 0.028D * weight);
                  double cx = dx * ring;
                  double cy = dy * ring;
                  frame.vertex(vc, matrix, cx + px * h + dx * h, cy + py * h + dy * h, color);
                  frame.vertex(vc, matrix, cx - px * h + dx * h, cy - py * h + dy * h, color);
                  frame.vertex(vc, matrix, cx - px * h - dx * h, cy - py * h - dy * h, color);
                  frame.vertex(vc, matrix, cx + px * h - dx * h, cy + py * h - dy * h, color);
                  break;
               }
               case TRIANGLE: {

                  double apex = ring + radius * 0.05D * weight;
                  double base = ring - radius * 0.045D * weight;
                  double half = Math.max(0.006D, radius * 0.026D * weight);
                  frame.vertex(vc, matrix, dx * apex, dy * apex, color);
                  frame.vertex(vc, matrix, dx * base + px * half, dy * base + py * half, color);
                  frame.vertex(vc, matrix, dx * base - px * half, dy * base - py * half, color);
                  frame.vertex(vc, matrix, dx * apex, dy * apex, color);
                  break;
               }
               case ARABIC: {

               }
               case ROMAN: {

                  String label = cfg.tickStyle == ClockConfig.TickStyle.ARABIC ? Integer.toString(i == 0 ? 12 : i) : ROMAN[i == 0 ? 11 : i - 1];
                  this.numeral(matrices, vertexConsumers, frame, dx * ring, dy * ring, radius, label, color);
               }
            }
         }

      }
   }

   private void numeral(PoseStack matrices, MultiBufferSource vertexConsumers, ClockRenderer.Frame frame, double a, double b, double radius, String label, int color) {
      float scale = (float)(radius * 0.0145D);
      if (!(scale <= 0.0F)) {
         Vec3 at = frame.point(a, b);
         Matrix4f rot = new Matrix4f((float)frame.right.x, (float)frame.right.y, (float)frame.right.z, 0.0F, (float)(-frame.up.x), (float)(-frame.up.y), (float)(-frame.up.z), 0.0F, (float)frame.normal.x, (float)frame.normal.y, (float)frame.normal.z, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
         Matrix4f local = (new Matrix4f()).translation((float)at.x, (float)at.y, (float)at.z).mul(rot).scale(scale);
         Matrix4f finalMatrix = (new Matrix4f(matrices.last().pose())).mul(local);
         int width = this.textRenderer.width(label);
         this.textRenderer.drawInBatch(label, (float)(-width) / 2.0F, -4.0F, color | -16777216, false, finalMatrix, vertexConsumers, DisplayMode.NORMAL, 0, frame.light);
      }
   }

   public static int segmentsFor(double radius) {
      double t = Mth.clamp((radius - 4.0D) / 12.0D, 0.0D, 1.0D);
      return (int)Math.round(48.0D + 144.0D * t);
   }

   public static int lightFor(Level world, BlockPos pos, ClockConfig cfg, int fallbackLight) {
      if (world == null) {
         return fallbackLight;
      } else {
         boolean daylight = isDaylight(world);
         return cfg.glow != ClockConfig.GlowMode.ALWAYS && (cfg.glow != ClockConfig.GlowMode.DAY || !daylight) && (cfg.glow != ClockConfig.GlowMode.NIGHT || daylight) ? LevelRenderer.getLightColor(world, pos) : 15728880;
      }
   }

   public static boolean isDaylight(Level world) {
      if (world == null) {
         return true;
      } else if (world.dimensionType().hasFixedTime()) {
         return true;
      } else {
         return world.getSkyDarken() < 4;
      }
   }

   public static int opaque(int rgb) {
      return -16777216 | rgb & 16777215;
   }

   private static final class Frame {
      private final Vec3 normal;
      private final Vec3 right;
      private final Vec3 up;
      private double depth;
      private int light;
      private Matrix3f normals;

      Frame(Direction facing) {
         this.normal = Vec3.atLowerCornerOf(facing.getNormal());
         Vec3 worldUp = facing.getAxis().isHorizontal() ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(0.0D, 0.0D, 1.0D);
         this.right = worldUp.cross(this.normal).normalize();
         this.up = this.normal.cross(this.right).normalize();
      }

      Vec3 point(double a, double b) {
         return new Vec3(0.5D + this.right.x * a + this.up.x * b + this.normal.x * this.depth, 0.5D + this.right.y * a + this.up.y * b + this.normal.y * this.depth, 0.5D + this.right.z * a + this.up.z * b + this.normal.z * this.depth);
      }

      void vertex(VertexConsumer vc, Matrix4f matrix, double a, double b, int argb) {
         float x = (float)(0.5D + this.right.x * a + this.up.x * b + this.normal.x * this.depth);
         float y = (float)(0.5D + this.right.y * a + this.up.y * b + this.normal.y * this.depth);
         float z = (float)(0.5D + this.right.z * a + this.up.z * b + this.normal.z * this.depth);
         vc.vertex(matrix, x, y, z).color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24 & 255).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(this.light).normal(this.normals, (float)this.normal.x, (float)this.normal.y, (float)this.normal.z).endVertex();
      }
   }
}
