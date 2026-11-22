package dev.js.mtrheadlight.xiamen.dep;

import dev.js.mtrheadlight.rawmodel.RawModelJson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DepPoleBlock extends Block implements EntityBlock {
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final float[] FALLBACK_BOX = new float[]{0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 16.0F};
   private final VoxelShape[] outlines = new VoxelShape[4];

   public DepPoleBlock() {
      super(Properties.of().mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL).noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)this.getStateDefinition().any()).setValue(FACING, Direction.NORTH));
      float[] bounds = RawModelJson.bounds(RawModelJson.id("dep_pole"));
      float[] box = bounds == null ? FALLBACK_BOX : bounds;

      for(int i = 0; i < this.outlines.length; ++i) {
         this.outlines[i] = rotatedBox(box, i);
      }

   }

   private static VoxelShape rotatedBox(float[] b, int quarterTurns) {
      double x1 = (double)b[0];
      double y1 = (double)b[1];
      double z1 = (double)b[2];
      double x2 = (double)b[3];
      double y2 = (double)b[4];
      double z2 = (double)b[5];

      for(int i = 0; i < (quarterTurns & 3); ++i) {
         double nx1 = 16.0D - z2;
         double nx2 = 16.0D - z1;
         z1 = x1;
         z2 = x2;
         x1 = nx1;
         x2 = nx2;
      }

      return Block.box(x1, y1, z1, x2, y2, z2);
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

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.outlines[blockRotation((Direction)state.getValue(FACING)) / 90];
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      return Shapes.empty();
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return (BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return true;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DepPoleBlockEntity(pos, state == null ? this.defaultBlockState() : state);
   }
}
