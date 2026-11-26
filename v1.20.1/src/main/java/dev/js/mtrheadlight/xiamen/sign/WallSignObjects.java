// 贴墙标识（厦门 / 角美 / LTE）的注册：3 个方块 + 3 个物品 + 1 个方块实体类型；
// 三面牌子都进「厦门风格物件」页签，位置由 LogoObjects.pinCoreTabOrder() 统一重排。
package dev.js.mtrheadlight.xiamen.sign;

import java.util.ArrayList;
import java.util.List;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * 贴墙标识（{@code mtrlte:wall_sign_*}）的注册入口（两端都要跑）。
 *
 * <h2>六个 id = 六张品牌 logo × 同一套 MTR「站名出入口」实现</h2>
 * <table border="1">
 *   <caption>id / 品牌 logo（行为见 {@link WallSignBlock}，渲染见 {@link WallSignClient}）/ 页签</caption>
 *   <tr><th>id（{@code mtrlte:} 前缀省略）</th><th>品牌 logo（MTR 画进站名贴图的那张，换成这个）</th>
 *       <th>进哪个页签</th></tr>
 *   <tr><td>{@code wall_sign_xiamen}</td><td>{@code block/xiamen_logo}</td>
 *       <td rowspan="3">{@code mtrlte:core}「厦门风格物件」</td></tr>
 *   <tr><td>{@code wall_sign_jiaomei}</td><td>{@code block/jiaomei_logo}</td></tr>
 *   <tr><td>{@code wall_sign_lte}</td><td>{@code block/lte_logo}</td></tr>
 *   <tr><td>{@code wall_sign_fuzhou}</td><td>{@code block/fuzhou_logo}</td>
 *       <td>{@code mtrlte:fuzhou}「福州地铁风格物件」</td></tr>
 *   <tr><td>{@code wall_sign_shenzhen}</td><td>{@code block/shenzhen_logo}</td>
 *       <td>{@code mtrlte:shenzhen}「深圳地铁风格物件」</td></tr>
 *   <tr><td>{@code wall_sign_mtr_hk}</td><td>{@code block/mtr_hk_logo}</td>
 *       <td>{@code mtrlte:mtr_hk}「香港地铁风格物件」</td></tr>
 * </table>
 * <p>六块方块的形状、站名、刷子行为<b>完全一样</b>（都继承 MTR 的 {@code BlockStationNameEntrance}），
 * 差别只有模型里叠的那张 logo 和进哪个页签 —— 所以是同一个 {@link WallSignBlock} 类实例化六次，
 * 也共用一个方块实体类型（{@link WallSignBlockEntity} 只注册一次，六个方块都挂在它上面）。
 *
 * <h2>方块设置：逐字段照 MTR 的 {@code mtr:station_name_entrance}</h2>
 * <p>javap {@code mtr.Blocks} 取证：{@code Settings.create().requiresTool().strength(2.0F).nonOpaque()}
 * —— 没有 {@code luminance}（那块板是普通贴图，不是自发光方块），这里也一样，
 * 免得把整块牌子点成一团白光（上一版就是 {@code luminance(15)}，看起来「不正常」）。
 *
 * <h2>页签顺序</h2>
 * <ul>
 *   <li><b>厦门风格物件</b>（{@link XiamenObjects#CORE_TAB_ID}）：厦门 / 角美 / LTE 三面牌子加到这一页，
 *       由 {@code LogoObjects.pinCoreTabOrder()} 统一重排（LTE logo → LTE 贴墙标识 → 厦门 logo →
 *       角美 logo → 两面贴墙标识 → 四件站外标志）。</li>
 *   <li><b>三个地区页签</b>（{@link dev.js.mtrheadlight.region.RegionalTabs}）：福州 / 深圳 / 香港各一面，
 *       由 {@code RegionalLogos.pinRegionalTabOrder()} 统一压到「标志块 → 贴墙标」两位。</li>
 * </ul>
 */
public final class WallSignObjects {

    /** 模组命名空间（与 {@link XiamenObjects#NAMESPACE} 一致）。 */
    public static final String NAMESPACE = XiamenObjects.NAMESPACE;

    /**
     * 一个标识：id + 品牌 logo 贴图名（不含 {@code block/} 与扩展名）+ 进哪个页签 + 方块 + 物品。
     */
    public record Entry(String id, String logo, Identifier tab, WallSignBlock block, Item item) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static BlockEntityType<WallSignBlockEntity> entityType;

    private static boolean registered;

    private WallSignObjects() {
    }

    /** 三个方块（客户端挂 cutout 渲染层用）。 */
    public static List<WallSignBlock> blocks() {
        final List<WallSignBlock> out = new ArrayList<>(ENTRIES.size());
        for (final Entry entry : ENTRIES) {
            out.add(entry.block());
        }
        return out;
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    /** 三个方块共用的方块实体类型（名字沿用 {@code mtrlte:wall_sign_xiamen}）。 */
    public static BlockEntityType<WallSignBlockEntity> entityType() {
        return entityType;
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        // 顺序 = 注册顺序：厦门 → 角美 → LTE（进「厦门风格物件」），再福州 → 深圳 → 香港（各进自己的地区页签）。
        // 页签里实际的先后由两个 pin 方法决定。
        final Entry xiamen = add("wall_sign_xiamen", "xiamen_logo", XiamenObjects.CORE_TAB_ID);
        final Entry jiaomei = add("wall_sign_jiaomei", "jiaomei_logo", XiamenObjects.CORE_TAB_ID);
        final Entry lte = add("wall_sign_lte", "lte_logo", XiamenObjects.CORE_TAB_ID);
        final Entry fuzhou = add("wall_sign_fuzhou", "fuzhou_logo",
                dev.js.mtrheadlight.region.RegionalTabs.FUZHOU_TAB_ID);
        final Entry shenzhen = add("wall_sign_shenzhen", "shenzhen_logo",
                dev.js.mtrheadlight.region.RegionalTabs.SHENZHEN_TAB_ID);
        final Entry mtrHk = add("wall_sign_mtr_hk", "mtr_hk_logo",
                dev.js.mtrheadlight.region.RegionalTabs.MTR_HK_TAB_ID);

        // 方块实体类型必须在这六个方块之后：Fabric 的 builder 会把方块记进去（校验方块的 BE 类型）。
        try {
            entityType = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(NAMESPACE, "wall_sign_xiamen"),
                    FabricBlockEntityTypeBuilder.create(WallSignBlockEntity::new,
                            xiamen.block(), jiaomei.block(), lte.block(),
                            fuzhou.block(), shenzhen.block(), mtrHk.block()).build());
        } catch (Throwable t) {
            LteLog.LOGGER.error("[lte] wall sign: could not register the block entity type", t);
        }

        // 按页签分组，一组一个回调：厦门三面进「厦门风格物件」，福州 / 深圳 / 香港各一面进各自的地区页签。
        // 这里只保证「有位」，最终位置由 LogoObjects.pinCoreTabOrder() / RegionalLogos.pinRegionalTabOrder() 重排。
        for (final Entry entry : ENTRIES) {
            try {
                final Identifier tab = entry.tab();
                ItemGroupEvents.modifyEntriesEvent(RegistryKey.of(RegistryKeys.ITEM_GROUP, tab))
                        .register(entries -> entries.add(entry.item()));
            } catch (Throwable t) {
                LteLog.LOGGER.warn("[lte] wall sign: could not add the creative tab entry for {}", entry.id(), t);
            }
        }

        for (final Entry entry : ENTRIES) {
            LteLog.LOGGER.info("[lte] wall sign: {} (logo={}, tab={})", entry.id(), entry.logo(), entry.tab());
        }
    }

    private static Entry add(final String id, final String logo, final Identifier tab) {
        final Identifier identifier = new Identifier(NAMESPACE, id);
        // 与 MTR 的 mtr:station_name_entrance 同一套设置（javap mtr.Blocks 取证），只多一张 logo 贴图。
        final WallSignBlock block = new WallSignBlock(AbstractBlock.Settings.create()
                .requiresTool()
                .strength(2.0F)
                .nonOpaque(), logo);
        Registry.register(Registries.BLOCK, identifier, block);
        final Item item = Registry.register(Registries.ITEM, identifier,
                new BlockItem(block, new Item.Settings()));
        final Entry entry = new Entry(id, logo, tab, block, item);
        ENTRIES.add(entry);
        return entry;
    }
}
