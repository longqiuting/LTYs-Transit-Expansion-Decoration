package dev.js.mtrheadlight;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.js.mtrheadlight.decor.LtedDashboardScreen;

/**
 * 模组列表里的「配置」按钮 —— 打开 LTED 仪表板（{@link LtedDashboardScreen}），
 * 与 {@code mtrlte:lted_dashboard} 物品右键、{@code /mtr_lte} 同一条路径。
 *
 * <p>只挂在客户端的 {@code modmenu} 入口上；Mod Menu 没装时这个类不会被加载。
 */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new LtedDashboardScreen(parent);
    }
}
