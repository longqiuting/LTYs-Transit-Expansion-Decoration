package dev.js.mtrheadlight.clock;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.Net;
import dev.js.mtrheadlight.forge.platform.Renderers;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ClockClient {
   private static boolean registered;

   private ClockClient() {
   }

   public static void register() {
      if (!registered) {
         registered = true;

         try {
            if (ClockObjects.entityType() != null) {
               Renderers.ber(ClockObjects.entityType(), ClockRenderer::new);
               LteLog.LOGGER.info("[lte] clock renderer registered ({}-block render distance)", 384);
            }
         } catch (Throwable var4) {
            LteLog.LOGGER.error("[lte] could not register the clock renderer", var4);
         }

         try {
            Net.registerClient(ClockNet.ID, (client, buf) -> {
               byte kind;
               BlockPos pos;
               ClockConfig cfg;
               try {
                  kind = buf.readByte();
                  pos = buf.readBlockPos();
                  cfg = kind != 0 && kind != 3 ? null : ClockConfig.readBuf(buf);
               } catch (Throwable var8) {
                  return;
               }

               client.execute(() -> {
                  if (kind == 0) {
                     client.setScreen(new ClockConfigScreen(pos, cfg));
                  } else if (kind == 3) {
                     applySynced(client, pos, cfg);
                  }

               });
            });
         } catch (Throwable var3) {
            LteLog.LOGGER.warn("[lte] clock config channel (client side) unavailable", var3);
         }

         try {
            Net.registerClient(ClockGlobalNet.ID, (client, buf) -> {
               ClockGlobalConfig cfg;
               try {
                  if (buf.readByte() != 0) {
                     return;
                  }

                  cfg = ClockGlobalConfig.readBuf(buf);
               } catch (Throwable var6) {
                  return;
               }

               client.execute(() -> ClockGlobalCache.set(cfg));
            });
         } catch (Throwable var2) {
            LteLog.LOGGER.warn("[lte] clock global channel (client side) unavailable", var2);
         }

         try {
            Net.onDisconnect(() -> ClockGlobalCache.clear());
         } catch (Throwable var1) {
         }

      }
   }

   private static void applySynced(Minecraft client, BlockPos pos, ClockConfig cfg) {
      Level world = client.level;
      if (world != null && pos != null && cfg != null) {
         BlockEntity var5 = world.getBlockEntity(pos);
         if (var5 instanceof ClockBlockEntity) {
            ClockBlockEntity entity = (ClockBlockEntity)var5;
            entity.applyClientConfig(cfg);
            LteLog.LOGGER.debug("[lte] clock cfg synced at {}: face=#{}", pos, Integer.toHexString(cfg.faceColor));
         }

      }
   }

   public static void sendSave(BlockPos pos, ClockConfig cfg) {
      try {
         if (!Net.canSend(ClockNet.ID)) {
            return;
         }

         Net.sendToServer(ClockNet.ID, ClockNet.savePacket(pos, cfg));
      } catch (Throwable var3) {
      }

   }

   public static void sendPreview(BlockPos pos) {
      try {
         if (!Net.canSend(ClockNet.ID)) {
            return;
         }

         Net.sendToServer(ClockNet.ID, ClockNet.previewPacket(pos));
      } catch (Throwable var2) {
      }

   }

   public static void sendGlobalSave(ClockGlobalConfig cfg) {
      try {
         if (!Net.canSend(ClockGlobalNet.ID)) {
            return;
         }

         Net.sendToServer(ClockGlobalNet.ID, ClockGlobalNet.savePacket(cfg));
      } catch (Throwable var2) {
      }

   }

   public static void sendGlobalPreview() {
      try {
         if (!Net.canSend(ClockGlobalNet.ID)) {
            return;
         }

         Net.sendToServer(ClockGlobalNet.ID, ClockGlobalNet.previewPacket());
      } catch (Throwable var1) {
      }

   }

   public static void openGlobalScreen(Minecraft client) {
      if (client != null) {
         ClockGlobalConfig cfg = ClockGlobalCache.get();
         client.setScreen(new ClockConfigScreen((BlockPos)null, cfg));
      }
   }
}
