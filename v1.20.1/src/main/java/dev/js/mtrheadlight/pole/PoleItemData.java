// 柱底物品上的「属性」NBT：ctrl+中键复制出来的模式 + 颜色，放下时原样套到方块实体上。
package dev.js.mtrheadlight.pole;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 柱底物品堆上的属性（= ctrl+中键复制到手里的那一份）。
 *
 * <h2>NBT 形状</h2>
 * <pre>
 *   { LtedPole: { mode: 1, color: 1234567 } }   // mode 1 = CUSTOM，color = 0xRRGGBB
 *   { }                                        // 没有这个 compound = 跟随车站色（出厂默认）
 * </pre>
 * 只有 CUSTOM 才写 {@code color}：跟随车站色的柱子复制过来也是「跟随车站色」，不写死一个颜色，
 * 否则「批量放置」出来的柱子会停在复制那一刻的线路色上，线路改色就跟不上了。
 *
 * <h2>谁读谁写</h2>
 * <ul>
 *   <li>写：客户端 ctrl+中键（{@code PoleClient#tryCopyProperties}）；</li>
 *   <li>读：服务端放下时（{@link AmtrPoleBottomItem#place}）→ {@code AmtrPoleBottomBlockEntity}；</li>
 *   <li>读（只显示）：物品的 tooltip 与创造的默认条目。</li>
 * </ul>
 * 客户端<b>不</b>拿这个 NBT 去改方块 —— 颜色的写入永远走服务端（C2S 包）。
 */
public final class PoleItemData {

    /** 物品 NBT 的根 compound。 */
    private static final String TAG_ROOT = "LtedPole";

    /** 模式：0 = 跟随车站色，1 = 自定义颜色。与方块实体的常量一致。 */
    private static final String TAG_MODE = "mode";

    /** 自定义颜色（{@code 0xRRGGBB}）。 */
    private static final String TAG_COLOR = "color";

    private PoleItemData() {
    }

    /** 把属性写进物品堆；{@code custom == false} 时<b>删掉</b>整个 compound（= 跟随车站色）。 */
    public static void write(final ItemStack stack, final boolean custom, final int rgb) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        final NbtCompound root = stack.getNbt();
        if (!custom) {
            if (root != null && root.contains(TAG_ROOT)) {
                root.remove(TAG_ROOT);
            }
            return;
        }
        final NbtCompound tag = stack.getOrCreateSubNbt(TAG_ROOT);
        tag.putInt(TAG_MODE, AmtrPoleBottomBlockEntity.MODE_CUSTOM);
        tag.putInt(TAG_COLOR, rgb & 0xFFFFFF);
    }

    /** 这一堆是不是「自定义颜色」模式。没有 NBT = false（跟随车站色）。 */
    public static boolean isCustom(final ItemStack stack) {
        final NbtCompound tag = tag(stack);
        return tag != null && tag.getInt(TAG_MODE) == AmtrPoleBottomBlockEntity.MODE_CUSTOM;
    }

    /** 复制过来的自定义颜色；没有就白色。 */
    public static int color(final ItemStack stack) {
        final NbtCompound tag = tag(stack);
        if (tag == null) {
            return PoleColors.WHITE;
        }
        final int rgb = tag.getInt(TAG_COLOR);
        return rgb == 0 ? PoleColors.WHITE : (rgb & 0xFFFFFF);
    }

    /** 物品 tooltip 的那一行（只有带属性的物品才有）。 */
    public static void appendTooltip(final ItemStack stack, final List<Text> lines) {
        if (!isCustom(stack)) {
            return;
        }
        final int rgb = color(stack);
        lines.add(Text.translatable("tooltip.mtrlte.amtr_pole.custom",
                String.format("#%06X", Integer.valueOf(rgb)))
                .formatted(Formatting.GRAY));
    }

    private static NbtCompound tag(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        final NbtCompound root = stack.getNbt();
        return root == null ? null : root.getCompound(TAG_ROOT);
    }
}
