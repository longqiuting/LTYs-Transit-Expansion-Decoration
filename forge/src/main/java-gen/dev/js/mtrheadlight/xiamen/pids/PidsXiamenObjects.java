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

public final class PidsXiamenObjects {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation ENTITY_ID = new ResourceLocation("mtrlte", "pids_xiamen");
   public static final String POLE_ID = "pids_xiamen_pole";
   private static PidsXiamenBlock metro;
   private static PidsXiamenBlock brt;
   private static PidsXiamenPoleBlock pole;
   private static Item metroItem;
   private static Item brtItem;
   private static Item poleItem;
   private static BlockEntityType<PidsXiamenBlockEntity> entityType;
   private static boolean registered;

   private PidsXiamenObjects() {
   }

   public static PidsXiamenBlock metro() {
      return metro;
   }

   public static PidsXiamenBlock brt() {
      return brt;
   }

   public static PidsXiamenPoleBlock pole() {
      return pole;
   }

   public static Item metroItem() {
      return metroItem;
   }

   public static Item brtItem() {
      return brtItem;
   }

   public static Item poleItem() {
      return poleItem;
   }

   public static BlockEntityType<PidsXiamenBlockEntity> entityType() {
      return entityType;
   }

   public static PidsXiamenBlock.Kind kindOf(Block block) {
      PidsXiamenBlock.Kind var10000;
      if (block instanceof PidsXiamenBlock pids) {
         var10000 = pids.kind();
      } else {
         var10000 = null;
      }

      return var10000;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         metro = register(PidsXiamenBlock.Kind.METRO);
         metroItem = registerItem(PidsXiamenBlock.Kind.METRO, metro);
         brt = register(PidsXiamenBlock.Kind.BRT);
         brtItem = registerItem(PidsXiamenBlock.Kind.BRT, brt);
         pole = (PidsXiamenPoleBlock)Reg.block(new ResourceLocation("mtrlte", "pids_xiamen_pole"), new PidsXiamenPoleBlock(Properties.of().strength(1.0F)));
         poleItem = (Item)Reg.item(new ResourceLocation("mtrlte", "pids_xiamen_pole"), new BlockItem(pole, new Item.Properties()));
         entityType = (BlockEntityType)Reg.blockEntityType(ENTITY_ID, PidsXiamenBlockEntity::new, new Block[]{metro, brt});

         try {
            Reg.tabEntries(XiamenObjects.CORE_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> {
               entries.method_45421(metroItem);
               entries.method_45421(brtItem);
               entries.method_45421(poleItem);
            });
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] xiamen pids: could not add the creative tab entries", var1);
         }

         LteLog.LOGGER.info("[lte] xiamen pids: metro={} brt={} pole={} be={}", new Object[]{new ResourceLocation("mtrlte", PidsXiamenBlock.Kind.METRO.id()), new ResourceLocation("mtrlte", PidsXiamenBlock.Kind.BRT.id()), new ResourceLocation("mtrlte", "pids_xiamen_pole"), ENTITY_ID});
      }
   }

   private static PidsXiamenBlock register(PidsXiamenBlock.Kind kind) {
      ResourceLocation id = new ResourceLocation("mtrlte", kind.id());
      return (PidsXiamenBlock)Reg.block(id, new PidsXiamenBlock(kind));
   }

   private static Item registerItem(PidsXiamenBlock.Kind kind, PidsXiamenBlock block) {
      ResourceLocation id = new ResourceLocation("mtrlte", kind.id());
      return (Item)Reg.item(id, new BlockItem(block, new Item.Properties()));
   }
}
