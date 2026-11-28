// 福州地铁售票机：上下两块，几何全部走运行时网格（素材不是原版模型格式）。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.rawmodel.RawModelJson;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * {@code mtrlte:fuzhou_ticket_machine_top} / {@code _bottom} —— 福州地铁售票机。
 *
 * <h2>为什么整台走 BER</h2>
 * <p>素材是 Blockbench 自由格式（UV/元素都不在原版模型的能力范围内），烘焙模型吃不下，
 * 所以方块本身没有 blockstate 模型，几何全部由 {@link FuzhouMachineRenderer} 用 rawmodels
 * 在运行时画（{@code fuzhou_machine_bottom} / {@code _top} / {@code _bottom_color} /
 * {@code _top_color} / {@code _screen}）。
 *
 * <h2>交互</h2>
 * <p>只有上半块（{@code _top}）有交互：手持 MTR 刷子右键打开配置面板（
 * {@link FuzhouMachineConfigScreen}）。下半块右键直接放行。
 */
public final class FuzhouMachineBlock extends BlockWithEntity {

    /** 四向（与 {@code HorizontalFacingBlock.FACING} 同一个属性实例）。 */
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    /** 模型读不到时的兜底包围盒（模型空间 0..16）。 */
    private static final float[] FALLBACK_BOX = {0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 16.0F};

    private final boolean top;
    private final String rawModel;
    private final String colorModel;
    private final VoxelShape[] shapes = new VoxelShape[4];

    public FuzhouMachineBlock(final AbstractBlock.Settings settings, final boolean top,
                              final String rawModel, final String colorModel) {
        super(settings);
        this.top = top;
        this.rawModel = rawModel;
        this.colorModel = colorModel;
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));

        final float[] bounds = RawModelJson.bounds(RawModelJson.id(rawModel));
        final float[] box = bounds == null ? FALLBACK_BOX : bounds;
        for (int i = 0; i < 4; i++) {
            this.shapes[i] = rotatedBox(box, i);
        }
    }

    public boolean isTop() {
        return this.top;
    }

    public String rawModel() {
        return this.rawModel;
    }

    public String colorModel() {
        return this.colorModel;
    }

    /**
     * 渲染类型：这里<b>故意</b>保持 {@code INVISIBLE} —— 方块没有任何方块模型，
     * 全部几何由 BER 画；如果改成 MODEL，原版会去找一个不存在的烘焙模型。
     */
    @Override
    public BlockRenderType getRenderType(final BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    protected void appendProperties(final StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(final ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    public BlockEntity createBlockEntity(final BlockPos pos, final BlockState state) {
        return new FuzhouMachineBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(final BlockState state, final World world, final BlockPos pos,
                              final PlayerEntity player, final Hand hand, final BlockHitResult hit) {
        if (!this.top || !player.getStackInHand(hand).isOf(mtr.Items.BRUSH.get())) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof final FuzhouMachineBlockEntity machine
                && player instanceof final net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            FuzhouMachineNet.sendOpen(serverPlayer, pos, machine);
        }
        return ActionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ 形状

    @Override
    public VoxelShape getOutlineShape(final BlockState state, final BlockView world,
                                      final BlockPos pos, final ShapeContext context) {
        return this.shapes[state.get(FACING).getHorizontal()];
    }

    private static VoxelShape rotatedBox(final float[] b, final int quarterTurns) {
        double x1 = b[0], y1 = b[1], z1 = b[2], x2 = b[3], y2 = b[4], z2 = b[5];
        for (int i = 0; i < quarterTurns; i++) {
            final double nx1 = 16.0D - z2;
            final double nx2 = 16.0D - z1;
            z1 = x1;
            z2 = x2;
            x1 = nx1;
            x2 = nx2;
        }
        return Block.createCuboidShape(x1, y1, z1, x2, y2, z2);
    }
}
