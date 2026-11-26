package dev.js.mtrheadlight.region;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.logo.LogoBlock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import dev.js.mtrheadlight.forge.platform.Reg;

// mojmap 版（与 Fabric 侧 v1.20.1/.../region/RegionalLogos.java 一一对应）：
// AbstractBlock.Settings → BlockBehaviour.Properties，Registries.ITEM → BuiltInRegistries.ITEM，
// Registry.register → Reg.block / Reg.item，ItemGroupEvents → Reg.tabEntries。
public final class RegionalLogos {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation FUZHOU_ID = new ResourceLocation("mtrlte", "fuzhou_logo");
   public static final ResourceLocation SHENZHEN_ID = new ResourceLocation("mtrlte", "shenzhen_logo");
   public static final ResourceLocation MTR_HK_ID = new ResourceLocation("mtrlte", "mtr_hk_logo");
   private static LogoBlock fuzhouLogo;
   private static LogoBlock shenzhenLogo;
   private static LogoBlock mtrHkLogo;
   private static Item fuzhouItem;
   private static Item shenzhenItem;
   private static Item mtrHkItem;
   private static boolean registered;
   // 每个地区页签开头的两件：标志块 → 贴墙标（贴墙标那一件的 id 在 WallSignObjects 里注册）。
   private static final Map<ResourceLocation, List<ResourceLocation>> TAB_FRONT = Map.of(
      RegionalTabs.FUZHOU_TAB_ID, List.of(FUZHOU_ID, new ResourceLocation("mtrlte", "wall_sign_fuzhou")),
      RegionalTabs.SHENZHEN_TAB_ID, List.of(SHENZHEN_ID, new ResourceLocation("mtrlte", "wall_sign_shenzhen")),
      RegionalTabs.MTR_HK_TAB_ID, List.of(MTR_HK_ID, new ResourceLocation("mtrlte", "wall_sign_mtr_hk"))
   );

   private RegionalLogos() {
   }

   public static LogoBlock fuzhouLogo() {
      return fuzhouLogo;
   }

   public static LogoBlock shenzhenLogo() {
      return shenzhenLogo;
   }

   public static LogoBlock mtrHkLogo() {
      return mtrHkLogo;
   }

   public static Item fuzhouItem() {
      return fuzhouItem;
   }

   public static Item shenzhenItem() {
      return shenzhenItem;
   }

   public static Item mtrHkItem() {
      return mtrHkItem;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         fuzhouLogo = registerBlock(FUZHOU_ID);
         shenzhenLogo = registerBlock(SHENZHEN_ID);
         mtrHkLogo = registerBlock(MTR_HK_ID);
         fuzhouItem = registerItem(FUZHOU_ID, fuzhouLogo);
         shenzhenItem = registerItem(SHENZHEN_ID, shenzhenLogo);
         mtrHkItem = registerItem(MTR_HK_ID, mtrHkLogo);
         addToTab(RegionalTabs.FUZHOU_TAB_ID, fuzhouItem);
         addToTab(RegionalTabs.SHENZHEN_TAB_ID, shenzhenItem);
         addToTab(RegionalTabs.MTR_HK_TAB_ID, mtrHkItem);
         LteLog.LOGGER.info("[lte] regional logos: {} / {} / {}", new Object[]{FUZHOU_ID, SHENZHEN_ID, MTR_HK_ID});
      }
   }

   public static void pinRegionalTabOrder() {
      for(Map.Entry<ResourceLocation, List<ResourceLocation>> tab : TAB_FRONT.entrySet()) {
         List<ResourceLocation> frontIds = tab.getValue();
         ResourceLocation tabId = tab.getKey();
         try {
            Reg.tabEntries(tabId, (Reg.TabEntriesConsumer)(entries) -> {
               List<ItemStack> stacks = entries.getDisplayStacks();
               List<ItemStack> front = new ArrayList(frontIds.size());

               for(ResourceLocation id : frontIds) {
                  remember(stacks, id, front);
               }

               if (!front.isEmpty()) {
                  stacks.addAll(0, front);
               }

            });
         } catch (Throwable var3) {
            LteLog.LOGGER.warn("[lte] regional tab order: could not pin {}", tabId, var3);
         }
      }

   }

   private static void remember(List<ItemStack> stacks, ResourceLocation id, List<ItemStack> out) {
      Item item = (Item)BuiltInRegistries.ITEM.get(id);
      if (item != null && item != Items.AIR) {
         Iterator<ItemStack> it = stacks.iterator();

         while(it.hasNext()) {
            ItemStack stack = (ItemStack)it.next();
            if (stack.is(item)) {
               out.add(stack);
               it.remove();
            }
         }

      }
   }

   private static void addToTab(ResourceLocation tabId, Item item) {
      try {
         Reg.tabEntries(tabId, (Reg.TabEntriesConsumer)(entries) -> entries.method_45421(item));
      } catch (Throwable var3) {
         LteLog.LOGGER.warn("[lte] regional logos: could not add {} to {}", new Object[]{item, tabId, var3});
      }

   }

   private static LogoBlock registerBlock(ResourceLocation id) {
      LogoBlock block = new LogoBlock(Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().destroyTime(2.0F).lightLevel((state) -> 10));
      return (LogoBlock)Reg.block(id, block);
   }

   private static Item registerItem(ResourceLocation id, LogoBlock block) {
      return (Item)Reg.item(id, new BlockItem(block, new Item.Properties()));
   }
}
