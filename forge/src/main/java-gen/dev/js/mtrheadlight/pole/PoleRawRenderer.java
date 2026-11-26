// 厦门地铁柱子的运行时渲染（Forge 侧，mojmap）。
package dev.js.mtrheadlight.pole;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 柱底的运行时网格渲染（Forge 侧）。与 Fabric 侧 {@code PoleRawRenderer} 同构：
 * 几何来自 {@code rawmodels/pole_core|pole_fins|pole_band}，色带乘方块实体的颜色，
 * 光照按面法线取邻格（和原版方块渲染同一条取法，否则整根发黑）。
 */
public class PoleRawRenderer implements BlockEntityRenderer<AmtrPoleBottomBlockEntity> {

    private static final String CORE = "pole_core";
    private static final String FINS = "pole_fins";
    private static final String BAND = "pole_band";

    public PoleRawRenderer(final BlockEntityRenderDispatcher dispatcher) {
    }

    @Override
    public void render(final AmtrPoleBottomBlockEntity entity, final float tickDelta,
                       final PoseStack matrices, final MultiBufferSource buffers,
                       final int light, final int overlay) {
        if (entity.getLevel() == null) {
            return;
        }
        matrices.pushPose();
        try {
            draw(entity, buffers, matrices, CORE, 1.0F, 1.0F, 1.0F);
            draw(entity, buffers, matrices, FINS, 1.0F, 1.0F, 1.0F);
            final int color = entity.cacheOnlyColor();
            draw(entity, buffers, matrices, BAND,
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F);
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole render skipped", t);
        }
        matrices.popPose();
    }

    private static void draw(final AmtrPoleBottomBlockEntity entity, final MultiBufferSource buffers,
                             final PoseStack matrices, final String name,
                             final float r, final float g, final float b) {
        final RawMesh mesh = RawModelJson.get(RawModelJson.id(name));
        if (mesh == null || mesh.quads().length == 0) {
            return;
        }
        final VertexConsumer buffer = buffers.getBuffer(RawModelRenderer.solidLayer(mesh));
        final PoseStack.Pose entry = matrices.last();
        for (final RawMesh.Quad quad : mesh.quads()) {
            final int quadLight = lightOf(entity, quad.normal());
            final float shade = quad.fullBright() ? 1.0F : quad.shade();
            final float[][] pos = quad.pos();
            final float[][] uv = quad.uv();
            final float[] n = quad.normal();
            for (int i = 0; i < 4; i++) {
                buffer.vertex(entry.pose(), pos[i][0] / 16.0F, pos[i][1] / 16.0F, pos[i][2] / 16.0F)
                        .color(shade * r, shade * g, shade * b, 1.0F)
                        .uv(uv[i][0], uv[i][1])
                        .overlayCoords(0xF000F0)
                        .uv2(quadLight)
                        .normal(entry.normal(), n[0], n[1], n[2])
                        .endVertex();
            }
        }
    }

    private static int lightOf(final AmtrPoleBottomBlockEntity entity, final float[] normal) {
        final Direction direction = dominantDirection(normal);
        final BlockPos base = entity.getBlockPos();
        final BlockPos neighbour = direction == null ? base : base.relative(direction);
        int light = LevelRenderer.getLightColor(entity.getLevel(), neighbour);
        if (light == 0) {
            light = LevelRenderer.getLightColor(entity.getLevel(), base);
        }
        return light == 0 ? LightTexture.FULL_BRIGHT : light;
    }

    private static Direction dominantDirection(final float[] normal) {
        if (normal == null) {
            return null;
        }
        final float ax = Math.abs(normal[0]);
        final float ay = Math.abs(normal[1]);
        final float az = Math.abs(normal[2]);
        if (ax >= ay && ax >= az) {
            return normal[0] > 0 ? Direction.EAST : Direction.WEST;
        }
        if (ay >= az) {
            return normal[1] > 0 ? Direction.UP : Direction.DOWN;
        }
        return normal[2] > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
