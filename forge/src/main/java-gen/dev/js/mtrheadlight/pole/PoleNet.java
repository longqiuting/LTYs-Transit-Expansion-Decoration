package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 柱子颜色通道 {@code mtrlte:pole_color}（Forge / mojmap 版）。
 *
 * <p>包体与 Fabric 侧逐字节一致（kind 字节 + BlockPos + int），传输走 {@link Net} 这个
 * SimpleChannel shim：{@code ServerPlayNetworking} → {@link Net#registerServer} /
 * {@link Net#send}，{@code ClientPlayNetworking} → {@link Net#registerClient} /
 * {@link Net#sendToServer}。
 */
public final class PoleNet {

    public static final ResourceLocation ID = new ResourceLocation("mtrlte", "pole_color");

    public static final byte KIND_OPEN = 0;
    public static final byte KIND_SET = 1;
    public static final byte KIND_RESET = 2;
    public static final byte KIND_SYNC = 3;
    public static final byte KIND_COPY = 4;

    private static final int MAX_DISTANCE = 64;

    private static boolean registered;

    private PoleNet() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        try {
            Net.registerServer(ID, (server, player, buf) -> {
                byte kind;
                BlockPos pos;
                int rgb;
                try {
                    kind = buf.readByte();
                    pos = buf.readBlockPos();
                    rgb = kind == KIND_SET ? buf.readInt() : 0;
                } catch (Throwable bad) {
                    return;
                }
                server.execute(() -> {
                    if (kind == KIND_SET) {
                        applySet(player, pos, rgb);
                    } else if (kind == KIND_RESET) {
                        applyReset(player, pos);
                    } else if (kind == KIND_COPY) {
                        applyCopy(player, pos);
                    }
                });
            });
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] pole colour channel unavailable", t);
        }
    }

    public static void sendOpen(ServerPlayer player, BlockPos pos, AmtrPoleBottomBlockEntity pole) {
        if (player == null || pos == null || pole == null || !Net.canSend(player, ID)) {
            return;
        }
        FriendlyByteBuf buf = Net.buf();
        buf.writeByte(KIND_OPEN);
        buf.writeBlockPos(pos);
        buf.writeInt(pole.isCustomMode() ? AmtrPoleBottomBlockEntity.MODE_CUSTOM
                : AmtrPoleBottomBlockEntity.MODE_AUTO);
        buf.writeInt(pole.effectiveColor());
        Net.send(player, ID, buf);
    }

    public static void sendSync(Level world, BlockPos pos, int mode, int rgb) {
        if (!(world instanceof ServerLevel serverLevel) || pos == null) {
            return;
        }
        for (ServerPlayer player : Net.tracking(serverLevel, pos)) {
            if (Net.canSend(player, ID)) {
                Net.send(player, ID, syncPacket(pos, mode, rgb));
            }
        }
    }

    public static FriendlyByteBuf syncPacket(BlockPos pos, int mode, int rgb) {
        FriendlyByteBuf buf = Net.buf();
        buf.writeByte(KIND_SYNC);
        buf.writeBlockPos(pos);
        buf.writeInt(mode);
        buf.writeInt(rgb & 0xFFFFFF);
        return buf;
    }

    public static FriendlyByteBuf setPacket(BlockPos pos, int rgb) {
        FriendlyByteBuf buf = Net.buf();
        buf.writeByte(KIND_SET);
        buf.writeBlockPos(pos);
        buf.writeInt(rgb & 0xFFFFFF);
        return buf;
    }

    public static FriendlyByteBuf resetPacket(BlockPos pos) {
        FriendlyByteBuf buf = Net.buf();
        buf.writeByte(KIND_RESET);
        buf.writeBlockPos(pos);
        return buf;
    }

    public static FriendlyByteBuf copyPacket(BlockPos pos) {
        FriendlyByteBuf buf = Net.buf();
        buf.writeByte(KIND_COPY);
        buf.writeBlockPos(pos);
        return buf;
    }

    private static void applySet(ServerPlayer player, BlockPos pos, int rgb) {
        AmtrPoleBottomBlockEntity pole = resolve(player, pos);
        if (pole == null) {
            return;
        }
        pole.setCustomColor(rgb & 0xFFFFFF);
        LteLog.LOGGER.debug("[lte] pole at {} coloured #{}", pos,
                String.format("%06X", Integer.valueOf(rgb & 0xFFFFFF)));
    }

    private static void applyReset(ServerPlayer player, BlockPos pos) {
        AmtrPoleBottomBlockEntity pole = resolve(player, pos);
        if (pole == null) {
            return;
        }
        pole.resetToStationColor();
    }

    /** 服务端：ctrl+中键复制属性 → 写进主手那堆柱底物品的 NBT，然后刷一次容器。 */
    private static void applyCopy(ServerPlayer player, BlockPos pos) {
        AmtrPoleBottomBlockEntity pole = resolve(player, pos);
        if (pole == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof AmtrPoleBottomItem)) {
            return;
        }
        PoleItemData.write(held, pole.isCustomMode(), pole.customColor());
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        String value = pole.isCustomMode()
                ? "#" + String.format("%06X", Integer.valueOf(pole.customColor()))
                : Component.translatable("message.mtrlte.amtr_pole.auto").getString();
        player.displayClientMessage(Component.translatable("message.mtrlte.amtr_pole.copied", value), true);
        LteLog.LOGGER.debug("[lte] pole at {} copied to {}'s hand", pos, player.getName().getString());
    }

    private static AmtrPoleBottomBlockEntity resolve(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) {
            return null;
        }
        Level world = player.level();
        if (world == null || !world.hasChunkAt(pos)) {
            return null;
        }
        if (player.blockPosition().distSqr(pos) > (double) MAX_DISTANCE * MAX_DISTANCE) {
            return null;
        }
        if (!(world.getBlockState(pos).getBlock() instanceof AmtrPoleBottomBlock)) {
            return null;
        }
        return world.getBlockEntity(pos) instanceof AmtrPoleBottomBlockEntity pole ? pole : null;
    }
}
