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
         pole = (PidsXiamenPoleBlock)Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation("mtrlte", "pids_xiamen_pole"), new PidsXiamenPoleBlock(Properties.of().strength(1.0F)));
         poleItem = (Item)Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("mtrlte", "pids_xiamen_pole"), new BlockItem(pole, new Item.Properties()));
         entityType = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ENTITY_ID, FabricBlockEntityTypeBuilder.create(PidsXiamenBlockEntity::new, new Block[]{metro, brt}).build());

         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> {
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
      return (PidsXiamenBlock)Registry.register(BuiltInRegistries.BLOCK, id, new PidsXiamenBlock(kind));
   }

   private static Item registerItem(PidsXiamenBlock.Kind kind, PidsXiamenBlock block) {
      ResourceLocation id = new ResourceLocation("mtrlte", kind.id());
      return (Item)Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
   }
}
