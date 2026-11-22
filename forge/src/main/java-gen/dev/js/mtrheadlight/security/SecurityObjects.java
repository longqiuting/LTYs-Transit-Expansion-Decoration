package dev.js.mtrheadlight.security;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import dev.js.mtrheadlight.forge.platform.Reg;

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
         liquidScanner = (LiquidScannerBlock)Reg.block(LIQUID_SCANNER_ID, new LiquidScannerBlock());
         liquidScannerItem = (Item)Reg.item(LIQUID_SCANNER_ID, new LiquidScannerItem(liquidScanner, new Item.Properties()));
         liquidScannerEntityType = (BlockEntityType)Reg.blockEntityType(LIQUID_SCANNER_ID, LiquidScannerBlockEntity::new, new Block[]{liquidScanner});
         LiquidScannerNet.register();

         try {
            Reg.tabEntries(XiamenObjects.CORE_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> entries.method_45421(liquidScannerItem));
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] liquid scanner could not be added to the core tab", var1);
         }

         LteLog.LOGGER.info("[lte] security: {}", LIQUID_SCANNER_ID);
      }
   }
}
