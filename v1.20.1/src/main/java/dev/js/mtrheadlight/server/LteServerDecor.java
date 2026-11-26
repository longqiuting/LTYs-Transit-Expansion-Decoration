package dev.js.mtrheadlight.server;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.decor.DecorConfig;
import net.fabricmc.api.ModInitializer;

/**
 * {@code main} 入口点 —— <b>LTED（装饰）</b>：厦门系列方块、PIDS / PSD-LCD、时钟、设备、装饰物件。
 *
 * <p>拆分前这里是一层「兼容壳」（{@code LteServerDecor}），内部按归属调 A 侧（灯光 / 碰撞 / 道床）
 * 与 B 侧两半。优化代码整体搬去 LTE 之后，壳和 A 侧都没了，本类直接就是 {@code main} 入口。
 *
 * <h2>顺序</h2>
 * 厦门物件页签 → 两个 logo → 贴墙标识 → 扶手 → 装饰 / RCC 站台 → PIDS / PSD-LCD
 * → 网络通道 → 发车倒数器 → 站外标志 → <b>厦门页签重排</b> → 售票机 / 充电宝 / 售卖机 / 安检 /
 * 时钟 / 闸机 / eyecandy / 方速屏蔽门。页签重排必须在「往这一页加条目的模块都注册完」之后，
 * 之后注册的模块只会往末尾追加，排不回来。
 */
public final class LteServerDecor implements ModInitializer {

    @Override
    public void onInitialize() {
        // 装饰侧自己的配置（线路条带 / 门灯配色）：config/mtrlte_decor.json。
        // 首次启动会把旧 config/mtrlte.json 里属于装饰的那几个键迁移过来（DecorConfig 里做）。
        try {
            DecorConfig.load();
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] decor config unavailable - using defaults", t);
        }

        // 厦门风格物件页签 + 屏蔽门方块（同样是两端都需要的 registry 条目）。
        blockModule("xiamen objects", dev.js.mtrheadlight.xiamen.XiamenObjects::register);

        // 标志方块：LTE logo + 角美地铁 logo（都进「厦门风格物件」页签）。
        // 必须排在上面两个页签之后 —— 往还没建的页签里加条目会抛异常。
        blockModule("logo blocks", dev.js.mtrheadlight.logo.LogoObjects::register);

        // LTED 仪表板物品（mtrlte:lted_dashboard）：只注册物品，页签位置由下面的 pin 决定。
        module("lted dashboard item", dev.js.mtrheadlight.decor.LtedCoreItems::register);

        // 贴墙标识（厦门 / 角美 / LTE）：方块直接继承 MTR 的 station_name_entrance，渲染器也是 MTR
        // 自己的 RenderStationNameTiled(dispatcher, true) —— 放置 / 朝向 / 站名 / 刷子 / 相邻连接全走 MTR。
        blockModule("wall sign blocks", dev.js.mtrheadlight.xiamen.sign.WallSignObjects::register);

        // 墙上扶手（进上面的「厦门风格物件」页签，所以排在其后）。
        blockModule("railing wall", dev.js.mtrheadlight.railing.RailingWallBlocks::register);

        // 玻璃扶手：与墙上扶手同一套模型空间约定，同样进「厦门风格物件」页签，排在它后面。
        blockModule("railing glass", dev.js.mtrheadlight.railing.RailingGlassBlocks::register);

        // 装饰方块（大理石地板 / 大理石站台）：站台继承 MTR 的 BlockPlatform，同样进「厦门风格物件」页签。
        blockModule("decor blocks", dev.js.mtrheadlight.decor.DecorBlocks::register);

        // 厦门地铁柱子（柱底可染色 / 柱中）：颜色跟随 MTR 车站色，刷子改色，ctrl+中键复制属性。
        blockModule("amtr pole blocks", dev.js.mtrheadlight.pole.PoleBlocks::register);

        // RCC 三种站台（平齐 / 内凹 / 石英）：独立页签 mtrlte:rcc_platform，同样继承 BlockPlatform。
        blockModule("rcc platforms", dev.js.mtrheadlight.decor.RccPlatforms::register);

        // 厦门 PIDS（地铁 / BRT）：模型与渲染照天津地铁，广告表走资源包。
        blockModule("xiamen pids blocks", dev.js.mtrheadlight.xiamen.pids.PidsXiamenObjects::register);

        // 厦门地铁「屏蔽门顶板 LCD 挂板」：独立方块（1~N 格横向连成一条），
        // 排版 / 文字 / 广告 / 配置界面全部复用上面的厦门 PIDS 那一套。
        blockModule("psd lcd block", dev.js.mtrheadlight.xiamen.pids.PsdLcdObjects::register);

        // 厦门 PIDS 界面通道：客户端改广告序号 / 服务端叫开界面（刷子右键）。
        module("xiamen pids channel", dev.js.mtrheadlight.xiamen.pids.PidsXiamenNet::registerServer);

        // 全局广告集通道（LTED 仪表板「显示屏 → 全局广告集」）：服务端存档 + 快照/保存，
        // 做法与全局时钟配置的 clock_global 通道一致。
        module("xiamen pids ad set channel", dev.js.mtrheadlight.xiamen.pids.PidsAdSetNet::register);

        // 挂墙发车倒数器：贴在站台附近的墙上，显示下一班车的发车倒计时（纯客户端读数据）。
        blockModule("dep on wall block", dev.js.mtrheadlight.xiamen.dep.DepOnWallObjects::register);

        // 挂顶发车倒数器：吊在站台上方的天花板上，读数和挂墙版同一套（排在它后面）。
        blockModule("dep on roof block", dev.js.mtrheadlight.xiamen.dep.DepOnRoofObjects::register);

        // 挂顶 DEP 挂杆：任意位置可放的细杆（几何是任意角旋转，走运行时 rawmodel），排在 DEP 那一组里。
        blockModule("dep pole block", dev.js.mtrheadlight.xiamen.dep.DepPoleObjects::register);

        // 站外标志（厦门 / 角美 × 挂墙 / 杆上）：灯箱牌，同样进「厦门风格物件」页签。
        blockModule("station sign blocks", dev.js.mtrheadlight.xiamen.sign.StationSignObjects::register);

        // 「厦门风格物件」页签的顺序（用户点名）：最前排 LTE logo → LTE 贴墙标识 → 厦门 logo →
        // 角美 logo → 两面贴墙标识 → 四件站外标志。
        // 这里必须排在 LogoObjects / WallSignObjects / StationSignObjects 之后 —— 页签条目的回调
        // 按注册顺序执行，重排的那一次得看得见它们；之后再注册的模块只会往末尾加，排不回来。
        module("core tab order", dev.js.mtrheadlight.logo.LogoObjects::pinCoreTabOrder);

        // LTED 仪表板排进「LTE 工具」页签（本模组唯一一页「工具」页，只剩这一件）。
        module("lted dashboard tab", dev.js.mtrheadlight.decor.LtedCoreItems::pinToolsTabOrder);

        // 售票机 / 余额查询机（各两格高）：售票机接方速 / 原版两套界面（刷子切），
        // 余额查询机服务端读记分板 mtr_balance 后用一句文字报余额。同样进「厦门风格物件」页签。
        blockModule("xiamen ticket / balance machines",
                dev.js.mtrheadlight.xiamen.machine.XiamenMachines::register);

        // 共享充电宝租借机（上半 / 下半各自独立的两块方块）+ 充电宝物品 + 投掷实体。
        // 租借 / 归还都在这里注册的服务端逻辑里：余额读 MTR 的记分板 mtr_balance（bind.MtrBalance）。
        blockModule("power bank charger", dev.js.mtrheadlight.xiamen.charger.ChargerObjects::register);

        // 饮料售卖机（上下两块联动）：空手右键 3 元出一瓶随机正面药水，刷子右键换药水池。
        blockModule("drinks machine", dev.js.mtrheadlight.xiamen.drinks.DrinksMachineObjects::register);

        // 安检类方块（液体安检机）：方块 + 物品 + 创造页签条目，排在厦门那一组之后注册。
        blockModule("security blocks", dev.js.mtrheadlight.security.SecurityObjects::register);

        // 时钟（代码画钟面 + 刷子配置 + 整点报时 + 红石控制）：同样进「厦门风格物件」页签。
        blockModule("clock blocks", dev.js.mtrheadlight.clock.ClockObjects::register);

        // 闸机（厦门地铁 / BRT × 进站 / 出站，共 4 个方块）：继承 MTR 的 BlockTicketBarrier，
        // 音效换成南滨的 crt_ticket / crt_ticket_error（这次一并注册这两个 SoundEvent）。
        blockModule("xiamen ticket gates", dev.js.mtrheadlight.xiamen.ticket.XiamenTicketGates::register);

        // eyecandy（.obj 几何，运行时由 BER 画）：厦门风格物件包 26 件追加进「厦门风格物件」页签，
        // 南普陀 34 件单开一页 mtrlte:nanputuo_decor。碰撞箱在注册期从 obj 顶点重推（两端都要，
        // 所以排在服务端也跑的这一段里）。每个方块带四向 rotation，MTR 刷子右键循环。
        blockModule("xiamen eyecandy decorations",
                dev.js.mtrheadlight.xiamen.eyecandy.EyecandyBlocks::register);

        // 方速（fangsu）屏蔽门搬运版：5 款旋转端门 + 2 款厦门滑动门，各拆左/右共 14 个方块，
        // 外加 2 块「厦门高」玻璃板（一个方块 4 种形态，刷子循环）。
        // 几何 / 材质 / 贴图 / 描述全部随 jar 走（assets/mtrlte/fangsu_psd/ + textures/fangsu_psd/），
        // 不需要装方速。滑动门继承 MTR 的 BlockPSDAPGDoorBase（列车到站自动开门、发车自动关门）；
        // 端门是普通方块 + 方块实体，玩家手持 MTR 驾驶钥匙右键开关（不接列车）。
        blockModule("fangsu psd doors", dev.js.mtrheadlight.xiamen.fangsu.FangsuPsdBlocks::register);
    }

    /**
     * 跑一个模块的注册；失败时 <b>ERROR + 全栈</b>，但绝不让服务端起不来。
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
