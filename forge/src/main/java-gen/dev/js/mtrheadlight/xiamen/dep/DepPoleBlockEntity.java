package dev.js.mtrheadlight.xiamen.dep;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DepPoleBlockEntity extends BlockEntity {
   public DepPoleBlockEntity(BlockPos pos, BlockState state) {
      super(DepPoleObjects.entityType(), pos, state == null ? Blocks.AIR.defaultBlockState() : state);
   }
}
