package dev.js.mtrheadlight.xiamen;

import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.BlockPlatform;
import mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

public final class XiamenPlatformDoorType {
   private static final String PSD = "psd";

   private XiamenPlatformDoorType() {
   }

   public static boolean isFullHeightFamily(Block above) {
      return above instanceof XiamenPsdBlocks.Door || above instanceof XiamenPsdBlocks.Glass || above instanceof XiamenPsdBlocks.GlassEnd;
   }

   public static BlockState actualState(BlockGetter world, BlockPos pos, BlockState state) {
      BlockPos abovePos = pos.above();
      BlockState above = world.getBlockState(abovePos);
      Direction facing = (Direction)IBlock.getStatePropertySafe(above, BlockStateProperties.HORIZONTAL_FACING);
      boolean isDoor = above.getBlock() instanceof BlockPSDAPGDoorBase;
      BlockState counterClockwise = world.getBlockState(abovePos.relative(facing.getCounterClockWise()));
      BlockState clockwise = world.getBlockState(abovePos.relative(facing.getClockWise()));
      boolean isDoorCounterClockwise = counterClockwise.getBlock() instanceof BlockPSDAPGDoorBase;
      boolean isDoorClockwise = clockwise.getBlock() instanceof BlockPSDAPGDoorBase;
      int side;
      if (isDoor && isDoorClockwise) {
         side = 2;
      } else if (isDoor && isDoorCounterClockwise) {
         side = 3;
      } else if (isDoorClockwise) {
         side = 1;
         facing = (Direction)IBlock.getStatePropertySafe(clockwise, BlockStateProperties.HORIZONTAL_FACING);
      } else if (isDoorCounterClockwise) {
         side = 4;
         facing = (Direction)IBlock.getStatePropertySafe(counterClockwise, BlockStateProperties.HORIZONTAL_FACING);
      } else {
         side = 0;
      }

      return (BlockState)((BlockState)withDoorType(state, "psd").setValue(BlockStateProperties.HORIZONTAL_FACING, facing)).setValue(BlockPlatform.SIDE, side);
   }

   private static BlockState withDoorType(BlockState state, String name) {
      Property property = BlockPlatform.DOOR_TYPE;

      for(Object candidate : property.getPossibleValues()) {
         if (candidate instanceof Enum && name.equalsIgnoreCase(((Enum)candidate).name())) {
            return (BlockState)state.setValue(property, (Comparable)candidate);
         }
      }

      return state;
   }
}
