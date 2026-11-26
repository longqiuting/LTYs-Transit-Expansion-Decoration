// Forge 侧的柱子客户端接线：染色 provider 事件 + ctrl+中键（InputEvent，不动 mixin）。
package dev.js.mtrheadlight.forge.client;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.pole.PoleClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * 厦门地铁柱子在 Forge 客户端的两个挂点（与 Fabric 侧一一对应，但用的是 Forge 的事件而不是 mixin）：
 *
 * <ol>
 *   <li>{@link RegisterColorHandlersEvent.Block} —— 给 {@code mtrlte:amtr_pole_bottom} 挂染色 provider
 *       （Fabric 侧是 {@code ColorProviderRegistry.BLOCK}）；</li>
 *   <li>{@link InputEvent.InteractionKeyMappingTriggered} —— ctrl+中键复制属性。
 *       Forge 在 {@code Minecraft#pickBlock()} 之前就 post 这个事件（button 2 + {@code keyPickItem}），
 *       取消它等于跳过原版拾取，所以这里<b>不需要</b> Fabric 侧那个 {@code doItemPick} mixin。</li>
 * </ol>
 *
 * <p>两条都在 mod 事件总线 / Forge 事件总线上注册，具体挂载在 {@code ClientBootstrap#init}。
 */
public final class PoleForgeClient {

    private PoleForgeClient() {
    }

    /** 客户端初始化：mod 总线上的染色事件 + 普通总线上的按键事件 + 颜色通道的客户端接收器。 */
    public static void init(final IEventBus modBus) {
        modBus.addListener(EventPriority.NORMAL, PoleForgeClient::onRegisterBlockColours);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(PoleForgeClient::onInteractionKey);
        try {
            PoleClient.register();
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] pole colour channel (client side) unavailable", t);
        }
    }

    /** 染色 provider（每帧每面都会被问，取色只读方块实体里的缓存字段）。 */
    private static void onRegisterBlockColours(final RegisterColorHandlersEvent.Block event) {
        try {
            dev.js.mtrheadlight.pole.PoleTint.onRegisterBlockColours(event.getBlockColors());
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] pole tint provider unavailable (the colour band stays white)", t);
        }
    }

    /**
     * ctrl + 中键。
     *
     * <p>只在 {@link PoleClient#tryCopyProperties} 真的接管了这次拾取时取消事件 —— 其余情况
     * （没按 ctrl / 准星不是柱底 / 手里不是柱底物品）一律原样放行，保持原版拾取。
     */
    private static void onInteractionKey(final InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isPickBlock()) {
            return;
        }
        if (PoleClient.tryCopyProperties(Minecraft.getInstance())) {
            event.setCanceled(true);
        }
    }
}
