// 福州地铁售票机的客户端接线：注册渲染器 + 收「打开配置面板」的包。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 售票机渲染器 + 配置面板的客户端入口。 */
public final class FuzhouMachinesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        try {
            final var type = FuzhouMachines.entityType();
            if (type != null) {
                BlockEntityRendererRegistry.register(type, FuzhouMachineRenderer::new);
                LteLog.LOGGER.info("[lte] fuzhou machine renderer registered");
            }
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] fuzhou machine renderer unavailable", t);
        }
        dev.js.mtrheadlight.machine.MachineConfigClient.init();
    }

    /** 该世界里所有线路的颜色（去重，最多 8 个）——面板上的「线路色」色块就是它。 */
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
            // 没装 MTR / 数据还没同步：面板上线路色块就是空的
        }
        return new ArrayList<>(colors);
    }
}
