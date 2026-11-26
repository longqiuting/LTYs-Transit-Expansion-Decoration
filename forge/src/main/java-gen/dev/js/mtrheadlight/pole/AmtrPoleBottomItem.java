package dev.js.mtrheadlight.pole;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 柱底的方块物品（Forge / mojmap 版）：放下时把 ctrl+中键复制来的属性套到方块实体上。
 *
 * <p>与 Fabric 侧 {@code pole/AmtrPoleBottomItem.java} 一一对应：
 * {@code place(ItemPlacementContext)} → {@link #place(BlockPlaceContext)}，
 * {@code ActionResult.isAccepted()} → {@link InteractionResult#consumesAction()}，
 * {@code appendTooltip} → {@link #appendHoverText}。
 */
public final class AmtrPoleBottomItem extends BlockItem {

    public AmtrPoleBottomItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(final BlockPlaceContext context) {
        final InteractionResult result = super.place(context);
        if (!result.consumesAction() || context.getLevel().isClientSide) {
            return result;
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof AmtrPoleBottomBlockEntity pole) {
            pole.applyFromItem(context.getItemInHand());
        }
        return result;
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level,
                                final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        PoleItemData.appendTooltip(stack, tooltip);
    }
}
