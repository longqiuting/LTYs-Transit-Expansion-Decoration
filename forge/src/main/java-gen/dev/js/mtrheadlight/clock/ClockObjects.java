package dev.js.mtrheadlight.clock;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ClockObjects {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation CLOCK_ID = new ResourceLocation("mtrlte", "clock");
   private static ClockBlock clock;
   private static Item clockItem;
   private static BlockEntityType<ClockBlockEntity> entityType;
   private static boolean registered;

   private ClockObjects() {
   }

   public static ClockBlock clock() {
      return clock;
   }

   public static Item clockItem() {
      return clockItem;
   }

   public static BlockEntityType<ClockBlockEntity> entityType() {
      return entityType;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         clock = (ClockBlock)Registry.register(BuiltInRegistries.BLOCK, CLOCK_ID, new ClockBlock());
         clockItem = (Item)Registry.register(BuiltInRegistries.ITEM, CLOCK_ID, new BlockItem(clock, new Item.Properties()));
         entityType = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CLOCK_ID, FabricBlockEntityTypeBuilder.create(ClockBlockEntity::new, new Block[]{clock}).build());
         ClockNet.register();
         ClockGlobalNet.register();

         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> entries.method_45421(clockItem));
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] clock could not be added to the core tab", var1);
         }

         LteLog.LOGGER.info("[lte] clock: {}", CLOCK_ID);
      }
   }
}
