// 柱子的客户端部分：染色 provider + 开颜色面板 + 收同步包 + ctrl+中键复制属性。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.ColorPickerScreen;
import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 柱底的客户端部分，四件事：
 *
 * <ol>
 *   <li><b>染色 provider</b>：{@code ColorProviderRegistry.BLOCK} 给 {@code amtr_pole_bottom} 挂一个
 *       {@code tintindex == 0} 的取色器 —— 模型里 {@code color} 那 8 个 element 的每个面都带
 *       {@code "tintindex": 0}，于是 MC 画它们的时候会来这里问颜色（其余 element 没 tintindex，
 *       永远按贴图原色画）。取色只读方块实体里缓存好的字段（{@link AmtrPoleBottomBlockEntity#effectiveColor}）。</li>
 *   <li><b>颜色面板</b>：收 {@code mtrlte:pole_color} 的 OPEN 包开 {@link ColorPickerScreen}
 *       （初值 = 服务端给的当前颜色）；「完成」把颜色发回服务端（{@link #sendSet}）。</li>
 *   <li><b>同步</b>：收同一个通道的 SYNC 包，把新状态写进<b>客户端这一份</b>方块实体 ——
 *       不写的话，界面点完「完成」颜色当场不变。</li>
 *   <li><b>ctrl+中键</b>：{@link #tryCopyProperties} 由 {@code MinecraftClientPickMixin}
 *       在 {@code MinecraftClient#doItemPick} 的 HEAD 调用；它返回 {@code true} 时这次「拾取方块」
 *       被取消，改成「把这一格的属性写进手里的柱底物品」。</li>
 * </ol>
 *
 * <p>由 {@code LtedClient} 调 {@link #register()}（与其它方块客户端模块一致）。
 * 每一步各自 try/catch：染色没挂上最多是那一圈不染色，通道没通最多是刷子右键不弹面板。
 */
public final class PoleClient implements net.fabricmc.api.ClientModInitializer {

    /** 颜色面板的标题（语言文件里的键）。 */
    private static final String LABEL_KEY = "gui.mtrlte.amtr_pole.color";

    private static boolean registered;

    @Override
    public void onInitializeClient() {
        register();
    }

    /** 客户端初始化时调一次。 */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PoleTint.register();
        registerRenderer();
        PoleProbe.register();
        registerReceiver();
    }

    // ------------------------------------------------------------------ 渲染

    /** 柱底几何由 BER 画（见 {@link PoleRawRenderer}），不走方块模型烘焙。 */
    private static void registerRenderer() {
        if (Boolean.getBoolean("lte.pole.baked")) { return; } // 默认 BER；-Dlte.pole.baked=true 时走方块模型（该路径在本方块上不渲染）
        try {
            final BlockEntityType<AmtrPoleBottomBlockEntity> type = PoleBlocks.bottomEntityType();
            if (type == null) {
                return;
            }
            BlockEntityRendererRegistry.register(type, PoleRawRenderer::new);
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole renderer unavailable", t);
        }
    }

    // ------------------------------------------------------------------ 通道

    private static void registerReceiver() {
        try {
            ClientPlayNetworking.registerGlobalReceiver(PoleNet.ID, (client, handler, buf, sender) -> {
                final byte kind;
                final BlockPos pos;
                final int mode;
                final int rgb;
                try {
                    kind = buf.readByte();
                    pos = buf.readBlockPos();
                    if (kind == PoleNet.KIND_OPEN || kind == PoleNet.KIND_SYNC) {
                        mode = buf.readInt();
                        rgb = buf.readInt();
                    } else {
                        return;
                    }
                } catch (final Throwable bad) {
                    return; // 坏包：什么也不做。
                }
                // 网络回调在 Netty 线程：开界面 / 改方块实体都必须回客户端主线程。
                client.execute(() -> {
                    if (kind == PoleNet.KIND_OPEN) {
                        openPicker(client, pos, rgb);
                    } else {
                        applySynced(client, pos, mode, rgb);
                    }
                });
            });
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole colour channel (client side) unavailable", t);
        }
    }

    /** 打开颜色面板：初值 = 服务端给的颜色，「完成」→ C2S 写回。 */
    private static void openPicker(final MinecraftClient client, final BlockPos pos, final int initial) {
        if (client == null || pos == null) {
            return;
        }
        final Screen parent = client.currentScreen;
        client.setScreen(new ColorPickerScreen(parent, Text.translatable(LABEL_KEY).getString(),
                initial & 0xFFFFFF, rgb -> sendSet(pos, rgb)));
    }

    /** 服务端推来的新状态 → 客户端这一份方块实体（随后染色 provider 立刻用上新颜色）。 */
    private static void applySynced(final MinecraftClient client, final BlockPos pos,
                                    final int mode, final int rgb) {
        final World world = client == null ? null : client.world;
        if (world == null || pos == null) {
            return;
        }
        if (world.getBlockEntity(pos) instanceof AmtrPoleBottomBlockEntity pole) {
            pole.applySynced(mode, rgb);
        } else {
            // 方块实体还没到（区块正在替换 / 刚好被拆）：区块数据里本来就带着状态，
            // 下一次刷新区块就会跟上，这里什么都不用补。
            LteLog.LOGGER.debug("[lte] pole sync for {} arrived but there is no block entity yet", pos);
        }
    }

    // ------------------------------------------------------------------ 发

    /** 把选好的颜色发回服务端（面板「完成」按钮）。 */
    public static void sendSet(final BlockPos pos, final int rgb) {
        try {
            if (!ClientPlayNetworking.canSend(PoleNet.ID)) {
                return;
            }
            ClientPlayNetworking.send(PoleNet.ID, PoleNet.setPacket(pos, rgb));
        } catch (final Throwable ignored) {
            // 发不出去就当没保存；界面照常关闭，柱子保持原来的颜色。
        }
    }

    /** 让服务端把这一格恢复成「跟随车站色」。 */
    public static void sendReset(final BlockPos pos) {
        try {
            if (!ClientPlayNetworking.canSend(PoleNet.ID)) {
                return;
            }
            ClientPlayNetworking.send(PoleNet.ID, PoleNet.resetPacket(pos));
        } catch (final Throwable ignored) {
            // 同上。
        }
    }

    /** 让服务端把这一格的属性写进手里的柱底物品（ctrl+中键）。 */
    public static void sendCopy(final BlockPos pos) {
        try {
            if (!ClientPlayNetworking.canSend(PoleNet.ID)) {
                return;
            }
            ClientPlayNetworking.send(PoleNet.ID, PoleNet.copyPacket(pos));
        } catch (final Throwable ignored) {
            // 发不出去就当没复制；原版拾取已经被取消，不会误捡一根白柱子。
        }
    }

    // ------------------------------------------------------------------ ctrl+中键

    /**
     * ctrl + 中键：把准星指着的那根柱底的属性复制进<b>手里的柱底物品</b>。
     *
     * <p>由 {@code MinecraftClientPickMixin} 在 {@code doItemPick()} 的 HEAD 调用。返回 {@code true}
     * 表示「这次拾取我处理了」，mixin 随即取消原版行为。
     *
     * <h2>为什么只发一个包、不改本地 NBT</h2>
     * <p>物品堆的 NBT 是<b>服务端权威</b>的（放下时 {@link AmtrPoleBottomItem#place} 读的是服务端
     * 那一份）。客户端自己改本地那堆的 NBT 只会「看起来复制成功」，落地时服务端照样按旧的写 ——
     * 所以这里只发 {@link PoleNet#KIND_COPY}，服务端写完再刷一次容器把槽位同步回来。
     *
     * <h2>为什么不满足条件就原样放行</h2>
     * <ul>
     *   <li>没按 ctrl → 原版拾取（「不按 ctrl 的中键保持原版行为」）；</li>
     *   <li>准星不是柱底 → 原版拾取；</li>
     *   <li>手里拿的不是柱底物品 → 原版拾取（复制进哪儿？没有目标就别拦）。</li>
     * </ul>
     */
    public static boolean tryCopyProperties(final MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return false;
        }
        if (!Screen.hasControlDown()) {
            return false;
        }
        final HitResult target = client.crosshairTarget;
        if (!(target instanceof BlockHitResult hit) || target.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        final BlockPos pos = hit.getBlockPos();
        if (!(client.world.getBlockEntity(pos) instanceof AmtrPoleBottomBlockEntity)) {
            return false;
        }
        final ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty() || !(held.getItem() instanceof AmtrPoleBottomItem)) {
            return false;
        }
        sendCopy(pos);
        return true;
    }
}
