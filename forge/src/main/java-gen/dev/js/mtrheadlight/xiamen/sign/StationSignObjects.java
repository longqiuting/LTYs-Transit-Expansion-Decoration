package dev.js.mtrheadlight.xiamen.sign;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public final class StationSignObjects {
   public static final String NAMESPACE = "mtrlte";
   private static final List<StationSignObjects.Entry> ENTRIES = new ArrayList();
   private static boolean registered;

   private StationSignObjects() {
   }

   public static List<StationSignBlock> blocks() {
      List<StationSignBlock> out = new ArrayList(ENTRIES.size());

      for(StationSignObjects.Entry entry : ENTRIES) {
         out.add(entry.block());
      }

      return out;
   }

   public static List<StationSignObjects.Entry> entries() {
      return ENTRIES;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         StationSignObjects.Entry wallXiamen = add("station_sign_wall_xiamen", true, "xiamen");
         StationSignObjects.Entry poleXiamen = add("station_sign_pole_xiamen", false, "xiamen");
         StationSignObjects.Entry wallJiaomei = add("station_sign_wall_jiaomei", true, "jiaomei");
         StationSignObjects.Entry poleJiaomei = add("station_sign_pole_jiaomei", false, "jiaomei");

         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> {
               entries.method_45421(wallXiamen.item());
               entries.method_45421(poleXiamen.item());
               entries.method_45421(wallJiaomei.item());
               entries.method_45421(poleJiaomei.item());
            });
         } catch (Throwable var6) {
            LteLog.LOGGER.warn("[lte] station sign: could not add the creative tab entries", var6);
         }

         for(StationSignObjects.Entry entry : ENTRIES) {
            LteLog.LOGGER.info("[lte] station sign: {} (wall={}, texture={})", new Object[]{entry.id(), entry.wallMounted(), entry.texture()});
         }

      }
   }

   private static StationSignObjects.Entry add(String id, boolean wallMounted, String texture) {
      ResourceLocation identifier = new ResourceLocation("mtrlte", id);
      StationSignBlock block = new StationSignBlock(Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.METAL).lightLevel((state) -> 15).noOcclusion(), wallMounted);
      Registry.register(BuiltInRegistries.BLOCK, identifier, block);
      Item item = (Item)Registry.register(BuiltInRegistries.ITEM, identifier, new BlockItem(block, new Item.Properties()));
      StationSignObjects.Entry entry = new StationSignObjects.Entry(id, wallMounted, texture, block, item);
      ENTRIES.add(entry);
      return entry;
   }

   public static record Entry(String id, boolean wallMounted, String texture, StationSignBlock block, Item item) {
   }
}
