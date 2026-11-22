package dev.js.mtrheadlight.xiamen.pids;

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
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public final class PsdLcdObjects {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation ID = new ResourceLocation("mtrlte", "psd_lcd_xiamen");
   private static PsdLcdBlock block;
   private static Item item;
   private static BlockEntityType<PsdLcdBlockEntity> entityType;
   private static boolean registered;

   private PsdLcdObjects() {
   }

   public static PsdLcdBlock block() {
      return block;
   }

   public static Item item() {
      return item;
   }

   public static BlockEntityType<PsdLcdBlockEntity> entityType() {
      return entityType;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         block = (PsdLcdBlock)Registry.register(BuiltInRegistries.BLOCK, ID, new PsdLcdBlock(Properties.of().requiresCorrectToolForDrops().strength(1.5F).noOcclusion()));
         item = (Item)Registry.register(BuiltInRegistries.ITEM, ID, new BlockItem(block, new Item.Properties()));
         entityType = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ID, FabricBlockEntityTypeBuilder.create(PsdLcdBlockEntity::new, new Block[]{block}).build());

         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> entries.method_45421(item));
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] psd lcd: could not add the creative tab entry", var1);
         }

         LteLog.LOGGER.info("[lte] psd lcd: block={} be={}", ID, ID);
      }
   }
}
