package dev.js.mtrheadlight.xiamen.fangsu;

import java.util.ArrayList;
import java.util.List;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FangsuPsdDoorBlock extends BlockPSDAPGDoorBase {
   private static final int HALVES = 2;
   private static final int FACINGS = 4;
   private final FangsuPsdDef def;
   private final VoxelShape[][] collision;
   private final VoxelShape[][] outline;
   private Item item;

   public FangsuPsdDoorBlock(FangsuPsdDef def) {
      this.def = def;
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(UNLOCKED, true));
      this.collision = new VoxelShape[2][4];
      this.outline = new VoxelShape[2][4];

      for(int half = 0; half < 2; ++half) {
         for(int rotation = 0; rotation < 4; ++rotation) {
            VoxelShape shape = buildShape(def.closeBoxes(), half, rotation * 90);
            this.collision[half][rotation] = shape;
            this.outline[half][rotation] = shape;
         }
      }

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

   public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
      return new FangsuPsdDoorBlock.FangsuPsdDoorEntity(pos, state);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = super.getStateForPlacement(context);
      return state == null ? null : (BlockState)state.setValue(FACING, FangsuPsdPlacement.facingOf(context));
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
      boolean upper = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER;
      if (upper) {
         if (direction != Direction.DOWN) {
            return state;
         }
      } else if (direction != Direction.UP) {
         return state;
      }

      return !neighborState.is(this) ? Blocks.AIR.defaultBlockState() : state;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return isOpen(world, pos) ? Shapes.empty() : this.collision[halfIndex(state)][shapeQuarterTurns(state)];
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.outline[halfIndex(state)][shapeQuarterTurns(state)];
   }

   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      return Shapes.empty();
   }

   private static boolean isOpen(BlockGetter world, BlockPos pos) {
      BlockEntity entity = world.getBlockEntity(pos);
      if (entity instanceof BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase door) {
         if (door.isOpen()) {
            return true;
         }
      }

      return false;
   }

   private static int halfIndex(BlockState state) {
      return IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.LOWER ? 0 : 1;
   }

   static int shapeQuarterTurns(BlockState state) {
      Direction facing = (Direction)IBlock.getStatePropertySafe(state, FACING);
      byte var10000;
      switch (facing) {
         case EAST:
            var10000 = 3;
            break;
         case SOUTH:
            var10000 = 0;
            break;
         case WEST:
            var10000 = 1;
            break;
         default:
            var10000 = 2;
      }

      return var10000;
   }

   static VoxelShape buildShape(List<double[]> boxes, int half, int yRotation) {
      double lowY = half == 0 ? 0.0D : 16.0D;
      double highY = lowY + 16.0D;
      List<VoxelShape> parts = new ArrayList(boxes.size());

      for(double[] box : boxes) {
         double x1 = box[0];
         double y1 = box[1];
         double z1 = box[2];
         double x2 = box[3];
         double y2 = box[4];
         double z2 = box[5];

         for(int i = 0; i < (yRotation % 360 + 360) % 360 / 90; ++i) {
            double nx1 = 16.0D - z2;
            double nx2 = 16.0D - z1;
            z1 = x1;
            z2 = x2;
            x1 = nx1;
            x2 = nx2;
         }

         x1 = Math.max(0.0D, x1);
         x2 = Math.min(16.0D, x2);
         z1 = Math.max(0.0D, z1);
         z2 = Math.min(16.0D, z2);
         double cy1 = Math.max(y1, lowY);
         double cy2 = Math.min(y2, highY);
         if (!(x2 - x1 < 0.01D) && !(z2 - z1 < 0.01D) && !(cy2 - cy1 < 0.01D)) {
            parts.add(Shapes.box(x1 / 16.0D, (cy1 - lowY) / 16.0D, z1 / 16.0D, x2 / 16.0D, (cy2 - lowY) / 16.0D, z2 / 16.0D));
         }
      }

      return parts.isEmpty() ? Shapes.empty() : Shapes.or(Shapes.empty(), (VoxelShape[])parts.toArray(new VoxelShape[0]));
   }

   public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!FangsuPsdInteract.isBrush(player.getItemInHand(hand))) {
         return super.use(state, world, pos, player, hand, hit);
      } else if (player.isShiftKeyDown()) {
         return super.use(state, world, pos, player, hand, hit);
      } else if (world.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         Boolean on = toggleColumnDoorSound(world, pos, state);
         if (on != null) {
            player.displayClientMessage(Component.translatable(on ? "opt.doorSound.on" : "opt.doorSound.off"), true);
         }

         return InteractionResult.CONSUME;
      }
   }

   private static Boolean toggleColumnDoorSound(Level world, BlockPos pos, BlockState state) {
      BlockPos lower = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
      Boolean now = null;

      for(int i = 0; i < 2; ++i) {
         BlockPos at = i == 0 ? lower : lower.above();
         BlockEntity var8 = world.getBlockEntity(at);
         if (var8 instanceof FangsuPsdDoorBlock.FangsuPsdDoorEntity door) {
            if (now == null) {
               now = !door.doorSoundEnabled();
            }

            door.setDoorSoundEnabled(now);
         }
      }

      return now;
   }

   public static class FangsuPsdDoorEntity extends BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase {
      public static final float MOVE_TICKS = 7.0F;
      private final FangsuPsdProgress animation = new FangsuPsdProgress();
      private static final String DOOR_SOUND_TAG = "lte_door_sound";
      private boolean doorSound = true;
      private static final int NO_OPEN_SEEN = -1;
      private int lastOpen = -1;
      private long lastOpenTick;
      private static final long STALE_TICKS = 40L;
      private boolean closingFired;
      private static boolean playLogged;

      public FangsuPsdDoorEntity(BlockPos pos, BlockState state) {
         super(FangsuPsdBlocks.entityType(), pos, state);
      }

      public float progress(long ticks, float tickDelta) {
         return this.animation.update((float)this.getOpenValue() / 32.0F, ticks, tickDelta);
      }

      public boolean doorSoundEnabled() {
         return this.doorSound;
      }

      public void setDoorSoundEnabled(boolean on) {
         if (this.doorSound != on) {
            this.doorSound = on;
            this.setChanged();
            this.syncData();
         }
      }

      public int tickDoorSound(Level world, BlockPos pos, boolean globalEnabled) {
         if (world != null && pos != null) {
            long ticks = world.getGameTime();
            int now = this.getOpenValue();
            int before = this.lastOpen;
            boolean fresh = before == -1 || ticks - this.lastOpenTick > 40L;
            this.lastOpen = now;
            this.lastOpenTick = ticks;
            if (!fresh && now != before) {
               boolean opening = before == 0 && now > 0;
               boolean closing = before > 0 && now < before && !this.closingFired;
               if (closing) {
                  this.closingFired = true;
               }

               if (now == 0) {
                  this.closingFired = false;
               }

               if (!opening && !closing) {
                  return 0;
               } else if (this.doorSound && globalEnabled) {
                  if (FangsuPsdSounds.BRT_DOOR == null) {
                     return 0;
                  } else {
                     return opening ? 1 : 2;
                  }
               } else {
                  return 0;
               }
            } else {
               return 0;
            }
         } else {
            return 0;
         }
      }

      public void readCompoundTag(CompoundTag nbt) {
         super.readCompoundTag(nbt);
         this.doorSound = !nbt.contains("lte_door_sound") || nbt.getBoolean("lte_door_sound");
      }

      public void writeCompoundTag(CompoundTag nbt) {
         super.writeCompoundTag(nbt);
         nbt.putBoolean("lte_door_sound", this.doorSound);
      }
   }
}
