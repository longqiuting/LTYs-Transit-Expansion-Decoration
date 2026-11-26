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

        // BER 注册的排队内容必须在 RegisterRenderers 之前就位；Forge 1.20.1 里那个事件
        // 反而早于 FMLClientSetupEvent，所以把初始化本身交给 Renderers 调度（谁先到谁跑）。
        Renderers.setClientInit(() -> new LtedClient().onInitializeClient());

        Net.installClientHooks();
        MinecraftForge.EVENT_BUS.addListener(Cmds::onRegisterClientCommands);

        // 厦门地铁柱子：染色 provider（RegisterColorHandlersEvent.Block）+ ctrl+中键 + 颜色通道接收器。
        dev.js.mtrheadlight.forge.client.PoleForgeClient.init(modBus);

        // Fabric ModMenu 的 Forge 等价物：模组列表 → 配置按钮。
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> ModMenuIntegration.configScreen(parent)));
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // 正常情况下这里已经是 no-op：Forge 1.20.1 的 RegisterRenderers 先到，客户端初始化
        // 已经在那里跑完（BER 队列随之填满）。保留这条是为了不依赖 Forge 的事件顺序。
        Renderers.ensureClientInit();
    }
}
