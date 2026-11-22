package dev.js.mtrheadlight.xiamen.fangsu;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class FangsuPsdSounds {
   public static final ResourceLocation BRT_DOOR_ID = new ResourceLocation("mtrlte", "brt_door");
   public static SoundEvent BRT_DOOR;
   private static boolean registered;

   private FangsuPsdSounds() {
   }

   public static void register() {
      if (!registered) {
         registered = true;
         BRT_DOOR = (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, BRT_DOOR_ID, SoundEvent.createVariableRangeEvent(BRT_DOOR_ID));
         LteLog.LOGGER.info("[lte] fangsu psd door sound: {}", BRT_DOOR_ID);
      }
   }
}
