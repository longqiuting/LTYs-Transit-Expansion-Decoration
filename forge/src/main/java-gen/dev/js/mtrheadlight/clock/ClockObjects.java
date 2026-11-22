package dev.js.mtrheadlight.clock;

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
import dev.js.mtrheadlight.forge.platform.Reg;

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
         clock = (ClockBlock)Reg.block(CLOCK_ID, new ClockBlock());
         clockItem = (Item)Reg.item(CLOCK_ID, new BlockItem(clock, new Item.Properties()));
         entityType = (BlockEntityType)Reg.blockEntityType(CLOCK_ID, ClockBlockEntity::new, new Block[]{clock});
         ClockNet.register();
         ClockGlobalNet.register();

         try {
            Reg.tabEntries(XiamenObjects.CORE_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> entries.method_45421(clockItem));
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] clock could not be added to the core tab", var1);
         }

         LteLog.LOGGER.info("[lte] clock: {}", CLOCK_ID);
      }
   }
}
