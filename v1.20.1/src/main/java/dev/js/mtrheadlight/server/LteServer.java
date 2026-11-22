package dev.js.mtrheadlight.server;

import dev.js.mtrheadlight.HeadlightConfig;
import dev.js.mtrheadlight.LteLog;
import net.fabricmc.api.ModInitializer;

/**
 * {@code main} 入口点 —— <b>兼容壳</b>：内部按归属调 A 侧与 B 侧两半。
 *
 * <h2>为什么要留这一层</h2>
 * {@code fabric.mod.json} 的 {@code main} 入口仍然只有一个 {@code LteServer}：拆分仓库时
 * 只需要删掉 {@link #onInitialize()} 里的<b>一行</b>（{@link LteServerDecor}），fabric 的入口列表
 * 与加载顺序都不用动。两半各自的职责见 {@link LteServerLights}（优化：灯光 / 光束 / 碰撞 / 道床 /
 * 连接器 / 服务端权威）与 {@link LteServerDecor}（装饰：厦门系列 / PIDS / 时钟 / 设备）。
 *
 * <h2>顺序（不能颠倒）</h2>
 * <ol>
 *   <li><b>A 先</b>：它建「LTE 工具」页签并注册仪表板 / 连接器 / 光束编辑器物品；</li>
 *   <li><b>B 后</b>：装饰模块往那个页签里放 {@code lte_logo} / {@code wall_sign_lte}，
 *       并建自己的「厦门风格物件」页签。</li>
 * </ol>
 * 往还没建的页签里加条目会抛异常，所以顺序是硬约束（与拆分前的单文件顺序完全一致）。
 *
 * <p>本类同时保留两个注册包装 {@link #module} / {@link #blockModule}：A / B 两侧共用，
 * 失败时的日志格式（ERROR + 全栈、点名漏网方块）与拆分前一字不差。
 */
public final class LteServer implements ModInitializer {

    @Override
    public void onInitialize() {
        // 先迁移 / 读取装饰侧的配置：必须在 LTE 读 mtrlte.json 之前（版本升级时那边会整份重写，
        // 旧键会被丢掉）。main 入口在客户端里也先于 client 入口执行，所以这一步覆盖两端。
        LteServerDecor.loadConfig();
        // A 侧（优化）。
        new LteServerLights().onInitialize();
        // B 侧（装饰）。拆仓库时把这一行删掉即可。
        new LteServerDecor().onInitialize();
        // 方块注册表自检：必须排在两侧都注册完之后（孤儿方块点名 / SERVER_STARTING 全表扫描）。
        LteServerLights.installRegistrySelfCheck();
    }

    /** 本侧配置（A 侧持有；保留这个入口，老的调用点不用改）。 */
    public static HeadlightConfig config() {
        return LteServerLights.config();
    }

    /**
     * 跑一个模块的注册；失败时 <b>ERROR + 全栈</b>（不再是 warn），但绝不让服务端起不来。
     *
     * <p>{@code name} 会出现在日志里 —— 服主一眼能看到是哪一块坏了。
     */
    static void module(final String name, final Runnable body) {
        try {
            body.run();
        } catch (final Throwable t) {
            LteLog.LOGGER.error("[lte] {} registration aborted (the rest of the mod keeps working)", name, t);
        }
    }

    /**
     * 注册方块的模块：除了 {@link #module} 的 ERROR 日志，还额外点名「建了但没注册」的方块。
     *
     * <p>1.20.1 的 {@code Block} 构造器一执行就挂进注册表的「待绑定」表，只有
     * {@code Registry.register} 才会补上 id。所以模块在「建了一批、还没注册完」的时候抛异常，
     * 就会留下一批孤儿 Block —— 注册表冻结时抛
     * {@code Some intrusive holders were not registered: [Reference{null=Block{minecraft:air}}]}，
     * 而且崩溃信息里全是一样的 {@code minecraft:air}，完全看不出是谁。
     *
     * <p>这里量两件事并写进日志：<b>注册进去几块</b>（注册表里本模组条目数变化）、
     * <b>建了但没注册几块</b>（「待绑定」表条目数变化）。随后立刻清掉这些孤儿引用
     * （{@link RegistrySelfCheck#purgePending}），服务端就能正常冻结注册表、起到 {@code Done}。
     */
    static void blockModule(final String name, final Runnable body) {
        final int blocksBefore = RegistrySelfCheck.countBlocks();
        final int oursBefore = RegistrySelfCheck.countBlocks("mtrlte");
        final int pendingBefore = RegistrySelfCheck.pendingCount();
        try {
            body.run();
        } catch (final Throwable t) {
            final int registered = RegistrySelfCheck.countBlocks("mtrlte") - oursBefore;
            final int leaked = Math.max(0, RegistrySelfCheck.pendingCount() - pendingBefore);
            LteLog.LOGGER.error("[lte] {} registration aborted: {} block(s) registered, {} block(s) created"
                            + " but never registered (blocks total {} -> {}); the rest of the mod keeps working",
                    name, Integer.valueOf(registered), Integer.valueOf(leaked),
                    Integer.valueOf(blocksBefore), Integer.valueOf(RegistrySelfCheck.countBlocks()), t);
            try {
                RegistrySelfCheck.purgePending(name);
            } catch (final Throwable ignored) {
                // 自检自己不许再抛。
            }
        }
    }
}
