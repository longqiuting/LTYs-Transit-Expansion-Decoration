package dev.js.mtrheadlight.decor;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.TabIcons;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * LTED 自己的物品与创造页签（{@code main} 入口，两端都注册）。
 *
 * <p>目前只有一件：{@code mtrlte:lted_dashboard}（右键开 LTED 仪表板）。物品本身<b>两端都注册</b>——
 * 只在客户端注册的物品在服务器上是「未知物品」，联机时会掉。
 *
 * <h2>「LTE 工具」页签只剩这一件</h2>
 * 拆分前这一页是「LTE 工具」：LTE logo → LTE 贴墙标识 → 仪表板 → 光束编辑器 → 三个连接器。
 * 优化侧整体搬去 LTE 之后，光束编辑器 / 连接器 / 原来的优化仪表板（{@code lte_dashboard}）都不在了，
 * logo 与贴墙标识按用户要求挪到「厦门地铁物件」页签的<b>开头两位</b>
 * （见 {@code LogoObjects#CORE_TAB_FRONT}），于是这一页只剩 LTED 自己的仪表板。
 *
 * <p>页签 id 沿用 {@code mtrlte:tools}（图标 {@link TabIcons#tools()}，贴图
 * {@code assets/mtrlte/textures/item/tab_tools.png}），这样升级上来的存档里页签顺序不会变。
 */
public final class LtedCoreItems {

    /** 物品 id：{@code mtrlte:lted_dashboard}（右键开 LTED 仪表板，仅客户端）。 */
    public static final Identifier LTED_DASHBOARD_ID = new Identifier("mtrlte", "lted_dashboard");

    /** 页签 id：{@code mtrlte:tools}（「LTE 工具」，现在只放仪表板）。 */
    public static final Identifier TOOLS_TAB_ID = new Identifier("mtrlte", "tools");

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

        // 页签图标：不是本页签里的物品，而是一个只画图标的物品（见 TabIcons 的类注释）。
        TabIcons.register();
        Registry.register(Registries.ITEM_GROUP, TOOLS_TAB_ID,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.mtrlte.tools"))
                        .icon(() -> new ItemStack(TabIcons.tools()))
                        .build());
        ItemGroupEvents.modifyEntriesEvent(RegistryKey.of(RegistryKeys.ITEM_GROUP, TOOLS_TAB_ID))
                .register(entries -> entries.add(dashboard));

        LteLog.LOGGER.info("[lte] lted dashboard item: {}", LTED_DASHBOARD_ID);
    }

    /**
     * 「LTE 工具」页签的顺序：只有 {@code lted_dashboard} 一件，重排没有意义 ——
     * 保留这个方法是因为 {@code LteServerDecor} 的注册顺序表里有它（页签条目回调按注册顺序执行，
     * 以后往这一页加东西时在这里点名即可）。
     */
    public static void pinToolsTabOrder() {
        if (dashboard == null) {
            return;
        }
        try {
            ItemGroupEvents.modifyEntriesEvent(RegistryKey.of(RegistryKeys.ITEM_GROUP, TOOLS_TAB_ID))
                    .register(entries -> {
                        entries.getDisplayStacks().removeIf(s -> s.isOf(dashboard));
                        entries.add(dashboard);
                    });
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] lted dashboard: could not pin the tools tab entry", t);
        }
    }

    /** 页签在 registry 里的那一份（调试 / 其它模组想往里加东西时用）。 */
    public static ItemGroup tab() {
        return Registries.ITEM_GROUP.get(TOOLS_TAB_ID);
    }
}
