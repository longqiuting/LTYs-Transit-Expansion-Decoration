package dev.js.mtrheadlight.security;

import dev.js.mtrheadlight.LteLog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import dev.js.mtrheadlight.forge.platform.Net;
import dev.js.mtrheadlight.forge.platform.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class LiquidScannerNet {
   public static final ResourceLocation ID = new ResourceLocation("mtrlte", "liquid_scanner_cfg");
   public static final byte KIND_OPEN = 0;
   private static final byte KIND_SAVE = 1;
   private static final int MAX_DISTANCE = 64;
   private static final int MAX_ENTRIES = 512;
   private static boolean registered;

   private LiquidScannerNet() {
   }

   public static void register() {
      if (!registered) {
         registered = true;

         try {
            Net.registerServer(ID, (server, player, buf) -> {
               List<ResourceLocation> ids = new ArrayList();

               BlockPos pos;
               try {
                  if (buf.readByte() != 1) {
                     return;
                  }

                  pos = buf.readBlockPos();
                  int n = Math.min(buf.readVarInt(), 512);

                  for(int i = 0; i < n; ++i) {
                     ResourceLocation id = ResourceLocation.tryParse(buf.readUtf());
                     if (id != null) {
                        ids.add(id);
                     }
                  }
               } catch (Throwable var10) {
                  return;
               }

               server.execute(() -> applySave(player, pos, ids));
            });
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] liquid scanner config channel unavailable", var1);
         }

      }
   }

   public static void sendOpen(ServerPlayer player, BlockPos pos, LiquidScannerBlockEntity entity) {
      if (player != null && Net.canSend(player, ID)) {
         Set<ResourceLocation> ids = (Set<ResourceLocation>)(entity == null ? new LinkedHashSet() : entity.confiscate());
         FriendlyByteBuf buf = Net.buf();
         buf.writeByte(0);
         buf.writeBlockPos(pos);
         buf.writeBoolean(entity != null && entity.isConfigured());
         buf.writeVarInt(ids.size());

         for(ResourceLocation id : ids) {
            buf.writeUtf(id.toString());
         }

         Net.send(player, ID, buf);
      }
   }

   private static void applySave(ServerPlayer player, BlockPos pos, List<ResourceLocation> ids) {
      if (player != null && pos != null) {
         Level world = player.level();
         if (world != null && world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            if (!(player.blockPosition().distSqr(pos) > 4096.0D)) {
               if (world.getBlockState(pos).getBlock() instanceof LiquidScannerBlock) {
                  BlockEntity dropped = world.getBlockEntity(pos);
                  LiquidScannerBlockEntity var10000;
                  if (dropped instanceof LiquidScannerBlockEntity) {
                     LiquidScannerBlockEntity be = (LiquidScannerBlockEntity)dropped;
                     var10000 = be;
                  } else {
                     var10000 = null;
                  }

                  LiquidScannerBlockEntity entity = var10000;
                  if (entity == null) {
                     entity = new LiquidScannerBlockEntity(pos, world.getBlockState(pos));
                     world.setBlockEntity(entity);
                  }

                  Set<ResourceLocation> valid = new LinkedHashSet();

                  for(ResourceLocation id : ids) {
                     if (BuiltInRegistries.POTION.containsKey(id) || LiquidScannerBlock.EXTRA_LIQUIDS.contains(id)) {
                        valid.add(id);
                     }
                  }

                  entity.setConfiscate(valid);
                  int droppedLted = ids.size() - valid.size();
                  if (droppedLted > 0) {
                     player.displayClientMessage(Component.translatable("message.mtrlte.liquid_scanner.savedPartial", new Object[]{valid.size(), droppedLted}), false);
                  } else {
                     player.displayClientMessage(Component.translatable("message.mtrlte.liquid_scanner.saved", new Object[]{valid.size()}), true);
                  }

               }
            }
         }
      }
   }

   public static FriendlyByteBuf savePacket(BlockPos pos, Collection<ResourceLocation> ids) {
      FriendlyByteBuf buf = Net.buf();
      buf.writeByte(1);
      buf.writeBlockPos(pos);
      buf.writeVarInt(ids.size());

      for(ResourceLocation id : ids) {
         buf.writeUtf(id.toString());
      }

      return buf;
   }
}
