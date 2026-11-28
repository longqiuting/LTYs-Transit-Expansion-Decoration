// 售票机配置面板的网络层（1.20.1 的 PacketByteBuf 通道）：S2C 打开、C2S 保存。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** 面板协议：打开（带当前配置）与保存（回写方块实体）。 */
public final class FuzhouMachineNet {

    public static final Identifier CHANNEL_OPEN = new Identifier("mtrlte", "fuzhou_machine_open");
    public static final Identifier CHANNEL_SAVE = new Identifier("mtrlte", "fuzhou_machine_save");

    private static boolean registered;

    private FuzhouMachineNet() {
    }

    /** 两端各调一次（幂等）。 */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        try {
            ServerPlayNetworking.registerGlobalReceiver(CHANNEL_SAVE,
                    (server, player, handler, buf, responseSender) -> {
                        final BlockPos pos = buf.readBlockPos();
                        final int mode = buf.readVarInt();
                        final int source = buf.readVarInt();
                        final int custom = buf.readInt();
                        final int line = buf.readInt();
                        final String text = buf.readString(128);
                        final boolean scroll = buf.readBoolean();
                        server.execute(() -> apply(player, pos, mode, source, custom, line, text, scroll));
                    });
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] fuzhou machine config channel unavailable", t);
        }
    }

    private static void apply(final ServerPlayerEntity player, final BlockPos pos, final int mode,
                              final int source, final int custom, final int line,
                              final String text, final boolean scroll) {
        if (!(player.getWorld().getBlockEntity(pos) instanceof final FuzhouMachineBlockEntity machine)) {
            return;
        }
        machine.applyConfig(mode, source, custom, line, text, scroll);
    }

    /** 打开面板（服务端调用）。 */
    public static void sendOpen(final ServerPlayerEntity player, final BlockPos pos,
                                final FuzhouMachineBlockEntity machine) {
        try {
            final PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(pos);
            buf.writeVarInt(machine.mode());
            buf.writeVarInt(machine.colorSource());
            buf.writeInt(machine.customColor());
            buf.writeInt(machine.lineColor());
            buf.writeString(machine.ledText(), 128);
            buf.writeBoolean(machine.ledScroll());
            ServerPlayNetworking.send(player, CHANNEL_OPEN, buf);
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] fuzhou machine could not open its config panel", t);
        }
    }
}
