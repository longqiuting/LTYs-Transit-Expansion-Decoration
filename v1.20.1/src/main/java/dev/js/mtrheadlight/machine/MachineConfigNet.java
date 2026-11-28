// 售票机配置面板的网络层（1.20.1 的 PacketByteBuf 通道）：S2C 打开、C2S 保存。
package dev.js.mtrheadlight.machine;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.machine.XiamenTicketMachineBlock;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * 所有售票机共用的配置面板协议。
 *
 * <p>面板只认「方块坐标」：服务端打开时按坐标读出这台机器的配置和能力，保存时按坐标写回去。
 * 支持哪些行由方块自己决定 —— 实现了 {@link MachineConfigurable} 的（福州那台）三项全开；
 * 老款（厦门 / 港铁）没有方块实体、没有颜色区和 LED，只有「售票机模式」一行能点，其余置灰。
 */
public final class MachineConfigNet {

    public static final Identifier CHANNEL_OPEN = new Identifier("mtrlte", "machine_config_open");
    public static final Identifier CHANNEL_SAVE = new Identifier("mtrlte", "machine_config_save");

    private static boolean registered;

    private MachineConfigNet() {
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
                        server.execute(() -> apply(player, pos, new MachineConfig(mode, source, custom,
                                line, text, scroll, 0)));
                    });
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] machine config channel unavailable", t);
        }
    }

    /** 这台方块支持哪些配置项（0 = 不是售票机，面板不打开）。 */
    public static int capabilitiesOf(final BlockState state) {
        if (state.getBlock() instanceof XiamenTicketMachineBlock) {
            return MachineConfig.CAP_MODE;
        }
        return 0;
    }

    /** 打开面板（服务端调用）。 */
    public static void open(final ServerPlayerEntity player, final BlockPos pos) {
        final BlockState state = player.getWorld().getBlockState(pos);
        try {
            final BlockEntity entity = player.getWorld().getBlockEntity(pos);
            final MachineConfig config;
            if (entity instanceof final MachineConfigurable configurable) {
                config = configurable.machineConfig();
            } else if (state.getBlock() instanceof final XiamenTicketMachineBlock machine) {
                config = new MachineConfig(machine.uiOf(state).ordinal(), 0, 0xFFFFFF, 0xFFFFFF,
                        MachineConfig.DEFAULT_TEXT, true, MachineConfig.CAP_MODE);
            } else {
                return;
            }
            final PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(pos);
            buf.writeVarInt(config.mode());
            buf.writeVarInt(config.colorSource());
            buf.writeInt(config.custom());
            buf.writeInt(config.line());
            buf.writeString(config.text(), 128);
            buf.writeBoolean(config.scroll());
            buf.writeVarInt(config.caps());
            ServerPlayNetworking.send(player, CHANNEL_OPEN, buf);
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] machine config panel could not open at {}", pos, t);
        }
    }

    private static void apply(final ServerPlayerEntity player, final BlockPos pos,
                              final MachineConfig config) {
        final BlockState state = player.getWorld().getBlockState(pos);
        final BlockEntity entity = player.getWorld().getBlockEntity(pos);
        if (entity instanceof final MachineConfigurable configurable) {
            configurable.applyMachineConfig(config.normalized());
            return;
        }
        if (state.getBlock() instanceof final XiamenTicketMachineBlock machine) {
            final int mode = config.normalized().mode();
            final XiamenTicketMachineBlock.TicketUi[] values = XiamenTicketMachineBlock.TicketUi.values();
            final BlockState target = state.with(XiamenTicketMachineBlock.UI, values[mode]);
            player.getWorld().setBlockState(pos, target, 3);
            // 上下一体的机器：模式写一半就够，另一半用同一个属性，渲染时读任意一半都一致
            final BlockPos other = pos.up();
            final BlockState otherState = player.getWorld().getBlockState(other);
            if (otherState.getBlock() == state.getBlock() && otherState.contains(XiamenTicketMachineBlock.UI)) {
                player.getWorld().setBlockState(other, otherState.with(XiamenTicketMachineBlock.UI, values[mode]), 3);
            }
        }
    }
}
