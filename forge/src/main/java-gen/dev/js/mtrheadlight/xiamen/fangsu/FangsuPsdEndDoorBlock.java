package dev.js.mtrheadlight.xiamen.fangsu;

import mtr.block.IBlock;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FangsuPsdEndDoorBlock extends HorizontalDirectionalBlock implements EntityBlock {
   public static final BooleanProperty OPEN = BooleanProperty.create("open");
   private static final int HALVES = 2;
   private static final int FACINGS = 4;
   private final FangsuPsdDef def;
   private final VoxelShape[][] collision;
   private final VoxelShape[][] outline;
   private Item item;

   public FangsuPsdEndDoorBlock(FangsuPsdDef def) {
      super(Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion());
      this.def = def;
      this.collision = new VoxelShape[2][4];
      this.outline = new VoxelShape[2][4];

      for(int half = 0; half < 2; ++half) {
         for(int rotation = 0; rotation < 4; ++rotation) {
            VoxelShape shape = FangsuPsdDoorBlock.buildShape(def.closeBoxes(), half, rotation * 90);
            this.collision[half][rotation] = shape;
            this.outline[half][rotation] = shape;
         }
      }

      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.getStateDefinition().any()).setValue(FACING, Direction.NORTH)).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)).setValue(OPEN, false));
   }

   public FangsuPsdDef def() {
      return this.def;
   }

   public void setItem(Item item) {
      this.item = item;
   }

   public Item asItem() {
      return this.item == null ? Items.AIR : this.item;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new FangsuPsdEndDoorBlock.FangsuPsdEndDoorEntity(pos, state);
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, BlockStateProperties.DOUBLE_BLOCK_HALF, OPEN});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return !IBlock.isReplaceable(ctx, Direction.UP, 2) ? null : (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(FACING, FangsuPsdPlacement.facingOf(ctx))).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)).setValue(OPEN, false);
   }

   public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      if (!world.isClientSide) {
         world.setBlock(pos.above(), (BlockState)state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 3);
         world.updateNeighborsAt(pos, Blocks.AIR);
         state.updateNeighbourShapes(world, pos, 3);
      }
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
      boolean upper = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
      if (upper) {
         if (direction != Direction.DOWN) {
            return state;
         }
      } else if (direction != Direction.UP) {
         return state;
      }

      return !neighborState.is(this) ? Blocks.AIR.defaultBlockState() : state;
   }

   public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
      if (!world.isClientSide && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
         BlockPos down = pos.below();
         BlockState lower = world.getBlockState(down);
         if (lower.is(this) && lower.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            if (player != null && !player.isCreative()) {
               world.destroyBlock(down, true, player);
            } else {
               IBlock.onBreakCreative(world, player, down);
            }
         }
      }

      super.playerWillDestroy(world, pos, state, player);
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return state.getValue(OPEN) ? Shapes.empty() : this.collision[halfIndex(state)][FangsuPsdDoorBlock.shapeQuarterTurns(state)];
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.outline[halfIndex(state)][FangsuPsdDoorBlock.shapeQuarterTurns(state)];
   }

   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      return Shapes.empty();
   }

   private static int halfIndex(BlockState state) {
      return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? 0 : 1;
   }

   public static boolean isOpen(BlockState state) {
      return state.hasProperty(OPEN) && state.getValue(OPEN);
   }

   public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!FangsuPsdInteract.isDriverKey(player.getItemInHand(hand))) {
         return InteractionResult.PASS;
      } else if (world.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         boolean open = !isOpen(state);
         setColumnOpen(world, pos, state, open);
         world.playSound((Player)null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
         return InteractionResult.CONSUME;
      }
   }

   private static void setColumnOpen(Level world, BlockPos pos, BlockState state, boolean open) {
      world.setBlock(pos, (BlockState)state.setValue(OPEN, open), 3);
      BlockPos other = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
      BlockState otherState = world.getBlockState(other);
      if (otherState.is(state.getBlock())) {
         world.setBlock(other, (BlockState)otherState.setValue(OPEN, open), 3);
      }

   }

   public static class FangsuPsdEndDoorEntity extends BlockEntityMapper {
      private final FangsuPsdProgress animation = new FangsuPsdProgress();

      public FangsuPsdEndDoorEntity(BlockPos pos, BlockState state) {
         super(FangsuPsdBlocks.endDoorEntityType(), pos, state == null ? Blocks.AIR.defaultBlockState() : state);
      }

      public float progress(BlockState state, long ticks, float tickDelta) {
         return this.animation.update(FangsuPsdEndDoorBlock.isOpen(state) ? 1.0F : 0.0F, ticks, tickDelta);
      }

      public void writeCompoundTag(CompoundTag nbt) {
      }
   }
}
