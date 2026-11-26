// 厦门地铁柱子的运行时渲染（Fabric 侧）。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;

/**
 * 柱底 = <b>运行时网格</b>（rawmodel）而不是方块烘焙模型。
 *
 * <h2>为什么不用 blockstate 模型</h2>
 * <p>柱底这个方块（带方块实体）在方块烘焙路径下探针确认「模型有 102 个面、不是 builtin」，
 * 但画面上一面都不画（同样的模型换到普通方块「柱中」上就正常）。改成和本模组其它装饰方块
 * 一样的 BER 逐面画，只要方块实体在就一定能画出来。
 *
 * <h2>三段网格</h2>
 * <ul>
 *   <li>{@code pole_core}：16³ 核心，灰混凝土；</li>
 *   <li>{@code pole_fins}：四周 8 片鳍片，白混凝土；</li>
 *   <li>{@code pole_band}：color 分组那一圈，白混凝土 + <b>乘方块实体的颜色</b>。</li>
 * </ul>
 *
 * <h2>光照：逐面取对面那一格</h2>
 * <p>原版方块渲染是「每个面取它朝向那一格的光」，所以柱子侧面用的是旁边空气的光。
 * BER 如果只取方块自己那一格，那格是实心方块的内部（天光/方块光都是 0），整根会发黑——
 * 这就是之前「BER 和普通渲染有色差、整体偏黑」的原因。这里按面法线取邻格光照，
 * 并乘原版那套逐面 shade（0.5 / 0.6 / 0.8 / 1.0），观感与普通方块一致。
 */
public final class PoleRawRenderer implements BlockEntityRenderer<AmtrPoleBottomBlockEntity> {

    private static final String CORE = "pole_core";
    private static final String FINS = "pole_fins";
    private static final String BAND = "pole_band";

    public PoleRawRenderer(final BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(final AmtrPoleBottomBlockEntity entity, final float tickDelta,
                       final MatrixStack matrices, final VertexConsumerProvider vertexConsumers,
                       final int light, final int overlay) {
        if (entity.getWorld() == null) {
            return;
        }
        matrices.push();
        try {
            draw(entity, vertexConsumers, matrices, CORE, 1.0F, 1.0F, 1.0F);
            draw(entity, vertexConsumers, matrices, FINS, 1.0F, 1.0F, 1.0F);
            final int color = entity.cacheOnlyColor();
            draw(entity, vertexConsumers, matrices, BAND,
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F);
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole render skipped", t);
        }
        matrices.pop();
    }

    private static void draw(final AmtrPoleBottomBlockEntity entity,
                             final VertexConsumerProvider consumers, final MatrixStack matrices,
                             final String name, final float r, final float g, final float b) {
        final RawMesh mesh = RawModelJson.get(RawModelJson.id(name));
        if (mesh == null || mesh.quads().length == 0) {
            return;
        }
        final VertexConsumer buffer = consumers.getBuffer(RawModelRenderer.solidLayer(mesh));
        final MatrixStack.Entry entry = matrices.peek();
        for (final RawMesh.Quad quad : mesh.quads()) {
            final int quadLight = lightOf(entity, quad.normal());
            final float shade = quad.fullBright() ? 1.0F : quad.shade();
            final float[][] pos = quad.pos();
            final float[][] uv = quad.uv();
            final float[] n = quad.normal();
            for (int i = 0; i < 4; i++) {
                buffer.vertex(entry.getPositionMatrix(), pos[i][0] / 16.0F, pos[i][1] / 16.0F,
                                pos[i][2] / 16.0F)
                        .color(shade * r, shade * g, shade * b, 1.0F)
                        .texture(uv[i][0], uv[i][1])
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(quadLight)
                        .normal(entry.getNormalMatrix(), n[0], n[1], n[2])
                        .next();
            }
        }
    }

    /** 面朝向的那一格的光照（和原版方块渲染同一条取法）；朝下时取正下方。 */
    private static int lightOf(final AmtrPoleBottomBlockEntity entity, final float[] normal) {
        final Direction direction = dominantDirection(normal);
        final BlockPos base = entity.getPos();
        final BlockPos neighbour = direction == null ? base : base.offset(direction);
        int light = WorldRenderer.getLightmapCoordinates(entity.getWorld(), neighbour);
        if (light == 0) {
            // 极端情况（邻格也没光数据）退到方块自己那格，至少不整面全黑。
            light = WorldRenderer.getLightmapCoordinates(entity.getWorld(), base);
        }
        return light == 0 ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;
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
