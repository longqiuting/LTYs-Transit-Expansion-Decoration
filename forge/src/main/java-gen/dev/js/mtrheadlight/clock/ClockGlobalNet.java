package dev.js.mtrheadlight.clock;

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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public final class ClockGlobalNet {
   public static final ResourceLocation ID = new ResourceLocation("mtrlte", "clock_global");
   public static final byte KIND_SNAPSHOT = 0;
   public static final byte KIND_SAVE = 1;
   public static final byte KIND_PREVIEW = 2;
   public static final int MIN_PERMISSION = 2;
   private static boolean registered;

   private ClockGlobalNet() {
   }

   public static void register() {
      if (!registered) {
         registered = true;

         try {
            ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, sender) -> {
               byte kind;
               ClockGlobalConfig cfg;
               try {
                  kind = buf.readByte();
                  cfg = kind == 1 ? ClockGlobalConfig.readBuf(buf) : null;
               } catch (Throwable var8) {
                  return;
               }

               server.execute(() -> {
                  if (kind == 1) {
                     applySave(server, player, cfg);
                  } else if (kind == 2) {
                     applyPreview(server, player);
                  }

               });
            });
         } catch (Throwable var2) {
            LteLog.LOGGER.warn("[lte] clock global channel unavailable", var2);
         }

         try {
            ServerPlayConnectionEvents.JOIN.register((ServerPlayConnectionEvents.Join)(handler, sender, server) -> {
               try {
                  send(handler.player, server);
               } catch (Throwable var4) {
               }

            });
         } catch (Throwable var1) {
            LteLog.LOGGER.debug("[lte] could not hook the clock global join sync", var1);
         }

      }
   }

   public static FriendlyByteBuf snapshotPacket(ClockGlobalConfig cfg) {
      FriendlyByteBuf buf = PacketByteBufs.create();
      buf.writeByte(0);
      cfg.writeBuf(buf);
      return buf;
   }

   public static void send(ServerPlayer player, MinecraftServer server) {
      if (player != null && ServerPlayNetworking.canSend(player, ID)) {
         ServerPlayNetworking.send(player, ID, snapshotPacket(ClockGlobalState.get(server).get()));
      }
   }

   public static void broadcast(MinecraftServer server, ClockGlobalConfig cfg) {
      if (server != null && cfg != null) {
         ClockGlobalConfig copy = cfg.copy();

         for(ServerPlayer player : PlayerLookup.all(server)) {
            if (ServerPlayNetworking.canSend(player, ID)) {
               ServerPlayNetworking.send(player, ID, snapshotPacket(copy));
            }
         }

      }
   }

   public static FriendlyByteBuf savePacket(ClockGlobalConfig cfg) {
      FriendlyByteBuf buf = PacketByteBufs.create();
      buf.writeByte(1);
      cfg.writeBuf(buf);
      return buf;
   }

   public static FriendlyByteBuf previewPacket() {
      FriendlyByteBuf buf = PacketByteBufs.create();
      buf.writeByte(2);
      return buf;
   }

   private static void applySave(MinecraftServer server, ServerPlayer player, ClockGlobalConfig cfg) {
      if (server != null && player != null && cfg != null) {
         if (!player.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("screen.mtrlte.clock.global.denied"), false);
            LteLog.LOGGER.info("[lte] clock global save refused for {}", player.getScoreboardName());
         } else {
            cfg.clamp();
            ClockGlobalState.get(server).set(cfg);
            broadcast(server, cfg);
            LteLog.LOGGER.debug("[lte] clock global config saved: enabled={} face=#{}", cfg.enabled, Integer.toHexString(cfg.config.faceColor));
         }
      }
   }

   private static void applyPreview(MinecraftServer server, ServerPlayer player) {
      if (server != null && player != null) {
         ClockGlobalConfig cfg = ClockGlobalState.get(server).get();
         player.level().playSound((Player)null, player.blockPosition(), ClockBlockEntity.soundOf(cfg.config), SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }
}
