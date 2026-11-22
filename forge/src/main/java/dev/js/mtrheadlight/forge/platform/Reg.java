package dev.js.mtrheadlight.forge.platform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 注册层 shim：把 java-gen（Fabric 反编译产物）里的
 * {@code Registry.register(BuiltInRegistries.X, id, value)} /
 * {@code FabricBlockEntityTypeBuilder} / {@code FabricItemGroup} /
 * {@code ItemGroupEvents} 统一换成 Forge 的 {@link DeferredRegister}。
 *
 * <p>调用点仍然是「先 new 出对象、再注册」的 Fabric 写法，所以这里收的是
 * <b>已经构造好的实例</b>（{@code () -> value}）；真正的写入发生在
 * {@code RegisterEvent} 阶段，由 {@link #register(IEventBus)} 挂上总线。
 *
 * <p>创造模式页签：Fabric 的 {@code ItemGroupEvents.modifyEntriesEvent(...)}
 * 在 Forge 对应 {@link BuildCreativeModeTabContentsEvent}。因为我们的页签全是
 * 自己建的（没有原版条目），shim 给每个页签维护一份 {@code List<ItemStack>}，
 * 依次跑完所有监听器，再把结果 accept 进事件 —— 与 Fabric 的语义一致。
 */
public final class Reg {

    public static final String MOD_ID = "mtrlte";

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    private static final Map<ResourceLocation, TabContents> TAB_CONTENTS = new LinkedHashMap<>();

    private Reg() {
    }

    /** 把 6 个 DeferredRegister 挂到 mod 事件总线上（mod 构造器里调用一次）。 */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        TABS.register(bus);
    }

    public static <T extends Block> T block(ResourceLocation id, T block) {
        BLOCKS.register(id.getPath(), () -> block);
        return block;
    }

    public static <T extends Item> T item(ResourceLocation id, T item) {
        ITEMS.register(id.getPath(), () -> item);
        return item;
    }

    /** {@code FabricBlockEntityTypeBuilder.create(...).build()} 的 Forge 等价物。 */
    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(
            ResourceLocation id, BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks) {
        BlockEntityType<T> type = BlockEntityType.Builder.of(factory, blocks).build(null);
        BLOCK_ENTITIES.register(id.getPath(), () -> type);
        return type;
    }

    /** {@code FabricEntityTypeBuilder.create(category, factory).dimensions(...).track...build()} 的 1:1 映射。 */
    public static <T extends Entity> EntityType<T> entityType(
            ResourceLocation id, EntityType.EntityFactory<T> factory, MobCategory category,
            float width, float height, int trackingRange, int updateInterval) {
        EntityType<T> type = EntityType.Builder.of(factory, category)
                .sized(width, height)
                .clientTrackingRange(trackingRange)
                .updateInterval(updateInterval)
                .build(id.getPath());
        ENTITIES.register(id.getPath(), () -> type);
        return type;
    }

    public static SoundEvent sound(ResourceLocation id) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        SOUNDS.register(id.getPath(), () -> event);
        return event;
    }

    public static CreativeModeTab tab(ResourceLocation id, Component title, Supplier<ItemStack> icon) {
        // displayItems 必须给一个非空生成器：Forge 的 BuildCreativeModeTabContentsEvent 只负责
        // 「往里加」，不会替我们补默认生成器；留 null 会在第一次打开页签时 NPE。
        CreativeModeTab tab = CreativeModeTab.builder()
                .title(title)
                .icon(icon)
                .displayItems((parameters, output) -> {
                })
                .build();
        TABS.register(id.getPath(), () -> tab);
        return tab;
    }

    /** {@code ItemGroupEvents.modifyEntriesEvent(key).register(...)} 的 Forge 侧入口。 */
    public static void tabEntries(ResourceLocation tabId, TabEntriesConsumer consumer) {
        contents(tabId).consumers.add(consumer);
    }

    /** mod 总线监听器：把攒下来的页签条目灌进 Forge 的页签构建事件。 */
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        TabContents tab = TAB_CONTENTS.get(event.getTabKey().location());
        if (tab == null) {
            return;
        }
        tab.build();
        for (ItemStack stack : tab.stacks) {
            event.accept(stack);
        }
    }

    private static TabContents contents(ResourceLocation tabId) {
        return TAB_CONTENTS.computeIfAbsent(tabId, id -> new TabContents());
    }

    /** Fabric {@code ItemGroupEvents.ModifyEntries} 的替身：既保留 {@code method_45421}，也实现 {@code Output}。 */
    public interface TabEntries extends CreativeModeTab.Output {

        List<ItemStack> getDisplayStacks();

        /** 反编译产物里的 {@code ModifyEntries#add(ItemConvertible)}（intermediary 名）。 */
        default void method_45421(ItemLike item) {
            accept(new ItemStack(item));
        }

        @Override
        default void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
            List<ItemStack> stacks = getDisplayStacks();
            for (ItemStack existing : stacks) {
                if (ItemStack.isSameItemSameTags(existing, stack)) {
                    return;
                }
            }
            stacks.add(stack);
        }
    }

    /** 页签条目监听器的函数式接口（替代 Fabric 的 ItemGroupEvents.ModifyEntries 强转）。 */
    @FunctionalInterface
    public interface TabEntriesConsumer {
        void accept(TabEntries entries);
    }

    private static final class TabContents implements TabEntries {

        private final List<TabEntriesConsumer> consumers = new ArrayList<>();
        private final List<ItemStack> stacks = new ArrayList<>();

        @Override
        public List<ItemStack> getDisplayStacks() {
            return stacks;
        }

        void build() {
            stacks.clear();
            for (TabEntriesConsumer consumer : consumers) {
                consumer.accept(this);
            }
        }
    }
}
