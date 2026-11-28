// 终点站指示灯发光层：整个方块按终点站色满亮，进站前 10 格淡入、列车离开站台后淡出。
package dev.js.mtrheadlight.region.hk;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import dev.js.mtrheadlight.rawmodel.RawModelRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 终点站指示灯的渲染：把素材那份模型再用<b>满亮 + 终点站色</b>画一层。
 *
 * <h2>为什么整块都变色发光</h2>
 * 按用户的要求「整个方块都改变颜色发光」——不是只亮那个 45° 斜面。做法是把同一份模型
 * （外露的面都在）取 lightmap 满亮、顶点色乘上终点站色。
 *
 * <h2>这一层怎么解决「和本体贴图打架」</h2>
 * <ul>
 *   <li>整份网格放大 {@value #OUTSET}（绕方块中心），把发光面推到灯体外面几毫米 ——
 *       不推开就是 z-fighting，画面上表现为一层黑边噪点；</li>
 *   <li>发光贴图用纯白，颜色完全由顶点色给，所以不会有素材贴图里的深色像素被染成黑斑；</li>
 *   <li>半透明图层 + 顶点 alpha：{@value #FADE_IN_SECONDS} 秒淡入、{@value #FADE_OUT_SECONDS} 秒淡出。</li>
 * </ul>
 *
 * <h2>旋转为什么取负</h2>
 * 方块模型的 {@code y} 旋转和 {@link RotationAxis#POSITIVE_Y} 的方向是相反的 —— 直接用
 * {@code +90} 会得到镜像位置（灯带跑到模型另一侧），所以这里一律取负。
 */
public final class HkDestinationLightRenderer implements BlockEntityRenderer<HkDestinationLightBlockEntity> {

    /** 发光层相对本体外扩的比例（绕方块中心缩放，约 0.6mm / 格）。 */
    private static final float OUTSET = 1.004F;
    /** 淡入时长（秒）。 */
    private static final float FADE_IN_SECONDS = 0.45F;
    /** 淡出时长（秒）。 */
    private static final float FADE_OUT_SECONDS = 0.8F;
    /** 状态表上限，超了整表清空（灯带很多也不会一直涨）。 */
    private static final int LIMIT = 2048;

    /** 每个方块的淡入淡出状态。 */
    private static final class Fade {
        private float alpha;
        private int rgb;
        private long lastMs;
    }

    private static final Map<Long, Fade> FADES = new ConcurrentHashMap<>();
    /** 发光层的半透明 RenderLayer，按贴图缓存（每帧新建会往缓冲区表里塞新键）。 */
    private static final Map<Identifier, RenderLayer> TRANSLUCENT = new ConcurrentHashMap<>();

    @Override
    public void render(final HkDestinationLightBlockEntity entity, final float tickDelta,
                       final MatrixStack matrices, final VertexConsumerProvider vertexConsumers,
                       final int light, final int overlay) {
        if (entity.getWorld() == null) {
            return;
        }
        final long key = entity.getPos().asLong();
        final int target = DestinationColors.colorAt(entity.getWorld(), entity.getPos());
        final float alpha = advance(key, target);
        if (alpha <= 0.01F) {
            return;
        }

        final boolean full = entity.getCachedState().getBlock() instanceof HkDestinationLightFullBlock;
        final RawMesh mesh = RawModelJson.get(RawModelJson.id(full
                ? "hk_destination_light_full" : "hk_destination_light"));
        if (mesh == null) {
            return;
        }

        matrices.push();
        // 绕方块中心外扩一点：发光面离开灯体表面，避免深度冲突
        matrices.translate(0.5D, 0.5D, 0.5D);
        matrices.scale(OUTSET, OUTSET, OUTSET);
        // 方块模型的 y 旋转与 POSITIVE_Y 反向，这里取负才对得上
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw(entity.getCachedState())));
        matrices.translate(-0.5D, -0.5D, -0.5D);

        final VertexConsumer consumer = vertexConsumers.getBuffer(
                TRANSLUCENT.computeIfAbsent(mesh.texture(), RenderLayer::getEntityTranslucent));
        final int rgb = FADES.get(key) == null ? target : FADES.get(key).rgb;
        final float r = ((rgb >> 16) & 0xFF) / 255.0F;
        final float g = ((rgb >> 8) & 0xFF) / 255.0F;
        final float b = (rgb & 0xFF) / 255.0F;
        RawModelRenderer.render(matrices, consumer, mesh, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV, true, r, g, b, alpha);
        matrices.pop();
    }

    /**
     * 推进这个方块的淡入淡出，返回当前 alpha。
     *
     * <p>目标色为 0 = 该灭：alpha 往下走；目标色非 0 = 亮：alpha 往上走，并记住颜色。
     * 前一列车刚解除占用、后一列车已经在等的时候，目标色直接从 0 之外换到新颜色，
     * alpha 保持 1 —— 也就是「不灭灯直接换色」。
     */
    private static float advance(final long key, final int target) {
        final long now = System.currentTimeMillis();
        final Fade fade = FADES.computeIfAbsent(key, k -> {
            final Fade created = new Fade();
            created.lastMs = now;
            return created;
        });
        final float dt = Math.min(0.25F, Math.max(0.0F, (now - fade.lastMs) / 1000.0F));
        fade.lastMs = now;

        if (target != 0) {
            fade.rgb = target;
            fade.alpha = Math.min(1.0F, fade.alpha + dt / FADE_IN_SECONDS);
        } else {
            fade.alpha = Math.max(0.0F, fade.alpha - dt / FADE_OUT_SECONDS);
        }
        if (FADES.size() > LIMIT) {
            FADES.clear();
        }
        return fade.alpha;
    }

    /** 断线 / 换存档：状态整表清空。 */
    public static void clear() {
        FADES.clear();
    }

    private static float yaw(final BlockState state) {
        // 满方块款没有 facing 属性 —— 直接读会抛 IllegalArgumentException（放置即崩）
        if (!state.contains(HkDestinationLightBlock.FACING)) {
            return 0.0F;
        }
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
    public boolean isInRenderDistance(final HkDestinationLightBlockEntity entity,
                                      final net.minecraft.util.math.Vec3d cameraPos) {
        return entity.getPos().isWithinDistance(cameraPos, 128.0D);
    }
}
