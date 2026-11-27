// 厦门地铁柱子（柱底）：普通方块。
//
// 曾经带方块实体 + 染色 + 颜色面板 + ctrl+中键复制，但这一块在「带方块实体的方块」上
// 用方块模型渲染时始终画不出来（同一份模型放到不带方块实体的方块上就正常），
// 所以按用户决定：颜色功能整套去掉，改成最普通的方块，模型直接烘焙渲染。
package dev.js.mtrheadlight.pole;

import net.minecraft.block.Block;

/** {@code mtrlte:amtr_pole_bottom} —— 厦门地铁柱子的<b>柱底</b>（普通方块，无方块实体）。 */
public final class AmtrPoleBottomBlock extends Block {

    public AmtrPoleBottomBlock(final Settings settings) {
        super(settings);
    }
}
