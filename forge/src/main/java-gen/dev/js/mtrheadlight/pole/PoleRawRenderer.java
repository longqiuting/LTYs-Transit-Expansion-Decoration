// 厦门地铁柱子的运行时渲染（Forge 侧，mojmap）。
package dev.js.mtrheadlight.pole;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

/**
 * 柱底的运行时网格渲染（Forge 侧）。与 Fabric 侧 {@code PoleRawRenderer} 同构：
 * 几何来自 {@code rawmodels/pole_core|pole_fins|pole_band}，色带乘方块实体的颜色。
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
        final int packedLight = LevelRenderer.getLightColor(entity.getLevel(), entity.getBlockPos());
        try {
            draw(buffers, matrices, CORE, packedLight, 1.0F, 1.0F, 1.0F);
            draw(buffers, matrices, FINS, packedLight, 1.0F, 1.0F, 1.0F);
            final int color = entity.cacheOnlyColor();
            draw(buffers, matrices, BAND, packedLight,
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F);
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole render skipped", t);
        }
        matrices.popPose();
    }

    private static void draw(final MultiBufferSource buffers, final PoseStack matrices,
                             final String name, final int light,
                             final float r, final float g, final float b) {
        final RawMesh mesh = RawModelJson.get(RawModelJson.id(name));
        if (mesh == null || mesh.quads().length == 0) {
            return;
        }
        RawModelRenderer.render(matrices, buffers.getBuffer(RawModelRenderer.solidLayer(mesh)),
                mesh, light, 0xF000F0, false, r, g, b);
    }
}
