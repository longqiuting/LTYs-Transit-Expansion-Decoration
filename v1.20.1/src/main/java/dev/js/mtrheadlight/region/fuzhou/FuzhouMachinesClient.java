// 福州地铁售票机的客户端接线：注册渲染器。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

/** 售票机渲染器注册（本体 + color 组 + LED 屏都在 {@link FuzhouMachineRenderer} 里画）。 */
public final class FuzhouMachinesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        try {
            final var type = FuzhouMachines.entityType();
            if (type == null) {
                return;
            }
            BlockEntityRendererRegistry.register(type, FuzhouMachineRenderer::new);
            LteLog.LOGGER.info("[lte] fuzhou machine renderer registered");
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] fuzhou machine renderer unavailable", t);
        }
    }
}
