package dev.js.mtrheadlight.server;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.decor.DecorConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * {@code main} 入口点的 <b>B 侧（LTED / 装饰）</b>：厦门系列方块、PIDS / PSD-LCD、时钟、设备、装饰物件。
 *
 * <p>拆分后整个类跟着 LTED 走：它引用的每一个注册模块都是装饰内容，反过来 LTE 侧
 * （{@link LteServerLights}）不引用这里的任何东西 —— 唯一的交叉边是
 * {@code LogoObjects} 往「LTE 工具」页签里放 {@code lte_logo}（用户点名的位置），
 * 所以 A 必须先跑（兼容壳 {@link LteServer} 保证了这个顺序）。
 *
 * <h2>顺序</h2>
 * 与拆分前逐条一致：厦门物件页签 → 两个 logo → 贴墙标识 → 扶手 → 装饰 / RCC 站台 → PIDS / PSD-LCD
 * → 网络通道 → 发车倒数器 → 站外标志 → <b>厦门页签重排</b> → 售票机 / 充电宝 / 售卖机 / 安检 /
 * 时钟 / 闸机 / eyecandy / 方速屏蔽门。页签重排必须在「往这一页加条目的模块都注册完」之后，
 * 之后注册的模块只会往末尾追加，排不回来。
 */
public final class LteServerDecor implements ModInitializer {

    /**
     * 读装饰侧配置；没有 {@code mtrlte_decor.json} 时把 {@code mtrlte.json} 里的旧键迁移过来。
     *
     * <p><b>必须早于任何可能重写 {@code mtrlte.json} 的保存</b>：LTE 的 {@code HeadlightConfig}
     * 在配置版本升级时会整份重写（新字段写出去、已经不认识的旧键被丢掉）。所以兼容壳
     * {@link LteServer#onInitialize()} 在调 A 侧之前先调本方法一次。
     */
    static void loadConfig() {
        try {
            DecorConfig.load();
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] decor config unavailable - using defaults", t);
        }
    }

    @Override
    public void onInitialize() {
        // 装饰侧自己的配置（线路条带 / 门灯配色）。兼容壳在 A 侧加载 mtrlte.json <b>之前</b>
        // 已经调过一次 {@link #loadConfig()}（见 {@link LteServer}），这里只是兜底：
        // 文件已经存在时是一次纯读取，不会重复迁移。
        loadConfig();

        // 厦门风格物件页签 + 屏蔽门方块（同样是两端都需要的 registry 条目）。
        LteServer.blockModule("xiamen objects", dev.js.mtrheadlight.xiamen.XiamenObjects::register);

        // 标志方块：LTE logo（进「LTE 工具」页签）+ 角美地铁 logo（进「厦门风格物件」页签）。
        // 必须排在上面两个页签之后 —— 往还没建的页签里加条目会抛异常。
        LteServer.blockModule("logo blocks", dev.js.mtrheadlight.logo.LogoObjects::register);

        // LTED 仪表板物品（mtrlte:lted_dashboard）：只注册物品，页签位置由下面的 pin 决定。
        LteServer.module("lted dashboard item", dev.js.mtrheadlight.decor.LtedCoreItems::register);

        // 贴墙标识（厦门 / 角美 / LTE）：方块直接继承 MTR 的 station_name_entrance，渲染器也是 MTR
        // 自己的 RenderStationNameTiled(dispatcher, true) —— 放置 / 朝向 / 站名 / 刷子 / 相邻连接全走 MTR。
        // 厦门 / 角美两面进「厦门风格物件」页签、紧跟两个 logo 块（LogoObjects.pinCoreTabOrder() 负责挪），
        // LTE 那面只进「LTE 工具」页签的第 2 位（LteCoreItems.pinToolsTabOrder() 负责挪），不进厦门页。
        LteServer.blockModule("wall sign blocks", dev.js.mtrheadlight.xiamen.sign.WallSignObjects::register);

        // 墙上扶手（进上面的「厦门风格物件」页签，所以排在其后）。
        LteServer.blockModule("railing wall", dev.js.mtrheadlight.railing.RailingWallBlocks::register);

        // 玻璃扶手：与墙上扶手同一套模型空间约定，同样进「厦门风格物件」页签，排在它后面。
        LteServer.blockModule("railing glass", dev.js.mtrheadlight.railing.RailingGlassBlocks::register);

        // 装饰方块（大理石地板 / 大理石站台）：站台继承 MTR 的 BlockPlatform，同样进「厦门风格物件」页签。
        LteServer.blockModule("decor blocks", dev.js.mtrheadlight.decor.DecorBlocks::register);

        // RCC 三种站台（平齐 / 内凹 / 石英）：独立页签 mtrlte:rcc_platform，同样继承 BlockPlatform。
        LteServer.blockModule("rcc platforms", dev.js.mtrheadlight.decor.RccPlatforms::register);

        // 厦门 PIDS（地铁 / BRT）：模型与渲染照天津地铁，广告表走资源包。
        LteServer.blockModule("xiamen pids blocks", dev.js.mtrheadlight.xiamen.pids.PidsXiamenObjects::register);

        // 厦门地铁「屏蔽门顶板 LCD 挂板」：独立方块（1~N 格横向连成一条），
        // 排版 / 文字 / 广告 / 配置界面全部复用上面的厦门 PIDS 那一套。
        LteServer.blockModule("psd lcd block", dev.js.mtrheadlight.xiamen.pids.PsdLcdObjects::register);

        // 厦门 PIDS 界面通道：客户端改广告序号 / 服务端叫开界面（刷子右键）。
        LteServer.module("xiamen pids channel", dev.js.mtrheadlight.xiamen.pids.PidsXiamenNet::registerServer);

        // 全局广告集通道（LTED 仪表板「显示屏 → 全局广告集」）：服务端存档 + 快照/保存，
        // 做法与全局时钟配置的 clock_global 通道一致。
        LteServer.module("xiamen pids ad set channel", dev.js.mtrheadlight.xiamen.pids.PidsAdSetNet::register);

        // 挂墙发车倒数器：贴在站台附近的墙上，显示下一班车的发车倒计时（纯客户端读数据）。
        LteServer.blockModule("dep on wall block", dev.js.mtrheadlight.xiamen.dep.DepOnWallObjects::register);

        // 挂顶发车倒数器：吊在站台上方的天花板上，读数和挂墙版同一套（排在它后面）。
        LteServer.blockModule("dep on roof block", dev.js.mtrheadlight.xiamen.dep.DepOnRoofObjects::register);

        // 挂顶 DEP 挂杆：任意位置可放的细杆（几何是任意角旋转，走运行时 rawmodel），排在 DEP 那一组里。
        LteServer.blockModule("dep pole block", dev.js.mtrheadlight.xiamen.dep.DepPoleObjects::register);

        // 站外标志（厦门 / 角美 × 挂墙 / 杆上）：灯箱牌，同样进「厦门风格物件」页签。
        LteServer.blockModule("station sign blocks", dev.js.mtrheadlight.xiamen.sign.StationSignObjects::register);

        // 「厦门风格物件」页签的顺序（用户点名）：最前排两个 logo → 四件站外标志；
        // 两个贴墙标识（厦门 / 角美）排到本页末尾。
        // 这里必须排在 LogoObjects / WallSignObjects / StationSignObjects 之后 —— 页签条目的回调
        // 按注册顺序执行，重排的那一次得看得见它们；之后再注册的模块只会往末尾加，排不回来。
        LteServer.module("core tab order", dev.js.mtrheadlight.logo.LogoObjects::pinCoreTabOrder);

        // LTED 仪表板排进「厦门地铁物件」页签前部（两个 logo 之后），必须在上面那次重排之后。
        LteServer.module("lted dashboard tab", dev.js.mtrheadlight.decor.LtedCoreItems::pinCoreTabOrder);

        // 售票机 / 余额查询机（各两格高）：售票机接方速 / 原版两套界面（刷子切），
        // 余额查询机服务端读记分板 mtr_balance 后用一句文字报余额。同样进「厦门风格物件」页签。
        LteServer.blockModule("xiamen ticket / balance machines",
                dev.js.mtrheadlight.xiamen.machine.XiamenMachines::register);

        // 共享充电宝租借机（上半 / 下半各自独立的两块方块）+ 充电宝物品 + 投掷实体。
        // 租借 / 归还都在这里注册的服务端逻辑里：余额读 MTR 的记分板 mtr_balance（bind.MtrBalance）。
        LteServer.blockModule("power bank charger", dev.js.mtrheadlight.xiamen.charger.ChargerObjects::register);

        // 饮料售卖机（上下两块联动）：空手右键 3 元出一瓶随机正面药水，刷子右键换药水池。
        LteServer.blockModule("drinks machine", dev.js.mtrheadlight.xiamen.drinks.DrinksMachineObjects::register);

        // 安检类方块（液体安检机）：方块 + 物品 + 创造页签条目，排在厦门那一组之后注册。
        LteServer.blockModule("security blocks", dev.js.mtrheadlight.security.SecurityObjects::register);

        // 时钟（代码画钟面 + 刷子配置 + 整点报时 + 红石控制）：同样进「厦门风格物件」页签。
        LteServer.blockModule("clock blocks", dev.js.mtrheadlight.clock.ClockObjects::register);

        // 闸机（厦门地铁 / BRT × 进站 / 出站，共 4 个方块）：继承 MTR 的 BlockTicketBarrier，
        // 音效换成南滨的 crt_ticket / crt_ticket_error（这次一并注册这两个 SoundEvent）。
        LteServer.blockModule("xiamen ticket gates", dev.js.mtrheadlight.xiamen.ticket.XiamenTicketGates::register);

        // eyecandy（.obj 几何，运行时由 BER 画）：厦门风格物件包 26 件追加进「厦门风格物件」页签，
        // 南普陀 34 件单开一页 mtrlte:nanputuo_decor。碰撞箱在注册期从 obj 顶点重推（两端都要，
        // 所以排在服务端也跑的这一段里）。每个方块带四向 rotation，MTR 刷子右键循环。
        LteServer.blockModule("xiamen eyecandy decorations",
                dev.js.mtrheadlight.xiamen.eyecandy.EyecandyBlocks::register);

        // 方速（fangsu）屏蔽门搬运版：5 款旋转端门 + 2 款厦门滑动门，各拆左/右共 14 个方块，
        // 外加 2 块「厦门高」玻璃板（一个方块 4 种形态，刷子循环）。
        // 几何 / 材质 / 贴图 / 描述全部随 jar 走（assets/mtrlte/fangsu_psd/ + textures/fangsu_psd/），
        // 不需要装方速。滑动门继承 MTR 的 BlockPSDAPGDoorBase（列车到站自动开门、发车自动关门）；
        // 端门是普通方块 + 方块实体，玩家手持 MTR 驾驶钥匙右键开关（不接列车）。
        LteServer.blockModule("fangsu psd doors", dev.js.mtrheadlight.xiamen.fangsu.FangsuPsdBlocks::register);

        try {
            ServerTickEvents.END_SERVER_TICK.register(server -> {
                // PIDS / 投影机：把「绑定过的站台」加进附近玩家的到站数据范围。
                // 不加这一步，站外显示屏会在 MTR 下一次 PIDS 数据包（≤3 秒）里被清成空屏。
                try {
                    dev.js.mtrheadlight.bind.PidsScheduleScope.tick(server);
                } catch (Throwable ignored) {
                    // 索引只是补数据，失败就退回 MTR 原行为。
                }
                // 玩家走进 64 格时补推一次方块实体 NBT（绑定那一刻的推送够不到后来的人）。
                try {
                    dev.js.mtrheadlight.bind.DisplayPushTicker.tick(server);
                } catch (Throwable ignored) {
                    // 同上。
                }
            });
        } catch (Throwable t) {
            LteLog.LOGGER.debug("[lte] could not hook the display data ticks", t);
        }
    }
}
