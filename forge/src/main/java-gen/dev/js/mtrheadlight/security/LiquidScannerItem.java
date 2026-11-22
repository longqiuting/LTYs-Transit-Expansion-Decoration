package dev.js.mtrheadlight.security;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class LiquidScannerItem extends BlockItem {
   public LiquidScannerItem(LiquidScannerBlock block, Item.Properties settings) {
      super(block, settings);
   }

   public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
      super.appendHoverText(stack, world, tooltip, context);
      tooltip.add(Component.translatable("tooltip.mtrlte.liquid_scanner").withStyle(ChatFormatting.GRAY));
   }
}
