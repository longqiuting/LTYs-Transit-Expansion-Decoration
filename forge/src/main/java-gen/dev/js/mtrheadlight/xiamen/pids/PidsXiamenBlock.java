package dev.js.mtrheadlight.xiamen.pids;

import mtr.Items;
import mtr.block.BlockPIDSBaseHorizontal;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityMapper;
import dev.js.mtrheadlight.forge.platform.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PidsXiamenBlock extends BlockPIDSBaseHorizontal {
   private final PidsXiamenBlock.Kind kind;
   private static final ThreadLocal<Boolean> PLAYER_BREAK = ThreadLocal.withInitial(() -> Boolean.FALSE);
   private static boolean clearingPartner;

   public PidsXiamenBlock(PidsXiamenBlock.Kind kind) {
      this.kind = kind;
   }

   public PidsXiamenBlock.Kind kind() {
      return this.kind;
   }

   public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
      return new PidsXiamenBlockEntity(pos, state == null ? this.defaultBlockState() : state);
   }

   public BlockEntityMapper newBlockEntity(BlockPos pos, BlockState state) {
      return this.createBlockEntity(pos, state);
   }

   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      return Shapes.empty();
   }

   public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (player.isHolding((Item)Items.BRUSH.get())) {
         if (world.isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            if (player instanceof ServerPlayer) {
               ServerPlayer serverPlayer = (ServerPlayer)player;
               if (Net.canSend(serverPlayer, PidsXiamenNet.ID)) {
                  PidsXiamenNet.openScreen(serverPlayer, world, pos);
                  return InteractionResult.SUCCESS;
               }
            }

            return super.use(state, world, pos, player, hand, hit);
         }
      } else {
         return super.use(state, world, pos, player, hand, hit);
      }
   }

   public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
      if (!world.isClientSide) {
         PLAYER_BREAK.set(Boolean.TRUE);
         this.dropOne(world, pos, player);
         this.removePartner(world, pos, player);
      }

      super.playerWillDestroy(world, pos, state, player);
   }

   private void dropOne(Level world, BlockPos pos, Player player) {
      if (player == null || !player.getAbilities().instabuild) {
         Block.popResource(world, pos, new ItemStack(this));
      }
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
      super.onRemove(state, world, pos, newState, moved);
      if (!world.isClientSide && !newState.is(this)) {
         if (PLAYER_BREAK.get()) {
            PLAYER_BREAK.set(Boolean.FALSE);
            this.removePartner(world, pos, (Player)null);
         } else {
            if (!clearingPartner && canStoreData(world, pos)) {
               Block.popResource(world, pos, new ItemStack(this));
            }

            this.removePartner(world, pos, (Player)null);
         }
      }
   }

   private void removePartner(Level world, BlockPos pos, Player player) {
      if (!clearingPartner) {
         Direction facing = (Direction)IBlock.getStatePropertySafe(world, pos, HorizontalDirectionalBlock.FACING);
         if (facing != null) {
            BlockPos partnerPos = pos.relative(facing);
            if (world.getBlockState(partnerPos).getBlock() == this) {
               clearingPartner = true;

               try {
                  world.destroyBlock(partnerPos, false, player, 512);
               } catch (Throwable var10) {
                  world.setBlock(partnerPos, Blocks.AIR.defaultBlockState(), 35);
               } finally {
                  clearingPartner = false;
               }

            }
         }
      }
   }

   public static boolean canStoreData(BlockGetter world, BlockPos pos) {
      Direction facing = (Direction)IBlock.getStatePropertySafe(world, pos, HorizontalDirectionalBlock.FACING);
      return facing == Direction.SOUTH || facing == Direction.WEST;
   }

   public static BlockPos dataPos(BlockGetter world, BlockPos pos) {
      if (canStoreData(world, pos)) {
         return pos;
      } else {
         Direction facing = (Direction)IBlock.getStatePropertySafe(world, pos, HorizontalDirectionalBlock.FACING);
         return facing == null ? pos : pos.relative(facing);
      }
   }

   public static enum Kind {
      METRO("pids_xiamen_metro", "block/pids_xiamen_metro"),
      BRT("pids_xiamen_brt", "block/pids_xiamen_brt");

      private final String id;
      private final String texturePath;

      private Kind(final String id, final String texturePath) {
         this.id = id;
         this.texturePath = texturePath;
      }

      public String id() {
         return this.id;
      }

      public ResourceLocation screenTexture() {
         return new ResourceLocation("mtrlte", "textures/" + this.texturePath + ".png");
      }

      public ResourceLocation itemTexture() {
         return new ResourceLocation("mtrlte", this.texturePath.replace("block/", "item/"));
      }
   }
}
