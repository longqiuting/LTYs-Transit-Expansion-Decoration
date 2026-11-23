package dev.js.mtrheadlight.platform;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * 内置资源包注册（Fabric 侧）。
 *
 * <p>包里放的是<b>原清晰度</b>贴图（{@code resourcepacks/lted_hd/}），默认包的贴图已经被
 * {@code tools/strip-textures.sh} 缩到最大边 512。玩家在「资源包」界面把
 * <b>LTED 高清贴图</b> 勾上即可恢复原图 —— 默认不勾（{@link ResourcePackActivationType#NORMAL}），
 * 弱显卡不受影响。
 *
 * <p>加载顺序：Fabric 的 mod 资源包（= 本模组默认的 512 贴图）排在内置包<b>下面</b>，
 * 所以勾选后高清图自然覆盖默认图，不需要额外干预。
 *
 * <p>Forge 侧同构实现见 {@code dev.js.mtrheadlight.forge.platform.BuiltinPacks}；
 * java-gen 里的调用点由 {@code tools/fix-java-gen-platform.js} 改写 import。
 */
public final class BuiltinPacks {

    /** mod id：与 fabric.mod.json / mods.toml 一致。 */
    public static final String MOD_ID = "mtrlte";

    /** 内置包在 jar 里的目录（Fabric 规定为 {@code resourcepacks/<id 的 path>}）。 */
    public static final String HD_PACK_PATH = "lted_hd";

    /** 语言键：简体中文「LTED 高清贴图」/ 英文「LTED HD Textures」。 */
    public static final String HD_NAME_KEY = "pack." + MOD_ID + "." + HD_PACK_PATH;

    private BuiltinPacks() {
    }

    /** 在客户端初始化阶段调用一次。 */
    public static void register() {
        final FabricLoader loader = FabricLoader.getInstance();
        final var container = loader.getModContainer(MOD_ID);
        if (container.isEmpty()) {
            LteLog.LOGGER.warn("[lte] builtin pack {}: 找不到 mod 容器，跳过", HD_PACK_PATH);
            return;
        }
        // NORMAL = 默认关闭、玩家可手动勾选（DEFAULT_ENABLED 是数据包才允许的）。
        final boolean ok = ResourceManagerHelper.registerBuiltinResourcePack(
                new Identifier(MOD_ID, HD_PACK_PATH),
                container.get(),
                Text.translatable(HD_NAME_KEY),
                ResourcePackActivationType.NORMAL);
        LteLog.LOGGER.info("[lte] builtin pack {}: {} (默认关闭)",
                MOD_ID + ":" + HD_PACK_PATH, ok ? "已注册" : "注册失败");
    }
}
