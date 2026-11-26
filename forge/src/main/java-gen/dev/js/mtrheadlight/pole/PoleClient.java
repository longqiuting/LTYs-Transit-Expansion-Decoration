package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.ColorPickerScreen;
import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 柱底的客户端部分（Forge / mojmap 版）：收同步包 + 开颜色面板 + ctrl+中键复制属性。
 *
 * <p>与 Fabric 侧 {@code pole/PoleClient.java} 行为一致。两个平台差异：
 * <ul>
 *   <li>染色 provider 的注册在 {@code PoleTint}（Forge 是 {@code RegisterColorHandlersEvent.Block}
 *       事件，不是 {@code ColorProviderRegistry}）；</li>
 *   <li>ctrl+中键没有 mixin：Forge 的 {@code InputEvent.InteractionKeyMappingTriggered} 在
 *       {@code pickBlock()} 之前触发、可以取消，见 {@code dev.js.mtrheadlight.forge.client.PoleForgeClient}。</li>
 * </ul>
 */
public final class PoleClient implements ClientInit {

    private static final String LABEL_KEY = "gui.mtrlte.amtr_pole.color";

    private static boolean registered;

    @Override
    public void onInitializeClient() {
        register();
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        registerReceiver();
    }

    private static void registerReceiver() {
        try {
            Net.registerClient(PoleNet.ID, (client, buf) -> {
                byte kind;
                BlockPos pos;
                int mode;
                int rgb;
                try {
                    kind = buf.readByte();
                    pos = buf.readBlockPos();
                    if (kind == PoleNet.KIND_OPEN || kind == PoleNet.KIND_SYNC) {
                        mode = buf.readInt();
                        rgb = buf.readInt();
                    } else {
                        return;
                    }
                } catch (Throwable bad) {
                    return;
                }
                client.execute(() -> {
                    if (kind == PoleNet.KIND_OPEN) {
                        openPicker(client, pos, rgb);
                    } else {
                        applySynced(client, pos, mode, rgb);
                    }
                });
            });
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] pole colour channel (client side) unavailable", t);
        }
    }

    private static void openPicker(Minecraft client, BlockPos pos, int initial) {
        if (client == null || pos == null) {
            return;
        }
        Screen parent = client.screen;
        client.setScreen(new ColorPickerScreen(parent, Component.translatable(LABEL_KEY).getString(),
                initial & 0xFFFFFF, rgb -> sendSet(pos, rgb)));
    }

    private static void applySynced(Minecraft client, BlockPos pos, int mode, int rgb) {
        Level level = client == null ? null : client.level;
        if (level == null || pos == null) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof AmtrPoleBottomBlockEntity pole) {
            pole.applySynced(mode, rgb);
        } else {
            LteLog.LOGGER.debug("[lte] pole sync for {} arrived but there is no block entity yet", pos);
        }
    }

    public static void sendSet(BlockPos pos, int rgb) {
        try {
            if (!Net.canSend(PoleNet.ID)) {
                return;
            }
            Net.sendToServer(PoleNet.ID, PoleNet.setPacket(pos, rgb));
        } catch (Throwable ignored) {
        }
    }

    public static void sendReset(BlockPos pos) {
        try {
            if (!Net.canSend(PoleNet.ID)) {
                return;
            }
            Net.sendToServer(PoleNet.ID, PoleNet.resetPacket(pos));
        } catch (Throwable ignored) {
        }
    }

    /** 让服务端把这一格的属性写进手里的柱底物品（ctrl+中键）。 */
    public static void sendCopy(BlockPos pos) {
        try {
            if (!Net.canSend(PoleNet.ID)) {
                return;
            }
            Net.sendToServer(PoleNet.ID, PoleNet.copyPacket(pos));
        } catch (Throwable ignored) {
        }
    }

    /**
     * ctrl + 中键：把准星指着的那根柱底的属性复制进手里的柱底物品。
     *
     * <p>只发 {@code PoleNet.KIND_COPY}（物品 NBT 是服务端权威的，客户端改本地那份不算数）；
     * 服务端写完刷一次容器，槽位里的 tooltip 随之更新。
     *
     * @return {@code true} = 这次拾取被接管（调用方应当取消原版行为）
     */
    public static boolean tryCopyProperties(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return false;
        }
        if (!Screen.hasControlDown()) {
            return false;
        }
        HitResult target = client.hitResult;
        if (!(target instanceof BlockHitResult hit) || target.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(client.level.getBlockEntity(pos) instanceof AmtrPoleBottomBlockEntity)) {
            return false;
        }
        Player player = client.player;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !(held.getItem() instanceof AmtrPoleBottomItem)) {
            return false;
        }
        sendCopy(pos);
        return true;
    }
}
