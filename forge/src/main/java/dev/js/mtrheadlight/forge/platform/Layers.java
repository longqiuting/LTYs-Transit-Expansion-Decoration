package dev.js.mtrheadlight.forge.platform;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

/**
 * 渲染层 shim：Fabric 的 {@code BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType)}
 * → Forge 的 {@link ItemBlockRenderTypes#setRenderLayer(Block, RenderType)}。
 *
 * <p>只能在客户端初始化阶段（{@code FMLClientSetupEvent} 及其后）调用，方块此时已注册完毕。
 */
public final class Layers {

    private Layers() {
    }

    public static void put(Block block, RenderType type) {
        if (block != null && type != null) {
            ItemBlockRenderTypes.setRenderLayer(block, type);
        }
    }
}
