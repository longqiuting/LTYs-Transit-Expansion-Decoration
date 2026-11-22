package dev.js.mtrheadlight.decor;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * LTED 自己的物品 + 页签位置（{@code main} 入口的 B 侧，两端都注册）。
 *
 * <p>目前只有一件：{@code mtrlte:lted_dashboard}（右键开 LTED 仪表板）。物品本身<b>两端都注册</b>——
 * 只在客户端注册的物品在服务器上是「未知物品」，联机时会掉。
 *
 * <p>页签位置：进「厦门地铁物件」页签（{@code mtrlte:core}，{@link dev.js.mtrheadlight.xiamen.XiamenObjects#CORE_TAB_ID}）
 * 的<b>前部</b> —— 紧跟在两个 logo（厦门 / 角美）之后，其余条目的相对顺序不变。
 * {@link #pinCoreTabOrder()} 必须在 {@code LogoObjects.pinCoreTabOrder()} 之后注册（页签条目的回调按
 * 注册顺序执行，早于那次重排就会被推到后面）。
 */
public final class LtedCoreItems {

    /** 物品 id：{@code mtrlte:lted_dashboard}（右键开 LTED 仪表板，仅客户端）。 */
    public static final Identifier LTED_DASHBOARD_ID = new Identifier("mtrlte", "lted_dashboard");

    /** 页签里「两个 logo」后面那一格；用 id 找，避免依赖 LogoObjects 的字段初始化顺序。 */
    private static final Identifier JIAOMEI_LOGO_ID = new Identifier("mtrlte", "jiaomei_logo");

    private static LtedDashboardItem dashboard;
    private static boolean registered;

    private LtedCoreItems() {
    }

    public static LtedDashboardItem dashboard() {
        return dashboard;
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        dashboard = Registry.register(Registries.ITEM, LTED_DASHBOARD_ID,
                new LtedDashboardItem(new Item.Settings().maxCount(1)));
        LteLog.LOGGER.info("[lte] lted dashboard item: {}", LTED_DASHBOARD_ID);
    }

    /**
     * 把仪表板放到「厦门地铁物件」页签前部（两个 logo 之后）。
     *
     * <p>做法与 {@code LogoObjects#pinCoreTabOrder()} 一致：拿页签正在拼的可变列表
     * （{@code entries.getDisplayStacks()}）→ 先摘掉自己（重进世界时不重复）→ 找到角美 logo 的位置 →
     * 插到它后面一格。找不到 logo（理论上不会）就放最前面。
     */
    public static void pinCoreTabOrder() {
        if (dashboard == null) {
            return;
        }
        try {
            ItemGroupEvents.modifyEntriesEvent(
                            RegistryKey.of(RegistryKeys.ITEM_GROUP,
                                    dev.js.mtrheadlight.xiamen.XiamenObjects.CORE_TAB_ID))
                    .register(entries -> {
                        final List<ItemStack> stacks = entries.getDisplayStacks();
                        stacks.removeIf(s -> s.isOf(dashboard));
                        int index = 0;
                        final Item logo = Registries.ITEM.get(JIAOMEI_LOGO_ID);
                        if (logo != null) {
                            for (int i = 0; i < stacks.size(); i++) {
                                if (stacks.get(i).isOf(logo)) {
                                    index = i + 1;
                                }
                            }
                        }
                        stacks.add(Math.min(index, stacks.size()), new ItemStack(dashboard));
                    });
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] lted dashboard: could not pin the core tab entry", t);
        }
    }
}
