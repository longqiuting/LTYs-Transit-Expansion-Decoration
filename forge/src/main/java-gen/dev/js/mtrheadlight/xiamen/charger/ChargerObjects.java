package dev.js.mtrheadlight.xiamen.charger;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import dev.js.mtrheadlight.forge.platform.Reg;

public final class ChargerObjects {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation TOP_ID = new ResourceLocation("mtrlte", "charger_top");
   public static final ResourceLocation BOTTOM_ID = new ResourceLocation("mtrlte", "charger_bottom");
   public static final ResourceLocation BE_ID = new ResourceLocation("mtrlte", "charger");
   public static final ResourceLocation POWER_BANK_ID = new ResourceLocation("mtrlte", "power_bank");
   public static final ResourceLocation PROJECTILE_ID = new ResourceLocation("mtrlte", "power_bank_projectile");
   private static ChargerBlock top;
   private static ChargerBlock bottom;
   private static Item topItem;
   private static Item bottomItem;
   private static PowerBankItem powerBank;
   private static BlockEntityType<ChargerBlockEntity> blockEntityType;
   private static EntityType<PowerBankProjectileEntity> projectileType;
   private static boolean registered;

   private ChargerObjects() {
   }

   public static ChargerBlock top() {
      return top;
   }

   public static ChargerBlock bottom() {
      return bottom;
   }

   public static PowerBankItem powerBankItem() {
      return powerBank;
   }

   public static BlockEntityType<ChargerBlockEntity> blockEntityType() {
      return blockEntityType;
   }

   public static EntityType<PowerBankProjectileEntity> projectileType() {
      return projectileType;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         top = (ChargerBlock)Reg.block(TOP_ID, new ChargerBlock(settings(true), true, "charger_top", "tooltip.mtrlte.charger_top"));
         topItem = (Item)Reg.item(TOP_ID, new BlockItem(top, new Item.Properties()));
         bottom = (ChargerBlock)Reg.block(BOTTOM_ID, new ChargerBlock(settings(false), false, "charger_bottom", "tooltip.mtrlte.charger_bottom"));
         bottomItem = (Item)Reg.item(BOTTOM_ID, new BlockItem(bottom, new Item.Properties()));
         blockEntityType = (BlockEntityType)Reg.blockEntityType(BE_ID, ChargerBlockEntity::new, new Block[]{top, bottom});
         powerBank = (PowerBankItem)Reg.item(POWER_BANK_ID, new PowerBankItem((new Item.Properties()).stacksTo(1)));
         projectileType = (EntityType)Reg.<PowerBankProjectileEntity>entityType(PROJECTILE_ID, (type, level) -> new PowerBankProjectileEntity(type, level), MobCategory.MISC, 0.25F, 0.25F, 4, 10);

         try {
            Reg.tabEntries(XiamenObjects.CORE_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> {
               entries.method_45421(topItem);
               entries.method_45421(bottomItem);
               entries.method_45421(powerBank);
            });
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] power bank charger: could not add the creative tab entries", var1);
         }

         LteLog.LOGGER.info("[lte] power bank charger: {} / {} / {} / {}", new Object[]{TOP_ID, BOTTOM_ID, POWER_BANK_ID, PROJECTILE_ID});
      }
   }

   private static BlockBehaviour.Properties settings(boolean glowing) {
      BlockBehaviour.Properties settings = Properties.of().mapColor(MapColor.COLOR_YELLOW).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.METAL).noOcclusion();
      return glowing ? settings.lightLevel((state) -> 7) : settings;
   }
}
