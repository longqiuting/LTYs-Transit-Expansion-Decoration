// 柱底凹槽里的那圈色带：四个方向各一片平面，颜色来自方块实体（自定义色 / 车站色）。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * 柱底凹槽那圈色带 —— <b>只画平面</b>，方块本体仍是普通方块模型（25 个 element），一行都不碰。
 *
 * <h2>为什么是渲染器画平面</h2>
 * <p>和 MTR 吊板（{@code RenderRouteSign extends BlockEntityRendererMapper}）同一个手法：颜色是数据
 * （车站线路色 / 刷子刷的自定义色），没法烘进模型，所以用方块实体渲染器在凹槽位置贴平面 quad。
 *
 * <h2>形状：四片主面 + 四片斜角</h2>
 * <p>柱子转角是斜的，只贴南北东西四片会在四个角留缝，所以每个角再加一片 45° 的斜角 quad，
 * 八片围成一圈。上下沿 y 2.72~3.68，外推 0.01 贴在最外表面外侧。
 *
 * <h2>光照</h2>
 * <p>每片取它朝向那一格的光照；取方块自己那一格会是实心方块内部的 0，整圈发黑。
 */
public final class PoleRawRenderer implements BlockEntityRenderer<AmtrPoleBottomBlockEntity> {

    /** 色带上下沿（模型坐标）。 */
    private static final float Y_BOTTOM = 2.72F / 16.0F;
    private static final float Y_TOP = 3.68F / 16.0F;
    /** 外表面（模型坐标 16.91 / -0.91 → 格坐标）。 */
    private static final float OUT = 16.91F / 16.0F;
    private static final float OUT_NEG = -0.91F / 16.0F;

    public PoleRawRenderer(final BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(final AmtrPoleBottomBlockEntity entity, final float tickDelta,
                       final MatrixStack matrices, final VertexConsumerProvider vertexConsumers,
                       final int light, final int overlay) {
        if (entity.getWorld() == null) {
            return;
        }
        try {
            int rgb = entity.cacheOnlyColor() & 0xFFFFFF;
            if (rgb == 0xFFFFFF) {
                final int resolved = PoleColors.resolve(entity.getWorld(), entity.getPos());
                if (resolved != 0) {
                    rgb = resolved & 0xFFFFFF;
                }
            }
            final float r = ((rgb >> 16) & 0xFF) / 255.0F;
            final float g = ((rgb >> 8) & 0xFF) / 255.0F;
            final float b = (rgb & 0xFF) / 255.0F;

            final VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getSolid());
            final MatrixStack.Entry entry = matrices.peek();

            final float y0 = Y_BOTTOM;
            final float y1 = Y_TOP;
            final float p = OUT;
            final float n = OUT_NEG;

            // 四片主面（北 / 南 / 西 / 东），各按自己的法线定绕序。
            quad(entry, buffer, entity, r, g, b, 0.0F, 0.0F, -1.0F,
                    new float[]{0.0F, y0, n}, new float[]{1.0F, y0, n},
                    new float[]{1.0F, y1, n}, new float[]{0.0F, y1, n});
            quad(entry, buffer, entity, r, g, b, 0.0F, 0.0F, 1.0F,
                    new float[]{1.0F, y0, p}, new float[]{0.0F, y0, p},
                    new float[]{0.0F, y1, p}, new float[]{1.0F, y1, p});
            quad(entry, buffer, entity, r, g, b, -1.0F, 0.0F, 0.0F,
                    new float[]{n, y0, 0.0F}, new float[]{n, y0, 1.0F},
                    new float[]{n, y1, 1.0F}, new float[]{n, y1, 0.0F});
            quad(entry, buffer, entity, r, g, b, 1.0F, 0.0F, 0.0F,
                    new float[]{p, y0, 1.0F}, new float[]{p, y0, 0.0F},
                    new float[]{p, y1, 0.0F}, new float[]{p, y1, 1.0F});

            // 四个 45° 转角：把相邻两片主面的外沿连起来（否则转角只有 90° 折角、看着没有色带）。
            final float dg = (float) (1.0D / Math.sqrt(2.0D));
            // (x=1, z=1)
            quad(entry, buffer, entity, r, g, b, dg, 0.0F, dg,
                    new float[]{1.0F, y0, p}, new float[]{p, y0, 1.0F},
                    new float[]{p, y1, 1.0F}, new float[]{1.0F, y1, p});
            // (x=1, z=0)
            quad(entry, buffer, entity, r, g, b, dg, 0.0F, -dg,
                    new float[]{p, y0, 0.0F}, new float[]{1.0F, y0, n},
                    new float[]{1.0F, y1, n}, new float[]{p, y1, 0.0F});
            // (x=0, z=0)
            quad(entry, buffer, entity, r, g, b, -dg, 0.0F, -dg,
                    new float[]{n, y0, 0.0F}, new float[]{0.0F, y0, n},
                    new float[]{0.0F, y1, n}, new float[]{n, y1, 0.0F});
            // (x=0, z=1)
            quad(entry, buffer, entity, r, g, b, -dg, 0.0F, dg,
                    new float[]{0.0F, y0, p}, new float[]{n, y0, 1.0F},
                    new float[]{n, y1, 1.0F}, new float[]{0.0F, y1, p});

        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole band render skipped", t);
        }
    }

    /** 一片 quad；绕序按法线自动修正（否则会被背面剔除，看起来「只贴了两面」）。 */
    private static void quad(final MatrixStack.Entry entry, final VertexConsumer buffer,
                             final AmtrPoleBottomBlockEntity entity,
                             final float cr, final float cg, final float cb,
                             final float nx, final float ny, final float nz,
                             final float[] a, final float[] b, final float[] c, final float[] d) {
        final Direction facing = dominant(nx, ny, nz);
        final int light = lightOf(entity, facing);
        // (b-a) × (d-a) 与法线同向才保留，否则交换 b/d
        final float[] e1 = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        final float[] e2 = {d[0] - a[0], d[1] - a[1], d[2] - a[2]};
        final float cx = e1[1] * e2[2] - e1[2] * e2[1];
        final float cy = e1[2] * e2[0] - e1[0] * e2[2];
        final float cz = e1[0] * e2[1] - e1[1] * e2[0];
        final boolean flip = cx * nx + cy * ny + cz * nz < 0.0F;
        vertex(entry, buffer, a, cr, cg, cb, light, nx, ny, nz);
        vertex(entry, buffer, flip ? d : b, cr, cg, cb, light, nx, ny, nz);
        vertex(entry, buffer, c, cr, cg, cb, light, nx, ny, nz);
        vertex(entry, buffer, flip ? b : d, cr, cg, cb, light, nx, ny, nz);
    }

    private static void vertex(final MatrixStack.Entry entry, final VertexConsumer buffer,
                               final float[] pos, final float cr, final float cg, final float cb,
                               final int light,
                               final float nx, final float ny, final float nz) {
        buffer.vertex(entry.getPositionMatrix(), pos[0], pos[1], pos[2])
                .color(cr, cg, cb, 1.0F)
                .texture(0.0F, 0.0F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry.getNormalMatrix(), nx, ny, nz)
                .next();
    }

    private static Direction dominant(final float nx, final float ny, final float nz) {
        if (Math.abs(nx) >= Math.abs(nz)) {
            return nx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return nz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static int lightOf(final AmtrPoleBottomBlockEntity entity, final Direction facing) {
        final BlockPos base = entity.getPos();
        int light = WorldRenderer.getLightmapCoordinates(entity.getWorld(), base.offset(facing));
        if (light == 0) {
            light = WorldRenderer.getLightmapCoordinates(entity.getWorld(), base);
        }
        return light == 0 ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;
    }
}
