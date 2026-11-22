package dev.js.mtrheadlight.server;

import dev.js.mtrheadlight.HeadlightConfig;
import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.net.ServerAuthority;
import dev.js.mtrheadlight.net.ServerHandshake;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * {@code main} 入口点的 <b>A 侧（LTE / 优化）</b>：灯光、光束、碰撞、道床、连接器、服务端权威。
 *
 * <p>这是拆分后的正式入口之一，由兼容壳 {@link LteServer#onInitialize()} 调用（顺序：先 A 后 B）。
 * 单独拿出来是为了拆分仓库时整个类跟着 LTE 走：装饰侧（{@link LteServerDecor}）不需要它，
 * 它也不需要装饰侧的任何类。
 *
 * <h2>它负责什么</h2>
 * <ol>
 *   <li>读本侧配置（{@code config/mtrlte.json}）；</li>
 *   <li>标记「本进程里有 LTE 的服务端」→ 客户端的撞击伤害自动让位（{@link ServerAuthority}）；</li>
 *   <li>核心物品（信号 / 投影机 / PIDS 连接器、仪表板、光束编辑器）+「LTE 工具」创造页签；</li>
 *   <li>{@code /ltebind} 与左键 / 拆块解绑；</li>
 *   <li>挂 {@code END_SERVER_TICK} → {@link ServerCollision#tick}；</li>
 *   <li>握手：玩家进服时告诉装了 LTE 的客户端「撞击由我结算」；</li>
 *   <li>方块注册表自检（早 + {@code SERVER_STARTING} 的全表扫描）。</li>
 * </ol>
 *
 * <p>失败模式与以前完全一致：每个模块的调用都包在 {@link LteServer#module} /
 * {@link LteServer#blockModule} 里，失败只打 ERROR + 全栈，绝不让服务端起不来。
 */
public final class LteServerLights implements ModInitializer {

    /** 本侧配置。dedicated server 上就是文件里的那一份。 */
    private static volatile HeadlightConfig config = new HeadlightConfig();

    public static HeadlightConfig config() {
        return config;
    }

    @Override
    public void onInitialize() {
        try {
            config = HeadlightConfig.load();
            ServerConfig.install(LteServerLights::config);
        } catch (Throwable t) {
            LteLog.LOGGER.error("[lte] server config unavailable - using defaults", t);
        }
        // 「本进程是 dedicated server」—— 客户端的撞击伤害自动让位（没有客户端，无所谓）。
        if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType()
                == net.fabricmc.api.EnvType.SERVER) {
            ServerAuthority.markDedicated();
        }

        LteServer.module("handshake", ServerHandshake::register);

        // LTE 核心物品 + 创造页签（物品注册必须在两端都跑，所以放在 main）。
        LteServer.module("connector items", dev.js.mtrheadlight.signal.LteCoreItems::register);

        // /ltebind：投影机 / PIDS 绑定表的查询（服务端，读 PersistentState）。
        LteServer.module("/ltebind", dev.js.mtrheadlight.bind.LteBindCommand::register);

        // 左键解绑（AttackBlockCallback，两端都挂）+ 方块被拆时自动解绑（PlayerBlockBreakEvents.AFTER）。
        LteServer.module("connector unbind hooks", dev.js.mtrheadlight.bind.BindUnbind::register);

        try {
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                try {
                    // 进服：把玩家所在维度的两张表都同步过去（渲染要它）。
                    final net.minecraft.server.network.ServerPlayerEntity player = handler.player;
                    final net.minecraft.server.world.ServerWorld world = player.getServerWorld();
                    final dev.js.mtrheadlight.signal.SignalLinkStore store =
                            dev.js.mtrheadlight.signal.SignalLinkStore.get(world);
                    dev.js.mtrheadlight.signal.SignalLinkNet.sendAll(player,
                            dev.js.mtrheadlight.signal.SignalLinkNet.dimensionOf(world),
                            store.links(), store.lampLinks());
                } catch (Throwable ignored) {
                    // 同步失败只是绑定的信号灯不亮，不影响进服。
                }
            });
        } catch (Throwable t) {
            LteLog.LOGGER.debug("[lte] could not hook the signal-link sync", t);
        }

        try {
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                try {
                    ServerHandshake.announce(server, handler.player);
                } catch (Throwable ignored) {
                    // 握手只是优化，失败不影响游戏。
                }
            });
        } catch (Throwable t) {
            LteLog.LOGGER.debug("[lte] could not hook the join event", t);
        }

        try {
            ServerTickEvents.END_SERVER_TICK.register(server -> {
                try {
                    ServerCollision.tick(server);
                } catch (Throwable t) {
                    // 服务端碰撞永远不许影响 tick（MTR 数据异常、类缺失都在这里被吞掉）。
                    ServerCollision.warnOnce(t);
                }
            });
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] server collision unavailable", t);
        }

        // 方块注册表自检（早）与 SERVER_STARTING 的全表扫描不在这里：它们必须在<b>所有</b>方块注册完
        // （含 B 侧的装饰方块）之后才跑，由兼容壳 {@link LteServer#onInitialize()} 最后调
        // {@link #installRegistrySelfCheck()}。

        LteLog.LOGGER.info("[lte] server initialised "
                        + "(collision={} damage={} mobs={})",
                Boolean.valueOf(config.trainCollision),
                Boolean.valueOf(config.trainCollisionDamage),
                Boolean.valueOf(config.trainCollisionMobs));
    }

    /**
     * 方块注册表自检：早先清一遍「建了但没注册」的孤儿方块，并在 {@code SERVER_STARTING}
     * （所有模组都注册完之后）做一次全表扫描。
     *
     * <p>1.20.1 的 {@code Block} 构造器会把自己挂进注册表的「待绑定」表，没有 register 就永远留在
     * 里面 —— 注册表冻结时抛
     * "Some intrusive holders were not registered: [Reference{null=Block{minecraft:air}}]"，
     * 崩栈里全是 minecraft:air，看不出是谁。这里先把漏网方块点名并清掉；真正的最后一道是
     * SimpleRegistryFreezeMixin —— 它跑在所有模组都注册完之后、冻结之前。
     *
     * <p>由 {@link LteServer} 在 A / B 两侧都注册完之后调用（装饰方块也要被这次自检看到）。
     */
    static void installRegistrySelfCheck() {
        try {
            RegistrySelfCheck.purgePending("init");
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] block registry self-check unavailable", t);
        }

        // 全注册表扫描放到 SERVER_STARTING：那时所有模组（不止我们）都注册完了，
        // 数出来的 "N blocks, M unregistered" 才是完整的一副牌。
        try {
            ServerLifecycleEvents.SERVER_STARTING.register(server -> {
                try {
                    RegistrySelfCheck.purgePending("server-starting");
                    RegistrySelfCheck.reportBlocks("server-starting");
                } catch (Throwable t) {
                    LteLog.LOGGER.warn("[lte] block registry self-check failed", t);
                }
            });
        } catch (Throwable t) {
            LteLog.LOGGER.debug("[lte] could not hook the block registry self-check", t);
        }
    }
}
