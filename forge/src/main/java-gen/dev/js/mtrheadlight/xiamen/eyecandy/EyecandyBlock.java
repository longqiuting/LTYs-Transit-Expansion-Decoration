package dev.js.mtrheadlight.xiamen.eyecandy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EyecandyBlock extends Block implements EntityBlock {
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   private final EyecandyEntry entry;
   private final VoxelShape[] shapes;
   private final boolean collides;

   public EyecandyBlock(BlockBehaviour.Properties settings, EyecandyEntry entry, List<double[]> boxes, boolean collides) {
      super(entry.glow() > 0 ? settings.lightLevel((state) -> entry.glow()) : settings);
      this.entry = entry;
      this.collides = collides;
      this.shapes = new VoxelShape[4];

      for(int i = 0; i < this.shapes.length; ++i) {
         this.shapes[i] = shapeOf(boxes, i * 90);
      }

      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(FACING, Direction.NORTH));
   }

   public EyecandyEntry entry() {
      return this.entry;
   }

   public boolean collides() {
      return this.collides;
   }

   public boolean hasWarningLight() {
      return EyecandyBlocks.isWarningLight(this.entry.key());
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
      return this.hasWarningLight() && (EyecandyBlocks.entityType() == null || type == EyecandyBlocks.entityType()) ? (BlockEntityTicker<T>)(BlockEntityTicker<EyecandyBlockEntity>)EyecandyBlockEntity::tick : null;
   }

   public VoxelShape shape() {
      return this.shapes[0];
   }

   public VoxelShape shapeFor(BlockState state) {
      return this.shapes[blockRotation((Direction)state.getValue(FACING)) / 90];
   }

   protected VoxelShape shapeFor(Direction facing) {
      return this.shapes[blockRotation(facing) / 90];
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return (BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new EyecandyBlockEntity(pos, state);
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapeFor(state);
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.collides ? this.shapeFor(state) : Shapes.empty();
   }

   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      return Shapes.empty();
   }

   static VoxelShape shapeOf(List<double[]> boxes) {
      return shapeOf(boxes, 0);
   }

   static VoxelShape shapeOf(List<double[]> boxes, int yRotation) {
      int steps = (yRotation % 360 + 360) % 360 / 90;
      List<VoxelShape> parts = new ArrayList(boxes.size());

      for(double[] b : boxes) {
         double[] r = rotateBox(b, steps);
         parts.add(Block.box(r[0], r[1], r[2], r[3], r[4], r[5]));
      }

      VoxelShape shape = Shapes.empty();

      for(VoxelShape part : parts) {
         shape = Shapes.or(shape, part);
      }

      return shape;
   }

   static double[] rotateBox(double[] box, int steps) {
      double x1 = box[0];
      double y1 = box[1];
      double z1 = box[2];
      double x2 = box[3];
      double y2 = box[4];
      double z2 = box[5];

      for(int i = 0; i < steps; ++i) {
         double nx1 = 16.0D - z2;
         double nx2 = 16.0D - z1;
         z1 = x1;
         z2 = x2;
         x1 = nx1;
         x2 = nx2;
      }

      return new double[]{x1, y1, z1, x2, y2, z2};
   }

   public static int blockRotation(Direction direction) {
      short var10000;
      switch (direction) {
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
}
