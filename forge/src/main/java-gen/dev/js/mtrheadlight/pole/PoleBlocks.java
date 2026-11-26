package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.forge.platform.Reg;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

/**
 * 厦门地铁柱子（Forge / mojmap 版）：{@code mtrlte:amtr_pole_bottom} + {@code mtrlte:amtr_pole_middle}。
 *
 * <p>与 Fabric 树 {@code v1.20.1/.../pole/PoleBlocks.java} 逐条对应，只换注册入口：
 * {@code Registry.register(Registries.BLOCK, id, v)} → {@link Reg#block(ResourceLocation, Block)}
 * （Forge 的 {@code DeferredRegister}，实例在 {@code RegisterEvent} 期间才真正落表）。
 *
 * <p>调用点在 {@code LtedForge#onRegister}（紧接 java-gen 的 {@code LteServerDecor#onInitialize}），
 * 那里整段都在 {@code RegisterEvent} 里跑，所以「先 new 出方块再注册」的 Fabric 写法在 Forge 上成立。
 */
public final class PoleBlocks {

    public static final String NAMESPACE = "mtrlte";
    public static final ResourceLocation BOTTOM_ID = new ResourceLocation("mtrlte", "amtr_pole_bottom");
    public static final ResourceLocation MIDDLE_ID = new ResourceLocation("mtrlte", "amtr_pole_middle");

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

    public static BlockEntityType<AmtrPoleBottomBlockEntity> bottomEntityType() {
        return bottomEntityType;
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        bottom = Reg.block(BOTTOM_ID, new AmtrPoleBottomBlock(poleSettings()));
        middle = Reg.block(MIDDLE_ID, new Block(poleSettings()));

        bottomItem = Reg.item(BOTTOM_ID, new AmtrPoleBottomItem(bottom, new Item.Properties()));
        middleItem = Reg.item(MIDDLE_ID, new BlockItem(middle, new Item.Properties()));

        bottomEntityType = Reg.blockEntityType(BOTTOM_ID, AmtrPoleBottomBlockEntity::new, bottom);

        PoleNet.register();

        try {
            Reg.tabEntries(XiamenObjects.CORE_TAB_ID, (Reg.TabEntriesConsumer) entries -> {
                entries.method_45421(bottomItem);
                entries.method_45421(middleItem);
            });
        } catch (Throwable t) {
            LteLog.LOGGER.warn("[lte] pole blocks could not be added to the core tab", t);
        }

        LteLog.LOGGER.info("[lte] pole: {} / {}", BOTTOM_ID, MIDDLE_ID);
    }

    private static Properties poleSettings() {
        return Properties.of()
                .mapColor(MapColor.SNOW)
                .requiresCorrectToolForDrops()
                .strength(1.5F, 6.0F)
                .sound(SoundType.STONE);
    }
}
