package dev.js.mtrheadlight.xiamen.machine;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class XiamenMachineBlock extends HorizontalDirectionalBlock {
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
   private final VoxelShape[] lowerShapes = new VoxelShape[4];
   private final VoxelShape[] upperShapes = new VoxelShape[4];
   private final String tooltipKey;
   protected static final int SCREEN_GLOW_UPPER = 8;
   protected static final int SCREEN_GLOW_LOWER = 6;

   protected static BlockBehaviour.Properties screenGlow(BlockBehaviour.Properties settings) {
      return settings.lightLevel(XiamenMachineBlock::screenGlowOf);
   }

   protected static int screenGlowOf(BlockState state) {
      return state.hasProperty(HALF) && state.getValue(HALF) == DoubleBlockHalf.UPPER ? 8 : 6;
   }

   protected XiamenMachineBlock(BlockBehaviour.Properties settings, double[] lowerBox, double[] upperBox, String tooltipKey) {
      super(settings);

      for(int i = 0; i < 4; ++i) {
         this.lowerShapes[i] = rotatedBox(lowerBox, i * 90);
         this.upperShapes[i] = rotatedBox(upperBox, i * 90);
      }

      this.tooltipKey = tooltipKey;
   }

   private static VoxelShape ltedBox(double x1, double x2, double y1, double y2, double z1, double z2) {
      return Block.box(x1, y1, z1, x2, y2, z2);
   }

   private static VoxelShape rotatedBox(double[] b, int yRotation) {
      double minX = b[0];
      double maxX = b[1];
      double minZ = b[2];
      double maxZ = b[3];
      VoxelShape var10000;
      switch ((yRotation % 360 + 360) % 360) {
         case 90:
            var10000 = ltedBox(16.0D - maxZ, 16.0D - minZ, 0.0D, 16.0D, minX, maxX);
            break;
         case 180:
            var10000 = ltedBox(16.0D - maxX, 16.0D - minX, 0.0D, 16.0D, 16.0D - maxZ, 16.0D - minZ);
            break;
         case 270:
            var10000 = ltedBox(minZ, maxZ, 0.0D, 16.0D, 16.0D - maxX, 16.0D - minX);
            break;
         default:
            var10000 = ltedBox(minX, maxX, 0.0D, 16.0D, minZ, maxZ);
      }

      return var10000;
   }

   public static int blockRotation(Direction facing) {
      short var10000;
      switch (facing) {
         case WEST:
            var10000 = 90;
            break;
         case NORTH:
            var10000 = 180;
            break;
         case EAST:
            var10000 = 270;
            break;
         default:
            var10000 = 0;
      }

      return var10000;
   }

   private VoxelShape shapeFor(BlockState state) {
      int index = blockRotation((Direction)state.getValue(FACING)) / 90;
      return state.getValue(HALF) == DoubleBlockHalf.UPPER ? this.upperShapes[index] : this.lowerShapes[index];
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FACING});
      builder.add(new Property[]{HALF});
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapeFor(state);
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapeFor(state);
   }

   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      return Shapes.empty();
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockPos pos = ctx.getClickedPos();
      Level world = ctx.getLevel();
      if (pos.getY() >= world.getMaxBuildHeight() - 1) {
         return null;
      } else {
         return !world.getBlockState(pos.above()).canBeReplaced(ctx) ? null : (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite())).setValue(HALF, DoubleBlockHalf.LOWER);
      }
   }

   public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
      world.setBlock(pos.above(), (BlockState)state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
         return this.isHalf(world.getBlockState(pos.below()), DoubleBlockHalf.LOWER);
      } else {
         return pos.getY() < world.getMaxBuildHeight() - 1;
      }
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
      if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
         if (direction == Direction.DOWN && !this.isHalf(neighborState, DoubleBlockHalf.LOWER)) {
            return Blocks.AIR.defaultBlockState();
         }
      } else if (direction == Direction.UP && !this.isHalf(neighborState, DoubleBlockHalf.UPPER)) {
         return Blocks.AIR.defaultBlockState();
      }

      return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
   }

   public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
      if (!world.isClientSide) {
         boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
         BlockPos partnerPos = lower ? pos.above() : pos.below();
         if (this.isHalf(world.getBlockState(partnerPos), lower ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER)) {
            world.setBlock(partnerPos, Blocks.AIR.defaultBlockState(), 3);
         }

         if (player == null || !player.isCreative()) {
            popResource(world, lower ? pos : partnerPos, new ItemStack(this));
         }
      }

      super.playerWillDestroy(world, pos, state, player);
   }

   private boolean isHalf(BlockState state, DoubleBlockHalf half) {
      return state.is(this) && state.getValue(HALF) == half;
   }

   protected static BlockPos lowerPos(BlockState state, BlockPos pos) {
      return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
   }

   public void appendHoverText(ItemStack stack, BlockGetter world, List<Component> tooltip, TooltipFlag context) {
      if (this.tooltipKey != null) {
         tooltip.add(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.GRAY));
      }

   }
}
