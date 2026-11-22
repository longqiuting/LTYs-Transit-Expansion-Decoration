package dev.js.mtrheadlight.xiamen.drinks;

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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import dev.js.mtrheadlight.forge.platform.Reg;

public final class DrinksMachineObjects {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation TOP_ID = new ResourceLocation("mtrlte", "drinks_machine_top");
   public static final ResourceLocation BOTTOM_ID = new ResourceLocation("mtrlte", "drinks_machine_bottom");
   public static final ResourceLocation DOUBLE_TOP_ID = new ResourceLocation("mtrlte", "drinks_machine_double_top");
   public static final ResourceLocation DOUBLE_BOTTOM_ID = new ResourceLocation("mtrlte", "drinks_machine_double_bottom");
   public static final ResourceLocation BE_ID = new ResourceLocation("mtrlte", "drinks_machine");
   private static DrinksMachineBlock top;
   private static DrinksMachineBlock bottom;
   private static DrinksMachineBlock doubleTop;
   private static DrinksMachineBlock doubleBottom;
   private static Item item;
   private static Item doubleItem;
   private static BlockEntityType<DrinksMachineBlockEntity> blockEntityType;
   private static boolean registered;

   private DrinksMachineObjects() {
   }

   public static DrinksMachineBlock top() {
      return top;
   }

   public static DrinksMachineBlock bottom() {
      return bottom;
   }

   public static DrinksMachineBlock doubleTop() {
      return doubleTop;
   }

   public static DrinksMachineBlock doubleBottom() {
      return doubleBottom;
   }

   public static DrinksMachineBlock top(DrinksMachineBlock.Variant variant) {
      return variant == DrinksMachineBlock.Variant.DOUBLE ? doubleTop : top;
   }

   public static DrinksMachineBlock bottom(DrinksMachineBlock.Variant variant) {
      return variant == DrinksMachineBlock.Variant.DOUBLE ? doubleBottom : bottom;
   }

   public static Item item() {
      return item;
   }

   public static Item doubleItem() {
      return doubleItem;
   }

   public static BlockEntityType<DrinksMachineBlockEntity> blockEntityType() {
      return blockEntityType;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         top = (DrinksMachineBlock)Reg.block(TOP_ID, new DrinksMachineBlock(settings(10), true, "tooltip.mtrlte.drinks_machine_top", DrinksMachineBlock.Variant.SINGLE));
         bottom = (DrinksMachineBlock)Reg.block(BOTTOM_ID, new DrinksMachineBlock(settings(6), false, "tooltip.mtrlte.drinks_machine_bottom", DrinksMachineBlock.Variant.SINGLE));
         item = (Item)Reg.item(BOTTOM_ID, new BlockItem(bottom, new Item.Properties()));
         doubleTop = (DrinksMachineBlock)Reg.block(DOUBLE_TOP_ID, new DrinksMachineBlock(settings(10), true, "tooltip.mtrlte.drinks_machine_top", DrinksMachineBlock.Variant.DOUBLE));
         doubleBottom = (DrinksMachineBlock)Reg.block(DOUBLE_BOTTOM_ID, new DrinksMachineBlock(settings(6), false, "tooltip.mtrlte.drinks_machine_bottom", DrinksMachineBlock.Variant.DOUBLE));
         doubleItem = (Item)Reg.item(DOUBLE_BOTTOM_ID, new BlockItem(doubleBottom, new Item.Properties()));
         blockEntityType = (BlockEntityType)Reg.blockEntityType(BE_ID, DrinksMachineBlockEntity::new, new Block[]{top, bottom, doubleTop, doubleBottom});

         try {
            Reg.tabEntries(XiamenObjects.CORE_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> {
               entries.method_45421(item);
               entries.method_45421(doubleItem);
            });
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] drinks machine: could not add the creative tab entries", var1);
         }

         LteLog.LOGGER.info("[lte] drinks machine: single {} / {} (item {}), double {} / {} (item {}), luminance {}/{}, price ${}", new Object[]{TOP_ID, BOTTOM_ID, BOTTOM_ID, DOUBLE_TOP_ID, DOUBLE_BOTTOM_ID, DOUBLE_BOTTOM_ID, 10, 6, 3});
      }
   }

   private static BlockBehaviour.Properties settings(int luminance) {
      return Properties.of().mapColor(MapColor.COLOR_RED).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.METAL).noOcclusion().lightLevel((state) -> luminance);
   }
}
