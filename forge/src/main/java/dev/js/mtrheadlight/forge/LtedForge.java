// @Mod 入口：注册 3 个迁移类（LogoBlock / ClockBlock / ClockBlockEntity）+ 1 个物品类。
package dev.js.mtrheadlight.forge;

import dev.js.mtrheadlight.forge.block.ClockBlock;
import dev.js.mtrheadlight.forge.block.ClockBlockEntity;
import dev.js.mtrheadlight.forge.block.LogoBlock;
import dev.js.mtrheadlight.forge.item.LtedTooltipItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge 侧的注册入口（M4a 工具链验证）。
 *
 * <p>和 Fabric 侧的 {@code ClockObjects} / {@code LogoObjects} 是同一批 id
 * （{@code mtrlte:clock} / {@code mtrlte:lte_logo}），换成 Forge 的
 * {@link DeferredRegister} 写法：
 * <ul>
 *   <li>{@link RegistryObject} 对应 Fabric 的 {@code Registry.register} 返回值；</li>
 *   <li>{@code BlockEntityType.Builder.of(...)} 对应
 *       {@code FabricBlockEntityTypeBuilder.create(...)}（fabric-api 的那层壳不需要了）；</li>
 *   <li>页签、网络包、BER 本轮都还没迁 —— 见报告里的工作量评估。</li>
 * </ul>
 */
@Mod(LtedForge.MOD_ID)
public final class LtedForge {

    public static final String MOD_ID = "mtrlte";

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    /** 标志方块：逐字照抄 MTR 的 {@code Blocks.LOGO} 设置（requiresTool / hardness 2 / light 10）。 */
    public static final RegistryObject<Block> LTE_LOGO = BLOCKS.register("lte_logo",
            () -> new LogoBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F)
                    .lightLevel(state -> 10)));

    /** 时钟方块：几何全部由 BER 画，方块本体只负责状态 + 状态机入口。 */
    public static final RegistryObject<Block> CLOCK = BLOCKS.register("clock",
            () -> new ClockBlock(BlockBehaviour.Properties.of()
                    .strength(0.6F)
                    .noOcclusion()
                    .dynamicShape()));

    public static final RegistryObject<BlockEntityType<ClockBlockEntity>> CLOCK_BE =
            BLOCK_ENTITIES.register("clock",
                    () -> BlockEntityType.Builder.of(ClockBlockEntity::new, CLOCK.get()).build(null));

    public static final RegistryObject<Item> CLOCK_ITEM = ITEMS.register("clock",
            () -> new BlockItem(CLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> LTE_TOOL = ITEMS.register("lte_tool",
            () -> new LtedTooltipItem(new Item.Properties()));

    public LtedForge() {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
