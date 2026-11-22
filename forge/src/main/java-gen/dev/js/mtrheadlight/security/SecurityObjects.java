package dev.js.mtrheadlight.security;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class SecurityObjects {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation LIQUID_SCANNER_ID = new ResourceLocation("mtrlte", "liquid_scanner");
   private static LiquidScannerBlock liquidScanner;
   private static Item liquidScannerItem;
   private static BlockEntityType<LiquidScannerBlockEntity> liquidScannerEntityType;
   private static boolean registered;

   private SecurityObjects() {
   }

   public static LiquidScannerBlock liquidScanner() {
      return liquidScanner;
   }

   public static Item liquidScannerItem() {
      return liquidScannerItem;
   }

   public static BlockEntityType<LiquidScannerBlockEntity> liquidScannerEntityType() {
      return liquidScannerEntityType;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         liquidScanner = (LiquidScannerBlock)Registry.register(BuiltInRegistries.BLOCK, LIQUID_SCANNER_ID, new LiquidScannerBlock());
         liquidScannerItem = (Item)Registry.register(BuiltInRegistries.ITEM, LIQUID_SCANNER_ID, new LiquidScannerItem(liquidScanner, new Item.Properties()));
         liquidScannerEntityType = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, LIQUID_SCANNER_ID, FabricBlockEntityTypeBuilder.create(LiquidScannerBlockEntity::new, new Block[]{liquidScanner}).build());
         LiquidScannerNet.register();

         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> entries.method_45421(liquidScannerItem));
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] liquid scanner could not be added to the core tab", var1);
         }

         LteLog.LOGGER.info("[lte] security: {}", LIQUID_SCANNER_ID);
      }
   }
}
