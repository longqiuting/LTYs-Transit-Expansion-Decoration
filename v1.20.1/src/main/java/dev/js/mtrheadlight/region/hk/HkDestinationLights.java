// 终点站指示灯的注册：方块 + 物品 + 方块实体类型（进香港页签）。
package dev.js.mtrheadlight.region.hk;

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

/** 港铁终点站指示灯（{@code mtrlte:hk_destination_light}）。 */
public final class HkDestinationLights {

    public static final String NAMESPACE = "mtrlte";
    public static final Identifier ID = new Identifier(NAMESPACE, "hk_destination_light");

    private static boolean registered;

    private static HkDestinationLightBlock block;
    private static Item item;
    private static BlockEntityType<HkDestinationLightBlockEntity> entityType;

    private HkDestinationLights() {
    }

    public static HkDestinationLightBlock block() {
        return block;
    }

    public static BlockEntityType<HkDestinationLightBlockEntity> entityType() {
        return entityType;
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        block = Registry.register(Registries.BLOCK, ID, new HkDestinationLightBlock(settings()));
        item = Registry.register(Registries.ITEM, ID, new BlockItem(block, new Item.Settings()));
        entityType = Registry.register(Registries.BLOCK_ENTITY_TYPE, ID,
                FabricBlockEntityTypeBuilder.create(HkDestinationLightBlockEntity::new, block).build());

        try {
            ItemGroupEvents.modifyEntriesEvent(RegistryKey.of(RegistryKeys.ITEM_GROUP,
                            RegionalTabs.MTR_HK_TAB_ID))
                    .register(entries -> entries.add(item));
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] destination light could not be added to the MTR HK tab", t);
        }

        LteLog.LOGGER.info("[lte] hk destination light: {}", ID);
    }

    private static AbstractBlock.Settings settings() {
        return AbstractBlock.Settings.create()
                .mapColor(MapColor.BLACK)
                .strength(1.0F, 3.0F)
                .nonOpaque()
                // 灯体本身不发亮：亮不亮由 BER 决定（没有进站车就是灭的），
                // 这样也不会为了「灭灯」去改方块状态、刷方块更新。
                .luminance(state -> 0)
                .sounds(BlockSoundGroup.METAL);
    }
}
