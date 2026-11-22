package dev.js.mtrheadlight.xiamen.fangsu;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class FangsuPsdSoundsClient {
   private static boolean logged;

   private FangsuPsdSoundsClient() {
   }

   public static void play(BlockPos pos, int action) {
      if (action == 1 || action == 2) {
         if (FangsuPsdSounds.BRT_DOOR != null) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.getSoundManager() != null) {
               client.getSoundManager().play(new SimpleSoundInstance(FangsuPsdSounds.BRT_DOOR, SoundSource.BLOCKS, 1.0F, 1.0F, RandomSource.create(), (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D));
               if (!logged) {
                  logged = true;
                  LteLog.LOGGER.info("[lte] fangsu psd door sound: played {} at {}", action, pos);
               }

            }
         }
      }
   }
}
