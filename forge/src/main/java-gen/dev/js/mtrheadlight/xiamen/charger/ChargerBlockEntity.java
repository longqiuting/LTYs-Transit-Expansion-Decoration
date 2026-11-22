package dev.js.mtrheadlight.xiamen.charger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ChargerBlockEntity extends BlockEntity {
   public ChargerBlockEntity(BlockPos pos, BlockState state) {
      super(ChargerObjects.blockEntityType(), pos, state == null ? Blocks.AIR.defaultBlockState() : state);
   }
}
