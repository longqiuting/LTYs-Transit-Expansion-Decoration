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

public final class RailingWallBlocks {
   private static final ResourceLocation CORE_TAB_ID = new ResourceLocation("mtrlte", "core");
   private static final List<RailingWallBlocks.Variant> VARIANTS = List.of(new RailingWallBlocks.Variant("railing_wall_flat", "railing_stair_flat_on_wall", boxes(box(0.0D, 18.0D, 0.0D, 3.0D, 22.0D, 16.0D))), new RailingWallBlocks.Variant("railing_wall_start", "railing_stair_start_on_wall", boxes(box(0.0D, 18.0D, -0.5D, 3.0D, 31.0D, 16.5D))), new RailingWallBlocks.Variant("railing_wall_start_mirror", "railing_stair_start_mirror_on_wall", boxes(box(0.0D, 18.0D, -0.5D, 3.0D, 31.0D, 16.5D))), new RailingWallBlocks.Variant("railing_wall_stair", "railing_stair_on_wall", boxes(box(0.0D, 14.0D, -0.5D, 3.0D, 31.0D, 16.5D))), new RailingWallBlocks.Variant("railing_wall_stair_mirror", "railing_stair_mirror_on_wall", boxes(box(0.0D, 14.0D, -0.5D, 3.0D, 31.0D, 16.5D))), new RailingWallBlocks.Variant("railing_wall_end", "railing_stair_end_on_wall", boxes(box(0.0D, 14.0D, -0.5D, 3.0D, 22.0D, 16.5D))), new RailingWallBlocks.Variant("railing_wall_end_mirror", "railing_stair_end_mirror_on_wall", boxes(box(0.0D, 14.0D, -0.5D, 3.0D, 22.0D, 16.5D))), new RailingWallBlocks.Variant("railing_wall_corner", "railing_stair_corner_on_wall", boxes(box(0.0D, 19.0D, 13.0D, 3.0D, 22.0D, 16.0D))), new RailingWallBlocks.Variant("railing_wall_corner_mirror", "railing_stair_corner_mirror_on_wall", boxes(box(0.0D, 19.0D, 0.0D, 3.0D, 22.0D, 3.0D))), new RailingWallBlocks.Variant("railing_wall_corner_2", "railing_stair_corner_2_on_wall", boxes(box(0.0D, 18.0D, 0.0D, 3.0D, 22.0D, 16.0D), box(1.0D, 18.0D, 1.0D, 16.0D, 22.0D, 3.0D), box(15.0D, 18.0D, 0.0D, 16.0D, 19.0D, 3.0D))), new RailingWallBlocks.Variant("railing_wall_corner_2_mirror", "railing_stair_corner_2_mirror_on_wall", boxes(box(0.0D, 18.0D, 0.0D, 3.0D, 22.0D, 16.0D), box(1.0D, 18.0D, 13.0D, 16.0D, 22.0D, 16.0D))));
   private static final List<Item> ITEMS = new ArrayList();
   private static boolean registered;

   private RailingWallBlocks() {
   }

   public static void register() {
      if (!registered) {
         registered = true;

         for(RailingWallBlocks.Variant variant : VARIANTS) {
            try {
               ResourceLocation id = new ResourceLocation("mtrlte", variant.id());
               RailingWallBlock block = (RailingWallBlock)Reg.block(id, new RailingWallBlock(settings(), variant.boxes()));
               ITEMS.add((Item)Reg.item(id, new BlockItem(block, new Item.Properties())));
            } catch (Throwable var4) {
               LteLog.LOGGER.error("[lte] railing wall variant {} failed to register - skipping it", variant.id(), var4);
               RegistrySelfCheck.purgePending("railing wall " + variant.id());
            }
         }

         Reg.tabEntries(CORE_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> ITEMS.forEach(entries::accept));
         LteLog.LOGGER.info("[lte] railing wall: {} variants ({} .. {})", new Object[]{VARIANTS.size(), ((RailingWallBlocks.Variant)VARIANTS.get(0)).id(), ((RailingWallBlocks.Variant)VARIANTS.get(VARIANTS.size() - 1)).id()});
      }
   }

   private static BlockBehaviour.Properties settings() {
      return Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion();
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
