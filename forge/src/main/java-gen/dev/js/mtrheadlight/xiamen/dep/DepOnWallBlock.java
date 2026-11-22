package dev.js.mtrheadlight.xiamen.dep;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DepOnWallBlock extends HorizontalDirectionalBlock implements EntityBlock {
   private static final double MIN_X = 1.0D;
   private static final double MAX_X = 15.0D;
   private static final double MIN_Y = 3.0D;
   private static final double MAX_Y = 13.0D;
   private static final double MIN_Z = 12.0D;
   private static final double MAX_Z = 16.0D;
   private static final VoxelShape SHAPE_NORTH = rotatedBox(0);
   private static final VoxelShape SHAPE_EAST = rotatedBox(90);
   private static final VoxelShape SHAPE_SOUTH = rotatedBox(180);
   private static final VoxelShape SHAPE_WEST = rotatedBox(270);

   public DepOnWallBlock() {
      super(Properties.of().mapColor(MapColor.METAL).strength(1.2F).sound(SoundType.METAL).noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)this.getStateDefinition().any()).setValue(FACING, Direction.NORTH));
   }

   private static VoxelShape box(double x1, double x2, double z1, double z2) {
      return Block.box(x1, 3.0D, z1, x2, 13.0D, z2);
   }

   private static VoxelShape rotatedBox(int yRotation) {
      switch ((yRotation % 360 + 360) % 360) {
         case 0:
         default:
            return box(1.0D, 15.0D, 12.0D, 16.0D);
         case 90:
            return box(0.0D, 4.0D, 1.0D, 15.0D);
         case 180:
            return box(1.0D, 15.0D, 0.0D, 4.0D);
         case 270:
            return box(12.0D, 16.0D, 1.0D, 15.0D);
      }
   }

   public static int blockRotation(Direction facing) {
      short var10000;
      switch (facing) {
         case EAST:
            var10000 = 90;
            break;
         case SOUTH:
            var10000 = 180;
            break;
         case WEST:
            var10000 = 270;
            break;
         case NORTH:
         case UP:
         case DOWN:
            var10000 = 0;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   private static VoxelShape shapeFor(Direction facing) {
      VoxelShape var10000;
      switch (facing) {
         case EAST:
            var10000 = SHAPE_EAST;
            break;
         case SOUTH:
            var10000 = SHAPE_SOUTH;
            break;
         case WEST:
            var10000 = SHAPE_WEST;
            break;
         case NORTH:
         case UP:
         case DOWN:
            var10000 = SHAPE_NORTH;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return shapeFor((Direction)state.getValue(FACING));
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return shapeFor((Direction)state.getValue(FACING));
   }

   public static BlockPos supportPos(BlockPos pos, Direction facing) {
      return pos.relative(facing.getOpposite());
   }

   private static boolean hasSupport(BlockState state, LevelReader world, BlockPos pos) {
      Direction facing = (Direction)state.getValue(FACING);
      BlockPos support = supportPos(pos, facing);
      return world.getBlockState(support).isFaceSturdy(world, support, facing);
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction side = ctx.getClickedFace();
      Direction facing = side.getAxis().isHorizontal() ? side : ctx.getHorizontalDirection().getOpposite();
      return (BlockState)this.defaultBlockState().setValue(FACING, facing);
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return true;
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
      return direction == ((Direction)state.getValue(FACING)).getOpposite() && !this.canSurvive(state, world, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DepOnWallBlockEntity(pos, state == null ? this.defaultBlockState() : state);
   }
}
