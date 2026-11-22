// 客户端接线：只在物理客户端加载（由 LtedForge 在 Dist.CLIENT 下调用）。
package dev.js.mtrheadlight.forge;

import dev.js.mtrheadlight.LtedClient;
import dev.js.mtrheadlight.ModMenuIntegration;
import dev.js.mtrheadlight.forge.platform.Cmds;
import dev.js.mtrheadlight.forge.platform.Net;
import dev.js.mtrheadlight.forge.platform.Renderers;
import dev.js.mtrheadlight.forge.platform.Resources;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge 客户端启动顺序：
 * <ol>
 *   <li>{@link FMLClientSetupEvent}：跑 java-gen 的 {@code LtedClient#onInitializeClient()}，
 *       这里会排队 BER 注册、立刻设置渲染层、建频道、挂客户端接收器、登记客户端命令；</li>
 *   <li>{@code EntityRenderersEvent.RegisterRenderers}：回放排队的 BER/实体渲染器；</li>
 *   <li>{@code RegisterClientReloadListenersEvent}：注册 PIDS 广告的资源重载监听器；</li>
 *   <li>{@code RegisterClientCommandsEvent}（Forge 总线）：回放 {@code Cmds.register(...)}
 *       攒下的客户端命令（{@code /mtr_lte}）。这条不能放到 {@code LtedForge} 构造器里：
 *       它引用的 {@code RegisterClientCommandsEvent} 是客户端专属类，专用服务端解析会崩。</li>
 * </ol>
 */
final class ClientBootstrap {

    private ClientBootstrap() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(ClientBootstrap::onClientSetup);
        modBus.addListener(Renderers::onRegisterRenderers);
        modBus.addListener(Resources::onRegisterClientReloadListeners);

        Net.installClientHooks();
        MinecraftForge.EVENT_BUS.addListener(Cmds::onRegisterClientCommands);

        // Fabric ModMenu 的 Forge 等价物：模组列表 → 配置按钮。
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> ModMenuIntegration.configScreen(parent)));
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // 不放进 enqueueWork：BER 注册是「排队」，但渲染层 / 频道必须在
        // RegisterRenderers 之前就绪，直接在主线程路径上跑最简单也最稳。
        new LtedClient().onInitializeClient();
    }
}
