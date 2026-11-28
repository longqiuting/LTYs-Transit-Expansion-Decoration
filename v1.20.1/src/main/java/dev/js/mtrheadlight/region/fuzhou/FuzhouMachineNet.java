// 福州地铁售票机的配置通道（下一步接面板）。现在只留接口，先保证渲染能跑起来。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/** 配置面板的网络层。 */
public final class FuzhouMachineNet {

    private FuzhouMachineNet() {
    }

    /** 打开配置面板（服务端调用）。 */
    public static void sendOpen(final ServerPlayerEntity player, final BlockPos pos,
                                final FuzhouMachineBlockEntity machine) {
        // TODO 面板：下一轮接 FuzhouMachineConfigScreen
        LteLog.LOGGER.info("[lte] fuzhou machine config requested at {} (mode={}, source={})",
                pos, machine.mode(), machine.colorSource());
    }
}
