package dev.js.mtrheadlight.xiamen.dep;

import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DepOnWallBlockEntity extends BlockEntityMapper implements DepCountdown.Holder {
   private DepOnWallLock depLock;

   public DepOnWallBlockEntity(BlockPos pos, BlockState state) {
      super(DepOnWallObjects.entityType(), pos, state);
   }

   public DepOnWallLock depLock() {
      return this.depLock;
   }

   public void setDepLock(DepOnWallLock lock) {
      this.depLock = lock;
   }
}
