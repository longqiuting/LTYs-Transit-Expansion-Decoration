package dev.js.mtrheadlight.xiamen.dep;

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

public final class DepOnRoofObjects {
   public static final String NAMESPACE = "mtrlte";
   public static final String ID = "dep_on_roof";
   public static final ResourceLocation IDENTIFIER = new ResourceLocation("mtrlte", "dep_on_roof");
   private static DepOnRoofBlock block;
   private static Item item;
   private static BlockEntityType<DepOnRoofBlockEntity> entityType;
   private static boolean registered;

   private DepOnRoofObjects() {
   }

   public static DepOnRoofBlock block() {
      return block;
   }

   public static Item item() {
      return item;
   }

   public static BlockEntityType<DepOnRoofBlockEntity> entityType() {
      return entityType;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         block = (DepOnRoofBlock)Registry.register(BuiltInRegistries.BLOCK, IDENTIFIER, new DepOnRoofBlock());
         item = (Item)Registry.register(BuiltInRegistries.ITEM, IDENTIFIER, new BlockItem(block, new Item.Properties()));
         entityType = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, IDENTIFIER, FabricBlockEntityTypeBuilder.create(DepOnRoofBlockEntity::new, new Block[]{block}).build());

         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> entries.method_45421(item));
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] dep on roof: could not add the creative tab entry", var1);
         }

         LteLog.LOGGER.info("[lte] dep on roof: block={} be={}", IDENTIFIER, IDENTIFIER);
      }
   }
}
