// 柱底的渲染：用原版方块渲染器原样画一遍（和方块模型走同一条管线，颜色/明暗完全一致）。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.random.Random;

/**
 * 柱底的渲染器（Fabric）。
 *
 * <h2>为什么它存在</h2>
 * <p>这个方块带方块实体（颜色存里面），而实测「带方块实体的方块」在这套环境里它的
 * <b>方块模型不会被区块网格画出来</b>（同一份模型挂在不带方块实体的方块上正常，探针里
 * 模型烘焙出 150 个面、{@code builtin=false}，但画面上一面都没有）。
 *
 * <h2>为什么这么画</h2>
 * <p>这里<b>不自己算光照、不自己拼几何</b>：直接调原版的
 * {@code BlockRenderManager#renderBlock}，把这一格当成普通方块重新画一遍 ——
 * 逐面光照、环境光遮蔽（AO）、染色 provider、渲染层全部走原版那一套，
 * 所以观感和柱中（普通方块）完全一致，不会有之前 BER 那种「整体偏黑」的色差。
 */
public final class PoleRawRenderer implements BlockEntityRenderer<AmtrPoleBottomBlockEntity> {

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
            final MinecraftClient client = MinecraftClient.getInstance();
            final BlockState state = entity.getCachedState();
            final VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getSolid());
            client.getBlockRenderManager().renderBlock(state, entity.getPos(), entity.getWorld(),
                    matrices, buffer, false, Random.create(state.getRenderingSeed(entity.getPos())));
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole render skipped", t);
        }
    }
}
