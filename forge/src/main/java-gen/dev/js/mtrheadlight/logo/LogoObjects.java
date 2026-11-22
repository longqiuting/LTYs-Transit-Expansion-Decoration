package dev.js.mtrheadlight.logo;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public final class LogoObjects {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation LTE_ID = new ResourceLocation("mtrlte", "lte_logo");
   public static final ResourceLocation JIAOMEI_ID = new ResourceLocation("mtrlte", "jiaomei_logo");
   private static LogoBlock lteLogo;
   private static LogoBlock jiaomeiLogo;
   private static Item lteItem;
   private static Item jiaomeiItem;
   private static boolean registered;
   private static final List<ResourceLocation> CORE_TAB_FRONT = List.of(LTE_ID, new ResourceLocation("mtrlte", "wall_sign_lte"), XiamenObjects.LOGO_ID, JIAOMEI_ID, new ResourceLocation("mtrlte", "wall_sign_xiamen"), new ResourceLocation("mtrlte", "wall_sign_jiaomei"), new ResourceLocation("mtrlte", "station_sign_wall_xiamen"), new ResourceLocation("mtrlte", "station_sign_pole_xiamen"), new ResourceLocation("mtrlte", "station_sign_wall_jiaomei"), new ResourceLocation("mtrlte", "station_sign_pole_jiaomei"));
   private static final List<ResourceLocation> CORE_TAB_TAIL = List.of();

   private LogoObjects() {
   }

   public static LogoBlock lteLogo() {
      return lteLogo;
   }

   public static LogoBlock jiaomeiLogo() {
      return jiaomeiLogo;
   }

   public static Item lteItem() {
      return lteItem;
   }

   public static Item jiaomeiItem() {
      return jiaomeiItem;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         lteLogo = registerBlock(LTE_ID);
         jiaomeiLogo = registerBlock(JIAOMEI_ID);
         lteItem = registerItem(LTE_ID, lteLogo);
         jiaomeiItem = registerItem(JIAOMEI_ID, jiaomeiLogo);

         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> {
               Item xiamenItem = XiamenObjects.logo().asItem();
               List<ItemStack> stacks = entries.getDisplayStacks();
               stacks.removeIf((s) -> s.is(xiamenItem) || s.is(jiaomeiItem) || s.is(lteItem));
               stacks.add(0, new ItemStack(jiaomeiItem));
               stacks.add(0, new ItemStack(xiamenItem));
               stacks.add(0, new ItemStack(lteItem));
            });
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] logo blocks: could not add the core tab entries", var1);
         }

         LteLog.LOGGER.info("[lte] logo blocks: {} / {}", LTE_ID, JIAOMEI_ID);
      }
   }

   public static void pinCoreTabOrder() {
      try {
         ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> {
            List<ItemStack> stacks = entries.getDisplayStacks();
            List<ItemStack> front = new ArrayList(CORE_TAB_FRONT.size());

            for(ResourceLocation id : CORE_TAB_FRONT) {
               remember(stacks, id, front);
            }

            if (!front.isEmpty()) {
               stacks.addAll(0, front);
            }

            List<ItemStack> tail = new ArrayList(CORE_TAB_TAIL.size());

            for(ResourceLocation id : CORE_TAB_TAIL) {
               remember(stacks, id, tail);
            }

            if (!tail.isEmpty()) {
               stacks.addAll(tail);
            }

         });
      } catch (Throwable var1) {
         LteLog.LOGGER.warn("[lte] core tab order: could not pin the front of the xiamen tab", var1);
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

   private static LogoBlock registerBlock(ResourceLocation id) {
      LogoBlock block = new LogoBlock(Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().destroyTime(2.0F).lightLevel((state) -> 10));
      return (LogoBlock)Registry.register(BuiltInRegistries.BLOCK, id, block);
   }

   private static Item registerItem(ResourceLocation id, LogoBlock block) {
      return (Item)Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
   }
}
