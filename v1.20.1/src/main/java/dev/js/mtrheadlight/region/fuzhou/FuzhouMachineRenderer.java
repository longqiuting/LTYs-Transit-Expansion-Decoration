// 福州地铁售票机的渲染：本体（rawmodels）+ 两个 color 组（纯色）+ LED 屏。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.pole.PoleColors;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
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
 * 售票机渲染：本体用 rawmodel 画（素材不是原版模型格式），两个 {@code color} 组和 LED 屏再各画一层。
 *
 * <p>关机模式（{@code MODE_OFF}）用 {@code fuzhou_ticket_machine_off} 贴图；方速 / 原版外观一致。
 * 颜色数据源：车站色 / 线路色 / 玩家自选，见 {@link FuzhouMachineBlockEntity}。
 */
public final class FuzhouMachineRenderer implements BlockEntityRenderer<FuzhouMachineBlockEntity> {

    private static final String OFF_TEXTURE = "textures/block/fuzhou_ticket_machine_off";

    public FuzhouMachineRenderer(final BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(final FuzhouMachineBlockEntity entity, final float tickDelta,
                       final MatrixStack matrices, final VertexConsumerProvider vertexConsumers,
                       final int light, final int overlay) {
        if (entity.getWorld() == null || !(entity.getCachedState().getBlock() instanceof final FuzhouMachineBlock block)) {
            return;
        }
        final FuzhouMachineBlockEntity config = configOf(entity);
        try {
            final float[] facing = rotationOf(entity.getCachedState().get(FuzhouMachineBlock.FACING));
            matrices.push();
            matrices.translate(0.5D, 0.0D, 0.5D);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(facing[0]));
            matrices.translate(-0.5D, 0.0D, -0.5D);

            final boolean off = config.mode() == FuzhouMachineBlockEntity.MODE_OFF;

            // 1) 本体
            final RawMesh body = RawModelJson.get(RawModelJson.id(block.rawModel(entity.getCachedState())));
            if (body != null) {
                final VertexConsumer consumer = vertexConsumers.getBuffer(
                        off ? cutoutLayerOf(body, OFF_TEXTURE) : RawModelRenderer.cutoutLayer(body));
                RawModelRenderer.render(matrices, consumer, body, light, OverlayTexture.DEFAULT_UV);
            }

            // 2) 面板颜色区（纯顶点色；关机时不画）
            if (!off) {
                final int rgb = resolveColor(config);
                final float r = ((rgb >> 16) & 0xFF) / 255.0F;
                final float g = ((rgb >> 8) & 0xFF) / 255.0F;
                final float b = (rgb & 0xFF) / 255.0F;
                final RawMesh color = RawModelJson.get(RawModelJson.id(block.colorModel(entity.getCachedState())));
                drawFlat(matrices, vertexConsumers, color, light, r, g, b);
            }

            // 3) LED 屏底（文字在下一轮加；先铺一块暗绿，方便确认位置）
            if (!off && FuzhouMachineBlock.isUpper(entity.getCachedState())) {
                final RawMesh screen = RawModelJson.get(RawModelJson.id("fuzhou_machine_screen"));
                if (screen != null) {
                    final VertexConsumer consumer = vertexConsumers.getBuffer(RawModelRenderer.solidLayer(screen));
                    RawModelRenderer.render(matrices, consumer, screen,
                            LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV,
                            false, 0.05F, 0.35F, 0.12F);
                }
            }

            matrices.pop();
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] fuzhou machine render skipped", t);
        }
    }

    /**
     * 纯色块：不走贴图（否则会采样到图集里的像素，浅色块里掺黑），用 1×1 纯白贴图 + 顶点色。
     */
    private static void drawFlat(final MatrixStack matrices, final VertexConsumerProvider vertexConsumers,
                                 final RawMesh mesh, final int light, final float r, final float g, final float b) {
        if (mesh == null) {
            return;
        }
        final VertexConsumer consumer = vertexConsumers.getBuffer(
                RenderLayer.getEntitySolid(new net.minecraft.util.Identifier("mtrlte", "textures/block/flat_white.png")));
        final MatrixStack.Entry entry = matrices.peek();
        for (final RawMesh.Quad quad : mesh.quads()) {
            final float shade = quad.fullBright() ? 1.0F : quad.shade();
            final float[][] pos = quad.pos();
            final float[] n = quad.normal();
            for (int i = 0; i < 4; i++) {
                consumer.vertex(entry.getPositionMatrix(), pos[i][0] / 16.0F, pos[i][1] / 16.0F, pos[i][2] / 16.0F)
                        .color(shade * r, shade * g, shade * b, 1.0F)
                        .texture(0.5F, 0.5F)
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(light)
                        .normal(entry.getNormalMatrix(), n[0], n[1], n[2])
                        .next();
            }
        }
    }

    /** 面板颜色：车站色 / 线路色 / 玩家自选。 */
    /** 配置统一存在上半段的方块实体里；下段渲染时去上段取。 */
    private static FuzhouMachineBlockEntity configOf(final FuzhouMachineBlockEntity entity) {
        if (FuzhouMachineBlock.isUpper(entity.getCachedState()) || entity.getWorld() == null) {
            return entity;
        }
        final net.minecraft.block.entity.BlockEntity up = entity.getWorld().getBlockEntity(entity.getPos().up());
        return up instanceof final FuzhouMachineBlockEntity upper ? upper : entity;
    }

    private static int resolveColor(final FuzhouMachineBlockEntity entity) {
        return switch (entity.colorSource()) {
            case FuzhouMachineBlockEntity.COLOR_CUSTOM -> entity.customColor();
            case FuzhouMachineBlockEntity.COLOR_LINE -> {
                final int rgb = PoleColors.resolveLine(entity.getWorld(), entity.getPos());
                yield rgb == 0 ? 0xFFFFFF : rgb;
            }
            default -> {
                final int rgb = PoleColors.resolve(entity.getWorld(), entity.getPos());
                yield rgb == 0 ? 0xFFFFFF : rgb;
            }
        };
    }

    /** 朝向 → 绕 Y 的旋转角度（模型默认朝北）。 */
    private static float[] rotationOf(final Direction facing) {
        return switch (facing) {
            case EAST -> new float[]{90.0F};
            case SOUTH -> new float[]{180.0F};
            case WEST -> new float[]{270.0F};
            default -> new float[]{0.0F};
        };
    }

    /** 关机贴图那一层（不缓存新 mesh，只在原 mesh 上换一张图）。 */
    private static RenderLayer cutoutLayerOf(final RawMesh mesh, final String texture) {
        if (mesh == null) {
            return RenderLayer.getSolid();
        }
        return RenderLayer.getEntityCutoutNoCull(new net.minecraft.util.Identifier(
                "mtrlte", texture + ".png"));
    }

    /** 光照用的是方块位置；屏幕自发光在上面单独处理。 */
    static int lightAt(final FuzhouMachineBlockEntity entity) {
        final BlockPos pos = entity.getPos();
        return WorldRenderer.getLightmapCoordinates(entity.getWorld(), pos);
    }
}
