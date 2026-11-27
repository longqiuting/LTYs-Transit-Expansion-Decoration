// 柱底凹槽里的那圈色带：四片平面 quad，颜色来自方块实体（自定义色 / 车站色），不是方块模型。
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
 * 柱底凹槽那圈色带 —— <b>只画四片平面</b>，方块本体仍然是普通方块模型（25 个 element）。
 *
 * <h2>为什么是渲染器画平面</h2>
 * <p>和 MTR 的吊板（{@code RenderRouteSign extends BlockEntityRendererMapper}）同一个手法：
 * 颜色是数据（车站线路色 / 刷子刷的自定义色），没法烘进模型，所以用方块实体渲染器在凹槽位置
 * 贴四片带颜色的 quad。方块模型那边<b>一行都不碰</b>。
 *
 * <h2>几何</h2>
 * <p>色带在 y 2.72~3.68 的凹槽里，四个方向各一片，贴在该高度模型的最外表面上（北 −0.91、
 * 南 16.91、西 −0.91、东 16.91），比表面再往外 0.01 避免 z-fighting。
 *
 * <h2>光照</h2>
 * <p>每片取它朝向那一格的光照（{@code WorldRenderer.getLightmapCoordinates}）—— 取方块自己那一格
 * 会是实心方块内部的 0，整圈发黑。
 */
public final class PoleRawRenderer implements BlockEntityRenderer<AmtrPoleBottomBlockEntity> {

    /** 色带上下沿（模型坐标，16 = 一格）。 */
    private static final float Y_BOTTOM = 2.72F;
    private static final float Y_TOP = 3.68F;
    /** 四个方向的外表面（模型坐标）+ 0.01 外推。 */
    private static final float OUT = 16.91F;
    private static final float OUT_NEG = -0.91F;

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
            final float y0 = Y_BOTTOM / 16.0F;
            final float y1 = Y_TOP / 16.0F;
            final float p = OUT / 16.0F;
            final float n = OUT_NEG / 16.0F;

            quad(entry, buffer, entity, Direction.NORTH, r, g, b,
                    n, 0.0F, y0, 16.0F, y0, 16.0F, y1, 0.0F, y1);
            quad(entry, buffer, entity, Direction.SOUTH, r, g, b,
                    p, 16.0F, y0, 0.0F, y0, 0.0F, y1, 16.0F, y1);
            quad(entry, buffer, entity, Direction.WEST, r, g, b,
                    n, 0.0F, y0, 16.0F, y0, 16.0F, y1, 0.0F, y1);
            quad(entry, buffer, entity, Direction.EAST, r, g, b,
                    p, 16.0F, y0, 0.0F, y0, 0.0F, y1, 16.0F, y1);
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole band render skipped", t);
        }
    }

    /**
     * 一片带颜色的 quad。
     *
     * @param fixed   固定轴上的坐标（模型单位 /16 后的格坐标）
     * @param axis    {@code 'z'} = 面朝南北（x 变化），{@code 'x'} = 面朝东西（z 变化）
     */
    private static void quad(final MatrixStack.Entry entry, final VertexConsumer buffer,
                             final AmtrPoleBottomBlockEntity entity, final Direction facing,
                             final float r, final float g, final float b,
                             final float fixed, final float ax, final float ay,
                             final float bx, final float by, final float cx, final float cy,
                             final float dx, final float dy) {
        final int light = lightOf(entity, facing);
        final boolean zAxis = facing == Direction.NORTH || facing == Direction.SOUTH;
        // 顶点顺序: (a) → (b) → (c) → (d)
        final float nx = facing.getOffsetX();
        final float ny = facing.getOffsetY();
        final float nz = facing.getOffsetZ();
        vertex(entry, buffer, zAxis, fixed, ax, ay, r, g, b, light, nx, ny, nz);
        vertex(entry, buffer, zAxis, fixed, bx, by, r, g, b, light, nx, ny, nz);
        vertex(entry, buffer, zAxis, fixed, cx, cy, r, g, b, light, nx, ny, nz);
        vertex(entry, buffer, zAxis, fixed, dx, dy, r, g, b, light, nx, ny, nz);
    }

    private static void vertex(final MatrixStack.Entry entry, final VertexConsumer buffer,
                               final boolean zAxis, final float fixed,
                               final float u, final float v,
                               final float r, final float g, final float b, final int light,
                               final float nx, final float ny, final float nz) {
        // u 传进来是模型单位（0..16），格坐标要 /16
        final float x = zAxis ? (u / 16.0F) : fixed;
        final float z = zAxis ? fixed : (u / 16.0F);
        buffer.vertex(entry.getPositionMatrix(), x, v, z)
                .color(r, g, b, 1.0F)
                .texture(0.0F, 0.0F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry.getNormalMatrix(), nx, ny, nz)
                .next();
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
