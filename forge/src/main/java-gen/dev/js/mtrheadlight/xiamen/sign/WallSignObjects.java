package dev.js.mtrheadlight.xiamen.sign;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import java.util.ArrayList;
import java.util.List;
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
         WallSignObjects.Entry xiamen = add("wall_sign_xiamen", "xiamen_logo", XiamenObjects.CORE_TAB_ID);
         WallSignObjects.Entry jiaomei = add("wall_sign_jiaomei", "jiaomei_logo", XiamenObjects.CORE_TAB_ID);
         WallSignObjects.Entry lte = add("wall_sign_lte", "lte_logo", XiamenObjects.CORE_TAB_ID);
         WallSignObjects.Entry fuzhou = add("wall_sign_fuzhou", "fuzhou_logo", dev.js.mtrheadlight.region.RegionalTabs.FUZHOU_TAB_ID);
         WallSignObjects.Entry shenzhen = add("wall_sign_shenzhen", "shenzhen_logo", dev.js.mtrheadlight.region.RegionalTabs.SHENZHEN_TAB_ID);
         WallSignObjects.Entry mtrHk = add("wall_sign_mtr_hk", "mtr_hk_logo", dev.js.mtrheadlight.region.RegionalTabs.MTR_HK_TAB_ID);

         try {
            entityType = (BlockEntityType)Reg.blockEntityType(new ResourceLocation("mtrlte", "wall_sign_xiamen"), WallSignBlockEntity::new,
               new Block[]{xiamen.block(), jiaomei.block(), lte.block(), fuzhou.block(), shenzhen.block(), mtrHk.block()});
         } catch (Throwable var7) {
            LteLog.LOGGER.error("[lte] wall sign: could not register the block entity type", var7);
         }

         // 按页签分组，一组一个回调（厦门三面进「厦门风格物件」，福州 / 深圳 / 香港各一面进各自的地区页签）。
         for(WallSignObjects.Entry entry : ENTRIES) {
            try {
               ResourceLocation tab = entry.tab();
               Reg.tabEntries(tab, (Reg.TabEntriesConsumer)(entries) -> entries.method_45421(entry.item()));
            } catch (Throwable var6) {
               LteLog.LOGGER.warn("[lte] wall sign: could not add the creative tab entry for {}", entry.id(), var6);
            }
         }

         for(WallSignObjects.Entry entry : ENTRIES) {
            LteLog.LOGGER.info("[lte] wall sign: {} (logo={}, tab={})", new Object[]{entry.id(), entry.logo(), entry.tab()});
         }

      }
   }

   private static WallSignObjects.Entry add(String id, String logo, ResourceLocation tab) {
      ResourceLocation identifier = new ResourceLocation("mtrlte", id);
      WallSignBlock block = new WallSignBlock(Properties.of().requiresCorrectToolForDrops().strength(2.0F).noOcclusion(), logo);
      Reg.block(identifier, block);
      Item item = (Item)Reg.item(identifier, new BlockItem(block, new Item.Properties()));
      WallSignObjects.Entry entry = new WallSignObjects.Entry(id, logo, tab, block, item);
      ENTRIES.add(entry);
      return entry;
   }

   public static record Entry(String id, String logo, ResourceLocation tab, WallSignBlock block, Item item) {
   }
}
