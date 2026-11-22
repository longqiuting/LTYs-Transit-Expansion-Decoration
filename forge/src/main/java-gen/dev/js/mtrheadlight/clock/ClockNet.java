package dev.js.mtrheadlight.clock;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.Net;
import dev.js.mtrheadlight.forge.platform.Net;
import dev.js.mtrheadlight.forge.platform.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ClockNet {
   public static final ResourceLocation ID = new ResourceLocation("mtrlte", "clock_cfg");
   public static final byte KIND_OPEN = 0;
   public static final byte KIND_SAVE = 1;
   public static final byte KIND_PREVIEW = 2;
   public static final byte KIND_SYNC = 3;
   private static final int MAX_DISTANCE = 64;
   private static boolean registered;

   private ClockNet() {
   }

   public static void register() {
      if (!registered) {
         registered = true;

         try {
            Net.registerServer(ID, (server, player, buf) -> {
               byte kind;
               BlockPos pos;
               ClockConfig cfg;
               try {
                  kind = buf.readByte();
                  pos = buf.readBlockPos();
                  cfg = kind == 1 ? ClockConfig.readBuf(buf) : null;
               } catch (Throwable var9) {
                  return;
               }

               server.execute(() -> {
                  if (kind == 1) {
                     applySave(player, pos, cfg);
                  } else if (kind == 2) {
                     applyPreview(player, pos);
                  }

               });
            });
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] clock config channel unavailable", var1);
         }

      }
   }

   public static void sendOpen(ServerPlayer player, BlockPos pos, ClockBlockEntity entity) {
      if (player != null && Net.canSend(player, ID)) {
         ClockConfig cfg = entity == null ? new ClockConfig() : entity.config();
         FriendlyByteBuf buf = Net.buf();
         buf.writeByte(0);
         buf.writeBlockPos(pos);
         cfg.writeBuf(buf);
         Net.send(player, ID, buf);
      }
   }

   public static void sendConfig(Level world, BlockPos pos, ClockConfig cfg) {
      if (world instanceof ServerLevel serverWorld) {
         if (pos != null && cfg != null) {
            for(ServerPlayer player : Net.tracking(serverWorld, pos)) {
               if (Net.canSend(player, ID)) {
                  Net.send(player, ID, syncPacket(pos, cfg));
               }
            }

            return;
         }
      }

   }

   public static FriendlyByteBuf syncPacket(BlockPos pos, ClockConfig cfg) {
      FriendlyByteBuf buf = Net.buf();
      buf.writeByte(3);
      buf.writeBlockPos(pos);
      cfg.writeBuf(buf);
      return buf;
   }

   public static FriendlyByteBuf savePacket(BlockPos pos, ClockConfig cfg) {
      FriendlyByteBuf buf = Net.buf();
      buf.writeByte(1);
      buf.writeBlockPos(pos);
      cfg.writeBuf(buf);
      return buf;
   }

   public static FriendlyByteBuf previewPacket(BlockPos pos) {
      FriendlyByteBuf buf = Net.buf();
      buf.writeByte(2);
      buf.writeBlockPos(pos);
      return buf;
   }

   private static void applySave(ServerPlayer player, BlockPos pos, ClockConfig cfg) {
      ClockBlockEntity entity = resolve(player, pos);
      if (entity != null && cfg != null) {
         cfg.clamp();
         entity.setConfig(cfg);
         LteLog.LOGGER.debug("[lte] clock cfg saved at {}: face=#{} hourLen={} tick={}", new Object[]{pos, Integer.toHexString(cfg.faceColor), cfg.hourLength, cfg.tickStyle.getSerializedName()});
      }
   }

   private static void applyPreview(ServerPlayer player, BlockPos pos) {
      ClockBlockEntity entity = resolve(player, pos);
      if (entity != null) {
         entity.playPreview();
      }
   }

   private static ClockBlockEntity resolve(ServerPlayer player, BlockPos pos) {
      if (player != null && pos != null) {
         Level world = player.level();
         if (world != null && world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            if (player.blockPosition().distSqr(pos) > 4096.0D) {
               return null;
            } else if (!(world.getBlockState(pos).getBlock() instanceof ClockBlock)) {
               return null;
            } else {
               BlockEntity var4 = world.getBlockEntity(pos);
               if (var4 instanceof ClockBlockEntity) {
                  ClockBlockEntity be = (ClockBlockEntity)var4;
                  return be;
               } else {
                  ClockBlockEntity created = new ClockBlockEntity(pos, world.getBlockState(pos));
                  world.setBlockEntity(created);
                  return created;
               }
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}
