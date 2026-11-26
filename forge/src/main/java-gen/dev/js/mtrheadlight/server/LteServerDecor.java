package dev.js.mtrheadlight.server;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.clock.ClockObjects;
import dev.js.mtrheadlight.decor.DecorBlocks;
import dev.js.mtrheadlight.decor.DecorConfig;
import dev.js.mtrheadlight.decor.LtedCoreItems;
import dev.js.mtrheadlight.decor.RccPlatforms;
import dev.js.mtrheadlight.logo.LogoObjects;
import dev.js.mtrheadlight.railing.RailingGlassBlocks;
import dev.js.mtrheadlight.railing.RailingWallBlocks;
import dev.js.mtrheadlight.security.SecurityObjects;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import dev.js.mtrheadlight.xiamen.charger.ChargerObjects;
import dev.js.mtrheadlight.xiamen.dep.DepOnRoofObjects;
import dev.js.mtrheadlight.xiamen.dep.DepOnWallObjects;
import dev.js.mtrheadlight.xiamen.dep.DepPoleObjects;
import dev.js.mtrheadlight.xiamen.drinks.DrinksMachineObjects;
import dev.js.mtrheadlight.xiamen.eyecandy.EyecandyBlocks;
import dev.js.mtrheadlight.xiamen.fangsu.FangsuPsdBlocks;
import dev.js.mtrheadlight.xiamen.machine.XiamenMachines;
import dev.js.mtrheadlight.xiamen.pids.PidsAdSetNet;
import dev.js.mtrheadlight.xiamen.pids.PidsXiamenNet;
import dev.js.mtrheadlight.xiamen.pids.PidsXiamenObjects;
import dev.js.mtrheadlight.xiamen.pids.PsdLcdObjects;
import dev.js.mtrheadlight.xiamen.sign.StationSignObjects;
import dev.js.mtrheadlight.xiamen.sign.WallSignObjects;
import dev.js.mtrheadlight.xiamen.ticket.XiamenTicketGates;
import dev.js.mtrheadlight.forge.platform.CommonInit;

public final class LteServerDecor implements CommonInit {
   public void onInitialize() {
      try {
         DecorConfig.load();
      } catch (Throwable var2) {
         LteLog.LOGGER.warn("[lte] decor config unavailable - using defaults", var2);
      }

      blockModule("xiamen objects", XiamenObjects::register);
      blockModule("regional tabs", dev.js.mtrheadlight.region.RegionalTabs::register);
      blockModule("regional logo blocks", dev.js.mtrheadlight.region.RegionalLogos::register);
      blockModule("logo blocks", LogoObjects::register);
      module("lted dashboard item", LtedCoreItems::register);
      blockModule("wall sign blocks", WallSignObjects::register);
      blockModule("railing wall", RailingWallBlocks::register);
      blockModule("railing glass", RailingGlassBlocks::register);
      blockModule("decor blocks", DecorBlocks::register);
      blockModule("rcc platforms", RccPlatforms::register);
      blockModule("xiamen pids blocks", PidsXiamenObjects::register);
      blockModule("psd lcd block", PsdLcdObjects::register);
      module("xiamen pids channel", PidsXiamenNet::registerServer);
      module("xiamen pids ad set channel", PidsAdSetNet::register);
      blockModule("dep on wall block", DepOnWallObjects::register);
      blockModule("dep on roof block", DepOnRoofObjects::register);
      blockModule("dep pole block", DepPoleObjects::register);
      blockModule("station sign blocks", StationSignObjects::register);
      module("core tab order", LogoObjects::pinCoreTabOrder);
      module("regional tab order", dev.js.mtrheadlight.region.RegionalLogos::pinRegionalTabOrder);
      module("lted dashboard tab", LtedCoreItems::pinToolsTabOrder);
      blockModule("xiamen ticket / balance machines", XiamenMachines::register);
      blockModule("mtr hk ticket machine", dev.js.mtrheadlight.region.RegionalMachines::register);
      blockModule("power bank charger", ChargerObjects::register);
      blockModule("drinks machine", DrinksMachineObjects::register);
      blockModule("security blocks", SecurityObjects::register);
      blockModule("clock blocks", ClockObjects::register);
      blockModule("xiamen ticket gates", XiamenTicketGates::register);
      blockModule("xiamen eyecandy decorations", EyecandyBlocks::register);
      blockModule("fangsu psd doors", FangsuPsdBlocks::register);
   }

   static void module(String name, Runnable body) {
      try {
         body.run();
      } catch (Throwable var3) {
         LteLog.LOGGER.error("[lte] {} registration aborted (the rest of the mod keeps working)", name, var3);
      }

   }

   static void blockModule(String name, Runnable body) {
      int blocksBefore = RegistrySelfCheck.countBlocks();
      int oursBefore = RegistrySelfCheck.countBlocks("mtrlte");
      int pendingBefore = RegistrySelfCheck.pendingCount();

      try {
         body.run();
      } catch (Throwable var10) {
         int registered = RegistrySelfCheck.countBlocks("mtrlte") - oursBefore;
         int leaked = Math.max(0, RegistrySelfCheck.pendingCount() - pendingBefore);
         LteLog.LOGGER.error("[lte] {} registration aborted: {} block(s) registered, {} block(s) created but never registered (blocks total {} -> {}); the rest of the mod keeps working", new Object[]{name, registered, leaked, blocksBefore, RegistrySelfCheck.countBlocks(), var10});

         try {
            RegistrySelfCheck.purgePending(name);
         } catch (Throwable var9) {
         }
      }

   }
}
