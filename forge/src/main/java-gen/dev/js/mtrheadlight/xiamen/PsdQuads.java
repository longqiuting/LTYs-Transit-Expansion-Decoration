package dev.js.mtrheadlight.xiamen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class PsdQuads {
   private PsdQuads() {
   }

   private static void vertex(PoseStack.Pose entry, VertexConsumer vc, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light, int overlay) {
      vc.vertex(entry.pose(), x / 16.0F, y / 16.0F, z / 16.0F).color(255, 255, 255, 255).uv(u, v).overlayCoords(overlay).uv2(light).normal(entry.normal(), nx, ny, nz).endVertex();
   }

   public static void quadZ(PoseStack matrices, VertexConsumer vc, float x0, float x1, float y0, float y1, float z, PsdQuads.Uv uv, boolean positive, int light, int overlay) {
      PoseStack.Pose entry = matrices.last();
      float nz = positive ? 1.0F : -1.0F;
      if (positive) {
         vertex(entry, vc, x0, y0, z, uv.u0(), uv.v1(), 0.0F, 0.0F, nz, light, overlay);
         vertex(entry, vc, x1, y0, z, uv.u1(), uv.v1(), 0.0F, 0.0F, nz, light, overlay);
         vertex(entry, vc, x1, y1, z, uv.u1(), uv.v0(), 0.0F, 0.0F, nz, light, overlay);
         vertex(entry, vc, x0, y1, z, uv.u0(), uv.v0(), 0.0F, 0.0F, nz, light, overlay);
      } else {
         vertex(entry, vc, x1, y0, z, uv.u1(), uv.v1(), 0.0F, 0.0F, nz, light, overlay);
         vertex(entry, vc, x0, y0, z, uv.u0(), uv.v1(), 0.0F, 0.0F, nz, light, overlay);
         vertex(entry, vc, x0, y1, z, uv.u0(), uv.v0(), 0.0F, 0.0F, nz, light, overlay);
         vertex(entry, vc, x1, y1, z, uv.u1(), uv.v0(), 0.0F, 0.0F, nz, light, overlay);
      }

   }

   public static void quadX(PoseStack matrices, VertexConsumer vc, float x, float y0, float y1, float z0, float z1, PsdQuads.Uv uv, boolean positive, int light, int overlay) {
      PoseStack.Pose entry = matrices.last();
      float nx = positive ? 1.0F : -1.0F;
      if (positive) {
         vertex(entry, vc, x, y0, z0, uv.u0(), uv.v1(), nx, 0.0F, 0.0F, light, overlay);
         vertex(entry, vc, x, y0, z1, uv.u1(), uv.v1(), nx, 0.0F, 0.0F, light, overlay);
         vertex(entry, vc, x, y1, z1, uv.u1(), uv.v0(), nx, 0.0F, 0.0F, light, overlay);
         vertex(entry, vc, x, y1, z0, uv.u0(), uv.v0(), nx, 0.0F, 0.0F, light, overlay);
      } else {
         vertex(entry, vc, x, y0, z1, uv.u1(), uv.v1(), nx, 0.0F, 0.0F, light, overlay);
         vertex(entry, vc, x, y0, z0, uv.u0(), uv.v1(), nx, 0.0F, 0.0F, light, overlay);
         vertex(entry, vc, x, y1, z0, uv.u0(), uv.v0(), nx, 0.0F, 0.0F, light, overlay);
         vertex(entry, vc, x, y1, z1, uv.u1(), uv.v0(), nx, 0.0F, 0.0F, light, overlay);
      }

   }

   public static void quadY(PoseStack matrices, VertexConsumer vc, float y, float x0, float x1, float z0, float z1, PsdQuads.Uv uv, boolean positive, int light, int overlay) {
      PoseStack.Pose entry = matrices.last();
      float ny = positive ? 1.0F : -1.0F;
      if (positive) {
         vertex(entry, vc, x0, y, z1, uv.u0(), uv.v1(), 0.0F, ny, 0.0F, light, overlay);
         vertex(entry, vc, x1, y, z1, uv.u1(), uv.v1(), 0.0F, ny, 0.0F, light, overlay);
         vertex(entry, vc, x1, y, z0, uv.u1(), uv.v0(), 0.0F, ny, 0.0F, light, overlay);
         vertex(entry, vc, x0, y, z0, uv.u0(), uv.v0(), 0.0F, ny, 0.0F, light, overlay);
      } else {
         vertex(entry, vc, x0, y, z0, uv.u0(), uv.v1(), 0.0F, ny, 0.0F, light, overlay);
         vertex(entry, vc, x1, y, z0, uv.u1(), uv.v1(), 0.0F, ny, 0.0F, light, overlay);
         vertex(entry, vc, x1, y, z1, uv.u1(), uv.v0(), 0.0F, ny, 0.0F, light, overlay);
         vertex(entry, vc, x0, y, z1, uv.u0(), uv.v0(), 0.0F, ny, 0.0F, light, overlay);
      }

   }

   public static void plate(PoseStack matrices, VertexConsumer front, VertexConsumer back, float x0, float x1, float y0, float y1, float z0, float z1, PsdQuads.UvSet uv, int light, int overlay) {
      quadZ(matrices, front, x0, x1, y0, y1, z1, uv.front(), true, light, overlay);
      quadZ(matrices, back, x0, x1, y0, y1, z0, uv.back(), false, light, overlay);
      quadY(matrices, front, y1, x0, x1, z0, z1, uv.top(), true, light, overlay);
      quadY(matrices, front, y0, x0, x1, z0, z1, uv.bottom(), false, light, overlay);
      quadX(matrices, front, x0, y0, y1, z0, z1, uv.left(), false, light, overlay);
      quadX(matrices, front, x1, y0, y1, z0, z1, uv.right(), true, light, overlay);
   }

   public static void cube(PoseStack matrices, VertexConsumer vc, float x0, float x1, float y0, float y1, float z0, float z1, float u0, float u1, int light, int overlay) {
      PsdQuads.Uv full = new PsdQuads.Uv(u0, u1, 0.0F, 1.0F);
      quadZ(matrices, vc, x0, x1, y0, y1, z1, full, true, light, overlay);
      quadZ(matrices, vc, x0, x1, y0, y1, z0, full, false, light, overlay);
      quadX(matrices, vc, x0, y0, y1, z0, z1, full, false, light, overlay);
      quadX(matrices, vc, x1, y0, y1, z0, z1, full, true, light, overlay);
      quadY(matrices, vc, y0, x0, x1, z0, z1, full, false, light, overlay);
      quadY(matrices, vc, y1, x0, x1, z0, z1, full, true, light, overlay);
   }

   public static record Uv(float u0, float u1, float v0, float v1) {
   }

   public static record UvSet(PsdQuads.Uv front, PsdQuads.Uv back, PsdQuads.Uv right, PsdQuads.Uv left, PsdQuads.Uv top, PsdQuads.Uv bottom) {
      public static PsdQuads.UvSet box(float w, float h, float d) {
         float u2 = d + w;
         float u3 = d + w + d;
         float u4 = d + w + d + w;
         float ah = d + h;
         return new PsdQuads.UvSet(new PsdQuads.Uv(u3 / u4, u4 / u4, d / ah, (d + h) / ah), new PsdQuads.Uv(d / u4, u2 / u4, d / ah, (d + h) / ah), new PsdQuads.Uv(0.0F, d / u4, d / ah, (d + h) / ah), new PsdQuads.Uv(u2 / u4, u3 / u4, d / ah, (d + h) / ah), new PsdQuads.Uv(u3 / u4, u4 / u4, 0.0F, d / ah), new PsdQuads.Uv(d / u4, u2 / u4, 0.0F, d / ah));
      }
   }
}
