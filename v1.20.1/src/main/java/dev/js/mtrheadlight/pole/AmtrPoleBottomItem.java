// 柱底的方块物品：放下时把「ctrl+中键复制来的属性」套到方块实体上。
package dev.js.mtrheadlight.pole;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * {@code mtrlte:amtr_pole_bottom} 的方块物品。
 *
 * <h2>为什么必须有自己的一类</h2>
 * <p>「ctrl+中键复制属性 → 批量放置」的那一半在这里：{@link #place} 走完原版的放置流程后，
 * <b>服务端</b>拿着手里这堆的 NBT 去改刚放下的那个方块实体（{@link AmtrPoleBottomBlockEntity#applyFromItem}）。
 * 没有 NBT = 跟随车站色 —— 于是落地的柱子会当场解析车站色。
 *
 * <p>属性只从物品流向方块，<b>反过来不成立</b>：玩家用刷子改完颜色，手里的物品堆一点都不变
 * （那才是「下次放下还是老颜色」的来源）。想批量就用 ctrl+中键重新复制一次。
 */
public final class AmtrPoleBottomItem extends BlockItem {

    public AmtrPoleBottomItem(final Block block, final Settings settings) {
        super(block, settings);
    }

    @Override
    public ActionResult place(final ItemPlacementContext context) {
        final ActionResult result = super.place(context);
        if (!result.isAccepted() || context.getWorld().isClient) {
            return result;
        }
        if (context.getWorld().getBlockEntity(context.getBlockPos()) instanceof AmtrPoleBottomBlockEntity pole) {
            pole.applyFromItem(context.getStack());
        }
        return result;
    }

    @Override
    public void appendTooltip(final ItemStack stack, @Nullable final World world,
                              final List<Text> tooltip, final TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        PoleItemData.appendTooltip(stack, tooltip);
    }
}
