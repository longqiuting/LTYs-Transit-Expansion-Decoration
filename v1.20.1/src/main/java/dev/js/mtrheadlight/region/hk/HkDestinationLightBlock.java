// 港铁终点站指示灯：贴在屏蔽门上方的一条灯带（整格一段，平铺成一条）。
package dev.js.mtrheadlight.region.hk;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

/**
 * 港铁终点站指示灯（屏蔽门顶上那条 LED）。
 *
 * <h2>为什么是「一个方块平铺」</h2>
 * 模型本身只有一格宽（0.2–15.8、y14–16、z0–2），需要多长就放多少段，顶边自然连成一条。
 * 这样没有「一格宽却要跨多格」的对齐问题，也不需要在方块实体里存「这条灯带有多长」。
 *
 * <h2>为什么没有发光 / 颜色属性</h2>
 * 颜色取决于「这个站台正在进站的车开往哪个终点站」，那是<b>纯客户端</b>信息（MTR 的列车
 * 只在客户端模拟，服务端没有）。所以：
 * <ul>
 *   <li>方块本体只是一条深色灯体（普通方块模型，走正常光照）；</li>
 *   <li>亮起来的那层彩色是 {@link HkDestinationLightRenderer} 画的：整块取满亮 + 乘终点站色；</li>
 *   <li>没有进站车时渲染器什么都不画 —— 灯就是灭的，方块本体仍在（和真实熄灭的灯带一样）。</li>
 * </ul>
 * 这样整条链路上没有一个服务端状态要同步，也就不会因为列车状态变化而刷方块更新。
 */
public class HkDestinationLightBlock extends HorizontalFacingBlock implements BlockEntityProvider {

    /** 灯带贴在方块哪一面朝外（水平四向）。 */
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    /** 灯体包围盒（模型空间 1/16 格）：{@code {minX, minY, minZ, maxX, maxY, maxZ}}，朝北那面。 */
    private static final float[] SHAPE = {0.2F, 14.0F, 0.0F, 15.8F, 16.0F, 2.0F};

    public HkDestinationLightBlock(final AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(final StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(final ItemPlacementContext ctx) {
        // 面朝玩家（玩家看的那一面就是灯带贴的那面），和贴墙标识同一个约定
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(final BlockState state, final BlockView world,
                                      final BlockPos pos, final ShapeContext context) {
        return rotated(SHAPE, state.get(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockView world,
                                        final BlockPos pos, final ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(final BlockPos pos, final BlockState state) {
        return new HkDestinationLightBlockEntity(pos, state);
    }

    /** 把「朝北」的盒子按朝向转过去。 */
    private static VoxelShape rotated(final float[] b, final Direction facing) {
        double x1 = b[0], y1 = b[1], z1 = b[2], x2 = b[3], y2 = b[4], z2 = b[5];
        final int turns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        for (int i = 0; i < turns; i++) {
            final double nx1 = 16.0D - z2;
            final double nx2 = 16.0D - z1;
            z1 = x1;
            z2 = x2;
            x1 = nx1;
            x2 = nx2;
        }
        return net.minecraft.block.Block.createCuboidShape(x1, y1, z1, x2, y2, z2);
    }

}
