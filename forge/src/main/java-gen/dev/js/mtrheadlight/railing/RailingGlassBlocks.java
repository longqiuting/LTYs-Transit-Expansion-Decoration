package dev.js.mtrheadlight.railing;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.server.RegistrySelfCheck;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import dev.js.mtrheadlight.forge.platform.Reg;

public final class RailingGlassBlocks {
   private static final ResourceLocation CORE_TAB_ID = new ResourceLocation("mtrlte", "core");
   private static final List<RailingGlassBlocks.Variant> VARIANTS = List.of(new RailingGlassBlocks.Variant("railing_glass_flat", "railing_stair_glass_flat", boxes(box(0.75D, 0.0D, 0.0D, 2.25D, 22.0D, 16.0D))), new RailingGlassBlocks.Variant("railing_glass_start", "railing_stair_glass_start", boxes(box(0.75D, 0.0D, 0.0D, 2.25D, 31.0D, 16.0D))), new RailingGlassBlocks.Variant("railing_glass_start_mirror", "railing_stair_glass_start_mirror", boxes(box(0.75D, 0.0D, 0.0D, 2.25D, 31.0D, 16.0D))), new RailingGlassBlocks.Variant("railing_glass_stair", "railing_stair_glass", boxes(box(0.75D, 0.0D, 0.0D, 2.25D, 31.0D, 17.1D))), new RailingGlassBlocks.Variant("railing_glass_stair_mirror", "railing_stair_glass_mirror", boxes(box(0.75D, 0.0D, -1.1D, 2.25D, 31.0D, 16.0D))), new RailingGlassBlocks.Variant("railing_glass_end", "railing_stair_glass_end", boxes(box(0.75D, 0.0D, 0.0D, 2.25D, 22.0D, 17.1D))), new RailingGlassBlocks.Variant("railing_glass_end_mirror", "railing_stair_glass_end_mirror", boxes(box(0.75D, 0.0D, -1.1D, 2.25D, 22.0D, 16.0D))), new RailingGlassBlocks.Variant("railing_glass_corner", "railing_stair_glass_corner", boxes(box(0.0D, 0.0D, 13.75D, 2.25D, 22.0D, 16.0D))), new RailingGlassBlocks.Variant("railing_glass_corner_mirror", "railing_stair_glass_corner_mirror", boxes(box(0.0D, 0.0D, 0.0D, 2.25D, 22.0D, 2.25D))), new RailingGlassBlocks.Variant("railing_glass_corner_2", "railing_stair_glass_corner_2", boxes(box(0.75D, 0.0D, 0.75D, 2.25D, 22.0D, 16.0D), box(0.75D, 0.0D, 0.75D, 16.0D, 22.0D, 2.25D))), new RailingGlassBlocks.Variant("railing_glass_corner_2_mirror", "railing_stair_glass_corner_2_mirror", boxes(box(0.75D, 0.0D, 0.0D, 2.25D, 22.0D, 15.25D), box(0.75D, 0.0D, 13.75D, 16.0D, 22.0D, 15.25D))));
   private static final List<Item> ITEMS = new ArrayList();
   private static final List<RailingWallBlock> BLOCKS = new ArrayList();
   private static boolean registered;

   private RailingGlassBlocks() {
   }

   public static void register() {
      if (!registered) {
         registered = true;

         for(RailingGlassBlocks.Variant variant : VARIANTS) {
            try {
               ResourceLocation id = new ResourceLocation("mtrlte", variant.id());
               RailingWallBlock block = (RailingWallBlock)Reg.block(id, new RailingWallBlock(settings(), variant.boxes()));
               BLOCKS.add(block);
               ITEMS.add((Item)Reg.item(id, new BlockItem(block, new Item.Properties())));
            } catch (Throwable var4) {
               LteLog.LOGGER.error("[lte] railing glass variant {} failed to register - skipping it", variant.id(), var4);
               RegistrySelfCheck.purgePending("railing glass " + variant.id());
            }
         }

         Reg.tabEntries(CORE_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> ITEMS.forEach(entries::accept));
         LteLog.LOGGER.info("[lte] railing glass: {} variants ({} .. {})", new Object[]{VARIANTS.size(), ((RailingGlassBlocks.Variant)VARIANTS.get(0)).id(), ((RailingGlassBlocks.Variant)VARIANTS.get(VARIANTS.size() - 1)).id()});
      }
   }

   static List<RailingWallBlock> blocks() {
      return List.copyOf(BLOCKS);
   }

   private static BlockBehaviour.Properties settings() {
      return Properties.of().mapColor(MapColor.NONE).strength(0.6F).sound(SoundType.GLASS).noOcclusion().isValidSpawn((state, world, pos, type) -> false).isRedstoneConductor((state, world, pos) -> false).isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false);
   }

   private static double[] box(double x1, double y1, double z1, double x2, double y2, double z2) {
      return new double[]{x1, y1, z1, x2, y2, z2};
   }

   private static double[][] boxes(double[]... boxes) {
      return boxes;
   }

   private static record Variant(String id, String model, double[][] boxes) {
   }
}
