package dev.js.mtrheadlight;

import dev.js.mtrheadlight.decor.LtedDashboardScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fabric 的 ModMenu 集成在 Forge 侧的等价物（M4b-4）。
 *
 * <p>Fabric 侧由 ModMenu 扫描 {@code ModMenuApi} 实现类；Forge 侧改由
 * {@code ClientBootstrap} 注册 {@code ConfigScreenHandler.ConfigScreenFactory}
 * 扩展点 —— 效果相同：模组列表里的「配置」按钮直接打开 LTED 仪表板。
 */
public final class ModMenuIntegration {

   private ModMenuIntegration() {
   }

   public static Screen configScreen(Screen parent) {
      return new LtedDashboardScreen(parent);
   }
}
