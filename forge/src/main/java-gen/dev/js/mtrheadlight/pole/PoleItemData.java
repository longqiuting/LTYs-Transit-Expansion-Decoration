package dev.js.mtrheadlight.pole;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 柱底物品上的属性 NBT（Forge / mojmap 版）。
 *
 * <p>形状与 Fabric 侧逐字一致（同一个 tag 名、同一个键名），只是 NBT 类型叫
 * {@link CompoundTag}（{@code NbtCompound}）、{@code getOrCreateSubNbt} → {@code getOrCreateTagElement}。
 */
public final class PoleItemData {

    private static final String TAG_ROOT = "LtedPole";
    private static final String TAG_MODE = "mode";
    private static final String TAG_COLOR = "color";

    private PoleItemData() {
    }

    public static void write(final ItemStack stack, final boolean custom, final int rgb) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        final CompoundTag root = stack.getTag();
        if (!custom) {
            if (root != null && root.contains(TAG_ROOT)) {
                root.remove(TAG_ROOT);
            }
            return;
        }
        final CompoundTag tag = stack.getOrCreateTagElement(TAG_ROOT);
        tag.putInt(TAG_MODE, AmtrPoleBottomBlockEntity.MODE_CUSTOM);
        tag.putInt(TAG_COLOR, rgb & 0xFFFFFF);
    }

    public static boolean isCustom(final ItemStack stack) {
        final CompoundTag tag = tag(stack);
        return tag != null && tag.getInt(TAG_MODE) == AmtrPoleBottomBlockEntity.MODE_CUSTOM;
    }

    public static int color(final ItemStack stack) {
        final CompoundTag tag = tag(stack);
        if (tag == null) {
            return PoleColors.WHITE;
        }
        final int rgb = tag.getInt(TAG_COLOR);
        return rgb == 0 ? PoleColors.WHITE : (rgb & 0xFFFFFF);
    }

    public static void appendTooltip(final ItemStack stack, final List<Component> lines) {
        if (!isCustom(stack)) {
            return;
        }
        lines.add(Component.translatable("tooltip.mtrlte.amtr_pole.custom",
                String.format("#%06X", Integer.valueOf(color(stack))))
                .withStyle(ChatFormatting.GRAY));
    }

    private static CompoundTag tag(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        final CompoundTag root = stack.getTag();
        return root == null ? null : root.getCompound(TAG_ROOT);
    }
}
