package dev.js.mtrheadlight.bind;

import dev.js.mtrheadlight.LteLog;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import mtr.data.IPIDS;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MtrTiles {
   private static final String PROJECTOR = "mtr.block.BlockArrivalProjectorBase$TileEntityArrivalProjectorBase";
   private static final String PIDS = "mtr.data.IPIDS$TileEntityPIDS";
   private static final int PUSH_RADIUS = 64;
   private static final double PUSH_RADIUS_SQ = 4096.0D;

   private MtrTiles() {
   }

   public static boolean isProjector(Object tile) {
      if (tile == null) {
         return false;
      } else {
         return hasSupertype(tile.getClass(), "mtr.block.BlockArrivalProjectorBase$TileEntityArrivalProjectorBase") || hasSupertypeNamed(tile.getClass(), "TileEntityArrivalProjector");
      }
   }

   public static boolean isPids(Object tile) {
      if (tile == null) {
         return false;
      } else {
         try {
            if (tile instanceof IPIDS.TileEntityPIDS) {
               return true;
            }
         } catch (Throwable var2) {
         }

         return hasSupertype(tile.getClass(), "mtr.data.IPIDS$TileEntityPIDS");
      }
   }

   private static boolean hasSupertype(Class<?> type, String name) {
      for(Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
         if (cursor.getName().equals(name)) {
            return true;
         }
      }

      return false;
   }

   private static boolean hasSupertypeNamed(Class<?> type, String fragment) {
      for(Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
         String simple = cursor.getSimpleName();
         if (simple != null && simple.contains(fragment)) {
            return true;
         }
      }

      return false;
   }

   public static Object tileAt(Level world, BlockPos pos) {
      try {
         return world.getBlockEntity(pos);
      } catch (Throwable var3) {
         return null;
      }
   }

   public static Set<Long> projectorPlatformIds(Object tile) {
      try {
         Object value = method(tile, "getPlatformIds").invoke(tile);
         return (Set<Long>)(value instanceof Set ? new LinkedHashSet<Long>((Set)value) : Collections.emptySet());
      } catch (Throwable var2) {
         return Collections.emptySet();
      }
   }

   public static boolean projectorSetPlatforms(Object tile, Collection<Long> ids) {
      if (!isProjector(tile)) {
         LteLog.LOGGER.info("[lte] projector setData: tile={} is not a projector -> skip", describe(tile));
         return false;
      } else {
         int page;
         try {
            page = ((Number)method(tile, "getDisplayPage").invoke(tile)).intValue();
         } catch (Throwable var5) {
            LteLog.LOGGER.info("[lte] projector setData: getDisplayPage failed: {}", var5.toString());
            return false;
         }

         try {
            method(tile, "setData", Set.class, Integer.TYPE).invoke(tile, new HashSet(ids), page);
            LteLog.LOGGER.info("[lte] projector setData: tile={} wrote {} ids={} page={}", new Object[]{describe(tile), ids.size(), ids, page});
            return true;
         } catch (Throwable var6) {
            Throwable cause = var6.getCause() == null ? var6 : var6.getCause();
            LteLog.LOGGER.info("[lte] projector setData: invoke failed: {}: {}", cause.getClass().getName(), cause.getMessage());
            return false;
         }
      }
   }

   public static Set<Long> pidsPlatformIds(Object tile) {
      try {
         Object value = method(tile, "getPlatformIds").invoke(tile);
         return (Set<Long>)(value instanceof Set ? new LinkedHashSet<Long>((Set)value) : Collections.emptySet());
      } catch (Throwable var2) {
         return Collections.emptySet();
      }
   }

   public static String[] pidsMessages(Object tile) {
      try {
         Field field = tile.getClass().getField("messages");
         Object value = field.get(tile);
         return value instanceof String[] ? (String[])((String[])value).clone() : new String[0];
      } catch (Throwable var3) {
         return new String[0];
      }
   }

   public static boolean pidsSetPlatforms(Object tile, Collection<Long> ids) {
      if (!isPids(tile)) {
         LteLog.LOGGER.info("[lte] pids setData: tile={} is not a PIDS -> skip", describe(tile));
         return false;
      } else {
         String step = "messages";

         try {
            String[] messages = pidsMessages(tile);
            step = "getMaxArrivals";
            int max = ((Number)method(tile, "getMaxArrivals").invoke(tile)).intValue();
            boolean[] hideArrival = new boolean[max];
            step = "getHideArrival";
            Method hide = method(tile, "getHideArrival", Integer.TYPE);

            for(int i = 0; i < max; ++i) {
               hideArrival[i] = Boolean.TRUE.equals(hide.invoke(tile, i));
            }

            step = "getDisplayPage";
            int page = ((Number)method(tile, "getDisplayPage").invoke(tile)).intValue();
            step = "setData";
            method(tile, "setData", String[].class, boolean[].class, Set.class, Integer.TYPE).invoke(tile, messages, hideArrival, new HashSet(ids), page);
            LteLog.LOGGER.info("[lte] pids setData: tile={} wrote {} ids={} page={}", new Object[]{describe(tile), ids.size(), ids, page});
            return true;
         } catch (Throwable var8) {
            Throwable cause = var8.getCause() == null ? var8 : var8.getCause();
            LteLog.LOGGER.info("[lte] pids setData: step={} failed: {}: {}", new Object[]{step, cause.getClass().getName(), cause.getMessage()});
            return false;
         }
      }
   }

   public static boolean pushToClients(ServerLevel world, Object tile, BlockPos pos) {
      if (world != null && pos != null && tile instanceof BlockEntity entity) {
         try {
            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(entity);
            double x = (double)pos.getX() + 0.5D;
            double y = (double)pos.getY() + 0.5D;
            double z = (double)pos.getZ() + 0.5D;
            boolean sent = false;

            for(ServerPlayer player : world.players()) {
               if (player.distanceToSqr(x, y, z) <= 4096.0D) {
                  player.connection.send(packet);
                  sent = true;
               }
            }

            return sent;
         } catch (Throwable var14) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void markChunkSaved(Level world, BlockPos pos) {
      if (world != null && pos != null) {
         try {
            world.getChunkAt(pos).setUnsaved(true);
         } catch (Throwable var3) {
         }

      }
   }

   private static Method method(Object target, String name, Class<?>... parameters) throws NoSuchMethodException {
      return target.getClass().getMethod(name, parameters);
   }

   public static String describe(Object tile) {
      if (tile == null) {
         return "null";
      } else {
         StringBuilder out = new StringBuilder();

         for(Class<?> cursor = tile.getClass(); cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
            if (out.length() > 0) {
               out.append(" <- ");
            }

            out.append(cursor.getName());
         }

         return out.toString();
      }
   }

   public static String join(Collection<Long> ids) {
      StringBuilder out = new StringBuilder();

      for(Long id : ids) {
         if (out.length() > 0) {
            out.append(", ");
         }

         out.append(id);
      }

      return out.toString();
   }

   public static boolean pidsBindSingle(Object tile, long platformId) {
      return pidsSetPlatforms(tile, Collections.singletonList(platformId));
   }

   public static record PidsTile(Object tile, BlockPos pos) {
   }
}
