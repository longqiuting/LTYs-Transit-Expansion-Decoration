package dev.js.mtrheadlight;

import dev.js.mtrheadlight.decor.DecorClient;
import dev.js.mtrheadlight.decor.DecorConfig;
import dev.js.mtrheadlight.logo.LogoClient;
import dev.js.mtrheadlight.region.RegionalLogosClient;
import dev.js.mtrheadlight.pole.PoleClient;
import dev.js.mtrheadlight.railing.RailingGlassClient;
import dev.js.mtrheadlight.security.SecurityClient;
import dev.js.mtrheadlight.xiamen.XiamenClient;
import dev.js.mtrheadlight.xiamen.charger.ChargerClient;
import dev.js.mtrheadlight.xiamen.dep.DepOnWallClient;
import dev.js.mtrheadlight.xiamen.drinks.DrinksMachineClient;
import dev.js.mtrheadlight.xiamen.fangsu.FangsuPsdClient;
import dev.js.mtrheadlight.xiamen.machine.XiamenMachinesClient;
import dev.js.mtrheadlight.xiamen.pids.PidsXiamenClient;
import dev.js.mtrheadlight.xiamen.pids.PsdLcdClient;
import dev.js.mtrheadlight.xiamen.sign.WallSignClient;
import net.fabricmc.api.ClientModInitializer;

/**
 * {@code client} 入口点的 <b>B 侧（LTED / 装饰）</b>：厦门系列方块、PIDS / PSD-LCD、时钟、设备、
 * 装饰物件、方速屏蔽门。
 *
 * <h2>第一件事永远是 {@link DecorConfig#load()}</h2>
 * 装饰侧自己的配置（线路条带 / 门灯配色）放在 {@code config/mtrlte_decor.json}。首次启动要把旧
 * {@code config/mtrlte.json} 里的那几个键读出来 —— 老版本把它们写在优化侧的配置里，
 * 那些类已经不在这个 jar 里了，所以只剩这一处迁移入口。
 *
 * <p>迁移发生在 {@code main} 入口 {@code LteServerDecor}（Fabric 先跑 main 再跑 client，所以它
 * 一定早于这里）；本类这次调用是<b>兜底</b>：文件已经存在时只是一次纯读取。
 *
 * <p>每一条初始化调用都与拆分前的注册顺序一致（渲染器 / 刷子界面 / 声音都按这个顺序挂）。
 */
public final class LtedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // ① 兜底读一次装饰侧配置（main 入口已经迁移过；这里只保证单独走到这一侧时也有一份）。
        DecorConfig.load();

        // ② 客户端指令：只剩装饰相关的 /mtr_lte dep（倒数器读数方向 + 取证）。
        Commands.register();

        // ④ 以下是装饰类，顺序不变。
        new XiamenClient().onInitializeClient();
        new RegionalLogosClient().onInitializeClient();
        new dev.js.mtrheadlight.region.fuzhou.FuzhouMachinesClient().onInitializeClient();
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                dev.js.mtrheadlight.region.hk.HkDestinationLights.entityType(),
                context -> new dev.js.mtrheadlight.region.hk.HkDestinationLightRenderer());
        new WallSignClient().onInitializeClient();
        new RailingGlassClient().onInitializeClient();
        new DecorClient().onInitializeClient();
        new PoleClient().onInitializeClient();
        new PidsXiamenClient().onInitializeClient();
        new PsdLcdClient().onInitializeClient();
        new DepOnWallClient().onInitializeClient();
        dev.js.mtrheadlight.LteLog.LOGGER.info("[lte] host probe: routeMap={} pids={} psd={} platform={}",
                dev.js.mtrheadlight.mixin.HostProbe.mtrRouteMap(),
                dev.js.mtrheadlight.mixin.HostProbe.mtrPidsRenderer(),
                dev.js.mtrheadlight.mixin.HostProbe.mtrPsdDoorRenderer(),
                dev.js.mtrheadlight.mixin.HostProbe.mtrBlockPlatform());
        new LogoClient().onInitializeClient();
        new XiamenMachinesClient().onInitializeClient();
        new ChargerClient().onInitializeClient();
        new DrinksMachineClient().onInitializeClient();
        new SecurityClient().onInitializeClient();
        new FangsuPsdClient().onInitializeClient();

        // ⑤ 动态贴图的生命周期：断线/换存档整表释放，每 tick 顺手回收过期的那几张。
        //    不挂这个的话，静态 map 会一直钉住 NativeImage + GL 贴图（换存档也赖着不走）。
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT
                .register((handler, client) -> {
                    dev.js.mtrheadlight.region.fuzhou.FuzhouLedTextures.clear();
                    dev.js.mtrheadlight.xiamen.pids.PsdLcdTextures.clear();
                    dev.js.mtrheadlight.xiamen.pids.PidsXiamenConsistTextures.clear();
                    dev.js.mtrheadlight.region.hk.DestinationColors.clear();
                    dev.js.mtrheadlight.bind.PidsStationResolver.resetCaches();
                });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK
                .register(client -> {
                    dev.js.mtrheadlight.region.fuzhou.FuzhouLedTextures.sweep();
                    dev.js.mtrheadlight.xiamen.pids.PsdLcdTextures.sweep();
                    dev.js.mtrheadlight.xiamen.pids.PidsXiamenConsistTextures.sweep();
                });
    }
}
