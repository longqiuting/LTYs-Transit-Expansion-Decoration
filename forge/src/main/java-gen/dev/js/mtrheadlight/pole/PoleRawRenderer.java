// 柱底凹槽里的那圈色带（Forge 侧）：四片平面 quad，颜色来自方块实体，方块模型不碰。
package dev.js.mtrheadlight.pole;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.js.mtrheadlight.LteLog;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 柱底凹槽色带（Forge 侧，和 Fabric 侧 {@code PoleRawRenderer} 同构）。
 *
 * <p>只画四片带颜色的平面：北/南/西/东各一片，贴在 y 2.72~3.68 那圈凹槽的外表面上。
 * 方块本体是普通方块模型（25 个 element），这里一行都不碰。
 *
 * <p>光照取面朝那一格（取方块自己那一格会是实心方块内部的 0，整圈发黑）。
 */
public class PoleRawRenderer implements BlockEntityRenderer<AmtrPoleBottomBlockEntity> {

    private static final float Y_BOTTOM = 2.72F / 16.0F;
    private static final float Y_TOP = 3.68F / 16.0F;
    private static final float OUT = 16.91F / 16.0F;
    private static final float OUT_NEG = -0.91F / 16.0F;

    public PoleRawRenderer(final BlockEntityRenderDispatcher dispatcher) {
    }

    @Override
    public void render(final AmtrPoleBottomBlockEntity entity, final float tickDelta,
                       final PoseStack matrices, final MultiBufferSource buffers,
                       final int light, final int overlay) {
        if (entity.getLevel() == null) {
            return;
        }
        try {
            int rgb = entity.cacheOnlyColor() & 0xFFFFFF;
            if (rgb == 0xFFFFFF) {
                final int resolved = PoleColors.resolve(entity.getLevel(), entity.getBlockPos());
                if (resolved != 0) {
                    rgb = resolved & 0xFFFFFF;
                }
            }
            final float r = ((rgb >> 16) & 0xFF) / 255.0F;
            final float g = ((rgb >> 8) & 0xFF) / 255.0F;
            final float b = (rgb & 0xFF) / 255.0F;

            final VertexConsumer buffer = buffers.getBuffer(RenderType.solid());
            final PoseStack.Pose entry = matrices.last();

            quad(entry, buffer, entity, Direction.NORTH, r, g, b,
                    OUT_NEG, 0.0F, Y_BOTTOM, 1.0F, Y_BOTTOM, 1.0F, Y_TOP, 0.0F, Y_TOP);
            quad(entry, buffer, entity, Direction.SOUTH, r, g, b,
                    OUT, 1.0F, Y_BOTTOM, 0.0F, Y_BOTTOM, 0.0F, Y_TOP, 1.0F, Y_TOP);
            quad(entry, buffer, entity, Direction.WEST, r, g, b,
                    OUT_NEG, 0.0F, Y_BOTTOM, 1.0F, Y_BOTTOM, 1.0F, Y_TOP, 0.0F, Y_TOP);
            quad(entry, buffer, entity, Direction.EAST, r, g, b,
                    OUT, 1.0F, Y_BOTTOM, 0.0F, Y_BOTTOM, 0.0F, Y_TOP, 1.0F, Y_TOP);

            // 四个 45° 转角（相邻两片主面的外沿相接）
            final float dg = (float) (1.0D / Math.sqrt(2.0D));
            quad2(entry, buffer, entity, dg, 0.0F, dg, r, g, b,
                    new float[]{1.0F, Y_BOTTOM, OUT}, new float[]{OUT, Y_BOTTOM, 1.0F},
                    new float[]{OUT, Y_TOP, 1.0F}, new float[]{1.0F, Y_TOP, OUT});
            quad2(entry, buffer, entity, dg, 0.0F, -dg, r, g, b,
                    new float[]{OUT, Y_BOTTOM, 0.0F}, new float[]{1.0F, Y_BOTTOM, OUT_NEG},
                    new float[]{1.0F, Y_TOP, OUT_NEG}, new float[]{OUT, Y_TOP, 0.0F});
            quad2(entry, buffer, entity, -dg, 0.0F, -dg, r, g, b,
                    new float[]{OUT_NEG, Y_BOTTOM, 0.0F}, new float[]{0.0F, Y_BOTTOM, OUT_NEG},
                    new float[]{0.0F, Y_TOP, OUT_NEG}, new float[]{OUT_NEG, Y_TOP, 0.0F});
            quad2(entry, buffer, entity, -dg, 0.0F, dg, r, g, b,
                    new float[]{0.0F, Y_BOTTOM, OUT}, new float[]{OUT_NEG, Y_BOTTOM, 1.0F},
                    new float[]{OUT_NEG, Y_TOP, 1.0F}, new float[]{0.0F, Y_TOP, OUT});
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole band render skipped", t);
        }
    }

    private static void quad(final PoseStack.Pose entry, final VertexConsumer buffer,
                             final AmtrPoleBottomBlockEntity entity, final Direction facing,
                             final float r, final float g, final float b, final float fixed,
                             final float ax, final float ay, final float bx, final float by,
                             final float cx, final float cy, final float dx, final float dy) {
        final int light = lightOf(entity, facing);
        final float nx = facing.getStepX();
        final float ny = facing.getStepY();
        final float nz = facing.getStepZ();
        final boolean zAxis = facing == Direction.NORTH || facing == Direction.SOUTH;
        vertex(entry, buffer, zAxis, fixed, ax, ay, r, g, b, light, nx, ny, nz);
        vertex(entry, buffer, zAxis, fixed, bx, by, r, g, b, light, nx, ny, nz);
        vertex(entry, buffer, zAxis, fixed, cx, cy, r, g, b, light, nx, ny, nz);
        vertex(entry, buffer, zAxis, fixed, dx, dy, r, g, b, light, nx, ny, nz);
    }

    private static void vertex(final PoseStack.Pose entry, final VertexConsumer buffer,
                               final boolean zAxis, final float fixed, final float u, final float v,
                               final float r, final float g, final float b, final int light,
                               final float nx, final float ny, final float nz) {
        final float x = zAxis ? u : fixed;
        final float z = zAxis ? fixed : u;
        buffer.vertex(entry.pose(), x, v, z)
                .color(r, g, b, 1.0F)
                .uv(0.0F, 0.0F)
                .overlayCoords(0xF000F0)
                .uv2(light)
                .normal(entry.normal(), nx, ny, nz)
                .endVertex();
    }

    /** 四角用的通用 quad（顶点数组 + 法线自动定绕序）。 */
    private static void quad2(final PoseStack.Pose entry, final VertexConsumer buffer,
                              final AmtrPoleBottomBlockEntity entity,
                              final float nx, final float ny, final float nz,
                              final float r, final float g, final float b,
                              final float[] a, final float[] bb, final float[] c, final float[] d) {
        final int light = lightOf(entity, Math.abs(nx) >= Math.abs(nz)
                ? (nx >= 0 ? Direction.EAST : Direction.WEST)
                : (nz >= 0 ? Direction.SOUTH : Direction.NORTH));
        final float[] e1 = {bb[0] - a[0], bb[1] - a[1], bb[2] - a[2]};
        final float[] e2 = {d[0] - a[0], d[1] - a[1], d[2] - a[2]};
        final float cx = e1[1] * e2[2] - e1[2] * e2[1];
        final float cy = e1[2] * e2[0] - e1[0] * e2[2];
        final float cz = e1[0] * e2[1] - e1[1] * e2[0];
        final boolean flip = cx * nx + cy * ny + cz * nz < 0.0F;
        final float[][] pts = {a, flip ? d : bb, c, flip ? bb : d};
        for (final float[] pt : pts) {
            buffer.vertex(entry.pose(), pt[0], pt[1], pt[2]).color(r, g, b, 1.0F).uv(0.0F, 0.0F)
                    .overlayCoords(0xF000F0).uv2(light).normal(entry.normal(), nx, ny, nz).endVertex();
        }
    }

    private static int lightOf(final AmtrPoleBottomBlockEntity entity, final Direction facing) {
        final BlockPos base = entity.getBlockPos();
        int light = LevelRenderer.getLightColor(entity.getLevel(), base.relative(facing));
        if (light == 0) {
            light = LevelRenderer.getLightColor(entity.getLevel(), base);
        }
        return light == 0 ? LightTexture.FULL_BRIGHT : light;
    }
}
