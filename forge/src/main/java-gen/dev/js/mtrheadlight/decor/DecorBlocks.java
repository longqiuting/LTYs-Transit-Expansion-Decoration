package dev.js.mtrheadlight.decor;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public final class DecorBlocks {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation MARBLE_FLOOR_ID = new ResourceLocation("mtrlte", "marble_floor");
   public static final ResourceLocation MARBLE_PLATFORM_ID = new ResourceLocation("mtrlte", "marble_platform");
   private static Block marbleFloor;
   private static Item marbleFloorItem;
   private static MarblePlatformBlock marblePlatform;
   private static Item marblePlatformItem;
   private static boolean registered;

   private DecorBlocks() {
   }

   public static Block marbleFloor() {
      return marbleFloor;
   }

   public static Item marbleFloorItem() {
      return marbleFloorItem;
   }

   public static MarblePlatformBlock marblePlatform() {
      return marblePlatform;
   }

   public static Item marblePlatformItem() {
      return marblePlatformItem;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         marbleFloor = (Block)Registry.register(BuiltInRegistries.BLOCK, MARBLE_FLOOR_ID, new Block(stoneSettings()));
         marbleFloorItem = (Item)Registry.register(BuiltInRegistries.ITEM, MARBLE_FLOOR_ID, new BlockItem(marbleFloor, new Item.Properties()));
         marblePlatform = (MarblePlatformBlock)Registry.register(BuiltInRegistries.BLOCK, MARBLE_PLATFORM_ID, new MarblePlatformBlock(Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.STONE)));
         marblePlatformItem = (Item)Registry.register(BuiltInRegistries.ITEM, MARBLE_PLATFORM_ID, new BlockItem(marblePlatform, new Item.Properties()));

         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> {
               entries.method_45421(marbleFloorItem);
               entries.method_45421(marblePlatformItem);
            });
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] marble blocks could not be added to the core tab", var1);
         }

         LteLog.LOGGER.info("[lte] decor: {} / {}", MARBLE_FLOOR_ID, MARBLE_PLATFORM_ID);
      }
   }

   private static BlockBehaviour.Properties stoneSettings() {
      return Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.STONE);
   }
}
