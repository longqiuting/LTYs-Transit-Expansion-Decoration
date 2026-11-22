// 迁移自 dev.js.mtrheadlight.security.LiquidScannerItem（yarn）→ official（mojmap）。
// 无渲染的纯物品：只多一条 tooltip。
package dev.js.mtrheadlight.forge.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 带 tooltip 的简单物品 {@code mtrlte:lte_tool}。
 *
 * <p>yarn → mojmap：
 * {@code appendTooltip} → {@link #appendHoverText}，
 * {@code TooltipContext} → {@link TooltipFlag}，
 * {@code Text} → {@link Component}，
 * {@code Formatting} → {@link ChatFormatting}，
 * {@code World} → {@link Level}，
 * {@code Item.Settings} → {@code Item.Properties}。
 */
public class LtedTooltipItem extends Item {

    public LtedTooltipItem(final Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level,
                                final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mtrlte.lte_tool").withStyle(ChatFormatting.GRAY));
    }
}
