// 福州地铁售票机的注册：上下两个方块 + 物品 + 方块实体类型 + 创造页签条目。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.region.RegionalTabs;
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

/** 福州地铁售票机的方块 / 物品 / 方块实体。幂等。 */
public final class FuzhouMachines {

    public static final String NAMESPACE = "mtrlte";

    public static final Identifier BOTTOM_ID = new Identifier(NAMESPACE, "fuzhou_ticket_machine_bottom");
    public static final Identifier TOP_ID = new Identifier(NAMESPACE, "fuzhou_ticket_machine_top");

    private static boolean registered;

    private static FuzhouMachineBlock bottom;
    private static FuzhouMachineBlock top;
    private static Item bottomItem;
    private static Item topItem;
    private static BlockEntityType<FuzhouMachineBlockEntity> entityType;

    private FuzhouMachines() {
    }

    public static FuzhouMachineBlock bottom() {
        return bottom;
    }

    public static FuzhouMachineBlock top() {
        return top;
    }

    public static BlockEntityType<FuzhouMachineBlockEntity> entityType() {
        return entityType;
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        bottom = Registry.register(Registries.BLOCK, BOTTOM_ID,
                new FuzhouMachineBlock(machineSettings(), false,
                        "fuzhou_machine_bottom", "fuzhou_machine_bottom_color"));
        top = Registry.register(Registries.BLOCK, TOP_ID,
                new FuzhouMachineBlock(machineSettings(), true,
                        "fuzhou_machine_top", "fuzhou_machine_top_color"));

        bottomItem = Registry.register(Registries.ITEM, BOTTOM_ID,
                new BlockItem(bottom, new Item.Settings()));
        topItem = Registry.register(Registries.ITEM, TOP_ID,
                new BlockItem(top, new Item.Settings()));

        entityType = Registry.register(Registries.BLOCK_ENTITY_TYPE, TOP_ID,
                FabricBlockEntityTypeBuilder.create(FuzhouMachineBlockEntity::new, top, bottom).build());

        try {
            ItemGroupEvents.modifyEntriesEvent(RegistryKey.of(RegistryKeys.ITEM_GROUP,
                            RegionalTabs.FUZHOU_TAB_ID))
                    .register(entries -> {
                        entries.add(bottomItem);
                        entries.add(topItem);
                    });
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] fuzhou machine could not be added to its tab", t);
        }

        LteLog.LOGGER.info("[lte] fuzhou machine: {} / {}", BOTTOM_ID, TOP_ID);
    }

    private static AbstractBlock.Settings machineSettings() {
        return AbstractBlock.Settings.create()
                .mapColor(MapColor.IRON_GRAY)
                .requiresTool()
                .strength(2.0F, 6.0F)
                .nonOpaque()
                .sounds(BlockSoundGroup.METAL);
    }

    /** 未使用的引用，避免 IDE 警告（页签 id 由 RegionalTabs 提供）。 */
    static RegistryKey<net.minecraft.item.ItemGroup> coreTab() {
        return RegistryKey.of(RegistryKeys.ITEM_GROUP, XiamenObjects.CORE_TAB_ID);
    }
}
