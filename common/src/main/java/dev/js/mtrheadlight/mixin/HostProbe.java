package dev.js.mtrheadlight.mixin;

/**
 * 探测 MTR（以及天津地铁）里本模组要注入的类是否存在。
 *
 * <p>装饰侧的 6 份 MTR mixin 都挂在这些宿主类上，{@code required=false}，所以注入前先问一句
 * 「这个类在不在」。每个探测都是一次类资源查找并包在 {@code Throwable} 里 —— 半加载 / 缺类的宿主
 * 永远不会把客户端带下来；结果缓存在静态字段里（游戏运行期间不会变）。
 *
 * <p>优化侧（灯光 / 碰撞 / 道床 / 内置车型）的探测方法随那些 mixin 一起删掉了
 * （ANTE / NTE 轨道探测、{@code TrainClientRegistry}、{@code TrainServer}、{@code SavedRailBase}）。
 */
public final class HostProbe {
    /** MTR 的 PIDS / 到站投影机渲染器（{@code getSchedules} 是「站外投影机」的注入目标）。 */
    private static final String MTR_PIDS_RENDERER = "mtr.render.RenderPIDS";
    /** 天津地铁自带的 PIDS 渲染器（它自己抄了一份 {@code getSchedules}，不吃 MTR 的那条 mixin）。 */
    private static final String TJ_PIDS_RENDERER = "ziyue.tjmetro.render.RenderPIDSTianjin";
    /** MTR 的屏蔽门门扇渲染器（贴图 id 命名空间切换的注入目标）。 */
    private static final String MTR_PSD_DOOR_RENDERER = "mtr.render.RenderPSDAPGDoor";
    /** MTR 的线路图位图生成器（线路条带重绘 / 缓存键 / 异步绘制的注入目标）。 */
    private static final String MTR_ROUTE_MAP = "mtr.client.RouteMapGenerator";
    /** MTR 的站台方块（{@code getActualState} 的门型判定是「屏蔽门切站台模型」的注入目标）。 */
    private static final String MTR_BLOCK_PLATFORM = "mtr.block.BlockPlatform";

    private static final boolean MTR_PIDS = present(MTR_PIDS_RENDERER);
    private static final boolean TJ_PIDS = present(TJ_PIDS_RENDERER);
    private static final boolean MTR_PSD_DOOR = present(MTR_PSD_DOOR_RENDERER);
    private static final boolean MTR_ROUTE_MAP_PRESENT = present(MTR_ROUTE_MAP);
    private static final boolean MTR_PLATFORM_BLOCK = present(MTR_BLOCK_PLATFORM);

    private HostProbe() {
    }

    /** {@code mtr.render.RenderPIDS} 是否存在（站外投影机顶渲染的注入目标）。 */
    public static boolean mtrPidsRenderer() {
        return MTR_PIDS;
    }

    /**
     * 天津地铁的 {@code ziyue.tjmetro.render.RenderPIDSTianjin} 是否存在。
     *
     * <p>它自带一份 {@code getSchedules}（javap：0 偏移就是 {@code RailwayData.getStation}），
     * MTR 那条 mixin 管不到它；没装天津地铁时这条注入整个不应用。
     */
    public static boolean tjPidsRenderer() {
        return TJ_PIDS;
    }

    /**
     * {@code mtr.render.RenderPSDAPGDoor} 是否存在 —— 屏蔽门门扇贴图命名空间切换的注入目标。
     *
     * <p>没有它就注入不了（门照样能用，只是贴图跟着资源包走）。
     */
    public static boolean mtrPsdDoorRenderer() {
        return MTR_PSD_DOOR;
    }

    /**
     * {@code mtr.client.RouteMapGenerator} 是否存在 —— 线路条带重绘的注入目标。
     *
     * <p>没有它就没有 {@code generateRouteMap} 可拦（条带回到 MTR 原版），所以单独探测。
     */
    public static boolean mtrRouteMap() {
        return MTR_ROUTE_MAP_PRESENT;
    }

    /**
     * {@code mtr.block.BlockPlatform} 是否存在 —— 站台门型识别（{@code getActualState}）的注入目标。
     *
     * <p>用<b>方块</b>类而不是客户端类探测：站台的 blockstate 在服务端也会算（门型决定服务端
     * 存下来的 state，再同步给客户端），所以 dedicated server 上这条注入必须生效。
     */
    public static boolean mtrBlockPlatform() {
        return MTR_PLATFORM_BLOCK;
    }

    private static boolean present(final String className) {
        try {
            // Resource lookup, NOT Class.forName: loading a host class this early would freeze it
            // before every other mod's mixins get a chance to apply, which can break unrelated mods
            // (EntityCulling's block-entity interface injection is exactly such a victim).
            final String path = className.replace('.', '/') + ".class";
            final ClassLoader own = HostProbe.class.getClassLoader();
            if (own != null && own.getResource(path) != null) {
                return true;
            }
            final ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            return ctx != null && ctx.getResource(path) != null;
        } catch (Throwable t) {
            // Missing host, or a loader that cannot see it: treat as absent.
            return false;
        }
    }
}
