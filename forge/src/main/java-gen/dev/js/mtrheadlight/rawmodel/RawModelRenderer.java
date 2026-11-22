package dev.js.mtrheadlight.rawmodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class RawModelRenderer {
   private static final Map<ResourceLocation, RenderType> CUTOUT_LAYERS = new ConcurrentHashMap();

   private RawModelRenderer() {
   }

   public static RenderType cutoutLayer(RawMesh mesh) {
      return (RenderType)CUTOUT_LAYERS.computeIfAbsent(mesh.texture(), RenderType::entityCutout);
   }

   public static void render(PoseStack matrices, VertexConsumer consumer, RawMesh mesh, int light, int overlay) {
      render(matrices, consumer, mesh, light, overlay, false);
   }

   public static void render(PoseStack matrices, VertexConsumer consumer, RawMesh mesh, int light, int overlay, boolean emissive) {
      if (mesh != null && consumer != null) {
         PoseStack.Pose entry = matrices.last();

         for(RawMesh.Quad quad : mesh.quads()) {
            boolean bright = emissive || quad.fullBright();
            int quadLight = bright ? 15728880 : light;
            float shade = bright ? 1.0F : quad.shade();
            float[][] pos = quad.pos();
            float[][] uv = quad.uv();
            float[] n = quad.normal();

            for(int i = 0; i < 4; ++i) {
               consumer.vertex(entry.pose(), pos[i][0] / 16.0F, pos[i][1] / 16.0F, pos[i][2] / 16.0F).color(shade, shade, shade, 1.0F).uv(uv[i][0], uv[i][1]).overlayCoords(overlay).uv2(quadLight).normal(entry.normal(), n[0], n[1], n[2]).endVertex();
            }
         }

      }
   }
}
