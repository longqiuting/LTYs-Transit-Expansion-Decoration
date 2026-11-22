package dev.js.mtrheadlight.xiamen.sign;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import java.util.ArrayList;
import java.util.List;
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

public final class WallSignObjects {
   public static final String NAMESPACE = "mtrlte";
   private static final List<WallSignObjects.Entry> ENTRIES = new ArrayList();
   private static BlockEntityType<WallSignBlockEntity> entityType;
   private static boolean registered;

   private WallSignObjects() {
   }

   public static List<WallSignBlock> blocks() {
      List<WallSignBlock> out = new ArrayList(ENTRIES.size());

      for(WallSignObjects.Entry entry : ENTRIES) {
         out.add(entry.block());
      }

      return out;
   }

   public static List<WallSignObjects.Entry> entries() {
      return ENTRIES;
   }

   public static BlockEntityType<WallSignBlockEntity> entityType() {
      return entityType;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         WallSignObjects.Entry xiamen = add("wall_sign_xiamen", "xiamen_logo");
         WallSignObjects.Entry jiaomei = add("wall_sign_jiaomei", "jiaomei_logo");
         WallSignObjects.Entry lte = add("wall_sign_lte", "lte_logo");

         try {
            entityType = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation("mtrlte", "wall_sign_xiamen"), FabricBlockEntityTypeBuilder.create(WallSignBlockEntity::new, new Block[]{xiamen.block(), jiaomei.block(), lte.block()}).build());
         } catch (Throwable var6) {
            LteLog.LOGGER.error("[lte] wall sign: could not register the block entity type", var6);
         }

         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> {
               entries.method_45421(xiamen.item());
               entries.method_45421(jiaomei.item());
               entries.method_45421(lte.item());
            });
         } catch (Throwable var5) {
            LteLog.LOGGER.warn("[lte] wall sign: could not add the creative tab entries", var5);
         }

         for(WallSignObjects.Entry entry : ENTRIES) {
            LteLog.LOGGER.info("[lte] wall sign: {} (logo={})", entry.id(), entry.logo());
         }

      }
   }

   private static WallSignObjects.Entry add(String id, String logo) {
      ResourceLocation identifier = new ResourceLocation("mtrlte", id);
      WallSignBlock block = new WallSignBlock(Properties.of().requiresCorrectToolForDrops().strength(2.0F).noOcclusion(), logo);
      Registry.register(BuiltInRegistries.BLOCK, identifier, block);
      Item item = (Item)Registry.register(BuiltInRegistries.ITEM, identifier, new BlockItem(block, new Item.Properties()));
      WallSignObjects.Entry entry = new WallSignObjects.Entry(id, logo, block, item);
      ENTRIES.add(entry);
      return entry;
   }

   public static record Entry(String id, String logo, WallSignBlock block, Item item) {
   }
}
