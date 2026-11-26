// 三个地区标志方块的注册：福州地铁 / 深圳地铁 / 香港地铁（各自进自己的地区页签）。
package dev.js.mtrheadlight.region;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.logo.LogoBlock;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * 三个地区标志方块：{@code fuzhou_logo} / {@code shenzhen_logo} / {@code mtr_hk_logo}。
 *
 * <h2>抄的是哪一份</h2>
 * <p>逐行照 {@code logo.LogoObjects}（LTE / 角美两个标志块）：同一个 {@link LogoBlock} 类实例化多次，
 * 方块与物品同 id，属性逐字段照抄 MTR 的 {@code mtr:logo}
 * （{@code requiresTool() + hardness(2.0F) + luminance(10)}，另加 {@code mapColor(IRON_GRAY)}）。
 * 模型是「满方块金属底板 + 外扩 0.05 的四侧 logo 覆盖层」，资源由
 * {@code tools/gen-region-logo-assets.js} 生成，贴图是用户给的那三张原图
 * （<b>直拷不缩不重绘</b>，页签图标用的也是同一份字节）。
 *
 * <h2>页签</h2>
 * <p>每个标志块只进它自己的地区页签（{@link RegionalTabs}），由
 * {@link #pinRegionalTabOrder()} 把「标志块 → 贴墙标」压成这两个位置的前两位，
 * 与「厦门风格物件」页签里 logo 在贴墙标识之前同一个规矩。
 *
 * <p>调用方：{@code LteServerDecor} 在 {@code RegionalTabs.register()} 之后调 {@link #register()}，
 * 在贴墙标识 / 售票机都注册完之后调 {@link #pinRegionalTabOrder()}。
 */
public final class RegionalLogos {

    /** 模组命名空间。 */
    public static final String NAMESPACE = XiamenObjects.NAMESPACE;

    /** 福州地铁标志：{@code mtrlte:fuzhou_logo}（「福州地铁风格物件」页签）。 */
    public static final Identifier FUZHOU_ID = new Identifier(NAMESPACE, "fuzhou_logo");

    /** 深圳地铁标志：{@code mtrlte:shenzhen_logo}（「深圳地铁风格物件」页签）。 */
    public static final Identifier SHENZHEN_ID = new Identifier(NAMESPACE, "shenzhen_logo");

    /** 香港地铁标志：{@code mtrlte:mtr_hk_logo}（「香港地铁风格物件」页签）。 */
    public static final Identifier MTR_HK_ID = new Identifier(NAMESPACE, "mtr_hk_logo");

    private static LogoBlock fuzhouLogo;
    private static LogoBlock shenzhenLogo;
    private static LogoBlock mtrHkLogo;
    private static Item fuzhouItem;
    private static Item shenzhenItem;
    private static Item mtrHkItem;
    private static boolean registered;

    /**
     * 每个地区页签开头的两件（与厦门页签「logo 在前、贴墙标识在后」一致）。
     * 贴墙标那一件的 id 在 {@code WallSignObjects} 里注册，这里只按 id 认人。
     */
    private static final Map<Identifier, List<Identifier>> TAB_FRONT = Map.of(
            RegionalTabs.FUZHOU_TAB_ID,
            List.of(FUZHOU_ID, new Identifier(NAMESPACE, "wall_sign_fuzhou")),
            RegionalTabs.SHENZHEN_TAB_ID,
            List.of(SHENZHEN_ID, new Identifier(NAMESPACE, "wall_sign_shenzhen")),
            RegionalTabs.MTR_HK_TAB_ID,
            List.of(MTR_HK_ID, new Identifier(NAMESPACE, "wall_sign_mtr_hk")));

    private RegionalLogos() {
    }

    public static LogoBlock fuzhouLogo() {
        return fuzhouLogo;
    }

    public static LogoBlock shenzhenLogo() {
        return shenzhenLogo;
    }

    public static LogoBlock mtrHkLogo() {
        return mtrHkLogo;
    }

    public static Item fuzhouItem() {
        return fuzhouItem;
    }

    public static Item shenzhenItem() {
        return shenzhenItem;
    }

    public static Item mtrHkItem() {
        return mtrHkItem;
    }

    /** 方块 + 物品 + 页签条目（两端都要跑）。幂等。 */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        fuzhouLogo = registerBlock(FUZHOU_ID);
        shenzhenLogo = registerBlock(SHENZHEN_ID);
        mtrHkLogo = registerBlock(MTR_HK_ID);
        fuzhouItem = registerItem(FUZHOU_ID, fuzhouLogo);
        shenzhenItem = registerItem(SHENZHEN_ID, shenzhenLogo);
        mtrHkItem = registerItem(MTR_HK_ID, mtrHkLogo);

        addToTab(RegionalTabs.FUZHOU_TAB_ID, fuzhouItem);
        addToTab(RegionalTabs.SHENZHEN_TAB_ID, shenzhenItem);
        addToTab(RegionalTabs.MTR_HK_TAB_ID, mtrHkItem);

        LteLog.LOGGER.info("[lte] regional logos: {} / {} / {}", FUZHOU_ID, SHENZHEN_ID, MTR_HK_ID);
    }

    /**
     * 把三个地区页签重排成「标志块 → 贴墙标 → 其余（售票机）」。
     *
     * <p>做法与 {@code LogoObjects#pinCoreTabOrder()} 逐字同构：从
     * {@code entries.getDisplayStacks()} 里按 id 把那一件摘出来（同一个 {@link ItemStack} 实例，
     * NBT 不丢），再整批插回下标 0；其余条目保持相对顺序、只被往后推。
     *
     * <h2>为什么由 {@code LteServerDecor} 在贴墙标识之后调</h2>
     * <p>{@code ItemGroupEvents.modifyEntriesEvent} 的回调按注册顺序执行，重排那一次必须
     * 「看得见」前面回调已经加进去的条目 —— 标志块在本类 {@link #register()} 里进，
     * 贴墙标在 {@code WallSignObjects} 里进，所以重排要排在它们之后。
     */
    public static void pinRegionalTabOrder() {
        for (final Map.Entry<Identifier, List<Identifier>> tab : TAB_FRONT.entrySet()) {
            try {
                ItemGroupEvents.modifyEntriesEvent(
                                RegistryKey.of(RegistryKeys.ITEM_GROUP, tab.getKey()))
                        .register(entries -> {
                            final List<ItemStack> stacks = entries.getDisplayStacks();
                            final List<ItemStack> front = new ArrayList<>(tab.getValue().size());
                            for (final Identifier id : tab.getValue()) {
                                remember(stacks, id, front);
                            }
                            if (!front.isEmpty()) {
                                stacks.addAll(0, front);
                            }
                        });
            } catch (Throwable t) {
                LteLog.LOGGER.warn("[lte] regional tab order: could not pin {}", tab.getKey(), t);
            }
        }
    }

    /** 把 {@code id} 那一件从 {@code stacks} 里摘出来放进 {@code out}（同一个实例，NBT 不丢）。 */
    private static void remember(final List<ItemStack> stacks, final Identifier id,
                                 final List<ItemStack> out) {
        final Item item = Registries.ITEM.get(id);
        if (item == null || item == Items.AIR) {
            // 没注册（版本差异 / 被别的模块挡了）：跳过，不影响其余排序。
            return;
        }
        for (final Iterator<ItemStack> it = stacks.iterator(); it.hasNext();) {
            final ItemStack stack = it.next();
            if (stack.isOf(item)) {
                out.add(stack);
                it.remove();
            }
        }
    }

    private static void addToTab(final Identifier tabId, final Item item) {
        try {
            ItemGroupEvents.modifyEntriesEvent(RegistryKey.of(RegistryKeys.ITEM_GROUP, tabId))
                    .register(entries -> entries.add(item));
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] regional logos: could not add {} to {}", item, tabId, t);
        }
    }

    /** MTR {@code Blocks.LOGO} 的设置，只有贴图不同（见 {@code LogoObjects#registerBlock}）。 */
    private static LogoBlock registerBlock(final Identifier id) {
        final LogoBlock block = new LogoBlock(AbstractBlock.Settings.create()
                .mapColor(MapColor.IRON_GRAY)
                .requiresTool()
                .hardness(2.0F)
                .luminance(state -> 10));
        return Registry.register(Registries.BLOCK, id, block);
    }

    private static Item registerItem(final Identifier id, final LogoBlock block) {
        return Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
    }
}
