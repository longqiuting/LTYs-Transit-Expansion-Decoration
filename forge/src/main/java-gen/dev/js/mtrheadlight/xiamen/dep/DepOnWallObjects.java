package dev.js.mtrheadlight.xiamen.dep;

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

public final class DepOnWallObjects {
   public static final String NAMESPACE = "mtrlte";
   public static final String ID = "dep_on_wall";
   public static final ResourceLocation IDENTIFIER = new ResourceLocation("mtrlte", "dep_on_wall");
   private static DepOnWallBlock block;
   private static Item item;
   private static BlockEntityType<DepOnWallBlockEntity> entityType;
   private static boolean registered;

   private DepOnWallObjects() {
   }

   public static DepOnWallBlock block() {
      return block;
   }

   public static Item item() {
      return item;
   }

   public static BlockEntityType<DepOnWallBlockEntity> entityType() {
      return entityType;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         block = (DepOnWallBlock)Reg.block(IDENTIFIER, new DepOnWallBlock());
         item = (Item)Reg.item(IDENTIFIER, new BlockItem(block, new Item.Properties()));
         entityType = (BlockEntityType)Reg.blockEntityType(IDENTIFIER, DepOnWallBlockEntity::new, new Block[]{block});

         try {
            Reg.tabEntries(XiamenObjects.CORE_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> entries.method_45421(item));
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] dep on wall: could not add the creative tab entry", var1);
         }

         LteLog.LOGGER.info("[lte] dep on wall: block={} be={}", IDENTIFIER, IDENTIFIER);
      }
   }
}
