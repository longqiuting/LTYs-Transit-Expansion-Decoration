package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class PidsAdSetNet {
   public static final ResourceLocation ID = new ResourceLocation("mtrlte", "pids_ad_set");
   public static final byte KIND_SNAPSHOT = 0;
   public static final byte KIND_SAVE = 1;
   public static final int MIN_PERMISSION = 2;
   private static boolean registered;

   private PidsAdSetNet() {
   }

   public static void register() {
      if (!registered) {
         registered = true;

         try {
            ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, sender) -> {
               PidsAdSet set;
               try {
                  byte kind = buf.readByte();
                  if (kind != 1) {
                     return;
                  }

                  set = PidsAdSet.readBuf(buf);
               } catch (Throwable var8) {
                  return;
               }

               server.execute(() -> applySave(server, player, set));
            });
         } catch (Throwable var2) {
            LteLog.LOGGER.warn("[lte] xiamen pids ad set channel unavailable", var2);
         }

         try {
            ServerPlayConnectionEvents.JOIN.register((ServerPlayConnectionEvents.Join)(handler, sender, server) -> {
               try {
                  send(handler.player, server);
               } catch (Throwable var4) {
               }

            });
         } catch (Throwable var1) {
            LteLog.LOGGER.debug("[lte] could not hook the xiamen pids ad set join sync", var1);
         }

      }
   }

   public static FriendlyByteBuf snapshotPacket(PidsAdSet set) {
      FriendlyByteBuf buf = PacketByteBufs.create();
      buf.writeByte(0);
      (set == null ? new PidsAdSet() : set).writeBuf(buf);
      return buf;
   }

   public static void send(ServerPlayer player, MinecraftServer server) {
      if (player != null && ServerPlayNetworking.canSend(player, ID)) {
         PidsAdSet set = PidsAdSetState.get(server).get();
         ServerPlayNetworking.send(player, ID, snapshotPacket(set));
      }
   }

   public static void broadcast(MinecraftServer server, PidsAdSet set) {
      if (server != null) {
         PidsAdSet copy = set == null ? new PidsAdSet() : set.copy();

         for(ServerPlayer player : PlayerLookup.all(server)) {
            if (ServerPlayNetworking.canSend(player, ID)) {
               ServerPlayNetworking.send(player, ID, snapshotPacket(copy));
            }
         }

      }
   }

   public static FriendlyByteBuf savePacket(PidsAdSet set) {
      FriendlyByteBuf buf = PacketByteBufs.create();
      buf.writeByte(1);
      (set == null ? new PidsAdSet() : set).writeBuf(buf);
      return buf;
   }

   private static void applySave(MinecraftServer server, ServerPlayer player, PidsAdSet set) {
      if (server != null && player != null && set != null) {
         boolean allowed = player.hasPermissions(2) || server != null && !server.l();
         if (!allowed) {
            player.displayClientMessage(Component.translatable("screen.mtrlte.pids_ad_set.denied"), false);
            send(player, server);
            LteLog.LOGGER.info("[lte] xiamen pids ad set save refused for {}", player.getScoreboardName());
         } else {
            PidsAdSetState.get(server).set(set);
            broadcast(server, set);
            LteLog.LOGGER.debug("[lte] xiamen pids ad set saved: enabled={} count={}", set.enabled(), set.size());
         }
      }
   }
}
