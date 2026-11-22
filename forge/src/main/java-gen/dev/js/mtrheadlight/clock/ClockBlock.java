package dev.js.mtrheadlight.clock;

import mtr.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ClockBlock extends Block implements EntityBlock {
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final VoxelShape SHAPE_NORTH = Block.box(3.0D, 3.0D, 15.0D, 13.0D, 13.0D, 16.0D);
   private static final VoxelShape SHAPE_SOUTH = Block.box(3.0D, 3.0D, 0.0D, 13.0D, 13.0D, 1.0D);
   private static final VoxelShape SHAPE_WEST = Block.box(15.0D, 3.0D, 3.0D, 16.0D, 13.0D, 13.0D);
   private static final VoxelShape SHAPE_EAST = Block.box(0.0D, 3.0D, 3.0D, 1.0D, 13.0D, 13.0D);
   private static final VoxelShape SHAPE_DOWN = Block.box(3.0D, 15.0D, 3.0D, 13.0D, 16.0D, 13.0D);
   private static final VoxelShape SHAPE_UP = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D);

   public ClockBlock() {
      super(Properties.of().mapColor(MapColor.QUARTZ).strength(0.6F).sound(SoundType.WOOD).noOcclusion().dynamicShape());
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.getStateDefinition().any()).setValue(FACING, Direction.NORTH)).setValue(POWERED, Boolean.FALSE));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, POWERED});
   }

   public static VoxelShape shapeFor(Direction facing) {
      switch (facing) {
         case SOUTH:
            return SHAPE_SOUTH;
         case WEST:
            return SHAPE_WEST;
         case EAST:
            return SHAPE_EAST;
         case UP:
            return SHAPE_UP;
         case DOWN:
            return SHAPE_DOWN;
         case NORTH:
         default:
            return SHAPE_NORTH;
      }
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return shapeFor((Direction)state.getValue(FACING));
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return true;
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction facing = ctx.getClickedFace();
      BlockPos pos = ctx.getClickedPos();
      boolean powered = ctx.getLevel().hasNeighborSignal(pos);
      return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, facing)).setValue(POWERED, powered);
   }

   public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos sourcePos, boolean notify) {
      if (!world.isClientSide) {
         boolean powered = world.hasNeighborSignal(pos);
         if (powered != state.getValue(POWERED)) {
            world.setBlock(pos, (BlockState)state.setValue(POWERED, powered), 3);
         }

      }
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
      if (!world.isClientSide && world.hasNeighborSignal(pos) != state.getValue(POWERED)) {
         world.setBlock(pos, (BlockState)state.setValue(POWERED, !state.getValue(POWERED)), 3);
      }

   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ClockBlockEntity(pos, state);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
      return ClockObjects.entityType() != null && type != ClockObjects.entityType() ? null : (BlockEntityTicker<T>)(BlockEntityTicker<ClockBlockEntity>)ClockBlockEntity::tick;
   }

   public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      ItemStack held = player.getItemInHand(hand);
      if (!held.is((Item)Items.BRUSH.get())) {
         return InteractionResult.PASS;
      } else if (!world.isClientSide && player instanceof ServerPlayer) {
         ServerPlayer serverPlayer = (ServerPlayer)player;
         BlockEntity var10 = world.getBlockEntity(pos);
         ClockBlockEntity var10002;
         if (var10 instanceof ClockBlockEntity) {
            ClockBlockEntity entity = (ClockBlockEntity)var10;
            var10002 = entity;
         } else {
            var10002 = null;
         }

         ClockNet.sendOpen(serverPlayer, pos, var10002);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.SUCCESS;
      }
   }
}
