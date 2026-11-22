package dev.js.mtrheadlight.xiamen;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.TabIcons;
import dev.js.mtrheadlight.server.RegistrySelfCheck;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import dev.js.mtrheadlight.forge.platform.Reg;

public final class XiamenObjects {
   public static final String NAMESPACE = "mtrlte";
   private static final String PSD_DOOR = "psd_door_xiamen";
   private static final String PSD_GLASS = "psd_glass_xiamen";
   private static final String PSD_END = "psd_end_xiamen";
   private static final String PSD_TOP = "psd_top_xiamen";
   private static final String APG_DOOR = "apg_door_xiamen";
   private static final String APG_GLASS = "apg_glass_xiamen";
   private static final String APG_END = "apg_end_xiamen";
   public static final ResourceLocation LOGO_ID = new ResourceLocation("mtrlte", "xiamen_logo");
   public static final ResourceLocation PSD_DOOR_BE_ID = new ResourceLocation("mtrlte", "psd_door_xiamen");
   public static final ResourceLocation PSD_TOP_BE_ID = new ResourceLocation("mtrlte", "psd_top_xiamen");
   public static final ResourceLocation APG_DOOR_BE_ID = new ResourceLocation("mtrlte", "apg_door_xiamen");
   public static final ResourceLocation APG_GLASS_BE_ID = new ResourceLocation("mtrlte", "apg_glass_xiamen");
   public static final ResourceLocation CORE_TAB_ID = new ResourceLocation("mtrlte", "core");
   private static final Map<XiamenLine, XiamenObjects.LineSet> SETS = new EnumMap(XiamenLine.class);
   private static XiamenLogoBlock logo;
   private static BlockEntityType<XiamenPsdBlocks.TileEntityXiamenPSDDoor> psdDoorEntityType;
   private static BlockEntityType<XiamenPsdBlocks.TileEntityXiamenPSDTop> psdTopEntityType;
   private static BlockEntityType<XiamenPsdBlocks.TileEntityXiamenAPGDoor> apgDoorEntityType;
   private static BlockEntityType<XiamenPsdBlocks.TileEntityXiamenAPGGlass> apgGlassEntityType;
   private static boolean registered;

   private XiamenObjects() {
   }

   public static XiamenObjects.LineSet set(XiamenLine line) {
      return (XiamenObjects.LineSet)SETS.get(line == null ? XiamenLine.L1 : line);
   }

   public static XiamenLine lineOf(Block block) {
      XiamenLine var10000;
      if (block instanceof XiamenPsdBlocks.LineAware aware) {
         var10000 = aware.line();
      } else {
         var10000 = XiamenLine.L1;
      }

      return var10000;
   }

   public static String textureVariantOf(Block block) {
      return lineOf(block).textureVariant();
   }

   public static XiamenPsdBlocks.Door psdDoor() {
      return set(XiamenLine.L1).door;
   }

   public static XiamenPsdBlocks.Glass psdGlass() {
      return set(XiamenLine.L1).glass;
   }

   public static XiamenPsdBlocks.GlassEnd psdEnd() {
      return set(XiamenLine.L1).end;
   }

   public static XiamenPsdBlocks.Top psdTop() {
      return set(XiamenLine.L1).top;
   }

   public static XiamenPsdBlocks.ApgDoor apgDoor() {
      return set(XiamenLine.L1).apgDoor;
   }

   public static XiamenPsdBlocks.ApgGlass apgGlass() {
      return set(XiamenLine.L1).apgGlass;
   }

   public static XiamenPsdBlocks.ApgEnd apgEnd() {
      return set(XiamenLine.L1).apgEnd;
   }

   public static Item psdDoorItem() {
      return set(XiamenLine.L1).doorItem;
   }

   public static Item psdGlassItem() {
      return set(XiamenLine.L1).glassItem;
   }

   public static Item psdEndItem() {
      return set(XiamenLine.L1).endItem;
   }

   public static Item apgDoorItem() {
      return set(XiamenLine.L1).apgDoorItem;
   }

   public static Item apgGlassItem() {
      return set(XiamenLine.L1).apgGlassItem;
   }

   public static Item apgEndItem() {
      return set(XiamenLine.L1).apgEndItem;
   }

   public static XiamenLogoBlock logo() {
      return logo;
   }

   public static BlockEntityType<XiamenPsdBlocks.TileEntityXiamenPSDDoor> psdDoorEntityType() {
      return psdDoorEntityType;
   }

   public static BlockEntityType<XiamenPsdBlocks.TileEntityXiamenPSDTop> psdTopEntityType() {
      return psdTopEntityType;
   }

   public static BlockEntityType<XiamenPsdBlocks.TileEntityXiamenAPGDoor> apgDoorEntityType() {
      return apgDoorEntityType;
   }

   public static BlockEntityType<XiamenPsdBlocks.TileEntityXiamenAPGGlass> apgGlassEntityType() {
      return apgGlassEntityType;
   }

   public static void register() {
      if (!registered) {
         registered = true;

         for(XiamenLine line : XiamenLine.values()) {
            try {
               register(line);
            } catch (Throwable var5) {
               LteLog.LOGGER.error("[lte] xiamen {} psd set failed to register - skipping that line", line, var5);
               RegistrySelfCheck.purgePending("xiamen " + String.valueOf(line));
            }
         }

         logo = registerLogo(LOGO_ID);
         psdDoorEntityType = (BlockEntityType)Reg.blockEntityType(PSD_DOOR_BE_ID, XiamenPsdBlocks.TileEntityXiamenPSDDoor::new, blocksOf((s) -> s.door));
         psdTopEntityType = (BlockEntityType)Reg.blockEntityType(PSD_TOP_BE_ID, XiamenPsdBlocks.TileEntityXiamenPSDTop::new, blocksOf((s) -> s.top));
         apgDoorEntityType = (BlockEntityType)Reg.blockEntityType(APG_DOOR_BE_ID, XiamenPsdBlocks.TileEntityXiamenAPGDoor::new, blocksOf((s) -> s.apgDoor));
         apgGlassEntityType = (BlockEntityType)Reg.blockEntityType(APG_GLASS_BE_ID, XiamenPsdBlocks.TileEntityXiamenAPGGlass::new, blocksOf((s) -> s.apgGlass));
         TabIcons.register();
         Reg.tab(CORE_TAB_ID, Component.translatable("itemGroup.mtrlte.core"), () -> new ItemStack(TabIcons.core()));
         Reg.tabEntries(CORE_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> {
            entries.method_45421(logo);

            for(XiamenLine line : XiamenLine.values()) {
               XiamenObjects.LineSet set = set(line);
               entries.method_45421(set.door);
               entries.method_45421(set.glass);
               entries.method_45421(set.end);
               entries.method_45421(set.apgDoor);
               entries.method_45421(set.apgGlass);
               entries.method_45421(set.apgEnd);
            }

         });

         for(XiamenLine line : XiamenLine.values()) {
            XiamenObjects.LineSet set = set(line);
            LteLog.LOGGER.info("[lte] xiamen {} psd: {} / {} / {} / {} | apg: {} / {} / {}", new Object[]{line, set.doorId, set.glassId, set.endId, set.topId, set.apgDoorId, set.apgGlassId, set.apgEndId});
         }

      }
   }

   private static Block[] blocksOf(Function<XiamenObjects.LineSet, Block> pick) {
      XiamenLine[] lines = XiamenLine.values();
      Block[] blocks = new Block[lines.length];

      for(int i = 0; i < lines.length; ++i) {
         blocks[i] = (Block)pick.apply(set(lines[i]));
      }

      return blocks;
   }

   private static void register(XiamenLine line) {
      XiamenObjects.LineSet set = set(line);
      set.door = new XiamenPsdBlocks.Door(line);
      Reg.block(set.doorId, set.door);
      set.doorItem = (Item)Reg.item(set.doorId, new PsdAPGItem(set.door, PsdAPGItem.Kind.DOOR, new Item.Properties()));
      set.glass = new XiamenPsdBlocks.Glass(line);
      Reg.block(set.glassId, set.glass);
      set.glassItem = (Item)Reg.item(set.glassId, new PsdAPGItem(set.glass, PsdAPGItem.Kind.GLASS, new Item.Properties()));
      set.end = new XiamenPsdBlocks.GlassEnd(line);
      Reg.block(set.endId, set.end);
      set.endItem = (Item)Reg.item(set.endId, new PsdAPGItem(set.end, PsdAPGItem.Kind.END, new Item.Properties()));
      set.top = new XiamenPsdBlocks.Top(line);
      Reg.block(set.topId, set.top);
      set.apgDoor = new XiamenPsdBlocks.ApgDoor(line);
      Reg.block(set.apgDoorId, set.apgDoor);
      set.apgDoorItem = (Item)Reg.item(set.apgDoorId, new PsdAPGItem(set.apgDoor, PsdAPGItem.Kind.APG_DOOR, new Item.Properties()));
      set.apgGlass = new XiamenPsdBlocks.ApgGlass(line);
      Reg.block(set.apgGlassId, set.apgGlass);
      set.apgGlassItem = (Item)Reg.item(set.apgGlassId, new PsdAPGItem(set.apgGlass, PsdAPGItem.Kind.APG_GLASS, new Item.Properties()));
      set.apgEnd = new XiamenPsdBlocks.ApgEnd(line);
      Reg.block(set.apgEndId, set.apgEnd);
      set.apgEndItem = (Item)Reg.item(set.apgEndId, new PsdAPGItem(set.apgEnd, PsdAPGItem.Kind.APG_END, new Item.Properties()));
   }

   private static XiamenLogoBlock registerLogo(ResourceLocation id) {
      XiamenLogoBlock block = new XiamenLogoBlock(Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().destroyTime(2.0F).lightLevel((state) -> 10));
      Reg.block(id, block);
      Reg.item(id, new BlockItem(block, new Item.Properties()));
      return block;
   }

   public static CreativeModeTab tab() {
      return (CreativeModeTab)BuiltInRegistries.CREATIVE_MODE_TAB.get(CORE_TAB_ID);
   }

   static {
      for(XiamenLine line : XiamenLine.values()) {
         SETS.put(line, new XiamenObjects.LineSet(line));
      }

   }

   public static final class LineSet {
      public final XiamenLine line;
      public final ResourceLocation doorId;
      public final ResourceLocation glassId;
      public final ResourceLocation endId;
      public final ResourceLocation topId;
      public final ResourceLocation apgDoorId;
      public final ResourceLocation apgGlassId;
      public final ResourceLocation apgEndId;
      public XiamenPsdBlocks.Door door;
      public XiamenPsdBlocks.Glass glass;
      public XiamenPsdBlocks.GlassEnd end;
      public XiamenPsdBlocks.Top top;
      public XiamenPsdBlocks.ApgDoor apgDoor;
      public XiamenPsdBlocks.ApgGlass apgGlass;
      public XiamenPsdBlocks.ApgEnd apgEnd;
      public Item doorItem;
      public Item glassItem;
      public Item endItem;
      public Item apgDoorItem;
      public Item apgGlassItem;
      public Item apgEndItem;

      private LineSet(XiamenLine line) {
         this.line = line;
         this.doorId = new ResourceLocation("mtrlte", line.id("psd_door_xiamen"));
         this.glassId = new ResourceLocation("mtrlte", line.id("psd_glass_xiamen"));
         this.endId = new ResourceLocation("mtrlte", line.id("psd_end_xiamen"));
         this.topId = new ResourceLocation("mtrlte", line.id("psd_top_xiamen"));
         this.apgDoorId = new ResourceLocation("mtrlte", line.id("apg_door_xiamen"));
         this.apgGlassId = new ResourceLocation("mtrlte", line.id("apg_glass_xiamen"));
         this.apgEndId = new ResourceLocation("mtrlte", line.id("apg_end_xiamen"));
      }

      public Block[] blocks() {
         return new Block[]{this.door, this.glass, this.end, this.top, this.apgDoor, this.apgGlass, this.apgEnd};
      }
   }
}
