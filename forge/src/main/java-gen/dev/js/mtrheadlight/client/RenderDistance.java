package dev.js.mtrheadlight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class RenderDistance {
   private static final int MIN = 64;
   private static final int MAX = 512;

   private RenderDistance() {
   }

   public static int blocks() {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.options != null) {
         int chunks = client.options.renderDistance().get();
         return Math.max(64, Math.min(512, chunks * 16 + 32));
      } else {
         return 64;
      }
   }

   public static boolean inRange(BlockEntity entity, Vec3 cameraPos) {
      double d = (double)blocks();
      Vec3 center = Vec3.atCenterOf(entity.getBlockPos());
      return center.distanceToSqr(cameraPos) < d * d;
   }
}
