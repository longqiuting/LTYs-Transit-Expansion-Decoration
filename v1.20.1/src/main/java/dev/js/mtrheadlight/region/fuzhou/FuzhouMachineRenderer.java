// 福州地铁售票机的渲染：本体（rawmodels）+ 两个 color 组（纯色）+ LED 走字屏。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.pole.PoleColors;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * 售票机渲染：本体用 rawmodel 画（素材不是原版模型格式），两个 {@code color} 组和 LED 屏再各画一层。
 *
 * <p>关机模式（{@code MODE_OFF}）用 {@code fuzhou_ticket_machine_off} 贴图；方速 / 原版外观一致。
 * 面板颜色由数据源决定（车站色 / 线路色 / 玩家自选），LED 文字来自方块实体。
 */
public final class FuzhouMachineRenderer implements BlockEntityRenderer<FuzhouMachineBlockEntity> {

    private static final String OFF_TEXTURE = "textures/block/fuzhou_ticket_machine_off";
    /** 滚动速度（每秒多少个字宽）与两段之间的空白间隔（字宽倍数）。 */
    private static final float SCROLL_SPEED = 0.012F;
    private static final float SCROLL_GAP = 0.6F;

    private static final Identifier FLAT_WHITE = new Identifier("mtrlte", "textures/block/flat_white.png");

    public FuzhouMachineRenderer(final BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(final FuzhouMachineBlockEntity entity, final float tickDelta,
                       final MatrixStack matrices, final VertexConsumerProvider vertexConsumers,
                       final int light, final int overlay) {
        final var world = entity.getWorld();
        if (world == null || !(entity.getCachedState().getBlock() instanceof final FuzhouMachineBlock block)) {
            return;
        }
        final FuzhouMachineBlockEntity config = configOf(entity);
        final boolean upper = FuzhouMachineBlock.isUpper(entity.getCachedState());
        try {
            matrices.push();
            matrices.translate(0.5D, 0.0D, 0.5D);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y
                    .rotationDegrees(rotationOf(entity.getCachedState().get(FuzhouMachineBlock.FACING))));
            matrices.translate(-0.5D, 0.0D, -0.5D);

            final boolean off = config.mode() == FuzhouMachineBlockEntity.MODE_OFF;

            // 1) 本体
            final RawMesh body = RawModelJson.get(RawModelJson.id(block.rawModel(entity.getCachedState())));
            if (body != null) {
                final VertexConsumer consumer = vertexConsumers.getBuffer(
                        off ? cutoutLayerOf(OFF_TEXTURE) : RawModelRenderer.cutoutLayer(body));
                RawModelRenderer.render(matrices, consumer, body, light, OverlayTexture.DEFAULT_UV);
            }

            // 2) 面板颜色区（纯顶点色；关机时不画）
            if (!off) {
                final int rgb = resolveColor(config);
                drawFlat(matrices, vertexConsumers,
                        RawModelJson.get(RawModelJson.id(block.colorModel(entity.getCachedState()))),
                        light,
                        ((rgb >> 16) & 0xFF) / 255.0F,
                        ((rgb >> 8) & 0xFF) / 255.0F,
                        (rgb & 0xFF) / 255.0F);
            }

            // 3) LED 走字屏（只有上半段有屏）
            if (!off && upper) {
                drawLed(matrices, vertexConsumers, config);
            }

            matrices.pop();
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] fuzhou machine render skipped", t);
        }
    }

    /** 配置统一存在上半段的方块实体里；下段渲染时去上段取。 */
    private static FuzhouMachineBlockEntity configOf(final FuzhouMachineBlockEntity entity) {
        if (FuzhouMachineBlock.isUpper(entity.getCachedState()) || entity.getWorld() == null) {
            return entity;
        }
        final var up = entity.getWorld().getBlockEntity(entity.getPos().up());
        return up instanceof final FuzhouMachineBlockEntity upper ? upper : entity;
    }

    /** 面板颜色：车站色 / 线路色 / 玩家自选。 */
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

    /**
     * LED 屏：点阵贴图贴在 screen 那一面（x 1..15、y 13..15、z=9），滚动时 UV 横向跑，
     * 越界就再画一片接上，做到无缝循环。
     */
    private static void drawLed(final MatrixStack matrices, final VertexConsumerProvider vertexConsumers,
                                final FuzhouMachineBlockEntity config) {
        final FuzhouLedTextures.Entry led = FuzhouLedTextures.get(config.ledText());
        if (led == null) {
            return;
        }
        final VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(led.id));
        final MatrixStack.Entry entry = matrices.peek();
        final float x1 = 1.0F / 16.0F;
        final float x2 = 15.0F / 16.0F;
        final float y0 = 13.0F / 16.0F;
        final float y1 = 15.0F / 16.0F;
        // 屏在模型的 +Z（南）那一面：模型默认朝南，facing=north 时正面朝玩家
        final float z = 9.8F / 16.0F + 0.002F;
        final float screenAspect = (x2 - x1) / (y1 - y0);
        final float span = Math.min(1.0F, screenAspect / led.aspect);

        if (!config.ledScroll()) {
            // 固定：居中整段不滚
            final float u0 = (1.0F - span) * 0.5F;
            ledQuad(entry, consumer, x1, x2, y0, y1, z, u0, u0 + span);
            return;
        }
        // 滚动（和 PIDS 一样）：整段从右往左走完、再空一段，才轮到下一段，绝不重叠
        final long time = config.getWorld() == null ? 0L : config.getWorld().getTime();
        final float cycle = 1.0F + SCROLL_GAP;
        final float pos = (time * SCROLL_SPEED) % cycle;
        if (pos >= 1.0F) {
            return; // 空白间隔
        }
        final float u1 = Math.min(1.0F, pos + span);
        final float covered = (u1 - pos) / span; // 屏幕上还有多少比例有字
        final float xe = x1 + (x2 - x1) * covered;
        ledQuad(entry, consumer, x1, xe, y0, y1, z, pos, u1);
    }

    private static void ledQuad(final MatrixStack.Entry entry, final VertexConsumer consumer,
                                final float x1, final float x2, final float y0, final float y1,
                                final float z, final float u0, final float u1) {
        // 屏是自发光：直接满亮，夜里也看得清
        final int light = net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE;
        consumer.vertex(entry.getPositionMatrix(), x1, y0, z)
                .color(255, 255, 255, 255).texture(u0, 1.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light)
                .normal(entry.getNormalMatrix(), 0.0F, 0.0F, 1.0F).next();
        consumer.vertex(entry.getPositionMatrix(), x2, y0, z)
                .color(255, 255, 255, 255).texture(u1, 1.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light)
                .normal(entry.getNormalMatrix(), 0.0F, 0.0F, -1.0F).next();
        consumer.vertex(entry.getPositionMatrix(), x2, y1, z)
                .color(255, 255, 255, 255).texture(u1, 0.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light)
                .normal(entry.getNormalMatrix(), 0.0F, 0.0F, -1.0F).next();
        consumer.vertex(entry.getPositionMatrix(), x1, y1, z)
                .color(255, 255, 255, 255).texture(u0, 0.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light)
                .normal(entry.getNormalMatrix(), 0.0F, 0.0F, -1.0F).next();
    }

    /** 纯色块：不走贴图（否则会采样到图集里的像素，浅色块里掺黑），用 1×1 纯白贴图 + 顶点色。 */
    private static void drawFlat(final MatrixStack matrices, final VertexConsumerProvider vertexConsumers,
                                 final RawMesh mesh, final int light,
                                 final float r, final float g, final float b) {
        if (mesh == null) {
            return;
        }
        final VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(FLAT_WHITE));
        final MatrixStack.Entry entry = matrices.peek();
        for (final RawMesh.Quad quad : mesh.quads()) {
            final float shade = quad.fullBright() ? 1.0F : quad.shade();
            final float[][] pos = quad.pos();
            final float[] n = quad.normal();
            for (int i = 0; i < 4; i++) {
                consumer.vertex(entry.getPositionMatrix(),
                                pos[i][0] / 16.0F, pos[i][1] / 16.0F, pos[i][2] / 16.0F)
                        .color(shade * r, shade * g, shade * b, 1.0F)
                        .texture(0.5F, 0.5F)
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(light)
                        .normal(entry.getNormalMatrix(), n[0], n[1], n[2])
                        .next();
            }
        }
    }

    /** 朝向 → 绕 Y 的旋转角度（模型默认朝北）。 */
    private static float rotationOf(final net.minecraft.util.math.Direction facing) {
        return switch (facing) {
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    /** 关机贴图那一层。 */
    private static RenderLayer cutoutLayerOf(final String texture) {
        return RenderLayer.getEntityCutoutNoCull(new Identifier("mtrlte", texture + ".png"));
    }
}
