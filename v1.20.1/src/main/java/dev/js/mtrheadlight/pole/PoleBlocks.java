// 厦门地铁柱子（柱底 / 柱中）的注册点：两个方块 + 两个物品 + 柱底的方块实体类型 + 创造页签条目。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * 厦门地铁柱子，两个方块（素材目录 {@code 素材/厦门地铁柱子}）：
 *
 * <table border="1">
 *   <caption>两个方块</caption>
 *   <tr><th>id</th><th>素材</th><th>element</th><th>可染色</th></tr>
 *   <tr><td>{@code mtrlte:amtr_pole_bottom}</td><td>{@code amtr_pole_bottom.json}</td><td>17（0..8 本体 +
 *       9..16 {@code color} 分组）</td><td>是（那一圈凸带）</td></tr>
 *   <tr><td>{@code mtrlte:amtr_pole_middle}</td><td>{@code amtr_pole.json}</td><td>9（16×16×16 核心 +
 *       8 片鳍片）</td><td>否</td></tr>
 * </table>
 *
 * <h2>为什么柱中是普通方块、柱底才有方块实体</h2>
 * <p>柱中没有任何状态（颜色、朝向都没有），做成普通 {@link Block} 就够 —— 少一个方块实体就少一份
 * NBT 与同步开销。柱底要存「跟随车站色 / 自定义颜色 + 解析出来的车站色」，所以只有它有
 * {@link AmtrPoleBottomBlockEntity}。
 *
 * <h2>碰撞与堆叠</h2>
 * <p>两块都是<b>整格</b>碰撞（{@code 0..16}，即不写 {@code collisionShape} 的默认值）。模型里的鳍片
 * 伸出格子（x/z −1.1..17.1）只是<b>画</b>到格子外面，碰撞与摆放仍按整格算，所以柱底与柱中可以直接
 * 上下叠起来，玩家也能站在上面。
 *
 * <h2>贴图</h2>
 * <p>{@code textures/block/white_concrete.png} / {@code gray_concrete.png} 就是素材目录里那两张
 * <b>16×16 原图</b>（没有缩放、没有重绘）—— 材质是 MC 白色 / 灰色混凝土。素材 JSON 里的贴图变量
 * {@code #0} = 白、{@code #1} = 灰，本类注册的模型文件已经把变量改成
 * {@code mtrlte:block/white_concrete} / {@code mtrlte:block/gray_concrete}。
 *
 * <h2>进哪个创造页签</h2>
 * <p>与大理石 / RCC 站台同一页：{@code mtrlte:core}（「厦门风格物件」），排在装饰方块之后。
 */
public final class PoleBlocks {

    /** 模组命名空间。 */
    public static final String NAMESPACE = "mtrlte";

    /** 柱底：{@code mtrlte:amtr_pole_bottom}。 */
    public static final Identifier BOTTOM_ID = new Identifier(NAMESPACE, "amtr_pole_bottom");

    /** 柱中：{@code mtrlte:amtr_pole_middle}。 */
    public static final Identifier MIDDLE_ID = new Identifier(NAMESPACE, "amtr_pole_middle");

    private static AmtrPoleBottomBlock bottom;
    private static Block middle;
    private static Item bottomItem;
    private static Item middleItem;
    private static BlockEntityType<AmtrPoleBottomBlockEntity> bottomEntityType;
    private static boolean registered;

    private PoleBlocks() {
    }

    public static AmtrPoleBottomBlock bottom() {
        return bottom;
    }

    public static Block middle() {
        return middle;
    }

    public static Item bottomItem() {
        return bottomItem;
    }

    public static Item middleItem() {
        return middleItem;
    }

    /** 柱底的方块实体类型 {@code mtrlte:amtr_pole_bottom}（同一个 id、另一个注册表）。 */
    public static BlockEntityType<AmtrPoleBottomBlockEntity> bottomEntityType() {
        return bottomEntityType;
    }

    /** 方块 + 物品 + 方块实体类型 + 页签条目，{@code LteServerDecor} 入口调一次。幂等。 */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        bottom = Registry.register(Registries.BLOCK, BOTTOM_ID,
                new AmtrPoleBottomBlock(poleSettings()));
        middle = Registry.register(Registries.BLOCK, MIDDLE_ID, new Block(poleSettings()));

        bottomItem = Registry.register(Registries.ITEM, BOTTOM_ID,
                new AmtrPoleBottomItem(bottom, new Item.Settings()));
        middleItem = Registry.register(Registries.ITEM, MIDDLE_ID,
                new BlockItem(middle, new Item.Settings()));

        // 柱底已改成普通方块：没有方块实体、没有颜色通道、没有染色。

        try {
            ItemGroupEvents.modifyEntriesEvent(RegistryKey.of(RegistryKeys.ITEM_GROUP,
                            XiamenObjects.CORE_TAB_ID))
                    .register(entries -> {
                        entries.add(bottomItem);
                        entries.add(middleItem);
                    });
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole blocks could not be added to the core tab", t);
        }

        LteLog.LOGGER.info("[lte] pole: {} / {}", BOTTOM_ID, MIDDLE_ID);
    }

    /**
     * 柱子的通用方块属性：整格、石头音效、需要镐（{@code mineable/pickaxe} 标签里有它们）。
     *
     * <p>{@code requiresTool()} 意味着「没有正确工具就不掉落」，所以这两块必须出现在
     * {@code data/minecraft/tags/blocks/mineable/pickaxe.json} 里，掉落表
     * {@code data/mtrlte/loot_tables/blocks/*.json} 只写「炸不掉就掉自己」。
     */
    private static AbstractBlock.Settings poleSettings() {
        return AbstractBlock.Settings.create()
                .mapColor(MapColor.WHITE)
                .requiresTool()
                .strength(1.5F, 6.0F)
                .sounds(BlockSoundGroup.STONE);
    }
}
