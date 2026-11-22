package dev.js.mtrheadlight.decor;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.TabIcons;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class LtedCoreItems {
   public static final ResourceLocation LTED_DASHBOARD_ID = new ResourceLocation("mtrlte", "lted_dashboard");
   public static final ResourceLocation TOOLS_TAB_ID = new ResourceLocation("mtrlte", "tools");
   private static LtedDashboardItem dashboard;
   private static boolean registered;

   private LtedCoreItems() {
   }

   public static LtedDashboardItem dashboard() {
      return dashboard;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         dashboard = (LtedDashboardItem)Registry.register(BuiltInRegistries.ITEM, LTED_DASHBOARD_ID, new LtedDashboardItem((new Item.Properties()).stacksTo(1)));
         TabIcons.register();
         Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TOOLS_TAB_ID, FabricItemGroup.builder().title(Component.translatable("itemGroup.mtrlte.tools")).icon(() -> new ItemStack(TabIcons.tools())).build());
         ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, TOOLS_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> entries.method_45421(dashboard));
         LteLog.LOGGER.info("[lte] lted dashboard item: {}", LTED_DASHBOARD_ID);
      }
   }

   public static void pinToolsTabOrder() {
      if (dashboard != null) {
         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, TOOLS_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> {
               entries.getDisplayStacks().removeIf((s) -> s.is(dashboard));
               entries.method_45421(dashboard);
            });
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] lted dashboard: could not pin the tools tab entry", var1);
         }

      }
   }

   public static CreativeModeTab tab() {
      return (CreativeModeTab)BuiltInRegistries.CREATIVE_MODE_TAB.get(TOOLS_TAB_ID);
   }
}
