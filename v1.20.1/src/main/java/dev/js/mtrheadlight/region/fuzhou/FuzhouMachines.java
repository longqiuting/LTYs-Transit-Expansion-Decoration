// 福州地铁售票机的注册：一个方块 id（HALF 双段）+ 一个物品 + 方块实体类型。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.region.RegionalTabs;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
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
 * 福州地铁售票机：<b>一个</b>方块 id（{@code mtrlte:fuzhou_ticket_machine}）+ 一个物品。
 *
 * <p>上下两段靠 {@code HALF} 属性区分（和 {@code mtrlte:ticket_machine_xiamen} 完全一致），
 * 所以创造页签里只有一个条目，不会有「(Bottom)」「(Top)」两个。
 */
public final class FuzhouMachines {

    public static final String NAMESPACE = "mtrlte";
    public static final Identifier ID = new Identifier(NAMESPACE, "fuzhou_ticket_machine");

    private static boolean registered;

    private static FuzhouMachineBlock block;
    private static Item item;
    private static BlockEntityType<FuzhouMachineBlockEntity> entityType;

    private FuzhouMachines() {
    }

    public static FuzhouMachineBlock block() {
        return block;
    }

    public static Item item() {
        return item;
    }

    public static BlockEntityType<FuzhouMachineBlockEntity> entityType() {
        return entityType;
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        block = Registry.register(Registries.BLOCK, ID, new FuzhouMachineBlock(machineSettings()));
        item = Registry.register(Registries.ITEM, ID, new BlockItem(block, new Item.Settings()));

        entityType = Registry.register(Registries.BLOCK_ENTITY_TYPE, ID,
                FabricBlockEntityTypeBuilder.create(FuzhouMachineBlockEntity::new, block).build());

        try {
            ItemGroupEvents.modifyEntriesEvent(RegistryKey.of(RegistryKeys.ITEM_GROUP,
                            RegionalTabs.FUZHOU_TAB_ID))
                    .register(entries -> entries.add(item));
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] fuzhou machine could not be added to its tab", t);
        }

        FuzhouMachineNet.register();

        LteLog.LOGGER.info("[lte] fuzhou machine: {} (single block, HALF lower/upper)", ID);
    }

    private static AbstractBlock.Settings machineSettings() {
        return AbstractBlock.Settings.create()
                .mapColor(MapColor.IRON_GRAY)
                .requiresTool()
                .strength(2.0F, 6.0F)
                .nonOpaque()
                // 微发光：上段（屏那半）7 级、下段 6 级，和厦门售票机一致；夜里机器自己照亮一小圈
                .luminance(state -> dev.js.mtrheadlight.region.fuzhou.FuzhouMachineBlock.isUpper(state) ? 7 : 6)
                .sounds(BlockSoundGroup.METAL);
    }
}
