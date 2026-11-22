package dev.js.mtrheadlight.rawmodel;

import net.minecraft.resources.ResourceLocation;

public final class RawMesh {
   private final ResourceLocation texture;
   private final RawMesh.Quad[] quads;

   public RawMesh(ResourceLocation texture, RawMesh.Quad[] quads) {
      this.texture = texture;
      this.quads = quads;
   }

   public ResourceLocation texture() {
      return this.texture;
   }

   public RawMesh.Quad[] quads() {
      return this.quads;
   }

   public int quadCount() {
      return this.quads.length;
   }

   public double[] bounds() {
      double[] box = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};

      for(RawMesh.Quad quad : this.quads) {
         for(float[] p : quad.pos()) {
            for(int axis = 0; axis < 3; ++axis) {
               if ((double)p[axis] < box[axis]) {
                  box[axis] = (double)p[axis];
               }

               if ((double)p[axis] > box[axis + 3]) {
                  box[axis + 3] = (double)p[axis];
               }
            }
         }
      }

      return box[0] > box[3] ? new double[6] : box;
   }

   public static record Quad(float[][] pos, float[][] uv, float[] normal, float shade, boolean fullBright, String group, int paragraph) {
      public Quad(float[][] pos, float[][] uv, float[] normal, float shade, boolean fullBright) {
         this(pos, uv, normal, shade, fullBright, "", 0);
      }

      public Quad(float[][] pos, float[][] uv, float[] normal, float shade, boolean fullBright, String group) {
         this(pos, uv, normal, shade, fullBright, group, 0);
      }
   }
}
