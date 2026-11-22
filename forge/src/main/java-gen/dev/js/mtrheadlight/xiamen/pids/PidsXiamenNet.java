package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import mtr.block.IBlock;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PidsXiamenNet {
   public static final ResourceLocation ID = new ResourceLocation("mtrlte", "pids_xiamen");
   private static final byte KIND_OPEN = 0;
   private static final byte KIND_AD = 1;
   private static final byte KIND_CATEGORIES = 2;
   private static boolean registeredServer;

   private PidsXiamenNet() {
   }

   public static void registerServer() {
      if (!registeredServer) {
         registeredServer = true;

         try {
            ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, sender) -> {
               try {
                  byte kind = buf.readByte();
                  if (kind != 1 && kind != 2) {
                     return;
                  }

                  BlockPos pos = buf.readBlockPos();
                  if (kind == 1) {
                     int index = buf.readVarInt();
                     server.execute(() -> applyAdIndex(player, pos, index));
                     return;
                  }

                  int count = buf.readVarInt();
                  long[] ids = new long[Math.max(0, Math.min(count, 256))];

                  for(int i = 0; i < ids.length; ++i) {
                     ids[i] = buf.readLong();
                  }

                  server.execute(() -> applyCategories(player, pos, ids));
               } catch (Throwable var10) {
                  LteLog.LOGGER.warn("[lte] xiamen pids: bad config packet", var10);
               }

            });
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] xiamen pids: could not register the server receiver", var1);
         }

      }
   }

   private static void applyAdIndex(ServerPlayer player, BlockPos pos, int index) {
      PidsXiamenBlockEntity pids = reachable(player, pos);
      if (pids != null) {
         try {
            pids.setAdvertisementIndex(index);
            pids.syncData();
         } catch (Throwable var5) {
            LteLog.LOGGER.debug("[lte] xiamen pids: could not apply the advertisement index", var5);
         }

      }
   }

   private static void applyCategories(ServerPlayer player, BlockPos pos, long[] ids) {
      PidsXiamenBlockEntity pids = reachable(player, pos);
      if (pids != null) {
         try {
            pids.setAdCategories(ids);
            pids.syncData();
         } catch (Throwable var5) {
            LteLog.LOGGER.debug("[lte] xiamen pids: could not apply the advertisement categories", var5);
         }

      }
   }

   private static PidsXiamenBlockEntity reachable(ServerPlayer player, BlockPos pos) {
      try {
         if (player != null && !player.isRemoved()) {
            Level world = player.serverLevel();
            if (world == null) {
               return null;
            } else {
               BlockPos dataPos = PidsXiamenBlock.dataPos(world, pos);
               BlockEntity var5 = world.getBlockEntity(dataPos);
               if (var5 instanceof PidsXiamenBlockEntity) {
                  PidsXiamenBlockEntity pids = (PidsXiamenBlockEntity)var5;
                  return player.blockPosition().distSqr(dataPos) > 64.0D ? null : pids;
               } else {
                  return null;
               }
            }
         } else {
            return null;
         }
      } catch (Throwable var6) {
         return null;
      }
   }

   public static void openScreen(ServerPlayer player, Level world, BlockPos pos) {
      if (player != null && world != null) {
         try {
            BlockPos partnerPos = partner(world, pos);

            for(BlockPos each : new BlockPos[]{pos, partnerPos}) {
               if (each != null) {
                  BlockEntity var9 = world.getBlockEntity(each);
                  if (var9 instanceof PidsXiamenBlockEntity) {
                     PidsXiamenBlockEntity pids = (PidsXiamenBlockEntity)var9;
                     pids.syncData();
                  }
               }
            }

            BlockPos dataPos = PidsXiamenBlock.dataPos(world, pos);
            send(player, dataPos, partnerPos == null ? dataPos : partnerPos);
         } catch (Throwable var10) {
            LteLog.LOGGER.warn("[lte] xiamen pids: could not open the config screen", var10);
         }

      }
   }

   public static void openScreenAt(ServerPlayer player, Level world, BlockPos dataPos, BlockPos partnerPos) {
      if (player != null && world != null && dataPos != null) {
         try {
            for(BlockPos each : new BlockPos[]{dataPos, partnerPos}) {
               if (each != null) {
                  BlockEntity var9 = world.getBlockEntity(each);
                  if (var9 instanceof PidsXiamenBlockEntity) {
                     PidsXiamenBlockEntity pids = (PidsXiamenBlockEntity)var9;
                     pids.syncData();
                  }
               }
            }

            send(player, dataPos, partnerPos == null ? dataPos : partnerPos);
         } catch (Throwable var10) {
            LteLog.LOGGER.warn("[lte] xiamen pids: could not open the config screen", var10);
         }

      }
   }

   private static void send(ServerPlayer player, BlockPos dataPos, BlockPos partnerPos) {
      if (ServerPlayNetworking.canSend(player, ID)) {
         FriendlyByteBuf buf = PacketByteBufs.create();
         buf.writeByte(0);
         buf.writeBlockPos(dataPos);
         buf.writeBlockPos(partnerPos == null ? dataPos : partnerPos);
         ServerPlayNetworking.send(player, ID, buf);
      }
   }

   private static BlockPos partner(Level world, BlockPos pos) {
      Direction facing = (Direction)IBlock.getStatePropertySafe(world, pos, HorizontalDirectionalBlock.FACING);
      if (facing == null) {
         return pos;
      } else {
         BlockPos other = pos.relative(facing);
         return world.getBlockState(other).getBlock() instanceof PidsXiamenBlock ? other : pos;
      }
   }

   static PidsXiamenNet.OpenPacket readOpen(FriendlyByteBuf buf) {
      return new PidsXiamenNet.OpenPacket(buf.readBlockPos(), buf.readBlockPos());
   }

   public static FriendlyByteBuf ad(BlockPos pos, int index) {
      FriendlyByteBuf buf = PacketByteBufs.create();
      buf.writeByte(1);
      buf.writeBlockPos(pos);
      buf.writeVarInt(Math.max(0, index));
      return buf;
   }

   public static FriendlyByteBuf categories(BlockPos pos, long[] ids) {
      FriendlyByteBuf buf = PacketByteBufs.create();
      buf.writeByte(2);
      buf.writeBlockPos(pos);
      int count = ids == null ? 0 : ids.length;
      buf.writeVarInt(count);

      for(int i = 0; i < count; ++i) {
         buf.writeLong(ids[i]);
      }

      return buf;
   }

   static byte readKind(FriendlyByteBuf buf) {
      return buf.readByte();
   }

   static boolean isOpen(byte kind) {
      return kind == 0;
   }

   static record OpenPacket(BlockPos dataPos, BlockPos partnerPos) {
   }
}
