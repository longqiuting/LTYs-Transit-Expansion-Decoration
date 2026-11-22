package dev.js.mtrheadlight.xiamen.sign;

import mtr.block.BlockStationNameBase;
import mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class WallSignBlockEntity extends BlockStationNameBase.TileEntityStationNameBase {
   public WallSignBlockEntity(BlockPos pos, BlockState state) {
      super(WallSignObjects.entityType(), pos, state, 0.0F, 0.00625F, false);
   }

   public int getColor(BlockState state) {
      int color = IBlock.getStatePropertySafe(state, BlockStationNameBase.COLOR);
      int var10000;
      switch (color) {
         case 1:
            var10000 = -5592406;
            break;
         case 2:
            var10000 = -16777216;
            break;
         default:
            var10000 = -1;
      }

      return var10000;
   }
}
