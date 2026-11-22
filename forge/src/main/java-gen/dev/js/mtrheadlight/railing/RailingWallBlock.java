package dev.js.mtrheadlight.railing;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RailingWallBlock extends Block {
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   private final VoxelShape[] shapes = new VoxelShape[4];

   public RailingWallBlock(BlockBehaviour.Properties settings, double[][] modelBoxes) {
      super(settings);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(FACING, Direction.NORTH));
      double[][] floorBoxes = stretchToFloor(modelBoxes);

      for(Direction face : Plane.HORIZONTAL) {
         this.shapes[face.get2DDataValue()] = rotate(floorBoxes, face);
      }

   }

   private static double[][] stretchToFloor(double[][] boxes) {
      double[][] out = new double[boxes.length][];

      for(int i = 0; i < boxes.length; ++i) {
         double[] b = boxes[i];
         out[i] = new double[]{b[0], 0.0D, b[2], b[3], b[4], b[5]};
      }

      return out;
   }

   private static VoxelShape rotate(double[][] boxes, Direction facing) {
      VoxelShape[] parts = new VoxelShape[boxes.length];

      for(int i = 0; i < boxes.length; ++i) {
         double[] b = boxes[i];
         double x1 = b[0];
         double y1 = b[1];
         double z1 = b[2];
         double x2 = b[3];
         double y2 = b[4];
         double z2 = b[5];
         VoxelShape var10002;
         switch (facing) {
            case NORTH:
               var10002 = Block.box(16.0D - z2, y1, x1, 16.0D - z1, y2, x2);
               break;
            case EAST:
               var10002 = Block.box(16.0D - x2, y1, 16.0D - z2, 16.0D - x1, y2, 16.0D - z1);
               break;
            case SOUTH:
               var10002 = Block.box(z1, y1, 16.0D - x2, z2, y2, 16.0D - x1);
               break;
            default:
               var10002 = Block.box(x1, y1, z1, x2, y2, z2);
         }

         parts[i] = var10002;
      }

      return parts.length == 1 ? parts[0] : Shapes.or(parts[0], (VoxelShape[])Arrays.copyOfRange(parts, 1, parts.length));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction side = ctx.getClickedFace();
      Direction facing = side.getAxis().isHorizontal() ? side.getOpposite() : ctx.getHorizontalDirection();
      return (BlockState)this.defaultBlockState().setValue(FACING, facing);
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapes[((Direction)state.getValue(FACING)).get2DDataValue()];
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapes[((Direction)state.getValue(FACING)).get2DDataValue()];
   }
}
