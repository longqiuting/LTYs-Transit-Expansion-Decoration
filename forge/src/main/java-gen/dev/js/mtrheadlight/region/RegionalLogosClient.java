// 地区标志块的客户端接线（Forge 侧）：三张挖空的 logo 贴图必须挂 cutout 渲染层。
package dev.js.mtrheadlight.region;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Layers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

/** 福州 / 深圳 / 港铁标志块：透明 logo 贴图走 cutout，否则整面被底色糊满。 */
public final class RegionalLogosClient implements ClientInit {

    @Override
    public void onInitializeClient() {
        put(RegionalLogos.fuzhouLogo(), "fuzhou logo");
        put(RegionalLogos.shenzhenLogo(), "shenzhen logo");
        put(RegionalLogos.mtrHkLogo(), "mtr hk logo");
    }

    private static void put(final Block block, final String what) {
        if (block == null) {
            return;
        }
        try {
            Layers.put(block, RenderType.cutout());
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] could not set the cutout layer for the {} block", what, t);
        }
    }
}
