// 迁移自 dev.js.mtrheadlight.logo.LogoBlock（yarn）—— 只有基类与构造器变了。
package dev.js.mtrheadlight.forge.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 标志方块 {@code mtrlte:lte_logo}。
 *
 * <p>yarn → mojmap 只涉及一处类型：{@code net.minecraft.block.AbstractBlock.Settings}
 * → {@code net.minecraft.world.level.block.state.BlockBehaviour.Properties}。
 * 方块本身没有覆写任何方法，行为和 Fabric 侧一致（满方块 + 四面 logo 覆盖层，
 * 由 {@code assets/mtrlte/models/block/lte_logo.json} 描述）。
 */
public class LogoBlock extends Block {

    public LogoBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }
}
