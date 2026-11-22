package dev.js.mtrheadlight.decor;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public final class RccPlatforms {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation TAB_ID = new ResourceLocation("mtrlte", "rcc_platform");
   public static final ResourceLocation PLATFORM_ID = new ResourceLocation("mtrlte", "platform");
   public static final ResourceLocation INDENTED_ID = new ResourceLocation("mtrlte", "platform_indented");
   public static final ResourceLocation QUARTZ_ID = new ResourceLocation("mtrlte", "platform_type_quartz");
   private static RccPlatformBlock platform;
   private static RccPlatformBlock indented;
   private static RccPlatformBlock quartz;
   private static Item platformItem;
   private static Item indentedItem;
   private static Item quartzItem;
   private static boolean registered;

   private RccPlatforms() {
   }

   public static RccPlatformBlock platform() {
      return platform;
   }

   public static RccPlatformBlock indented() {
      return indented;
   }

   public static RccPlatformBlock quartz() {
      return quartz;
   }

   public static Block[] all() {
      return platform != null && indented != null && quartz != null ? new Block[]{platform, indented, quartz} : new Block[0];
   }

   public static void register() {
      if (!registered) {
         registered = true;
         platform = registerBlock(PLATFORM_ID, false, MapColor.STONE);
         indented = registerBlock(INDENTED_ID, true, MapColor.STONE);
         quartz = registerBlock(QUARTZ_ID, false, MapColor.QUARTZ);
         platformItem = registerItem(PLATFORM_ID, platform);
         indentedItem = registerItem(INDENTED_ID, indented);
         quartzItem = registerItem(QUARTZ_ID, quartz);
         Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_ID, FabricItemGroup.builder().title(Component.translatable("itemGroup.mtrlte.rcc_platform")).icon(() -> new ItemStack(platformItem)).build());
         ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> {
            entries.method_45421(platformItem);
            entries.method_45421(indentedItem);
            entries.method_45421(quartzItem);
         });
         LteLog.LOGGER.info("[lte] rcc platforms: {} / {} / {} (tab {})", new Object[]{PLATFORM_ID, INDENTED_ID, QUARTZ_ID, TAB_ID});
      }
   }

   private static RccPlatformBlock registerBlock(ResourceLocation id, boolean isIndented, MapColor mapColor) {
      return (RccPlatformBlock)Registry.register(BuiltInRegistries.BLOCK, id, new RccPlatformBlock(Properties.of().mapColor(mapColor).requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.STONE), isIndented));
   }

   private static Item registerItem(ResourceLocation id, Block block) {
      return (Item)Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
   }

   public static CreativeModeTab tab() {
      return (CreativeModeTab)BuiltInRegistries.CREATIVE_MODE_TAB.get(TAB_ID);
   }
}
