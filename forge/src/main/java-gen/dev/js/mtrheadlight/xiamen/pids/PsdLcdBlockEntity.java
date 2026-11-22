package dev.js.mtrheadlight.xiamen.pids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class PsdLcdBlockEntity extends PidsXiamenBlockEntity {
   private static final int ARRIVALS = 2;

   public PsdLcdBlockEntity(BlockPos pos, BlockState state) {
      super(PsdLcdObjects.entityType(), pos, state, PidsXiamenBlock.Kind.METRO);
   }

   public int getMaxArrivals() {
      return 2;
   }

   public AABB getRenderBoundingBox() {
      return (new AABB(this.worldPosition)).inflate(24.0D, 4.0D, 24.0D);
   }
}
