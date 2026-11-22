package dev.js.mtrheadlight.xiamen.machine;

import dev.js.mtrheadlight.LteLog;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import dev.js.mtrheadlight.forge.platform.Env;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class FangsuTicketUi {
   private static final String HOOKS_CLASS = "com.fangsu.client.ClientHooks";
   private static final String OPEN_METHOD = "openTicketMachineScreen";
   private static final String MOD_ID = "fangsu";
   private static volatile BiConsumer<Component, BlockPos> opener;
   private static boolean attempted;

   private FangsuTicketUi() {
   }

   public static boolean loaded() {
      try {
         return Env.isModLoaded("fangsu");
      } catch (Throwable var1) {
         return false;
      }
   }

   public static void installClientBridge() {
      if (!attempted) {
         attempted = true;
         if (!loaded()) {
            LteLog.LOGGER.info("[lte] ticket machine: fangsu not installed, vanilla MTR UI only");
         } else {
            try {
               Class<?> hooks = Class.forName("com.fangsu.client.ClientHooks");

               for(Method candidate : hooks.getMethods()) {
                  if ("openTicketMachineScreen".equals(candidate.getName())) {
                     Class<?>[] params = candidate.getParameterTypes();
                     if (params.length == 2 && params[0].isAssignableFrom(Component.class) && params[1].isAssignableFrom(BlockPos.class)) {
                        candidate.setAccessible(true);
                        opener = (title, pos) -> {
                           try {
                              candidate.invoke((Object)null, title, pos);
                           } catch (ReflectiveOperationException var4) {
                              throw new IllegalStateException("fangsu openTicketMachineScreen failed", var4);
                           }
                        };
                        LteLog.LOGGER.info("[lte] ticket machine: fangsu UI bridge ready ({})", candidate);
                        return;
                     }
                  }
               }

               LteLog.LOGGER.warn("[lte] ticket machine: {}#{} not found, falling back to the vanilla UI", "com.fangsu.client.ClientHooks", "openTicketMachineScreen");
            } catch (Throwable var6) {
               LteLog.LOGGER.warn("[lte] ticket machine: could not reach the fangsu UI", var6);
            }

         }
      }
   }

   public static boolean open(Component title, BlockPos pos) {
      installClientBridge();
      BiConsumer<Component, BlockPos> hook = opener;
      if (hook == null) {
         return false;
      } else {
         try {
            hook.accept(title, pos);
            return true;
         } catch (Throwable var4) {
            opener = null;
            LteLog.LOGGER.warn("[lte] ticket machine: the fangsu UI threw, disabling the bridge", var4);
            return false;
         }
      }
   }
}
