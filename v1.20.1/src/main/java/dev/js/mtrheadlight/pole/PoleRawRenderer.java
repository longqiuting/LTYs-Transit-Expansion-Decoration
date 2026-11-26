// 厦门地铁柱子的运行时渲染（Fabric 侧）。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

/**
 * 柱底 = <b>运行时网格</b>（rawmodel）而不是方块烘焙模型。
 *
 * <h2>为什么不用 blockstate 模型</h2>
 * <p>柱底在部分客户端环境里整块不渲染（同一份 jar 在别的客户端正常，物品也正常），
 * 排查到方块模型烘焙这一环后改成和本模组其它装饰方块一样的路子：素材 JSON 直接转成
 * {@link RawMesh}，由 BER 逐面画。这样不经过方块模型烘焙、方块图集和染色管线，
 * 只要方块实体在就一定能画出来。
 *
 * <h2>三段网格</h2>
 * <ul>
 *   <li>{@code pole_core}：16³ 核心，灰混凝土；</li>
 *   <li>{@code pole_fins}：四周 8 片鳍片，白混凝土；</li>
 *   <li>{@code pole_band}：color 分组那一圈，白混凝土 + <b>乘方块实体的颜色</b>。</li>
 * </ul>
 * 三段分开是因为一个 rawmodel 只能绑一张贴图，而核心和鳍片贴图不同、色带要单独染色。
 *
 * <p>光照用方块位置的真实光照（{@link WorldRenderer#getLightmapCoordinates}），
 * 与别的装饰方块一致；天空光/方块光都吃。
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
        matrices.translate(0.0, 0.0, 0.0);
        final int packedLight = light;
        try {
            draw(vertexConsumers, matrices, CORE, packedLight, 1.0F, 1.0F, 1.0F);
            draw(vertexConsumers, matrices, FINS, packedLight, 1.0F, 1.0F, 1.0F);
            final int color = entity.cacheOnlyColor();
            draw(vertexConsumers, matrices, BAND, packedLight,
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F);
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole render skipped", t);
        }
        matrices.pop();
    }

    private static void draw(final VertexConsumerProvider consumers, final MatrixStack matrices,
                             final String name, final int light,
                             final float r, final float g, final float b) {
        final RawMesh mesh = RawModelJson.get(RawModelJson.id(name));
        if (mesh == null || mesh.quads().length == 0) {
            return;
        }
        final VertexConsumer buffer = consumers.getBuffer(RawModelRenderer.solidLayer(mesh));
        RawModelRenderer.render(matrices, buffer, mesh, light, OverlayTexture.DEFAULT_UV, false, r, g, b);
    }
}
