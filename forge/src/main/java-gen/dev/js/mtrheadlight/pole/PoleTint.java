// 柱子染色 provider（Forge 侧）—— 已停用（柱底改成普通方块，没有颜色那一圈了）。
package dev.js.mtrheadlight.pole;

import net.minecraft.client.color.block.BlockColors;

/** 停用占位：柱底不再染色，保留方法名以免平台层引用处编译失败。 */
public final class PoleTint {

    private PoleTint() {
    }

    public static void register() {
        // 柱底由带方块实体 + 染色改成普通方块，这里不再注册任何 provider。
    }

    /** {@code PoleForgeClient} 的 {@code RegisterColorHandlersEvent.Block} 回调；现在什么也不做。 */
    public static void onRegisterBlockColours(final BlockColors colors) {
        // 停用：柱底没有 tintindex，也没有方块实体。
    }
}
