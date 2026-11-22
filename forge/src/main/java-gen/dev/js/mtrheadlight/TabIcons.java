package dev.js.mtrheadlight;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class TabIcons {
   public static final ResourceLocation CORE_ID = new ResourceLocation("mtrlte", "tab_core");
   public static final ResourceLocation TOOLS_ID = new ResourceLocation("mtrlte", "tab_tools");
   private static Item core;
   private static Item tools;
   private static boolean registered;

   private TabIcons() {
   }

   public static void register() {
      if (!registered) {
         registered = true;
         core = (Item)Registry.register(BuiltInRegistries.ITEM, CORE_ID, new Item(new Item.Properties()));
         tools = (Item)Registry.register(BuiltInRegistries.ITEM, TOOLS_ID, new Item(new Item.Properties()));
      }
   }

   public static Item core() {
      register();
      return core;
   }

   public static Item tools() {
      register();
      return tools;
   }
}
