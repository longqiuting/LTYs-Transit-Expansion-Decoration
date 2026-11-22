package dev.js.mtrheadlight.xiamen.sign;

import mtr.block.BlockStationNameEntrance;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WallSignBlock extends BlockStationNameEntrance {
   private final String logo;

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return true;
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction side = ctx.getClickedFace();
      Direction facing = side.getAxis().isHorizontal() ? side.getOpposite() : ctx.getHorizontalDirection().getOpposite();
      return (BlockState)this.defaultBlockState().setValue(FACING, facing);
   }

   public WallSignBlock(BlockBehaviour.Properties settings, String logo) {
      super(settings);
      this.logo = logo == null ? "xiamen_logo" : logo;
   }

   public String logo() {
      return this.logo;
   }

   public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
      return new WallSignBlockEntity(pos, state);
   }

   public BlockEntityMapper newBlockEntity(BlockPos pos, BlockState state) {
      return this.createBlockEntity(pos, state);
   }
}
