// 福州地铁售票机：一个方块 id + HALF（下/上两段），几何走运行时网格。
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
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * {@code mtrlte:fuzhou_ticket_machine} —— 福州地铁售票机。
 *
 * <h2>一个 id + HALF（跟厦门售票机同一套做法）</h2>
 * <p>不是一个 id 上下两块、也不是两个物品：和 {@code mtrlte:ticket_machine_xiamen} 一样，
 * 一个方块 id 带 {@code FACING} + {@code HALF}，放置时自动补上上半段、拆任一半两段一起掉，
 * 所以创造页签里只有<b>一个</b>物品。
 *
 * <h2>为什么整台走 BER</h2>
 * <p>素材是 Blockbench 自由格式（元素 / UV / 旋转不在原版方块模型的能力范围内），烘焙模型吃不下，
 * 所以方块本身没有模型几何，由 {@link FuzhouMachineRenderer} 按 {@code HALF} 选
 * {@code fuzhou_machine_bottom} / {@code fuzhou_machine_top} 在运行时画。
 */
public final class FuzhouMachineBlock extends BlockWithEntity {

    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    private static final float[] FALLBACK_BOX = {0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 16.0F};

    private final VoxelShape[] lowerShapes = new VoxelShape[4];
    private final VoxelShape[] upperShapes = new VoxelShape[4];

    public FuzhouMachineBlock(final AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(HALF, DoubleBlockHalf.LOWER));

        final float[] lower = boxOf("fuzhou_machine_bottom", 0.0F);
        final float[] upper = boxOf("fuzhou_machine_top", 0.0F);
        for (int i = 0; i < 4; i++) {
            this.lowerShapes[i] = rotatedBox(lower, i);
            this.upperShapes[i] = rotatedBox(upper, i);
        }
    }

    private static float[] boxOf(final String rawModel, final float dy) {
        final float[] b = RawModelJson.bounds(RawModelJson.id(rawModel));
        if (b == null) {
            return FALLBACK_BOX;
        }
        return new float[]{b[0], b[1] + dy, b[2], b[3], b[4] + dy, b[5]};
    }

    /** 上段才有交互（配置面板）。 */
    public static boolean isUpper(final BlockState state) {
        return state.contains(HALF) && state.get(HALF) == DoubleBlockHalf.UPPER;
    }

    /** 该半段用哪份原始模型（渲染器用）。 */
    public String rawModel(final BlockState state) {
        return isUpper(state) ? "fuzhou_machine_top" : "fuzhou_machine_bottom";
    }

    /** 该半段的 color 组模型（渲染器用）。 */
    public String colorModel(final BlockState state) {
        return isUpper(state) ? "fuzhou_machine_top_color" : "fuzhou_machine_bottom_color";
    }

    /** 方块本身没有烘焙模型：全部几何由 BER 画。 */
    @Override
    public BlockRenderType getRenderType(final BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    protected void appendProperties(final StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(final ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing())
                .with(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void onPlaced(final World world, final BlockPos pos, final BlockState state,
                         @Nullable final LivingEntity placer, final ItemStack itemStack) {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
    }

    /** 拆任一半，另一半一起掉（与原版双高方块一致）。 */
    @Override
    public void onStateReplaced(final BlockState state, final World world, final BlockPos pos,
                                final BlockState newState, final boolean moved) {
        if (!state.isOf(newState.getBlock()) && state.contains(HALF)) {
            final BlockPos other = state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos.up();
            final BlockState otherState = world.getBlockState(other);
            if (otherState.isOf(this)) {
                world.setBlockState(other, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public ItemStack getPickStack(final BlockView world, final BlockPos pos, final BlockState state) {
        return new ItemStack(FuzhouMachines.item());
    }

    @Override
    public BlockEntity createBlockEntity(final BlockPos pos, final BlockState state) {
        return new FuzhouMachineBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(final BlockState state, final BlockView world,
                                      final BlockPos pos, final ShapeContext context) {
        final int index = state.get(FACING).getHorizontal();
        return isUpper(state) ? this.upperShapes[index] : this.lowerShapes[index];
    }

    @Override
    public ActionResult onUse(final BlockState state, final World world, final BlockPos pos,
                              final PlayerEntity player, final Hand hand, final BlockHitResult hit) {
        if (!isUpper(state)) {
            return ActionResult.PASS;
        }

        // ① 刷子：打开配置面板（模式 / 面板颜色 / LED 文字）
        if (player.getStackInHand(hand).isOf(mtr.Items.BRUSH.get())) {
            if (world.isClient) {
                return ActionResult.SUCCESS;
            }
            if (player instanceof final net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                dev.js.mtrheadlight.machine.MachineConfigNet.open(serverPlayer, pos);
            }
            return ActionResult.SUCCESS;
        }

        // ② 普通右键：按配置好的模式开售票界面（和厦门 / 港铁那两台同一套行为）
        final int mode = world.getBlockEntity(pos) instanceof final FuzhouMachineBlockEntity machine
                ? machine.mode() : FuzhouMachineBlockEntity.MODE_FANGSU;

        if (mode == FuzhouMachineBlockEntity.MODE_OFF) {
            if (!world.isClient) {
                player.sendMessage(net.minecraft.text.Text.translatable(
                        "message.mtrlte.ticket_machine.powered_off"), true);
            }
            return ActionResult.SUCCESS;
        }

        if (mode == FuzhouMachineBlockEntity.MODE_FANGSU) {
            if (world.isClient) {
                if (dev.js.mtrheadlight.xiamen.machine.FangsuTicketUi.open(
                        net.minecraft.text.Text.translatable("screen.mtrlte.ticket_machine_fuzhou"), pos)) {
                    return ActionResult.SUCCESS;
                }
                // 客户端没装方速：提示一次，不要把方块顺手放上去
                player.sendMessage(net.minecraft.text.Text.translatable(
                        "message.mtrlte.ticket_machine.client_missing"), true);
                return ActionResult.SUCCESS;
            }
            // 服务端没装方速（客户端装了）：退回原版界面，免得点了没反应
            if (!dev.js.mtrheadlight.xiamen.machine.FangsuTicketUi.loaded()) {
                openVanilla(world, player);
            }
            return ActionResult.SUCCESS;
        }

        // ③ 原版 MTR 界面：服务端发包
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        openVanilla(world, player);
        return ActionResult.SUCCESS;
    }

    /** 原版（MTR）售票界面：服务端把「开界面」的包发给点方块的玩家。 */
    private static void openVanilla(final World world, final PlayerEntity player) {
        if (player instanceof final net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            mtr.packet.PacketTrainDataGuiServer.openTicketMachineScreenS2C(world, serverPlayer);
        }
    }

    @Override
    public void appendTooltip(final ItemStack stack, final net.minecraft.world.BlockView world,
                              final List<Text> tooltip, final TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.mtrlte.fuzhou_ticket_machine").formatted(Formatting.GRAY));
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
