package dev.js.mtrheadlight.server;

import dev.js.mtrheadlight.LteLog;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class RegistrySelfCheck {
   private static final String[] PENDING_FIELDS = new String[]{"intrusiveValueToEntry", "intrusiveValues", "field_40584"};

   private RegistrySelfCheck() {
   }

   public static void reportBlocks(String phase) {
      int total = 0;
      int unregistered = 0;

      for(Block block : BuiltInRegistries.BLOCK) {
         ++total;
         if (block != Blocks.AIR && block != Blocks.CAVE_AIR && block != Blocks.VOID_AIR) {
            try {
               BlockState state = block.defaultBlockState();
               if (state != null && state.isAir()) {
                  ++unregistered;
                  LteLog.LOGGER.warn("[lte] unregistered block: class={} id={} state={} ({} phase)", new Object[]{block.getClass().getName(), String.valueOf(idOf(block)), String.valueOf(state), phase});
               }
            } catch (Throwable var6) {
               ++unregistered;
               LteLog.LOGGER.warn("[lte] unregistered block: class={} id={} state=<unreadable: {}> ({} phase)", new Object[]{block.getClass().getName(), String.valueOf(idOf(block)), var6.toString(), phase});
            }
         }
      }

      LteLog.LOGGER.info("[lte] block registry: {} blocks, {} unregistered", total, unregistered);
   }

   private static ResourceLocation idOf(Block block) {
      try {
         return BuiltInRegistries.BLOCK.getKey(block);
      } catch (Throwable var2) {
         return null;
      }
   }

   public static int countBlocks() {
      int n = 0;

      for(Block ignored : BuiltInRegistries.BLOCK) {
         ++n;
      }

      return n;
   }

   public static int countBlocks(String namespace) {
      int n = 0;

      for(Block block : BuiltInRegistries.BLOCK) {
         ResourceLocation id = idOf(block);
         if (id != null && namespace.equals(id.getNamespace())) {
            ++n;
         }
      }

      return n;
   }

   public static int pendingCount() {
      Map<Object, Object> pending = pendingTable(BuiltInRegistries.BLOCK);
      return pending == null ? 0 : pending.size();
   }

   public static void purgePending(String phase) {
      Map<Object, Object> pending = pendingTable(BuiltInRegistries.BLOCK);
      if (pending != null && !pending.isEmpty()) {
         List<Object> leaked = new ArrayList();

         for(Map.Entry<Object, Object> entry : pending.entrySet()) {
            Object key = entry.getKey();
            if (!(key instanceof Block)) {
               leaked.add(key);
            } else {
               Block block = (Block)key;
               if (BuiltInRegistries.BLOCK.getResourceKey(block).isEmpty()) {
                  leaked.add(key);
                  LteLog.LOGGER.warn("[lte] unregistered block: class={} state={} (never reached the registry, id lookup would fall back to minecraft:air) [{} phase]", new Object[]{block.getClass().getName(), safeState(block), phase});
               }
            }
         }

         if (!leaked.isEmpty()) {
            for(Object key : leaked) {
               pending.remove(key);
            }

            LteLog.LOGGER.warn("[lte] dropped {} leaked block reference(s) before the block registry froze [{} phase] - without this the server would crash with 'Some intrusive holders were not registered'. The blocks named above are missing from this run (their registration threw).", leaked.size(), phase);
         }
      }
   }

   private static String safeState(Block block) {
      try {
         return String.valueOf(block.defaultBlockState());
      } catch (Throwable var2) {
         return "<unreadable: " + String.valueOf(var2) + ">";
      }
   }

   private static Map<Object, Object> pendingTable(Object registry) {
      Field field = findPendingField(registry.getClass());
      if (field == null) {
         return null;
      } else {
         try {
            field.setAccessible(true);
            Object raw = field.get(registry);
            return raw instanceof Map ? (Map)raw : null;
         } catch (Throwable var3) {
            return null;
         }
      }
   }

   private static Field findPendingField(Class<?> type) {
      for(Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
         for(String name : PENDING_FIELDS) {
            try {
               return c.getDeclaredField(name);
            } catch (NoSuchFieldException var7) {
            }
         }
      }

      return null;
   }
}
