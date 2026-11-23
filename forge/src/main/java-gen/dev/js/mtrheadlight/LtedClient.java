package dev.js.mtrheadlight;

import dev.js.mtrheadlight.decor.DecorClient;
import dev.js.mtrheadlight.decor.DecorConfig;
import dev.js.mtrheadlight.logo.LogoClient;
import dev.js.mtrheadlight.railing.RailingGlassClient;
import dev.js.mtrheadlight.security.SecurityClient;
import dev.js.mtrheadlight.xiamen.XiamenClient;
import dev.js.mtrheadlight.xiamen.charger.ChargerClient;
import dev.js.mtrheadlight.xiamen.dep.DepOnWallClient;
import dev.js.mtrheadlight.xiamen.drinks.DrinksMachineClient;
import dev.js.mtrheadlight.xiamen.fangsu.FangsuPsdClient;
import dev.js.mtrheadlight.xiamen.machine.XiamenMachinesClient;
import dev.js.mtrheadlight.xiamen.pids.PidsXiamenClient;
import dev.js.mtrheadlight.xiamen.pids.PsdLcdClient;
import dev.js.mtrheadlight.forge.platform.BuiltinPacks;
import dev.js.mtrheadlight.xiamen.sign.WallSignClient;
import dev.js.mtrheadlight.forge.platform.ClientInit;

public final class LtedClient implements ClientInit {
   public void onInitializeClient() {
      DecorConfig.load();
      BuiltinPacks.register();
      Commands.register();
      (new XiamenClient()).onInitializeClient();
      (new WallSignClient()).onInitializeClient();
      (new RailingGlassClient()).onInitializeClient();
      (new DecorClient()).onInitializeClient();
      (new PidsXiamenClient()).onInitializeClient();
      (new PsdLcdClient()).onInitializeClient();
      (new DepOnWallClient()).onInitializeClient();
      (new LogoClient()).onInitializeClient();
      (new XiamenMachinesClient()).onInitializeClient();
      (new ChargerClient()).onInitializeClient();
      (new DrinksMachineClient()).onInitializeClient();
      (new SecurityClient()).onInitializeClient();
      (new FangsuPsdClient()).onInitializeClient();
   }
}
