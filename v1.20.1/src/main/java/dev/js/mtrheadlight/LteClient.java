package dev.js.mtrheadlight;

import dev.js.mtrheadlight.collision.CollisionBootstrap;
import dev.js.mtrheadlight.mtr.BuiltInTrainsInit;
import dev.js.mtrheadlight.signal.SignalLinkClient;
import net.fabricmc.api.ClientModInitializer;

/**
 * {@code client} 入口点的 <b>A 侧（LTE / 优化）</b>：灯光 / 光束 / 散射、碰撞、内置车型、车辆灯 JSON、
 * 信号连接器。
 *
 * <h2>为什么现在只有两个 client 入口</h2>
 * 拆分前 {@code fabric.mod.json} 的 {@code client} 列了 18 个 {@code ClientModInitializer}，混着优化与装饰。
 * 现在按归属收成两个：本类（A）与 {@link LtedClient}（B），各自内部按<b>原来的注册顺序</b>调用那批
 * 初始化器 —— 顺序保持不变，所以注册 / 页签 / 渲染器的结果与拆分前一致。
 *
 * <h2>与 B 侧的先后</h2>
 * {@code fabric.mod.json} 里 {@link LtedClient} 排在本类<b>之前</b>，与拆分前的注册顺序一致（先装饰
 * 渲染器，再灯光 / 碰撞）。装饰侧配置的迁移不依赖这个顺序：{@code main} 入口 {@code LteServer}
 * 先跑，早在两边之前就把 {@code mtrlte_decor.json} 准备好了。
 */
public final class LteClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 核心客户端：读配置、每 tick 的灯光控制、/mtr_lte、渲染钩子。
        new MtrHeadlightClient().onInitializeClient();
        // 列车碰撞（推出 / 车顶 / 道床）。
        new CollisionBootstrap().onInitializeClient();
        // 内置车型屏蔽（disableBuiltInTrains）。
        new BuiltInTrainsInit().onInitializeClient();
        // 车辆灯 JSON（资源包覆盖）。
        new VehicleLightsJsonClient().onInitializeClient();
        // 信号连接器：绑定表的客户端镜像与信号灯状态同步。
        new SignalLinkClient().onInitializeClient();
    }
}
