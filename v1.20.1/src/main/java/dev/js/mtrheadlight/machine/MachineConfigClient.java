// 售票机配置面板的客户端接线：收「打开面板」的包、把线路色列给面板用。
package dev.js.mtrheadlight.machine;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 面板的客户端入口（幂等，任何客户端初始化阶段都能调）。 */
public final class MachineConfigClient {

    private static boolean initialised;

    private MachineConfigClient() {
    }

    public static void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        try {
            ClientPlayNetworking.registerGlobalReceiver(MachineConfigNet.CHANNEL_OPEN,
                    (client, handler, buf, responseSender) -> {
                        final BlockPos pos = buf.readBlockPos();
                        final int mode = buf.readVarInt();
                        final int source = buf.readVarInt();
                        final int custom = buf.readInt();
                        final int line = buf.readInt();
                        final String text = buf.readString(128);
                        final boolean scroll = buf.readBoolean();
                        final int caps = buf.readVarInt();
                        final MachineConfig config = new MachineConfig(mode, source, custom, line,
                                text, scroll, caps);
                        client.execute(() -> client.setScreen(
                                new MachineConfigScreen(pos, config, lineColors())));
                    });
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] machine config panel unavailable on the client", t);
        }
    }

    /** 世界里所有线路的颜色（去重，最多 8 个）——面板上「线路色」那排色块就是它。 */
    private static List<Integer> lineColors() {
        final Set<Integer> colors = new LinkedHashSet<>();
        try {
            for (final mtr.data.Route route : mtr.client.ClientData.ROUTES) {
                final int rgb = route.color & 0xFFFFFF;
                if (rgb != 0) {
                    colors.add(rgb);
                }
                if (colors.size() >= 8) {
                    break;
                }
            }
        } catch (final Throwable ignored) {
            // 没装 MTR 或数据还没同步：面板上线路色块就是空的
        }
        return new ArrayList<>(colors);
    }
}
