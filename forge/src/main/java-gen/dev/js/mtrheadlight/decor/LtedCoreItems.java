package dev.js.mtrheadlight.decor;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.TabIcons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.js.mtrheadlight.forge.platform.Reg;

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
         dashboard = (LtedDashboardItem)Reg.item(LTED_DASHBOARD_ID, new LtedDashboardItem((new Item.Properties()).stacksTo(1)));
         TabIcons.register();
         Reg.tab(TOOLS_TAB_ID, Component.translatable("itemGroup.mtrlte.tools"), () -> new ItemStack(TabIcons.tools()));
         Reg.tabEntries(TOOLS_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> entries.method_45421(dashboard));
         LteLog.LOGGER.info("[lte] lted dashboard item: {}", LTED_DASHBOARD_ID);
      }
   }

   public static void pinToolsTabOrder() {
      if (dashboard != null) {
         try {
            Reg.tabEntries(TOOLS_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> {
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
