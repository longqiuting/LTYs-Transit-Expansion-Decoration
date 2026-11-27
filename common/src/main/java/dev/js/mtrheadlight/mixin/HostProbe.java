package dev.js.mtrheadlight.mixin;

/**
 * 探测 MTR（以及天津地铁）里本模组要注入的类是否存在。
 *
 * <p>装饰侧的 6 份 MTR mixin 都挂在这些宿主类上，{@code required=false}，所以注入前先问一句
 * 「这个类在不在」。每个探测都是一次类资源查找并包在 {@code Throwable} 里 —— 半加载 / 缺类的宿主
 * 永远不会把客户端带下来；结果缓存在静态字段里（游戏运行期间不会变）。
 *
 * <p><b>Fabric 上必须再退一步问加载器</b>：mixin 配置插件（{@code MtrMixinPlugin}）是在别的 mod
 * 的 jar 还没全部挂到 class loader 之前跑的，{@code getResource("mtr/client/RouteMapGenerator.class")}
 * 这时查不到，探测全部返回 false —— 结果是所有挂 MTR 的 mixin 在 Fabric 上被静默跳过
 * （贴墙标一直画 MTR 自己的 logo、线路条带不重绘，都是这个原因）。所以资源查不到时再用
 * {@code FabricLoader.isModLoaded} 兜底。
 */
public final class HostProbe {
    /** MTR 的 PIDS / 到站投影机渲染器（{@code getSchedules} 是「站外投影机」的注入目标）。 */
    private static final String MTR_PIDS_RENDERER = "mtr.render.RenderPIDS";
    /** 天津地铁自带的 PIDS 渲染器（它自己抄了一份 {@code getSchedules}，不吃 MTR 的那条 mixin）。 */
    private static final String TJ_PIDS_RENDERER = "ziyue.tjmetro.render.RenderPIDSTianjin";
    /** MTR 的屏蔽门门扇渲染器（贴图 id 命名空间切换的注入目标）。 */
    private static final String MTR_PSD_DOOR_RENDERER = "mtr.render.RenderPSDAPGDoor";
    /** MTR 的线路图位图生成器（线路条带重绘 / 缓存键 / 异步绘制 / 贴墙标 logo 的注入目标）。 */
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

    /** 天津地铁的 {@code ziyue.tjmetro.render.RenderPIDSTianjin} 是否存在。 */
    public static boolean tjPidsRenderer() {
        return TJ_PIDS;
    }

    /** {@code mtr.render.RenderPSDAPGDoor} 是否存在 —— 屏蔽门门扇贴图命名空间切换的注入目标。 */
    public static boolean mtrPsdDoorRenderer() {
        return MTR_PSD_DOOR;
    }

    /** {@code mtr.client.RouteMapGenerator} 是否存在 —— 线路条带 / 贴墙标 logo 的注入目标。 */
    public static boolean mtrRouteMap() {
        return MTR_ROUTE_MAP_PRESENT;
    }

    /** {@code mtr.block.BlockPlatform} 是否存在 —— 站台门型识别的注入目标。 */
    public static boolean mtrBlockPlatform() {
        return MTR_PLATFORM_BLOCK;
    }

    private static boolean present(final String className) {
        // Resource lookup, NOT Class.forName: loading a host class this early would freeze it
        // before every other mod's mixins get a chance to apply, which can break unrelated mods
        // (EntityCulling's block-entity interface injection is exactly such a victim).
        final String path = className.replace('.', '/') + ".class";
        try {
            final ClassLoader own = HostProbe.class.getClassLoader();
            if (own != null && own.getResource(path) != null) {
                return true;
            }
        } catch (Throwable ignored) {
            // 资源查询失败：继续往下问加载器
        }
        try {
            final ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            if (ctx != null && ctx.getResource(path) != null) {
                return true;
            }
        } catch (Throwable ignored) {
            // 同上
        }
        return modLoadedFor(className);
    }

    /**
     * 反射问 Fabric 的 mod 列表「宿主装了没有」。
     *
     * <p>用反射而不是直接 import：{@code common/} 的这份代码在 Forge 侧也有对应实现（{@code java-gen}），
     * 不能硬依赖 fabric-loader 的类。
     */
    private static boolean modLoadedFor(final String className) {
        final String modId;
        if (className.startsWith("mtr.")) {
            modId = "mtr";
        } else if (className.startsWith("ziyue.")) {
            modId = "tjmetro";
        } else {
            return false;
        }
        try {
            final Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            final Object loader = loaderClass.getMethod("getInstance").invoke(null);
            return (Boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (Throwable t) {
            return false;
        }
    }
}
