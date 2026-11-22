package dev.js.mtrheadlight.xiamen;

import mtr.block.BlockAPGDoor;
import mtr.block.BlockAPGGlass;
import mtr.block.BlockAPGGlassEnd;
import mtr.block.BlockPSDAPGBase;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.BlockPSDAPGGlassBase;
import mtr.block.BlockPSDAPGGlassEndBase;
import mtr.block.BlockPSDTop;
import mtr.block.IBlock;
import mtr.block.BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide;
import mtr.block.BlockPSDTop.EnumPersistent;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class XiamenPsdBlocks {
   private XiamenPsdBlocks() {
   }

   private static Item itemOrAir(Item item) {
      return item == null ? Items.AIR : item;
   }

   private static BlockState withoutTemp(BlockState state) {
      return state.hasProperty(BlockPSDAPGDoorBase.TEMP) ? (BlockState)state.setValue(BlockPSDAPGDoorBase.TEMP, false) : state;
   }

   public static final class ApgDoor extends BlockAPGDoor implements XiamenPsdBlocks.LineAware {
      private final XiamenLine line;

      public ApgDoor(XiamenLine line) {
         this.line = line;
         this.registerDefaultState(XiamenPsdBlocks.withoutTemp(this.defaultBlockState()));
      }

      public XiamenLine line() {
         return this.line;
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState state = super.getStateForPlacement(context);
         return state == null ? null : XiamenPsdBlocks.withoutTemp(state);
      }

      public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
         return new XiamenPsdBlocks.TileEntityXiamenAPGDoor(pos, state);
      }

      public BlockEntityMapper newBlockEntity(BlockPos pos, BlockState state) {
         return this.createBlockEntity(pos, state);
      }

      public Item asItem() {
         return XiamenPsdBlocks.itemOrAir(XiamenObjects.set(this.line).apgDoorItem);
      }
   }

   public static final class ApgEnd extends BlockAPGGlassEnd implements XiamenPsdBlocks.LineAware {
      private final XiamenLine line;

      public ApgEnd(XiamenLine line) {
         this.line = line;
      }

      public XiamenLine line() {
         return this.line;
      }

      public Item asItem() {
         return XiamenPsdBlocks.itemOrAir(XiamenObjects.set(this.line).apgEndItem);
      }
   }

   public static final class ApgGlass extends BlockAPGGlass implements XiamenPsdBlocks.LineAware {
      private final XiamenLine line;

      public ApgGlass(XiamenLine line) {
         this.line = line;
      }

      public XiamenLine line() {
         return this.line;
      }

      public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
         return new XiamenPsdBlocks.TileEntityXiamenAPGGlass(pos, state);
      }

      public BlockEntityMapper newBlockEntity(BlockPos pos, BlockState state) {
         return this.createBlockEntity(pos, state);
      }

      public Item asItem() {
         return XiamenPsdBlocks.itemOrAir(XiamenObjects.set(this.line).apgGlassItem);
      }
   }

   public static final class Door extends BlockPSDAPGDoorBase implements XiamenPsdBlocks.LineAware {
      private final XiamenLine line;

      public Door(XiamenLine line) {
         this.line = line;
         this.registerDefaultState(XiamenPsdBlocks.withoutTemp(this.defaultBlockState()));
      }

      public XiamenLine line() {
         return this.line;
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         BlockState state = super.getStateForPlacement(context);
         return state == null ? null : XiamenPsdBlocks.withoutTemp(state);
      }

      public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
         return new XiamenPsdBlocks.TileEntityXiamenPSDDoor(pos, state);
      }

      public BlockEntityMapper newBlockEntity(BlockPos pos, BlockState state) {
         return this.createBlockEntity(pos, state);
      }

      public Item asItem() {
         return XiamenPsdBlocks.itemOrAir(XiamenObjects.set(this.line).doorItem);
      }
   }

   public static final class Glass extends BlockPSDAPGGlassBase implements XiamenPsdBlocks.LineAware {
      private final XiamenLine line;

      public Glass(XiamenLine line) {
         this.line = line;
      }

      public XiamenLine line() {
         return this.line;
      }

      public Item asItem() {
         return XiamenPsdBlocks.itemOrAir(XiamenObjects.set(this.line).glassItem);
      }
   }

   public static final class GlassEnd extends BlockPSDAPGGlassEndBase implements XiamenPsdBlocks.LineAware {
      private final XiamenLine line;

      public GlassEnd(XiamenLine line) {
         this.line = line;
      }

      public XiamenLine line() {
         return this.line;
      }

      public Item asItem() {
         return XiamenPsdBlocks.itemOrAir(XiamenObjects.set(this.line).endItem);
      }
   }

   public interface LineAware {
      XiamenLine line();
   }

   public static final class TileEntityXiamenAPGDoor extends BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase {
      public TileEntityXiamenAPGDoor(BlockPos pos, BlockState state) {
         super(XiamenObjects.apgDoorEntityType(), pos, state);
      }
   }

   public static final class TileEntityXiamenAPGGlass extends BlockPSDTop.TileEntityRouteBase {
      public TileEntityXiamenAPGGlass(BlockPos pos, BlockState state) {
         super(XiamenObjects.apgGlassEntityType(), pos, state);
      }
   }

   public static final class TileEntityXiamenPSDDoor extends BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase {
      public TileEntityXiamenPSDDoor(BlockPos pos, BlockState state) {
         super(XiamenObjects.psdDoorEntityType(), pos, state);
      }
   }

   public static final class TileEntityXiamenPSDTop extends BlockPSDTop.TileEntityRouteBase {
      public TileEntityXiamenPSDTop(BlockPos pos, BlockState state) {
         super(XiamenObjects.psdTopEntityType(), pos, state);
      }
   }

   public static final class Top extends BlockPSDTop implements XiamenPsdBlocks.LineAware {
      private final XiamenLine line;

      public Top(XiamenLine line) {
         this.line = line;
      }

      public XiamenLine line() {
         return this.line;
      }

      public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
         return new XiamenPsdBlocks.TileEntityXiamenPSDTop(pos, state);
      }

      public BlockEntityMapper newBlockEntity(BlockPos pos, BlockState state) {
         return this.createBlockEntity(pos, state);
      }

      public Item asItem() {
         return XiamenPsdBlocks.itemOrAir(XiamenObjects.set(this.line).glassItem);
      }

      public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
         return direction == Direction.DOWN && IBlock.getStatePropertySafe(state, PERSISTENT) == EnumPersistent.NONE && !(neighborState.getBlock() instanceof BlockPSDAPGBase) ? Blocks.AIR.defaultBlockState() : actualState(world, pos);
      }

      public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
         Item brush = (Item)mtr.Items.BRUSH.get();
         return IBlock.checkHoldingItem(world, player, (item) -> {
            Direction facing = (Direction)IBlock.getStatePropertySafe(state, FACING);
            if (item == brush) {
               world.setBlockAndUpdate(pos, (BlockState)state.cycle(ARROW_DIRECTION));
               this.propagate(world, pos, facing.getClockWise(), ARROW_DIRECTION, 1);
               this.propagate(world, pos, facing.getCounterClockWise(), ARROW_DIRECTION, 1);
            } else {
               boolean shouldBePersistent = IBlock.getStatePropertySafe(state, PERSISTENT) == EnumPersistent.NONE;
               this.setState(world, pos, shouldBePersistent);
               this.propagate(world, pos, facing.getClockWise(), (offsetPos) -> this.setState(world, offsetPos, shouldBePersistent), 1);
               this.propagate(world, pos, facing.getCounterClockWise(), (offsetPos) -> this.setState(world, offsetPos, shouldBePersistent), 1);
            }

         }, (Runnable)null, new Item[]{brush, Items.SHEARS});
      }

      private void setState(Level world, BlockPos pos, boolean persistent) {
         Block blockBelow = world.getBlockState(pos.below()).getBlock();
         if (blockBelow instanceof BlockPSDAPGBase) {
            BlockPSDTop.EnumPersistent value;
            if (!persistent) {
               value = EnumPersistent.NONE;
            } else if (blockBelow instanceof BlockPSDAPGDoorBase) {
               value = EnumPersistent.ARROW;
            } else if (blockBelow instanceof BlockPSDAPGGlassEndBase) {
               value = EnumPersistent.BLANK;
            } else {
               value = EnumPersistent.ROUTE;
            }

            world.setBlockAndUpdate(pos, (BlockState)world.getBlockState(pos).setValue(PERSISTENT, value));
         }
      }

      public static BlockState actualState(BlockGetter world, BlockPos pos) {
         Direction facing = null;
         IBlock.EnumSide side = null;
         boolean airLeft = false;
         boolean airRight = false;
         BlockState stateBelow = world.getBlockState(pos.below());
         Block blockBelow = stateBelow.getBlock();
         if (blockBelow instanceof BlockPSDAPGBase) {
            side = blockBelow instanceof BlockPSDAPGDoorBase ? (IBlock.EnumSide)IBlock.getStatePropertySafe(stateBelow, IBlock.SIDE) : (IBlock.EnumSide)IBlock.getStatePropertySafe(stateBelow, IBlock.SIDE_EXTENDED);
            if (blockBelow instanceof BlockPSDAPGGlassEndBase) {
               if (IBlock.getStatePropertySafe(stateBelow, BlockPSDAPGGlassEndBase.TOUCHING_LEFT) == EnumPSDAPGGlassEndSide.AIR) {
                  airLeft = true;
               }

               if (IBlock.getStatePropertySafe(stateBelow, BlockPSDAPGGlassEndBase.TOUCHING_RIGHT) == EnumPSDAPGGlassEndSide.AIR) {
                  airRight = true;
               }
            }

            facing = (Direction)IBlock.getStatePropertySafe(stateBelow, FACING);
         }

         BlockState oldState = world.getBlockState(pos);
         BlockState base = oldState.getBlock() instanceof XiamenPsdBlocks.Top ? oldState : XiamenObjects.set(XiamenObjects.lineOf(blockBelow)).top.defaultBlockState();
         BlockState newState = (BlockState)((BlockState)base.setValue(AIR_LEFT, airLeft)).setValue(AIR_RIGHT, airRight);
         if (facing != null) {
            newState = (BlockState)newState.setValue(FACING, facing);
         }

         if (side != null) {
            newState = (BlockState)newState.setValue(SIDE_EXTENDED, side);
         }

         return newState;
      }
   }
}
