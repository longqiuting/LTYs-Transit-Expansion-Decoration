package dev.js.mtrheadlight.xiamen.pids;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector4f;

final class PidsXiamenText {
   private PidsXiamenText() {
   }

   static void drawRun(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, PidsXiamenText.Segment[] segments, float x, float boxTop, float boxHeight, PidsXiamenLayout.HAlign align, float maxWidth, float clipLeft, float clipRight) {
      if (segments != null && segments.length != 0) {
         float total = 0.0F;

         for(PidsXiamenText.Segment segment : segments) {
            total += width(textRenderer, segment.text(), segment.size());
         }

         float shrink = 1.0F;
         if (maxWidth > 0.0F && total > maxWidth) {
            shrink = maxWidth / total;
            total = maxWidth;
         }

         float var10001;
         switch (align) {
            case LEFT:
               var10001 = 0.0F;
               break;
            case CENTER:
               var10001 = -total * 0.5F;
               break;
            case RIGHT:
               var10001 = -total;
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         float startX = x + var10001;
         float bottom = boxTop + boxHeight;
         float cursor = startX;
         boolean clip = !Float.isInfinite(clipLeft) || !Float.isInfinite(clipRight);
         Matrix4f world = clip ? new Matrix4f(matrices.last().pose()) : null;
         MultiBufferSource provider = (MultiBufferSource)(clip ? new PidsXiamenText.ClippingProvider(immediate, clipLeft, clipRight, world) : immediate);

         for(PidsXiamenText.Segment segment : segments) {
            if (segment.text() != null && !segment.text().isEmpty()) {
               float size = Math.max(1.0F, segment.size() * shrink);
               float scale = size / 8.0F;
               blit(textRenderer, provider, segment.text(), cursor, bottom - size, scale, segment.color(), clip ? new PoseStack() : matrices);
               cursor += (float)textRenderer.width(segment.text()) * scale;
            }
         }

      }
   }

   static float width(Font textRenderer, String text, float size) {
      return text != null && !text.isEmpty() ? (float)textRenderer.width(text) * (size / 8.0F) : 0.0F;
   }

   private static void blit(Font textRenderer, MultiBufferSource provider, String text, float x, float yTop, float scale, int color, PoseStack stack) {
      stack.pushPose();

      try {
         stack.translate(x, yTop, 0.0F);
         stack.scale(scale, scale, 1.0F);
         textRenderer.drawInBatch(text, 0.0F, 0.0F, color, false, stack.last().pose(), provider, DisplayMode.NORMAL, 0, 15728880);
      } finally {
         stack.popPose();
      }

   }

   private static final class ClippingConsumer implements VertexConsumer {
      private final VertexConsumer delegate;
      private final float left;
      private final float right;
      private final Matrix4f world;
      private final float[] xs = new float[4];
      private final float[] ys = new float[4];
      private final float[] zs = new float[4];
      private final float[] us = new float[4];
      private final float[] vs = new float[4];
      private int count;
      private int done;
      private int red = 255;
      private int green = 255;
      private int blue = 255;
      private int alpha = 255;
      private int lightU = -1;
      private int lightV = -1;
      private int overlayU;
      private int overlayV;
      private boolean hasOverlay;
      private boolean hasNormal;
      private float nx;
      private float ny = 1.0F;
      private float nz;
      private final Vector4f vector = new Vector4f();

      ClippingConsumer(VertexConsumer delegate, float left, float right, Matrix4f world) {
         this.delegate = delegate;
         this.left = left;
         this.right = right;
         this.world = world;
      }

      public VertexConsumer vertex(double x, double y, double z) {
         this.raw((float)x, (float)y, (float)z);
         return this;
      }

      public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
         this.vector.set(x, y, z, 1.0F);
         matrix.transform(this.vector);
         this.raw(this.vector.x(), this.vector.y(), this.vector.z());
         return this;
      }

      private void raw(float x, float y, float z) {
         if (this.count >= 4) {
            this.flush();
         }

         this.xs[this.count] = x;
         this.ys[this.count] = y;
         this.zs[this.count] = z;
         ++this.count;
      }

      public VertexConsumer color(int r, int g, int b, int a) {
         this.red = r;
         this.green = g;
         this.blue = b;
         this.alpha = a;
         return this;
      }

      public VertexConsumer uv(float u, float v) {
         if (this.count > 0) {
            this.us[this.count - 1] = u;
            this.vs[this.count - 1] = v;
         }

         return this;
      }

      public VertexConsumer overlayCoords(int u, int v) {
         this.overlayU = u;
         this.overlayV = v;
         this.hasOverlay = true;
         return this;
      }

      public VertexConsumer uv2(int u, int v) {
         this.lightU = u;
         this.lightV = v;
         return this;
      }

      public VertexConsumer normal(float x, float y, float z) {
         this.nx = x;
         this.ny = y;
         this.nz = z;
         this.hasNormal = true;
         return this;
      }

      public void defaultColor(int r, int g, int b, int a) {
         this.delegate.defaultColor(r, g, b, a);
      }

      public void unsetDefaultColor() {
         this.delegate.unsetDefaultColor();
      }

      public void endVertex() {
         ++this.done;
         if (this.done >= 4) {
            this.flush();
         }

      }

      private void flush() {
         int n = this.count;
         this.count = 0;
         this.done = 0;
         if (n > 0) {
            if (n < 4) {
               for(int i = 0; i < n; ++i) {
                  this.emit(this.xs[i], this.ys[i], this.zs[i], this.us[i], this.vs[i]);
               }

            } else {
               float x0 = this.xs[0];
               float x1 = this.xs[0];
               int i0 = 0;
               int i1 = 0;

               for(int i = 1; i < 4; ++i) {
                  if (this.xs[i] < x0) {
                     x0 = this.xs[i];
                     i0 = i;
                  }

                  if (this.xs[i] > x1) {
                     x1 = this.xs[i];
                     i1 = i;
                  }
               }

               if (!(x1 <= this.left) && !(x0 >= this.right)) {
                  float dx = x1 - x0;
                  float uAtLeft = this.us[i0];
                  float du = this.us[i1] - uAtLeft;

                  for(int i = 0; i < 4; ++i) {
                     float clamped = Math.max(this.left, Math.min(this.right, this.xs[i]));
                     float u;
                     if (!(Math.abs(dx) < 1.0E-4F) && !(Math.abs(du) < 1.0E-6F)) {
                        u = uAtLeft + (clamped - x0) / dx * du;
                     } else {
                        u = this.us[i];
                     }

                     this.emit(clamped, this.ys[i], this.zs[i], u, this.vs[i]);
                  }

               }
            }
         }
      }

      private void emit(float x, float y, float z, float u, float v) {
         this.delegate.vertex(this.world, x, y, z).color(this.red, this.green, this.blue, this.alpha).uv(u, v);
         if (this.hasOverlay) {
            this.delegate.overlayCoords(this.overlayU, this.overlayV);
         }

         if (this.lightU >= 0) {
            this.delegate.uv2(this.lightU, this.lightV);
         }

         if (this.hasNormal) {
            this.delegate.normal(this.nx, this.ny, this.nz);
         }

         this.delegate.endVertex();
      }
   }

   private static final class ClippingProvider implements MultiBufferSource {
      private final MultiBufferSource delegate;
      private final float left;
      private final float right;
      private final Matrix4f world;
      private final Map<RenderType, PidsXiamenText.ClippingConsumer> consumers = new HashMap();

      ClippingProvider(MultiBufferSource delegate, float left, float right, Matrix4f world) {
         this.delegate = delegate;
         this.left = left;
         this.right = right;
         this.world = world;
      }

      public VertexConsumer getBuffer(RenderType layer) {
         return (VertexConsumer)this.consumers.computeIfAbsent(layer, (key) -> new PidsXiamenText.ClippingConsumer(this.delegate.getBuffer(key), this.left, this.right, this.world));
      }
   }

   static record Segment(String text, float size, int color) {
   }
}
