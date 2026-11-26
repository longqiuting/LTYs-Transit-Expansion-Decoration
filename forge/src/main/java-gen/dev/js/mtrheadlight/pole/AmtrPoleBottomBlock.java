package dev.js.mtrheadlight.pole;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * {@code mtrlte:amtr_pole_bottom}（Forge / mojmap 版）。
 *
 * <p>与 Fabric 树 {@code pole/AmtrPoleBottomBlock.java} 同一套行为，只换名字：
 * {@code BlockWithEntity}/{@code createBlockEntity} → {@link EntityBlock}/{@link #newBlockEntity}，
 * {@code onUse} → {@link #use}（{@code ActionResult} → {@link InteractionResult}），
 * {@code afterBreak} → {@link #playerDestroy}。刷子判定仍是 {@code mtr.Items.BRUSH.get()}。
 *
 * <p>模型里 {@code color} 那 8 个 element 带 {@code tintindex: 0}，颜色由
 * {@code RegisterColorHandlersEvent.Block} 注册的 provider 每帧提供（见 {@code PoleTint}），
 * 读的就是 {@link #effectiveTint} —— 与 Fabric 侧同一个入口名。
 */
public final class AmtrPoleBottomBlock extends Block implements EntityBlock {

    public AmtrPoleBottomBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new AmtrPoleBottomBlockEntity(pos, state);
    }

    /**
     * 方块实体 ticker：只负责「AUTO 模式下车站色还没解析出来」时重试（{@code /setblock} / 结构方块
     * / 旧存档放下的柱子也要能取到车站色）。
     */
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state,
            final net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (type != PoleBlocks.bottomEntityType()) {
            return null;
        }
        return (net.minecraft.world.level.block.entity.BlockEntityTicker<T>)
                (net.minecraft.world.level.block.entity.BlockEntityTicker<AmtrPoleBottomBlockEntity>)
                        AmtrPoleBottomBlockEntity::tick;
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos,
                                 final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (!player.getItemInHand(hand).is(mtr.Items.BRUSH.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof AmtrPoleBottomBlockEntity pole)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isCrouching()) {
            pole.resetToStationColor();
        } else if (player instanceof ServerPlayer serverPlayer) {
            PoleNet.sendOpen(serverPlayer, pos, pole);
        }
        return InteractionResult.SUCCESS;
    }

    /** 染色 provider 的入口：只读方块实体里缓存好的颜色（每帧每面都会调）。 */
    public static int effectiveTint(@Nullable final BlockAndTintGetter level, @Nullable final BlockPos pos) {
        if (level == null || pos == null) {
            return PoleColors.WHITE;
        }
        if (level.getBlockEntity(pos) instanceof AmtrPoleBottomBlockEntity pole) {
            return pole.effectiveColor();
        }
        return PoleColors.WHITE;
    }

    /** 破坏时把自定义颜色留在掉落物上（与 Fabric 侧的 {@code afterBreak} 一致）。 */
    @Override
    public void playerDestroy(final Level level, final Player player, final BlockPos pos,
                              final BlockState state, @Nullable final BlockEntity blockEntity,
                              final ItemStack tool) {
        if (!level.isClientSide && !player.isCreative()
                && blockEntity instanceof AmtrPoleBottomBlockEntity pole) {
            final ItemStack drop = new ItemStack(PoleBlocks.bottomItem());
            if (pole.isCustomMode()) {
                PoleItemData.write(drop, true, pole.customColor());
            }
            final ItemEntity entity = new ItemEntity(level,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
            return;
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
}
