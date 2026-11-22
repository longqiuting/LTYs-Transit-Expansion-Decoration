// 迁移自 dev.js.mtrheadlight.clock.ClockBlock（yarn）→ official（mojmap）。
// 行为保持：6 向 FACING、空碰撞箱、1px 薄板轮廓、红石 POWERED、刷子右键（MTR 的刷子）。
package dev.js.mtrheadlight.forge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerPlayer;

/**
 * 时钟方块 {@code mtrlte:clock}（Forge / mojmap 版）。
 *
 * <p>与 Fabric 版逐条对应，只换名字：
 * <ul>
 *   <li>{@code BlockEntityProvider} → {@link EntityBlock}，
 *       {@code createBlockEntity} → {@link #newBlockEntity}；</li>
 *   <li>{@code AbstractBlock.Settings.create()} → {@code BlockBehaviour.Properties.of()}；</li>
 *   <li>轮廓 {@code getOutlineShape} → {@link #getShape}，{@code getCollisionShape} 同名；</li>
 *   <li>{@code canPlaceAt} → {@link #canSurvive}，{@code getPlacementState} → {@link #getStateForPlacement}；</li>
 *   <li>{@code neighborUpdate} → {@link #neighborChanged}，{@code onBlockAdded} → {@link #onPlace}；</li>
 *   <li>{@code onUse} → {@link #use}，{@code ActionResult} → {@link InteractionResult}。</li>
 * </ul>
 *
 * <p><b>MTR 依赖</b>：刷子判定仍是 {@code mtr.Items.BRUSH.get()} —— 证明 YMTR 的 Forge jar
 * 在 official 映射下可以直接当 compileOnly 用（它的签名本来就是
 * {@code net.minecraft.world.item.Item}）。
 *
 * <p><b>本轮降级</b>：打开配置界面的网络包（Fabric 侧的 {@code ClockNet.sendOpen}）还没迁，
 * 这里先只回 {@link InteractionResult#SUCCESS} 并把坐标打到日志，等网络层移植后再接。
 */
public final class ClockBlock extends Block implements EntityBlock {

    /** 钟面朝向（6 向）。 */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** 被红石充能。 */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape SHAPE_NORTH = Block.box(3.0D, 3.0D, 15.0D, 13.0D, 13.0D, 16.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(3.0D, 3.0D, 0.0D, 13.0D, 13.0D, 1.0D);
    private static final VoxelShape SHAPE_WEST = Block.box(15.0D, 3.0D, 3.0D, 16.0D, 13.0D, 13.0D);
    private static final VoxelShape SHAPE_EAST = Block.box(0.0D, 3.0D, 3.0D, 1.0D, 13.0D, 13.0D);
    private static final VoxelShape SHAPE_DOWN = Block.box(3.0D, 15.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    private static final VoxelShape SHAPE_UP = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D);

    public ClockBlock(final Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    public static VoxelShape shapeFor(final Direction facing) {
        switch (facing) {
            case SOUTH:
                return SHAPE_SOUTH;
            case WEST:
                return SHAPE_WEST;
            case EAST:
                return SHAPE_EAST;
            case UP:
                return SHAPE_UP;
            case DOWN:
                return SHAPE_DOWN;
            case NORTH:
            default:
                return SHAPE_NORTH;
        }
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    /** 碰撞箱是空的：时钟不挡人。 */
    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                        final CollisionContext context) {
        return Shapes.empty();
    }

    /** 任意位置可放。 */
    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction facing = context.getClickedFace();
        final BlockPos pos = context.getClickedPos();
        final boolean powered = context.getLevel().hasNeighborSignal(pos);
        return defaultBlockState().setValue(FACING, facing).setValue(POWERED, Boolean.valueOf(powered));
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                final Block neighborBlock, final BlockPos neighborPos, final boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        final boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED).booleanValue()) {
            level.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(powered)), Block.UPDATE_ALL);
        }
    }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos,
                        final BlockState oldState, final boolean movedByPiston) {
        if (!level.isClientSide && level.hasNeighborSignal(pos) != state.getValue(POWERED).booleanValue()) {
            level.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(!state.getValue(POWERED).booleanValue())),
                    Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ClockBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                 final BlockEntityType<T> type) {
        if (type != dev.js.mtrheadlight.forge.LtedForge.CLOCK_BE.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<ClockBlockEntity>) ClockBlockEntity::tick;
    }

    /** 刷子右键 → 配置界面（网络层待迁，见类注释）。 */
    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos,
                                 final Player player, final InteractionHand hand, final BlockHitResult hit) {
        final ItemStack held = player.getItemInHand(hand);
        if (!held.is(mtr.Items.BRUSH.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        ClockBlockEntity.LOGGER.debug("[lte] clock at {} opened by {} (config UI not ported yet)",
                pos, serverPlayer.getScoreboardName());
        return InteractionResult.SUCCESS;
    }
}
