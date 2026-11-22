package dev.js.mtrheadlight.xiamen.fangsu;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.rawmodel.RawMesh;
import dev.js.mtrheadlight.rawmodel.RawObjParser;
import dev.js.mtrheadlight.server.RegistrySelfCheck;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class FangsuPsdBlocks {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation ENTITY_ID = new ResourceLocation("mtrlte", "fangsu_psd_door");
   public static final ResourceLocation END_DOOR_ENTITY_ID = new ResourceLocation("mtrlte", "fangsu_psd_end_door");
   public static final ResourceLocation GLASS_ENTITY_ID = new ResourceLocation("mtrlte", "fangsu_psd_glass");
   private static final List<FangsuPsdDef> DEFS = new ArrayList();
   private static final List<FangsuPsdGlassDef> GLASS_DEFS = new ArrayList();
   private static final Map<ResourceLocation, Block> BLOCKS = new LinkedHashMap();
   private static final Map<ResourceLocation, FangsuPsdGlassBlock> GLASS_BLOCKS = new LinkedHashMap();
   private static final Map<ResourceLocation, Item> ITEMS = new LinkedHashMap();
   private static final Map<String, Map<String, List<RawMesh>>> MESHES = new ConcurrentHashMap();
   private static BlockEntityType<FangsuPsdDoorBlock.FangsuPsdDoorEntity> entityType;
   private static BlockEntityType<FangsuPsdEndDoorBlock.FangsuPsdEndDoorEntity> endDoorEntityType;
   private static BlockEntityType<FangsuPsdGlassBlock.FangsuPsdGlassEntity> glassEntityType;
   private static boolean registered;

   private FangsuPsdBlocks() {
   }

   public static BlockEntityType<FangsuPsdDoorBlock.FangsuPsdDoorEntity> entityType() {
      return entityType;
   }

   public static BlockEntityType<FangsuPsdEndDoorBlock.FangsuPsdEndDoorEntity> endDoorEntityType() {
      return endDoorEntityType;
   }

   public static BlockEntityType<FangsuPsdGlassBlock.FangsuPsdGlassEntity> glassEntityType() {
      return glassEntityType;
   }

   public static List<FangsuPsdDef> defs() {
      return DEFS;
   }

   public static List<FangsuPsdGlassDef> glassDefs() {
      return GLASS_DEFS;
   }

   public static Item item(FangsuPsdDef def) {
      return (Item)ITEMS.get(new ResourceLocation("mtrlte", def.id()));
   }

   public static Item glassItem(FangsuPsdGlassDef def) {
      return (Item)ITEMS.get(new ResourceLocation("mtrlte", def.id()));
   }

   public static void register() {
      if (!registered) {
         registered = true;

         try {
            FangsuPsdSounds.register();
         } catch (Throwable var10) {
            LteLog.LOGGER.error("[lte] could not register the fangsu psd door sound - doors stay silent", var10);
         }

         DEFS.clear();
         DEFS.addAll(FangsuPsdDef.load());
         List<Block> autoBlocks = new ArrayList(4);
         List<Block> manualBlocks = new ArrayList(10);

         for(FangsuPsdDef def : DEFS) {
            try {
               ResourceLocation id = new ResourceLocation("mtrlte", def.id());
               Block block;
               Item item;
               if (def.kind() == FangsuPsdDef.Kind.END_DOOR) {
                  FangsuPsdEndDoorBlock endDoor = new FangsuPsdEndDoorBlock(def);
                  Registry.register(BuiltInRegistries.BLOCK, id, endDoor);
                  item = registerItem(id, endDoor);
                  endDoor.setItem(item);
                  manualBlocks.add(endDoor);
                  block = endDoor;
               } else {
                  FangsuPsdDoorBlock autoDoor = new FangsuPsdDoorBlock(def);
                  Registry.register(BuiltInRegistries.BLOCK, id, autoDoor);
                  item = registerItem(id, autoDoor);
                  autoDoor.setItem(item);
                  autoBlocks.add(autoDoor);
                  block = autoDoor;
               }

               BLOCKS.put(id, block);
               ITEMS.put(id, item);
            } catch (Throwable var9) {
               LteLog.LOGGER.error("[lte] fangsu psd door {} failed to register - skipping it", def.id(), var9);
               RegistrySelfCheck.purgePending("fangsu psd " + def.id());
            }
         }

         GLASS_DEFS.clear();
         GLASS_DEFS.addAll(FangsuPsdGlassDef.load());
         List<Block> glassBlocks = new ArrayList(GLASS_DEFS.size());

         for(FangsuPsdGlassDef def : GLASS_DEFS) {
            try {
               ResourceLocation id = new ResourceLocation("mtrlte", def.id());
               FangsuPsdGlassBlock block = new FangsuPsdGlassBlock(def);
               Registry.register(BuiltInRegistries.BLOCK, id, block);
               Item item = registerItem(id, block);
               block.setItem(item);
               GLASS_BLOCKS.put(id, block);
               ITEMS.put(id, item);
               glassBlocks.add(block);
            } catch (Throwable var8) {
               LteLog.LOGGER.error("[lte] fangsu psd glass {} failed to register - skipping it", def.id(), var8);
               RegistrySelfCheck.purgePending("fangsu psd glass " + def.id());
            }
         }

         if (autoBlocks.isEmpty() && manualBlocks.isEmpty()) {
            LteLog.LOGGER.warn("[lte] fangsu psd doors: no variant could be loaded, nothing registered");
         } else {
            if (!autoBlocks.isEmpty()) {
               entityType = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ENTITY_ID, FabricBlockEntityTypeBuilder.create(FangsuPsdDoorBlock.FangsuPsdDoorEntity::new, (Block[])autoBlocks.toArray(new Block[0])).build());
            }

            if (!manualBlocks.isEmpty()) {
               endDoorEntityType = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, END_DOOR_ENTITY_ID, FabricBlockEntityTypeBuilder.create(FangsuPsdEndDoorBlock.FangsuPsdEndDoorEntity::new, (Block[])manualBlocks.toArray(new Block[0])).build());
            }

            if (!glassBlocks.isEmpty()) {
               glassEntityType = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, GLASS_ENTITY_ID, FabricBlockEntityTypeBuilder.create(FangsuPsdGlassBlock.FangsuPsdGlassEntity::new, (Block[])glassBlocks.toArray(new Block[0])).build());
            }

            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> {
               for(FangsuPsdDef def : DEFS) {
                  Item item = item(def);
                  if (item != null) {
                     entries.method_45421(item);
                  }
               }

               for(FangsuPsdGlassDef def : GLASS_DEFS) {
                  Item item = glassItem(def);
                  if (item != null) {
                     entries.method_45421(item);
                  }
               }

            });
            LteLog.LOGGER.info("[lte] fangsu psd doors: {} auto + {} manual doors + {} glass panels registered ({} / {} / {})", new Object[]{autoBlocks.size(), manualBlocks.size(), GLASS_DEFS.size(), ENTITY_ID, END_DOOR_ENTITY_ID, GLASS_ENTITY_ID});
         }
      }
   }

   private static Item registerItem(ResourceLocation id, Block block) {
      return (Item)Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
   }

   public static Map<String, List<RawMesh>> meshes(FangsuPsdDef def) {
      return meshes(def.model(), def.flipV(), def.id());
   }

   public static Map<String, List<RawMesh>> meshes(FangsuPsdGlassDef def) {
      return meshes(def.model(), def.flipV(), def.id());
   }

   private static Map<String, List<RawMesh>> meshes(ResourceLocation model, boolean flipV, String cacheKey) {
      return (Map)MESHES.computeIfAbsent(cacheKey, (id) -> {
         Map<String, Map<ResourceLocation, List<RawMesh.Quad>>> byGroup = new LinkedHashMap();

         for(RawMesh mesh : RawObjParser.materials(model, flipV, (float[])null, (float[])null, (float[])null, (boolean[])null).values()) {
            boolean bright = mesh.texture().getPath().endsWith("ltg.png");

            for(RawMesh.Quad quad : mesh.quads()) {
               RawMesh.Quad stored = bright && !quad.fullBright() ? new RawMesh.Quad(quad.pos(), quad.uv(), quad.normal(), quad.shade(), true, quad.group()) : quad;
               ((List)((Map)byGroup.computeIfAbsent(quad.group() == null ? "" : quad.group(), (key) -> new LinkedHashMap())).computeIfAbsent(mesh.texture(), (key) -> new ArrayList())).add(stored);
            }
         }

         Map<String, List<RawMesh>> out = new LinkedHashMap();
         byGroup.forEach((group, byTexture) -> {
            List<RawMesh> list = new ArrayList(byTexture.size());
            byTexture.forEach((texture, quads) -> list.add(new RawMesh(texture, (RawMesh.Quad[])quads.toArray(new RawMesh.Quad[0]))));
            out.put(group, list);
         });
         return out;
      });
   }
}
