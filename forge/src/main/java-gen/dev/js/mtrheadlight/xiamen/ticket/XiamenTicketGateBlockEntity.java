package dev.js.mtrheadlight.xiamen.ticket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class XiamenTicketGateBlockEntity extends BlockEntity {
   public XiamenTicketGateBlockEntity(BlockPos pos, BlockState state) {
      super(XiamenTicketGates.entityType(), pos, state == null ? Blocks.AIR.defaultBlockState() : state);
   }
}
