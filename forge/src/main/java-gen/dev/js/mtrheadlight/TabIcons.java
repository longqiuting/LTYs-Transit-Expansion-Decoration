package dev.js.mtrheadlight;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import dev.js.mtrheadlight.forge.platform.Reg;

// mojmap 版（yarn → mojmap：Identifier → ResourceLocation，Registries.ITEM → BuiltInRegistries.ITEM，
// Registry.register → Reg.item）。与 Fabric 侧 v1.20.1/.../TabIcons.java 一一对应。
public final class TabIcons {
   public static final ResourceLocation CORE_ID = new ResourceLocation("mtrlte", "tab_core");
   public static final ResourceLocation TOOLS_ID = new ResourceLocation("mtrlte", "tab_tools");
   public static final ResourceLocation FUZHOU_ID = new ResourceLocation("mtrlte", "tab_fuzhou");
   public static final ResourceLocation SHENZHEN_ID = new ResourceLocation("mtrlte", "tab_shenzhen");
   public static final ResourceLocation MTR_HK_ID = new ResourceLocation("mtrlte", "tab_mtr_hk");
   private static Item core;
   private static Item tools;
   private static Item fuzhou;
   private static Item shenzhen;
   private static Item mtrHk;
   private static boolean registered;

   private TabIcons() {
   }

   public static void register() {
      if (!registered) {
         registered = true;
         core = (Item)Reg.item(CORE_ID, new Item(new Item.Properties()));
         tools = (Item)Reg.item(TOOLS_ID, new Item(new Item.Properties()));
         fuzhou = (Item)Reg.item(FUZHOU_ID, new Item(new Item.Properties()));
         shenzhen = (Item)Reg.item(SHENZHEN_ID, new Item(new Item.Properties()));
         mtrHk = (Item)Reg.item(MTR_HK_ID, new Item(new Item.Properties()));
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

   public static Item fuzhou() {
      register();
      return fuzhou;
   }

   public static Item shenzhen() {
      register();
      return shenzhen;
   }

   public static Item mtrHk() {
      register();
      return mtrHk;
   }
}
