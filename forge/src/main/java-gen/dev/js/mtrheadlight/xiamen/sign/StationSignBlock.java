package dev.js.mtrheadlight.xiamen.sign;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StationSignBlock extends HorizontalDirectionalBlock {
   private final double minX;
   private final double maxX;
   private final double minZ;
   private final double maxZ;
   private final boolean wallMounted;
   private final VoxelShape[] shapes = new VoxelShape[4];

   public StationSignBlock(BlockBehaviour.Properties settings, boolean wallMounted) {
      super(settings);
      this.wallMounted = wallMounted;
      if (wallMounted) {
         this.minX = 5.4D;
         this.maxX = 10.6D;
         this.minZ = 0.0D;
         this.maxZ = 16.0D;
      } else {
         this.minX = 0.0D;
         this.maxX = 16.0D;
         this.minZ = 5.4D;
         this.maxZ = 10.6D;
      }

      this.shapes[0] = this.rotatedBox(0);
      this.shapes[1] = this.rotatedBox(90);
      this.shapes[2] = this.rotatedBox(180);
      this.shapes[3] = this.rotatedBox(270);
      this.registerDefaultState((BlockState)((BlockState)this.getStateDefinition().any()).setValue(FACING, Direction.NORTH));
   }

   public boolean isWallMounted() {
      return this.wallMounted;
   }

   private VoxelShape box(double x1, double x2, double z1, double z2) {
      return Block.box(x1, 0.0D, z1, x2, 16.0D, z2);
   }

   private VoxelShape rotatedBox(int yRotation) {
      switch ((yRotation % 360 + 360) % 360) {
         case 0:
         default:
            return this.box(this.minX, this.maxX, this.minZ, this.maxZ);
         case 90:
            return this.box(16.0D - this.maxZ, 16.0D - this.minZ, this.minX, this.maxX);
         case 180:
            return this.box(16.0D - this.maxX, 16.0D - this.minX, 16.0D - this.maxZ, 16.0D - this.minZ);
         case 270:
            return this.box(this.minZ, this.maxZ, 16.0D - this.maxX, 16.0D - this.minX);
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
         default:
            var10000 = 0;
      }

      return var10000;
   }

   private VoxelShape shapeFor(Direction facing) {
      return this.shapes[blockRotation(facing) / 90];
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapeFor((Direction)state.getValue(FACING));
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapeFor((Direction)state.getValue(FACING));
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      if (!this.wallMounted) {
         return (BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
      } else {
         Direction side = ctx.getClickedFace();
         Direction wall = side.getAxis().isHorizontal() ? side.getOpposite() : ctx.getHorizontalDirection().getOpposite();
         return (BlockState)this.defaultBlockState().setValue(FACING, wall);
      }
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return true;
   }
}
