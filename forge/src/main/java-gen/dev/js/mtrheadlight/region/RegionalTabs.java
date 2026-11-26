package dev.js.mtrheadlight.region;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.TabIcons;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.js.mtrheadlight.forge.platform.Reg;

// mojmap 版（与 Fabric 侧 v1.20.1/.../region/RegionalTabs.java 一一对应）：
// Fabric 的 FabricItemGroup.builder() → Reg.tab(id, Component, Supplier<ItemStack>)，
// Text.translatable → Component.translatable，Identifier → ResourceLocation。
public final class RegionalTabs {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation FUZHOU_TAB_ID = new ResourceLocation("mtrlte", "fuzhou");
   public static final ResourceLocation SHENZHEN_TAB_ID = new ResourceLocation("mtrlte", "shenzhen");
   public static final ResourceLocation MTR_HK_TAB_ID = new ResourceLocation("mtrlte", "mtr_hk");
   private static boolean registered;

   private RegionalTabs() {
   }

   public static void register() {
      if (!registered) {
         registered = true;
         TabIcons.register();
         tab(FUZHOU_TAB_ID, "itemGroup.mtrlte.fuzhou", TabIcons.fuzhou());
         tab(SHENZHEN_TAB_ID, "itemGroup.mtrlte.shenzhen", TabIcons.shenzhen());
         tab(MTR_HK_TAB_ID, "itemGroup.mtrlte.mtr_hk", TabIcons.mtrHk());
         LteLog.LOGGER.info("[lte] regional tabs: {} / {} / {}", new Object[]{FUZHOU_TAB_ID, SHENZHEN_TAB_ID, MTR_HK_TAB_ID});
      }
   }

   private static void tab(ResourceLocation id, String titleKey, Item icon) {
      Reg.tab(id, Component.translatable(titleKey), () -> new ItemStack(icon));
   }
}
