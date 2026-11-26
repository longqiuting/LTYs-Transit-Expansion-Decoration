// @Mod 入口：Forge 平台层（M4b-4 建立，M4b-5 接完事件）。
//
// 真正的业务注册全部在 java-gen 的 dev.js.mtrheadlight.server.LteServerDecor / LtedClient 里
// （与 Fabric 树同构），这里只做「Forge 事件总线接线」，顺序不能乱：
//   1) Reg（DeferredRegister：Block/Item/BET/EntityType/SoundEvent/CreativeModeTab）先绑 mod 总线，
//      否则后面 LteServerDecor 里 new 出来的对象没有地方排队；
//   2) LteServerDecor#onInitialize —— 与 Fabric 的 ModInitializer 同一份代码，含页签条目回放
//      （Reg.tabEntries）、Net 的 SimpleChannel 注册、Net.onJoin 监听器；
//   3) FMLCommonSetupEvent —— 注册阶段结束后做一次注册表自检；
//   4) 客户端（Dist.CLIENT 才加载）：FMLClientSetupEvent → LtedClient#onInitializeClient
//      （渲染层 + BER 排队 + 客户端频道 + 命令），EntityRenderersEvent.RegisterRenderers 回放 BER，
//      RegisterClientReloadListenersEvent 注册资源重载监听器。
//
// M4a 的占位注册（lte_logo / clock）已移除：java-gen 里的 LogoObjects / ClockObjects
// 注册的是同一批 id，两处都注册会在 Registry 阶段撞名崩溃。占位类本身保留在
// dev.js.mtrheadlight.forge.block 下作为工具链样品；lte_tool 是 M4a 独有的，继续保留。
package dev.js.mtrheadlight.forge;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.item.LtedTooltipItem;
import dev.js.mtrheadlight.forge.platform.Cmds;
import dev.js.mtrheadlight.forge.platform.Net;
import dev.js.mtrheadlight.forge.platform.Reg;
import dev.js.mtrheadlight.server.LteServerDecor;
import dev.js.mtrheadlight.server.RegistrySelfCheck;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegistryObject;

@Mod(LtedForge.MOD_ID)
public final class LtedForge {

    public static final String MOD_ID = Reg.MOD_ID;

    public LtedForge() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1) M4b-6：Fabric 树是「Registry.register(id, new X(...))」——实例在构造时就已注册。
        //    Forge 1.20.1 的 Block/Item 构造器会自己往注册表里插（intrusive holder），
        //    因此**实例的 new 必须发生在 RegisterEvent 期间**：在 @Mod 构造器里 new 会拿到
        //    "Registry is already frozen"。这里把实例化延后到 BLOCKS 的 RegisterEvent。
        modBus.addListener(LtedForge::onRegister);
        Reg.register(modBus);

        // 2) mod 总线：页签内容回放 + 注册完成后的自检。
        modBus.addListener(Reg::onBuildTabContents);
        modBus.addListener(LtedForge::onCommonSetup);

        // 3) Forge 总线（双端都安全的事件）：
        //    * Net.installCommonHooks —— 玩家进服（PlayerLoggedInEvent）后广播频道数据；
        //    * RegisterCommandsEvent —— 服务端命令表。本模组目前没有服务端命令（/mtr_lte 是
        //      客户端命令，见 ClientBootstrap），这里挂上是为了 shim 完整：以后加服务端命令
        //      只要 Cmds.server(...) 注册即可，不需要再动入口。
        Net.installCommonHooks();
        MinecraftForge.EVENT_BUS.addListener(Cmds::onRegisterServerCommands);

        // 4) M4a 样品物品（Fabric 侧没有，保留以证明 item 链路可用）。
        //    M4b-6：同 1)，Item 构造器也会自注册 → 挪到 onRegister 里 new。

        // 5) 客户端：渲染器 / 渲染层 / 资源重载 / 客户端网络接收器 / 客户端命令。
        //    只在物理客户端加载 —— 这里面引用了 net.minecraft.client.* 与
        //    RegisterClientCommandsEvent/RegisterClientReloadListenersEvent 这些专用服务端
        //    不存在的类，绝不能提到这里之外。
        if (FMLEnvironment.dist.isClient()) {
            ClientBootstrap.init(modBus);
        }
    }

    /**
     * 注册阶段：在 BLOCKS 的 RegisterEvent 里跑 java-gen 的整套注册流程。
     *
     * <p>为什么必须是这个时机：Forge 1.20.1 给 {@code Block}/{@code Item} 的构造器打了补丁，
     * 构造时就会往 {@code BuiltInRegistries} 里建 intrusive holder；注册表在
     * {@code RegisterEvent} 之外是 frozen 的，所以 Fabric 那套「new 完立刻 register」的写法
     * 只有在这个回调里才成立。
     *
     * <p>顺序要求：本监听器必须在 {@link Reg#register} 之前挂上，这样它在 DeferredRegister
     * 自己的监听器之前执行 —— 先由业务代码把实例造出来，再由 DeferredRegister 用正式 id 落表。
     */
    private static boolean registered;

    private static void onRegister(net.minecraftforge.registries.RegisterEvent event) {
        // 只跑第一次 —— 无论第一个到的是哪个注册表（本机实测 SOUND_EVENT 早于 BLOCK）。
        // 整个 RegisterEvent 阶段内所有注册表都是 unfrozen 的，所以在这里 new 任何方块/物品都合法；
        // 而且此时 6 个 DeferredRegister 都还没收到自己的事件，可以继续往里塞条目。
        if (registered) {
            return;
        }
        registered = true;
        new LteServerDecor().onInitialize();
        // 厦门地铁柱子（柱底 / 柱中）：在本回调里 new + 落表（同 LteServerDecor 里的其它模块）。
        try {
            dev.js.mtrheadlight.pole.PoleBlocks.register();
        } catch (Throwable t) {
            LteLog.LOGGER.error("[lte] amtr pole blocks registration aborted", t);
        }
        Reg.item(new ResourceLocation(MOD_ID, "lte_tool"), new LtedTooltipItem(new Item.Properties()));
    }

    /** 注册阶段结束（RegisterEvent 已跑完）后的一次体检，输出到 latest.log 方便排查。 */
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> LteLog.LOGGER.info(
                "[lte] common setup: {} block(s) registered under {}",
                RegistrySelfCheck.countBlocks(MOD_ID), MOD_ID));
    }

    /**
     * M4a 工具链样品（{@code forge/block/ClockBlockEntity}）的方块实体类型。
     *
     * <p>id 用 {@code legacy_clock}：java-gen 的 {@code ClockObjects} 已经注册了
     * {@code mtrlte:clock}，两边同名会在 Registry 阶段崩。这个条目只服务于样品类
     * 自身的构造器 / ticker 比较，不参与实际内容。
     */
    public static final RegistryObject<BlockEntityType<dev.js.mtrheadlight.forge.block.ClockBlockEntity>> CLOCK_BE =
            Reg.BLOCK_ENTITIES.register("legacy_clock",
                    () -> BlockEntityType.Builder.of(
                            dev.js.mtrheadlight.forge.block.ClockBlockEntity::new).build(null));
}
