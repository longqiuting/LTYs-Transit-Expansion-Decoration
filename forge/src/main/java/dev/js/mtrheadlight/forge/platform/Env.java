package dev.js.mtrheadlight.forge.platform;

import java.nio.file.Path;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * 环境 shim：Fabric 的 {@code FabricLoader} → Forge 的 {@link FMLPaths} / {@link ModList}。
 */
public final class Env {

    private Env() {
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** 模组元数据版本（拿不到时返回 {@code "?"}，与 Fabric 侧 {@code orElse("?")} 一致）。 */
    public static String modVersion(String modId) {
        return ModList.get()
                .getModContainerById(modId)
                .map(ModContainer::getModInfo)
                .map(info -> info.getVersion().toString())
                .orElse("?");
    }
}
