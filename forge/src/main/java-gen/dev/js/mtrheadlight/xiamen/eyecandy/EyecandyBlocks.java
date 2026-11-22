package dev.js.mtrheadlight.xiamen.eyecandy;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawObjParser;
import dev.js.mtrheadlight.server.RegistrySelfCheck;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.VoxelShape;
import dev.js.mtrheadlight.forge.platform.Reg;

public final class EyecandyBlocks {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation NANPUTUO_TAB_ID = new ResourceLocation("mtrlte", "nanputuo_decor");
   public static final ResourceLocation ENTITY_ID = new ResourceLocation("mtrlte", "eyecandy");
   private static final Set<String> CORNER_KEYS = new LinkedHashSet(List.of("msd_corner_railing", "msd_corner_railing_mirror", "msd_corner_railing_2", "msd_corner_railing_2_mirror", "xiamen_brt_railing_corner"));
   private static final Set<String> NO_COLLISION_KEYS = new LinkedHashSet(List.of("bridge_pier_20"));
   private static final double NUDGE_UNITS = 2.5D;
   private static final double MIN_SIZE_UNITS = 1.0D;
   private static final int GAP_UNITS = 2;
   private static final int CLOSE_PASSES = 3;
   private static final List<EyecandyEntry> ENTRIES = new ArrayList();
   private static final Map<String, EyecandyBlock> BLOCKS = new LinkedHashMap();
   private static final Map<String, Item> ITEMS = new LinkedHashMap();
   private static final Set<String> WARNING_LIGHT_KEYS = new LinkedHashSet(List.of("xiamen_brt_psd_left", "xiamen_brt_psd_right"));
   private static final Map<String, RawMesh[]> LIGHT_MESHES = new ConcurrentHashMap();
   private static final Map<String, String> MODEL_TEXTS = new LinkedHashMap();
   private static BlockEntityType<EyecandyBlockEntity> entityType;
   private static boolean registered;

   private EyecandyBlocks() {
   }

   public static List<EyecandyEntry> entries() {
      return ENTRIES;
   }

   public static List<EyecandyBlock> blocks() {
      return new ArrayList(BLOCKS.values());
   }

   public static EyecandyBlock block(String key) {
      return (EyecandyBlock)BLOCKS.get(key);
   }

   public static Item item(String key) {
      return (Item)ITEMS.get(key);
   }

   public static BlockEntityType<EyecandyBlockEntity> entityType() {
      return entityType;
   }

   public static boolean isCorner(String key) {
      return CORNER_KEYS.contains(key);
   }

   public static boolean hasNoCollision(String key) {
      return NO_COLLISION_KEYS.contains(key);
   }

   public static double[] boxOf(EyecandyEntry entry) {
      String text = modelText(entry);
      if (text == null) {
         return new double[]{0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D};
      } else {
         double[] units = RawObjParser.bounds(text, entry.scale(), entry.rotation(), entry.translation(), entry.mirror());
         double[] box = new double[6];

         for(int axis = 0; axis < 3; ++axis) {
            double min = units[axis] / 16.0D;
            double max = units[axis + 3] / 16.0D;
            if (min < 0.0D && min > -0.15625D) {
               min = 0.0D;
            }

            if (max - min < 0.0625D) {
               max = min + 0.0625D;
            }

            box[axis] = min;
            box[axis + 3] = max;
         }

         return box;
      }
   }

   public static List<double[]> boxesOf(EyecandyEntry entry) {
      List<double[]> out = new ArrayList();
      if (hasNoCollision(entry.key())) {
         out.add(new double[]{0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D});
         return out;
      } else {
         if (isCorner(entry.key())) {
            String text = modelText(entry);
            if (text != null) {
               int[] grid = RawObjParser.footprint(text, entry.scale(), entry.rotation(), entry.translation(), entry.mirror());
               double[] box = boxOf(entry);
               List<double[]> corner = cornerBoxes(grid, Math.max(0.0D, box[1] * 16.0D), Math.max(1.0D, box[4] * 16.0D));
               if (!corner.isEmpty()) {
                  out.addAll(corner);
                  return out;
               }
            }

            LteLog.LOGGER.warn("[lte] eyecandy {}: corner footprint is empty, falling back to an AABB", entry.key());
         }

         double[] box = boxOf(entry);
         out.add(new double[]{box[0] * 16.0D, box[1] * 16.0D, box[2] * 16.0D, box[3] * 16.0D, box[4] * 16.0D, box[5] * 16.0D});
         return out;
      }
   }

   public static VoxelShape shapeOf(EyecandyEntry entry) {
      EyecandyBlock block = (EyecandyBlock)BLOCKS.get(entry.key());
      return block != null ? block.shape() : EyecandyBlock.shapeOf(boxesOf(entry));
   }

   public static RawMesh mesh(EyecandyEntry entry) {
      return RawObjParser.get(entry.modelId(), entry.texture(), entry.flipV(), entry.scale(), entry.rotation(), entry.translation(), entry.mirror());
   }

   public static boolean isWarningLight(String key) {
      return WARNING_LIGHT_KEYS.contains(key);
   }

   public static RawMesh baseMesh(EyecandyEntry entry) {
      return !isWarningLight(entry.key()) ? mesh(entry) : lightParts(entry)[0];
   }

   public static RawMesh lightMesh(EyecandyEntry entry, boolean lit) {
      if (!isWarningLight(entry.key())) {
         return null;
      } else {
         RawMesh[] parts = lightParts(entry);
         return lit ? parts[2] : parts[1];
      }
   }

   private static RawMesh[] lightParts(EyecandyEntry entry) {
      RawMesh[] cached = (RawMesh[])LIGHT_MESHES.get(entry.key());
      if (cached != null) {
         return cached;
      } else {
         RawMesh full = mesh(entry);
         RawMesh[] parts = splitLight(entry.key(), full);
         if (full != null) {
            LIGHT_MESHES.put(entry.key(), parts);
         }

         return parts;
      }
   }

   static RawMesh[] splitLight(String key, RawMesh full) {
      if (full == null) {
         return new RawMesh[]{null, null, null};
      } else {
         List<RawObjParser.GroupRun> lightRuns = new ArrayList();

         for(RawObjParser.GroupRun run : RawObjParser.groupRuns(full)) {
            if (isLightGroup(run.name())) {
               lightRuns.add(run);
            }
         }

         RawMesh base = RawObjParser.select(full, (name) -> !isLightGroup(name));
         RawMesh empty = new RawMesh(full.texture(), new RawMesh.Quad[0]);
         if (lightRuns.isEmpty()) {
            LteLog.LOGGER.warn("[lte] eyecandy {}: the obj has no light group (o light / light2), the warning light is drawn as a static part of the body", key);
            return new RawMesh[]{base, empty, empty};
         } else {
            RawMesh off = null;
            RawMesh on = null;

            for(RawObjParser.GroupRun run : lightRuns) {
               RawMesh piece = new RawMesh(full.texture(), run.quads());
               if (isLitName(run.name())) {
                  if (on == null) {
                     on = piece;
                  }
               } else if (off == null) {
                  off = piece;
               } else if (on == null) {
                  on = piece;
               }
            }

            if (off == null) {
               off = empty;
            }

            if (on == null) {
               on = off;
               LteLog.LOGGER.warn("[lte] eyecandy {}: only one light group and no name hint (light2 / light_on), the warning light cannot blink", key);
            }

            LteLog.LOGGER.info("[lte] eyecandy {}: warning light split into base {} + off {} + on {} quad(s)", new Object[]{key, base.quadCount(), off.quadCount(), on.quadCount()});
            return new RawMesh[]{base, off, on};
         }
      }
   }

   private static boolean isLightGroup(String name) {
      return name != null && name.toLowerCase(Locale.ROOT).startsWith("light");
   }

   private static boolean isLitName(String name) {
      if (name == null) {
         return false;
      } else {
         String lower = name.toLowerCase(Locale.ROOT);
         return lower.endsWith("2") || lower.endsWith("on") || lower.contains("lit") || lower.contains("bright");
      }
   }

   static List<double[]> cornerBoxes(int[] grid, double yMin, double yMax) {
      int size = 16;
      int[] filled = (int[])grid.clone();

      for(int pass = 0; pass < 3; ++pass) {
         int[] next = (int[])filled.clone();

         for(int z = 0; z < 16; ++z) {
            for(int x = 0; x < 16; ++x) {
               if (filled[z * 16 + x] == 0) {
                  boolean horizontal = at(filled, x - 1, z) && at(filled, x + 1, z);
                  boolean vertical = at(filled, x, z - 1) && at(filled, x, z + 1);
                  if (horizontal || vertical) {
                     next[z * 16 + x] = 1;
                  }
               }
            }
         }

         filled = next;
      }

      List<List<int[]>> rows = new ArrayList(16);

      for(int z = 0; z < 16; ++z) {
         List<int[]> runs = new ArrayList();
         int x = 0;

         while(x < 16) {
            if (filled[z * 16 + x] == 0) {
               ++x;
            } else {
               int end = x;
               int scan = x;

               while(scan < 16) {
                  if (filled[z * 16 + scan] != 0) {
                     end = scan++;
                  } else {
                     int look;
                     for(look = scan; look < 16 && look - scan < 2 && filled[z * 16 + look] == 0; ++look) {
                     }

                     if (look >= 16 || filled[z * 16 + look] == 0) {
                        break;
                     }

                     scan = look;
                  }
               }

               runs.add(new int[]{x, end + 1});
               x = end + 1;
            }
         }

         rows.add(runs);
      }

      List<double[]> out = new ArrayList();
      int z0 = 0;

      while(z0 < 16) {
         if (((List)rows.get(z0)).isEmpty()) {
            ++z0;
         } else {
            int z1;
            for(z1 = z0; z1 + 1 < 16 && sameRuns((List)rows.get(z1 + 1), (List)rows.get(z0)); ++z1) {
            }

            for(int[] run : (List<int[]>)rows.get(z0)) {
               out.add(new double[]{(double)run[0], yMin, (double)z0, (double)run[1], yMax, (double)(z1 + 1)});
            }

            z0 = z1 + 1;
         }
      }

      return out;
   }

   private static boolean at(int[] grid, int x, int z) {
      int size = 16;
      return x >= 0 && z >= 0 && x < 16 && z < 16 && grid[z * 16 + x] != 0;
   }

   private static boolean sameRuns(List<int[]> a, List<int[]> b) {
      if (a.size() != b.size()) {
         return false;
      } else {
         for(int i = 0; i < a.size(); ++i) {
            if (((int[])a.get(i))[0] != ((int[])b.get(i))[0] || ((int[])a.get(i))[1] != ((int[])b.get(i))[1]) {
               return false;
            }
         }

         return true;
      }
   }

   private static String modelText(EyecandyEntry entry) {
      String cached = (String)MODEL_TEXTS.get(entry.key());
      if (cached != null) {
         return cached.isEmpty() ? null : cached;
      } else {
         String path = "/assets/" + entry.modelId().getNamespace() + "/" + entry.modelId().getPath();

         try {
            InputStream in = EyecandyBlocks.class.getResourceAsStream(path);

            String text;
            label60: {
               String var5;
               try {
                  if (in == null) {
                     MODEL_TEXTS.put(entry.key(), "");
                     LteLog.LOGGER.warn("[lte] eyecandy {}: {} is missing (collision box falls back to a full block)", entry.key(), path);
                     text = null;
                     break label60;
                  }

                  text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                  MODEL_TEXTS.put(entry.key(), text);
                  var5 = text;
               } catch (Throwable var7) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }
                  }

                  throw var7;
               }

               if (in != null) {
                  in.close();
               }

               return var5;
            }

            if (in != null) {
               in.close();
            }

            return text;
         } catch (Throwable var8) {
            MODEL_TEXTS.put(entry.key(), "");
            LteLog.LOGGER.warn("[lte] eyecandy {}: could not read {} ({})", new Object[]{entry.key(), path, var8.toString()});
            return null;
         }
      }
   }

   public static void register() {
      if (!registered) {
         registered = true;

         for(EyecandyEntry.Pack pack : EyecandyEntry.Pack.values()) {
            List<EyecandyEntry> loaded = EyecandyEntry.loadAll(pack);
            if (loaded.isEmpty()) {
               LteLog.LOGGER.warn("[lte] eyecandy: {} is missing or empty, no {} decorations registered", pack.descriptor(), pack.name().toLowerCase(Locale.ROOT));
            } else {
               ENTRIES.addAll(loaded);
            }
         }

         if (!ENTRIES.isEmpty()) {
            List<Block> blocks = new ArrayList(ENTRIES.size());

            for(EyecandyEntry entry : ENTRIES) {
               try {
                  List<double[]> boxes = boxesOf(entry);
                  EyecandyBlock block = new EyecandyBlock(Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.8F).sound(SoundType.STONE).noOcclusion(), entry, boxes, !hasNoCollision(entry.key()));
                  Reg.block(entry.blockId(), block);
                  Item item = (Item)Reg.item(entry.blockId(), new BlockItem(block, new Item.Properties()));
                  blocks.add(block);
                  BLOCKS.put(entry.key(), block);
                  ITEMS.put(entry.key(), item);
               } catch (Throwable var6) {
                  LteLog.LOGGER.error("[lte] eyecandy {} failed to register - skipping it", entry.blockId(), var6);
                  RegistrySelfCheck.purgePending("eyecandy " + entry.key());
               }
            }

            entityType = (BlockEntityType)Reg.blockEntityType(ENTITY_ID, EyecandyBlockEntity::new, (Block[])blocks.toArray(new Block[0]));
            Reg.tabEntries(XiamenObjects.CORE_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> addAll(entries, EyecandyEntry.Pack.XIAMEN));
            List<EyecandyEntry> nanputuo = entriesOf(EyecandyEntry.Pack.NANPUTUO);
            if (!nanputuo.isEmpty()) {
               Reg.tab(NANPUTUO_TAB_ID, Component.translatable("itemGroup.mtrlte.nanputuo_decor"), () -> new ItemStack((ItemLike)ITEMS.get(((EyecandyEntry)nanputuo.get(0)).key())));
               Reg.tabEntries(NANPUTUO_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> addAll(entries, EyecandyEntry.Pack.NANPUTUO));
            }

            for(EyecandyEntry entry : ENTRIES) {
               List<double[]> boxes = boxesOf(entry);
               double[] box = boxOf(entry);
               LteLog.LOGGER.info("[lte] eyecandy {} ({}) {} box(es), aabb {}x{}x{} at [{}, {}, {}]{}", new Object[]{entry.blockId(), entry.displayName(), boxes.size(), round(box[3] - box[0]), round(box[4] - box[1]), round(box[5] - box[2]), round(box[0]), round(box[1]), round(box[2]), hasNoCollision(entry.key()) ? " (outline only)" : (isCorner(entry.key()) ? " (corner)" : "")});
            }

            LteLog.LOGGER.info("[lte] eyecandy: {} decorations registered ({} into {}, {} into {})", new Object[]{ENTRIES.size(), entriesOf(EyecandyEntry.Pack.XIAMEN).size(), XiamenObjects.CORE_TAB_ID, nanputuo.size(), NANPUTUO_TAB_ID});
         }
      }
   }

   public static CreativeModeTab tab(ResourceLocation id) {
      return (CreativeModeTab)BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
   }

   private static List<EyecandyEntry> entriesOf(EyecandyEntry.Pack pack) {
      List<EyecandyEntry> out = new ArrayList();

      for(EyecandyEntry entry : ENTRIES) {
         if (entry.pack() == pack) {
            out.add(entry);
         }
      }

      return out;
   }

   private static void addAll(CreativeModeTab.Output entries, EyecandyEntry.Pack pack) {
      for(EyecandyEntry entry : ENTRIES) {
         if (entry.pack() == pack) {
            Item item = (Item)ITEMS.get(entry.key());
            if (item != null) {
               entries.accept(item);
            }
         }
      }

   }

   private static double round(double value) {
      return (double)Math.round(value * 1000.0D) / 1000.0D;
   }
}
