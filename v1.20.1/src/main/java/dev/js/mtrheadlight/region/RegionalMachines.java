// 地区机器（目前只有港铁新售票机）的注册入口。
package dev.js.mtrheadlight.region;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * 港铁新售票机 {@code mtrlte:ticket_machine_mtr} 的注册入口（两端都要跑）。
 *
 * <h2>抄的是哪一份</h2>
 * <p>形状与 {@code XiamenMachines#register()} 里售票机那一段逐行同构：一个方块 id + 一个物品 id
 * （方块与物品同 id），属性用同一份 {@code settings()}（铁壳 / 需要镐 / nonOpaque），
 * 物品进「香港地铁风格物件」页签（{@link RegionalTabs#MTR_HK_TAB_ID}）。
 *
 * <p>没有方块实体类型：售票机的两种界面都由 MTR / 方速自己的界面系统开，方块本身不需要存数据
 * （和厦门售票机一样 —— 厦门那台的 BE 类型只给余额查询机用）。
 */
public final class RegionalMachines {

    /** 模组命名空间（与 {@link XiamenObjects#NAMESPACE} 一致）。 */
    public static final String NAMESPACE = XiamenObjects.NAMESPACE;

    /** 港铁新售票机（两格高）。 */
    public static final Identifier MTR_TICKET_ID = new Identifier(NAMESPACE, "ticket_machine_mtr");

    private static MtrTicketMachineBlock ticket;
    private static Item ticketItem;
    private static boolean registered;

    private RegionalMachines() {
    }

    public static MtrTicketMachineBlock ticket() {
        return ticket;
    }

    /** 方块 + 物品 + 创造页签条目。 */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ticket = Registry.register(Registries.BLOCK, MTR_TICKET_ID,
                new MtrTicketMachineBlock(settings()));
        ticketItem = Registry.register(Registries.ITEM, MTR_TICKET_ID,
                new BlockItem(ticket, new Item.Settings()));

        try {
            ItemGroupEvents.modifyEntriesEvent(
                            RegistryKey.of(RegistryKeys.ITEM_GROUP, RegionalTabs.MTR_HK_TAB_ID))
                    .register(entries -> entries.add(ticketItem));
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] mtr ticket machine: could not add the creative tab entry", t);
        }

        LteLog.LOGGER.info("[lte] mtr ticket machine: {} (default ui={})",
                MTR_TICKET_ID, MtrTicketMachineBlock.defaultUi());
    }

    /**
     * 每台机器一份设置 —— 不能共用同一个 {@code Settings} 实例（{@code AbstractBlock} 构造时会把
     * 战利品表 id 之类的字段写进这个对象），所以这里与 {@code XiamenMachines#settings()} 各写一份。
     */
    private static AbstractBlock.Settings settings() {
        return AbstractBlock.Settings.create()
                .mapColor(MapColor.IRON_GRAY)
                .requiresTool()
                .strength(2.0F)
                .sounds(BlockSoundGroup.METAL)
                .nonOpaque();
    }
}
