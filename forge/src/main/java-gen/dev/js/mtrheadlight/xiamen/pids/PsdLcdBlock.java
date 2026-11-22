package dev.js.mtrheadlight.xiamen.pids;

import java.util.List;
import mtr.Items;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityMapper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PsdLcdBlock extends HorizontalDirectionalBlock implements EntityBlock {
   public static final String ID = "psd_lcd_xiamen";
   public static final int MAX_SEGMENTS = 32;
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty HALF_CUT = BooleanProperty.create("half_cut");
   private static final float PANEL_Z0 = 10.0F;
   private static final float PANEL_Z1 = 16.0F;
   public static final float CUT_BOTTOM_Y_PX = 7.5F;

   public PsdLcdBlock(BlockBehaviour.Properties settings) {
      super(settings);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.getStateDefinition().any()).setValue(FACING, Direction.NORTH)).setValue(HALF_CUT, false));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, HALF_CUT});
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return outline((Direction)state.getValue(FACING), isCut(state));
   }

   public static boolean isCut(BlockState state) {
      return state != null && state.hasProperty(HALF_CUT) && state.getValue(HALF_CUT);
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return outline((Direction)state.getValue(FACING), isCut(state));
   }

   private static VoxelShape outline(Direction facing, boolean cut) {
      float y0 = cut ? 7.5F : 0.0F;
      float y1 = 16.0F;
      float z0 = 10.0F;
      float z1 = 16.0F;
      VoxelShape var10000;
      switch (facing == null ? Direction.NORTH : facing) {
         case EAST:
            var10000 = Block.box(0.0D, (double)y0, 0.0D, 6.0D, 16.0D, 16.0D);
            break;
         case SOUTH:
            var10000 = Block.box(0.0D, (double)y0, 0.0D, 16.0D, 16.0D, 6.0D);
            break;
         case WEST:
            var10000 = Block.box(10.0D, (double)y0, 0.0D, 16.0D, 16.0D, 16.0D);
            break;
         default:
            var10000 = Block.box(0.0D, (double)y0, 10.0D, 16.0D, 16.0D, 16.0D);
      }

      return var10000;
   }

   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      return Shapes.empty();
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return true;
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction facing = ctx.getHorizontalDirection().getOpposite();
      return (BlockState)this.defaultBlockState().setValue(FACING, facing);
   }

   public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
      return new PsdLcdBlockEntity(pos, state);
   }

   public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (player.isHolding((Item)Items.BRUSH.get())) {
         if (world.isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            if (player instanceof ServerPlayer) {
               ServerPlayer serverPlayer = (ServerPlayer)player;
               if (ServerPlayNetworking.canSend(serverPlayer, PidsXiamenNet.ID)) {
                  PidsXiamenNet.openScreenAt(serverPlayer, world, anchorOf(world, pos), partnerOf(world, anchorOf(world, pos)));
                  return InteractionResult.SUCCESS;
               }
            }

            return InteractionResult.PASS;
         }
      } else if (player.isHolding(net.minecraft.world.item.Items.SHEARS)) {
         if (world.isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            boolean cut = !isCut(state);
            world.setBlock(pos, (BlockState)state.setValue(HALF_CUT, cut), 3);
            world.playSound((Player)null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.2F);
            return InteractionResult.SUCCESS;
         }
      } else {
         return super.use(state, world, pos, player, hand, hit);
      }
   }

   public static boolean isLcd(BlockState state) {
      return state.getBlock() instanceof PsdLcdBlock;
   }

   public static Direction stepOf(Direction facing) {
      return facing.getClockWise();
   }

   public static BlockPos anchorOf(BlockGetter world, BlockPos pos) {
      Direction facing = (Direction)IBlock.getStatePropertySafe(world, pos, FACING);
      if (facing == null) {
         return pos;
      } else {
         Direction step = stepOf(facing);
         BlockPos cursor = pos;

         for(int i = 0; i < 32; ++i) {
            BlockPos next = cursor.relative(step);
            if (!isLcd(world.getBlockState(next))) {
               return cursor;
            }

            cursor = next;
         }

         return cursor;
      }
   }

   public static int lengthOf(BlockGetter world, BlockPos anchor) {
      Direction facing = (Direction)IBlock.getStatePropertySafe(world, anchor, FACING);
      if (facing == null) {
         return 1;
      } else {
         Direction back = stepOf(facing).getOpposite();
         int length = 1;

         for(BlockPos cursor = anchor; length < 32; ++length) {
            BlockPos next = cursor.relative(back);
            if (!isLcd(world.getBlockState(next))) {
               return length;
            }

            cursor = next;
         }

         return length;
      }
   }

   public static int indexOf(BlockGetter world, BlockPos pos) {
      BlockPos anchor = anchorOf(world, pos);
      Direction facing = (Direction)IBlock.getStatePropertySafe(world, pos, FACING);
      if (facing == null) {
         return 0;
      } else {
         Direction back = stepOf(facing).getOpposite();
         BlockPos cursor = anchor;

         int index;
         for(index = 0; index < 32 && !cursor.equals(pos); ++index) {
            cursor = cursor.relative(back);
         }

         return index;
      }
   }

   public static BlockPos partnerOf(BlockGetter world, BlockPos anchor) {
      Direction facing = (Direction)IBlock.getStatePropertySafe(world, anchor, FACING);
      if (facing == null) {
         return anchor;
      } else {
         BlockPos next = anchor.relative(stepOf(facing).getOpposite());
         return isLcd(world.getBlockState(next)) ? next : anchor;
      }
   }

   public static List<BlockPos> run(BlockGetter world, BlockPos anchor) {
      Direction facing = (Direction)IBlock.getStatePropertySafe(world, anchor, FACING);
      if (facing == null) {
         return List.of(anchor);
      } else {
         Direction back = stepOf(facing).getOpposite();
         int length = lengthOf(world, anchor);
         BlockPos[] out = new BlockPos[length];

         for(int i = 0; i < length; ++i) {
            out[i] = anchor.relative(back, i);
         }

         return List.of(out);
      }
   }
}
