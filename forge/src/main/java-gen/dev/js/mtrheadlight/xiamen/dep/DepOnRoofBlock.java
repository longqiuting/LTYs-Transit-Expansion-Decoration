package dev.js.mtrheadlight.xiamen.dep;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DepOnRoofBlock extends HorizontalDirectionalBlock implements EntityBlock {
   static final float MODEL_YAW_DEGREES = -45.0F;
   static final double MODEL_PIVOT_X = 8.0D;
   static final double MODEL_PIVOT_Z = 7.15D;
   static final int NORMAL_OFFSET_DEGREES = 135;
   private static final double[][] FRAME = new double[][]{{1.0D, 0.0D, 5.3D, 15.0D, 1.0D, 9.3D}, {1.0D, 9.0D, 5.3D, 15.0D, 10.0D, 9.3D}, {1.0D, 1.0D, 5.3D, 2.0D, 9.0D, 9.3D}, {14.0D, 1.0D, 5.3D, 15.0D, 9.0D, 9.3D}};
   private static final double SLICE = 2.5D;
   private static final VoxelShape[] SHAPES = new VoxelShape[4];

   public DepOnRoofBlock() {
      super(Properties.of().mapColor(MapColor.METAL).strength(1.2F).sound(SoundType.METAL).noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)this.getStateDefinition().any()).setValue(FACING, Direction.NORTH));
   }

   private static List<double[]> frameBoxes() {
      List<double[]> out = new ArrayList();

      for(double[] b : FRAME) {
         double width = b[3] - b[0];
         int slices = Math.max(1, (int)Math.ceil(width / 2.5D));

         for(int i = 0; i < slices; ++i) {
            double x1 = b[0] + width * (double)i / (double)slices;
            double x2 = b[0] + width * (double)(i + 1) / (double)slices;
            out.add(yawedAabb(x1, b[1], b[2], x2, b[4], b[5]));
         }
      }

      return out;
   }

   private static double[] yawedAabb(double x1, double y1, double z1, double x2, double y2, double z2) {
      double rad = Math.toRadians(-45.0D);
      double cos = Math.cos(rad);
      double sin = Math.sin(rad);
      double minX = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE;
      double minZ = Double.MAX_VALUE;
      double maxZ = -Double.MAX_VALUE;

      for(double x : new double[]{x1, x2}) {
         for(double z : new double[]{z1, z2}) {
            double dx = x - 8.0D;
            double dz = z - 7.15D;
            double rx = 8.0D + dx * cos - dz * sin;
            double rz = 7.15D + dx * sin + dz * cos;
            minX = Math.min(minX, rx);
            maxX = Math.max(maxX, rx);
            minZ = Math.min(minZ, rz);
            maxZ = Math.max(maxZ, rz);
         }
      }

      return new double[]{minX, y1, minZ, maxX, y2, maxZ};
   }

   private static VoxelShape shapeOf(List<double[]> boxes, int yRotation) {
      List<VoxelShape> parts = new ArrayList(boxes.size());

      for(double[] b : boxes) {
         double x1 = b[0];
         double x2 = b[3];
         double z1 = b[2];
         double z2 = b[5];
         switch ((yRotation % 360 + 360) % 360) {
            case 90:
               double nx1 = 16.0D - z2;
               double nx2 = 16.0D - z1;
               z1 = x1;
               z2 = x2;
               x1 = nx1;
               x2 = nx2;
               break;
            case 180:
               double nx1 = 16.0D - x2;
               double nx2 = 16.0D - x1;
               double nz1 = 16.0D - z2;
               double nz2 = 16.0D - z1;
               x1 = nx1;
               x2 = nx2;
               z1 = nz1;
               z2 = nz2;
               break;
            case 270:
               double nx1 = z1;
               double nx2 = z2;
               z1 = 16.0D - x2;
               z2 = 16.0D - x1;
               x1 = nx1;
               x2 = nx2;
         }

         parts.add(Block.box(x1, b[1], z1, x2, b[4], z2));
      }

      VoxelShape shape = Shapes.empty();

      for(VoxelShape part : parts) {
         shape = Shapes.or(shape, part);
      }

      return shape;
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

   private static int angleOf(Direction direction) {
      return blockRotation(direction);
   }

   public static Direction facingForNormal(Direction wanted) {
      int wantedAngle = angleOf(wanted);
      int rotation = Math.round((float)(wantedAngle - 135) / 90.0F) * 90;
      rotation = (rotation % 360 + 360) % 360;
      Direction var10000;
      switch (rotation) {
         case 90:
            var10000 = Direction.EAST;
            break;
         case 180:
            var10000 = Direction.SOUTH;
            break;
         case 270:
            var10000 = Direction.WEST;
            break;
         default:
            var10000 = Direction.NORTH;
      }

      return var10000;
   }

   private static VoxelShape shapeFor(Direction facing) {
      return SHAPES[blockRotation(facing) / 90];
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

   public static BlockPos supportPos(BlockPos pos) {
      return pos.above();
   }

   private static boolean hasSupport(LevelReader world, BlockPos pos) {
      BlockPos support = supportPos(pos);
      return world.getBlockState(support).isFaceSturdy(world, support, Direction.DOWN);
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction facing = facingForNormal(ctx.getHorizontalDirection().getOpposite());
      return (BlockState)this.defaultBlockState().setValue(FACING, facing);
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return true;
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
      return direction == Direction.UP && !this.canSurvive(state, world, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DepOnRoofBlockEntity(pos, state == null ? this.defaultBlockState() : state);
   }

   static {
      List<double[]> boxes = frameBoxes();

      for(int i = 0; i < SHAPES.length; ++i) {
         SHAPES[i] = shapeOf(boxes, i * 90);
      }

   }
}
