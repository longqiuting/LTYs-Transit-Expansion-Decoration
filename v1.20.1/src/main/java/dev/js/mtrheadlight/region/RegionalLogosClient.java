// 地区标志块的客户端接线：三张挖空的 logo 贴图必须挂 cutout 渲染层。
package dev.js.mtrheadlight.region;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;

/**
 * 福州 / 深圳 / 港铁三个标志块的渲染层。
 *
 * <p>logo 贴图外圈 alpha = 0，方块本身却是不透明满方块。走默认的 solid 层时原版<b>不做 alpha 测试</b>，
 * 透明像素会按它自己的 RGB 画出来 —— 表现就是「整面被 logo 的底色糊满」（福州绿、港铁红），
 * 只剩一个极淡的图形轮廓。和 MTR 给 {@code Blocks.LOGO} 挂 {@code getCutout()} 是同一个理由。
 *
 * <p>逐个 try/catch：一个挂了不影响其它两个。
 */
public final class RegionalLogosClient implements ClientModInitializer {

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
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout());
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] could not set the cutout layer for the {} block", what, t);
        }
    }
}
