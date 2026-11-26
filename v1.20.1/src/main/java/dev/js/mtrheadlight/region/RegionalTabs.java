// 三个地区创造页签：福州地铁 / 深圳地铁 / 香港地铁风格物件。
package dev.js.mtrheadlight.region;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.TabIcons;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * 福州 / 深圳 / 香港三个地区页签（两端都要跑）。
 *
 * <h2>抄的是哪一份</h2>
 * <p>与 {@link XiamenObjects#register()} 里「厦门风格物件」页签的注册方式逐字同构：页签图标是一个
 * <b>只画图标的物品</b>（见 {@code TabIcons} 的类注释），标题走
 * {@code itemGroup.mtrlte.<id>} 翻译键，条目由各功能模块自己用
 * {@code ItemGroupEvents.modifyEntriesEvent} 往里加。
 *
 * <h2>顺序</h2>
 * <p>三个页签按用户点名的顺序注册：<b>福州 → 深圳 → 香港</b>。创造菜单里的页签顺序 = 注册顺序，
 * 而 {@code LteServerDecor} 把本模块紧排在「厦门风格物件」之后，所以四条地铁风格页签
 * （厦门 / 福州 / 深圳 / 香港）在创造菜单里是<b>连续</b>的一段。
 *
 * <h2>各页签里放什么</h2>
 * <ul>
 *   <li>{@code mtrlte:fuzhou}：{@code fuzhou_logo} + {@code wall_sign_fuzhou}；</li>
 *   <li>{@code mtrlte:shenzhen}：{@code shenzhen_logo} + {@code wall_sign_shenzhen}；</li>
 *   <li>{@code mtrlte:mtr_hk}：{@code mtr_hk_logo} + {@code wall_sign_mtr_hk} + {@code ticket_machine_mtr}。</li>
 * </ul>
 * <p>标志块与贴墙标由 {@link RegionalLogos} / {@code WallSignObjects} 加，售票机由
 * {@link RegionalMachines} 加；条目顺序最后由 {@link RegionalLogos#pinRegionalTabOrder()} 统一压定
 * （标志块在左、贴墙标在右，与厦门页签「logo 在前、贴墙标识在后」一致）。
 */
public final class RegionalTabs {

    /** 模组命名空间（与 {@link XiamenObjects#NAMESPACE} 一致）。 */
    public static final String NAMESPACE = XiamenObjects.NAMESPACE;

    /** 页签 id：{@code mtrlte:fuzhou}（「福州地铁风格物件」）。 */
    public static final Identifier FUZHOU_TAB_ID = new Identifier(NAMESPACE, "fuzhou");

    /** 页签 id：{@code mtrlte:shenzhen}（「深圳地铁风格物件」）。 */
    public static final Identifier SHENZHEN_TAB_ID = new Identifier(NAMESPACE, "shenzhen");

    /** 页签 id：{@code mtrlte:mtr_hk}（「香港地铁风格物件」）。 */
    public static final Identifier MTR_HK_TAB_ID = new Identifier(NAMESPACE, "mtr_hk");

    private static boolean registered;

    private RegionalTabs() {
    }

    /** 注册三个页签（{@code main} 入口调一次，在 {@code XiamenObjects.register()} 之后）。 */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        // 图标物品先注册：页签的 icon supplier 是延迟求值的，但物品必须是已注册的条目。
        TabIcons.register();

        register(FUZHOU_TAB_ID, "itemGroup.mtrlte.fuzhou", TabIcons.fuzhou());
        register(SHENZHEN_TAB_ID, "itemGroup.mtrlte.shenzhen", TabIcons.shenzhen());
        register(MTR_HK_TAB_ID, "itemGroup.mtrlte.mtr_hk", TabIcons.mtrHk());

        LteLog.LOGGER.info("[lte] regional tabs: {} / {} / {}",
                FUZHOU_TAB_ID, SHENZHEN_TAB_ID, MTR_HK_TAB_ID);
    }

    private static void register(final Identifier id, final String titleKey,
                                 final net.minecraft.item.Item icon) {
        Registry.register(Registries.ITEM_GROUP, id,
                FabricItemGroup.builder()
                        .displayName(Text.translatable(titleKey))
                        .icon(() -> new ItemStack(icon))
                        .build());
    }
}
