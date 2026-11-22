package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
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
import dev.js.mtrheadlight.forge.platform.Reg;

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
         block = (PsdLcdBlock)Reg.block(ID, new PsdLcdBlock(Properties.of().requiresCorrectToolForDrops().strength(1.5F).noOcclusion()));
         item = (Item)Reg.item(ID, new BlockItem(block, new Item.Properties()));
         entityType = (BlockEntityType)Reg.blockEntityType(ID, PsdLcdBlockEntity::new, new Block[]{block});

         try {
            Reg.tabEntries(XiamenObjects.CORE_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> entries.method_45421(item));
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] psd lcd: could not add the creative tab entry", var1);
         }

         LteLog.LOGGER.info("[lte] psd lcd: block={} be={}", ID, ID);
      }
   }
}
