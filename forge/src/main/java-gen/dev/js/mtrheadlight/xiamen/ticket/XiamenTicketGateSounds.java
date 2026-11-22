package dev.js.mtrheadlight.xiamen.ticket;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class XiamenTicketGateSounds {
   public static final ResourceLocation CRT_TICKET_ID = new ResourceLocation("mtrlte", "crt_ticket");
   public static final ResourceLocation CRT_TICKET_ERROR_ID = new ResourceLocation("mtrlte", "crt_ticket_error");
   public static SoundEvent CRT_TICKET;
   public static SoundEvent CRT_TICKET_ERROR;
   private static boolean registered;

   private XiamenTicketGateSounds() {
   }

   public static void register() {
      if (!registered) {
         registered = true;
         CRT_TICKET = (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, CRT_TICKET_ID, SoundEvent.createVariableRangeEvent(CRT_TICKET_ID));
         CRT_TICKET_ERROR = (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, CRT_TICKET_ERROR_ID, SoundEvent.createVariableRangeEvent(CRT_TICKET_ERROR_ID));
         LteLog.LOGGER.info("[lte] ticket gate sounds: {} / {}", CRT_TICKET_ID, CRT_TICKET_ERROR_ID);
      }
   }
}
