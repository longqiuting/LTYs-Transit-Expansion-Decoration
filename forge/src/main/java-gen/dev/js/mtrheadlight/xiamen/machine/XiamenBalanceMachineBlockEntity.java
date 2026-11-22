package dev.js.mtrheadlight.xiamen.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class XiamenBalanceMachineBlockEntity extends BlockEntity {
   public XiamenBalanceMachineBlockEntity(BlockPos pos, BlockState state) {
      super(XiamenMachines.balanceEntityType(), pos, state == null ? Blocks.AIR.defaultBlockState() : state);
   }
}
