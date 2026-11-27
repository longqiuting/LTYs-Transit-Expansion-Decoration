// 柱子染色 provider —— 已停用（柱底改成普通方块，没有颜色那一圈了）。
package dev.js.mtrheadlight.pole;

/** 停用占位：柱底不再染色，保留类名以免其它代码引用处编译失败。 */
public final class PoleTint {

    private PoleTint() {
    }

    public static void register() {
        // 柱底由带方块实体 + 染色改成普通方块，这里不再注册任何 provider。
    }
}
