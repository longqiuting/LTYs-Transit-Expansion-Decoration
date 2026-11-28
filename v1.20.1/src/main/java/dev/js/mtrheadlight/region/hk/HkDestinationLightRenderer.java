// 终点站指示灯发光层：整个方块按终点站色满亮，没有进站车时什么都不画（= 灭）。
package dev.js.mtrheadlight.region.hk;

import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/**
 * 终点站指示灯的渲染：把素材那份模型再用<b>满亮 + 终点站色</b>画一层。
 *
 * <h2>为什么整块都变色发光</h2>
 * 按用户的要求「整个方块都改变颜色发光」——不是只亮那个 45° 斜面。做法是把同一份模型
 * （3 个 element，外露的面都在）取 lightmap 满亮、顶点色乘上终点站色；夜里/阴影里也不会被压暗。
 *
 * <h2>为什么采用「再加一层」而不是改方块模型</h2>
 * 方块模型是深色灯体（熄灯时看到的就是它），彩色发光这层是 BER 画的：
 * <ul>
 *   <li>没有进站车 → 这一层一条 quad 都不画，灯带看起来就是灭的，方块本体仍在；</li>
 *   <li>颜色来自客户端实时列车状态，不需要方块状态、不需要同步、不需要重载模型；</li>
 *   <li>{@code RenderLayer} 按贴图缓存一份（{@link RawModelRenderer#solidLayer}），不会每帧新建图层。</li>
 * </ul>
 *
 * <p>代价：每段灯带每帧几个 quad，几十段站台门也就是几百个 quad —— 和空气散射那种全屏后处理
 * 不是一个量级。
 */
public final class HkDestinationLightRenderer implements BlockEntityRenderer<HkDestinationLightBlockEntity> {



    @Override
    public void render(final HkDestinationLightBlockEntity entity, final float tickDelta,
                       final MatrixStack matrices, final VertexConsumerProvider vertexConsumers,
                       final int light, final int overlay) {
        if (entity.getWorld() == null) {
            return;
        }
        // 0 = 没有进站车（或拿不到终点站色）→ 灭
        final int rgb = DestinationColors.colorAt(entity.getWorld(), entity.getPos());
        if (rgb == 0) {
            return;
        }
        final RawMesh mesh = RawModelJson.get(RawModelJson.id("hk_destination_light"));
        if (mesh == null) {
            return;
        }

        matrices.push();
        // 方块模型的默认朝向是北；和 blockstate 的 y 旋转保持同一套角度
        matrices.translate(0.5D, 0.0D, 0.5D);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw(entity.getCachedState())));
        matrices.translate(-0.5D, 0.0D, -0.5D);

        final VertexConsumer consumer = vertexConsumers.getBuffer(RawModelRenderer.solidLayer(mesh));
        final float r = ((rgb >> 16) & 0xFF) / 255.0F;
        final float g = ((rgb >> 8) & 0xFF) / 255.0F;
        final float b = (rgb & 0xFF) / 255.0F;
        RawModelRenderer.render(matrices, consumer, mesh, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV, true, r, g, b);
        matrices.pop();
    }

    private static float yaw(final BlockState state) {
        final Direction facing = state.get(HkDestinationLightBlock.FACING);
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    /** 灯带很小，但要在站台另一端也看得到：给足渲染距离。 */
    @Override
    public int getRenderDistance() {
        return 128;
    }

    @Override
    public boolean isInRenderDistance(final HkDestinationLightBlockEntity entity, final net.minecraft.util.math.Vec3d cameraPos) {
        return entity.getPos().isWithinDistance(cameraPos, 128.0D);
    }

}
