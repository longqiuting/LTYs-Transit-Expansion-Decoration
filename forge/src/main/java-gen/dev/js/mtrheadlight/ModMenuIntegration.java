package dev.js.mtrheadlight;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.js.mtrheadlight.decor.LtedDashboardScreen;

public final class ModMenuIntegration implements ModMenuApi {
   public ConfigScreenFactory<?> getModConfigScreenFactory() {
      return (parent) -> new LtedDashboardScreen(parent);
   }
}
