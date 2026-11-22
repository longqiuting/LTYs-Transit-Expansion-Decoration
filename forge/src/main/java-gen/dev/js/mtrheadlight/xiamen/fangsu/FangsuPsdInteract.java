package dev.js.mtrheadlight.xiamen.fangsu;

import mtr.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class FangsuPsdInteract {
   private FangsuPsdInteract() {
   }

   static boolean isBrush(ItemStack stack) {
      return matches(stack, safe("brush"));
   }

   static boolean isDriverKey(ItemStack stack) {
      return matches(stack, safe("driver_key"));
   }

   private static Item safe(String which) {
      try {
         return which.equals("brush") ? (Item)Items.BRUSH.get() : (Item)Items.DRIVER_KEY.get();
      } catch (Throwable var2) {
         return net.minecraft.world.item.Items.AIR;
      }
   }

   private static boolean matches(ItemStack stack, Item wanted) {
      return wanted != null && wanted != net.minecraft.world.item.Items.AIR && stack != null && !stack.isEmpty() && stack.getItem() == wanted;
   }
}
