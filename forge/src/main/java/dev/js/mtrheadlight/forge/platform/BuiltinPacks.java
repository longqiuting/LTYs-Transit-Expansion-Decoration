package dev.js.mtrheadlight.forge.platform;

import dev.js.mtrheadlight.LteLog;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.resource.PathPackResources;

/**
 * 内置资源包注册（Forge 侧 shim，对应 Fabric 的 {@code dev.js.mtrheadlight.platform.BuiltinPacks}）。
 *
 * <p>包里放的是<b>原清晰度</b>贴图（jar 内 {@code resourcepacks/lted_hd/}），默认包的贴图已经被
 * {@code tools/strip-textures.sh} 缩到最大边 512。玩家在「资源包」界面把 <b>LTED 高清贴图</b>
 * 勾上即可恢复原图；本类只负责把它<b>列出来</b>（{@link PackSource#BUILT_IN} +
 * {@code Pack.Position.TOP}），默认不启用由玩家自己决定。
 *
 * <p>{@link #register()} 与 Fabric 侧方法同名同签名（java-gen 里的调用点因此不用分支），
 * 真正的挂载点是 mod 总线的 {@link AddPackFindersEvent}，由 {@code ClientBootstrap} 接线。
 */
public final class BuiltinPacks {

    /** mod id：与 mods.toml 一致。 */
    public static final String MOD_ID = "mtrlte";

    /** 内置包在 jar 里的目录（与 Fabric 侧同一个目录，双方共用一套资源）。 */
    public static final String HD_PACK_PATH = "resourcepacks/lted_hd";

    /** 资源包在存档/选项里用的 id（与 Fabric 侧一致：{@code mtrlte:lted_hd}）。 */
    public static final String HD_PACK_ID = MOD_ID + ":lted_hd";

    /** 语言键：简体中文「LTED 高清贴图」/ 英文「LTED HD Textures」。 */
    public static final String HD_NAME_KEY = "pack." + MOD_ID + ".lted_hd";

    private BuiltinPacks() {
    }

    /**
     * 与 Fabric 侧同名入口（java-gen 的调用点因此不需要分支）。
     *
     * <p>Forge 侧这里只打一行日志：真正的挂载必须等到 {@link AddPackFindersEvent}，
     * 那条总线在 {@code ClientBootstrap} 里接（与客户端初始化谁先谁后都无所谓）。
     */
    public static void register() {
        LteLog.LOGGER.info("[lte] builtin pack {}: 由 AddPackFindersEvent 挂载（默认关闭）", HD_PACK_ID);
    }

    /** mod 总线：{@code ClientBootstrap} 注册。 */
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        final IModFileInfo modFileInfo = ModList.get().getModFileById(MOD_ID);
        if (modFileInfo == null) {
            LteLog.LOGGER.warn("[lte] builtin pack {}: 找不到 mod 文件，跳过", HD_PACK_ID);
            return;
        }
        final Path root;
        try {
            root = modFileInfo.getFile().findResource(HD_PACK_PATH.split("/"));
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] builtin pack {}: 解析包目录失败：{}", HD_PACK_ID, t.toString());
            return;
        }
        if (root == null || !Files.isDirectory(root)) {
            LteLog.LOGGER.warn("[lte] builtin pack {}: jar 里没有 {}，跳过", HD_PACK_ID, HD_PACK_PATH);
            return;
        }
        event.addRepositorySource(consumer -> {
            // required=false → 默认不勾；Position.TOP → 排在默认包（mod 资源）上面，勾上即覆盖。
            final Pack pack = Pack.readMetaAndCreate(HD_PACK_ID,
                    Component.translatable(HD_NAME_KEY), false,
                    id -> new PathPackResources(id, true, root),
                    PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack == null) {
                LteLog.LOGGER.warn("[lte] builtin pack {}: pack.mcmeta 读取失败，未挂载", HD_PACK_ID);
                return;
            }
            consumer.accept(pack);
            LteLog.LOGGER.info("[lte] builtin pack {}: 已挂载（默认关闭）", HD_PACK_ID);
        });
    }
}
