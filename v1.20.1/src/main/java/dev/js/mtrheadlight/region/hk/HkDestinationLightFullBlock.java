// 港铁终点站指示灯 · 满方块款：一格完整的方块，整块发光（适合埋进墙体 / 吊顶里做整块色灯）。
package dev.js.mtrheadlight.region.hk;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * 满方块款终点站指示灯。
 *
 * <p>和贴门顶那条灯带同一套颜色规则（{@link DestinationColors}）与渲染方式（
 * {@link HkDestinationLightRenderer} 满亮 + 终点站色），只是外形换成一整格立方体：
 * 埋进墙里、吊顶里、柱子侧面都能用，哪面外露哪面就发光。
 *
 * <p>这个款没有朝向属性 —— 立方体四面一样，放下去不需要转。
 */
public class HkDestinationLightFullBlock extends Block implements BlockEntityProvider {

    public HkDestinationLightFullBlock(final AbstractBlock.Settings settings) {
        super(settings);
    }

    /** 带方块实体的方块默认是 INVISIBLE，必须显式改成 MODEL，否则深色灯体不渲染。 */
    @Override
    public BlockRenderType getRenderType(final BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(final BlockPos pos, final BlockState state) {
        return new HkDestinationLightBlockEntity(pos, state);
    }
}
