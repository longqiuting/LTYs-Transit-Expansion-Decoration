package dev.js.mtrheadlight.xiamen.dep;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

final class DepOnWallDigits {
   private static final int A = 1;
   private static final int B = 2;
   private static final int C = 4;
   private static final int D = 8;
   private static final int E = 16;
   private static final int F = 32;
   private static final int G = 64;
   private static final int[] GLYPHS = new int[]{63, 6, 91, 79, 102, 109, 125, 7, 127, 111};

   private DepOnWallDigits() {
   }

   static float width(String text, float height) {
      if (text != null && !text.isEmpty()) {
         float digitWidth = height * 0.5F;
         float colonWidth = height * 0.18F;
         float gap = height * 0.08F;
         float total = 0.0F;

         for(int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (i > 0) {
               total += gap;
            }

            total += c == ':' ? colonWidth : digitWidth;
         }

         return total;
      } else {
         return 0.0F;
      }
   }

   static void draw(VertexConsumer buffer, Matrix4f position, Matrix3f normal, String text, float left, float top, float height, int argb) {
      if (text != null && !text.isEmpty() && !(height <= 0.0F)) {
         float digitWidth = height * 0.5F;
         float colonWidth = height * 0.18F;
         float gap = height * 0.08F;
         float thickness = height * 0.13F;
         float x = left;

         for(int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (i > 0) {
               x += gap;
            }

            if (c == ':') {
               drawColon(buffer, position, normal, x, top, colonWidth, height, thickness, argb);
               x += colonWidth;
            } else {
               drawDigit(buffer, position, normal, c, x, top, digitWidth, height, thickness, argb);
               x += digitWidth;
            }
         }

      }
   }

   private static void drawDigit(VertexConsumer buffer, Matrix4f position, Matrix3f normal, char c, float left, float top, float width, float height, float thickness, int argb) {
      int digit = c - 48;
      if (digit >= 0 && digit < GLYPHS.length) {
         int mask = GLYPHS[digit];
         float half = thickness * 0.5F;
         float x0 = left + half;
         float x1 = left + width - half;
         float y0 = top + half;
         float y1 = top + height - half;
         float yMid = top + height * 0.5F;
         if ((mask & 1) != 0) {
            horizontal(buffer, position, normal, x0, x1, y0, thickness, argb);
         }

         if ((mask & 8) != 0) {
            horizontal(buffer, position, normal, x0, x1, y1, thickness, argb);
         }

         if ((mask & 64) != 0) {
            horizontal(buffer, position, normal, x0, x1, yMid, thickness, argb);
         }

         if ((mask & 32) != 0) {
            vertical(buffer, position, normal, x0, y0, yMid, thickness, argb);
         }

         if ((mask & 2) != 0) {
            vertical(buffer, position, normal, x1, y0, yMid, thickness, argb);
         }

         if ((mask & 16) != 0) {
            vertical(buffer, position, normal, x0, yMid, y1, thickness, argb);
         }

         if ((mask & 4) != 0) {
            vertical(buffer, position, normal, x1, yMid, y1, thickness, argb);
         }

      }
   }

   private static void drawColon(VertexConsumer buffer, Matrix4f position, Matrix3f normal, float left, float top, float width, float height, float thickness, int argb) {
      float size = thickness * 1.15F;
      float cx = left + width * 0.5F;
      float half = size * 0.5F;
      dot(buffer, position, normal, cx, top + height * 0.3F, half, argb);
      dot(buffer, position, normal, cx, top + height * 0.7F, half, argb);
   }

   private static void horizontal(VertexConsumer buffer, Matrix4f position, Matrix3f normal, float x0, float x1, float y, float thickness, int argb) {
      float h = thickness * 0.5F;
      float px1 = x0 + h;
      float py1 = y + h;
      float px2 = x1 - h;
      float py2 = y + h;
      float px4 = x1 - h;
      float py4 = y - h;
      float px5 = x0 + h;
      float py5 = y - h;
      hexagon(buffer, position, normal, argb, x0, y, px1, py1, px2, py2, x1, y, px4, py4, px5, py5);
   }

   private static void vertical(VertexConsumer buffer, Matrix4f position, Matrix3f normal, float x, float y0, float y1, float thickness, int argb) {
      float h = thickness * 0.5F;
      hexagon(buffer, position, normal, argb, x, y0, x - h, y0 + h, x - h, y1 - h, x, y1, x + h, y1 - h, x + h, y0 + h);
   }

   private static void dot(VertexConsumer buffer, Matrix4f position, Matrix3f normal, float cx, float cy, float half, int argb) {
      quad(buffer, position, normal, argb, cx - half, cy - half, cx - half, cy + half, cx + half, cy + half, cx + half, cy - half);
   }

   private static void hexagon(VertexConsumer buffer, Matrix4f position, Matrix3f normal, int argb, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float x5, float y5) {
      float cx = (x0 + x1 + x2 + x3 + x4 + x5) / 6.0F;
      float cy = (y0 + y1 + y2 + y3 + y4 + y5) / 6.0F;
      quad(buffer, position, normal, argb, x0, y0, x1, y1, x2, y2, cx, cy);
      quad(buffer, position, normal, argb, x2, y2, x3, y3, x4, y4, cx, cy);
      quad(buffer, position, normal, argb, x4, y4, x5, y5, x0, y0, cx, cy);
   }

   private static void quad(VertexConsumer buffer, Matrix4f position, Matrix3f normal, int argb, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
      vertex(buffer, position, normal, x0, y0, argb);
      vertex(buffer, position, normal, x1, y1, argb);
      vertex(buffer, position, normal, x2, y2, argb);
      vertex(buffer, position, normal, x3, y3, argb);
   }

   private static void vertex(VertexConsumer buffer, Matrix4f position, Matrix3f normal, float x, float y, int argb) {
      buffer.vertex(position, x, y, 0.0F).color(argb).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
   }
}
