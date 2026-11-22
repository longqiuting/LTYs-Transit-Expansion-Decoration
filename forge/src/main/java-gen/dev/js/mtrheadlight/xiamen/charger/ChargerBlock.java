package dev.js.mtrheadlight.xiamen.charger;

import dev.js.mtrheadlight.bind.MtrBalance;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChargerBlock extends HorizontalDirectionalBlock implements EntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final int TOP_LUMINANCE = 7;
   private static final float[] FALLBACK_BOX = new float[]{3.0F, 0.0F, 3.0F, 13.0F, 16.0F, 13.0F};
   private final boolean top;
   private final String rawModel;
   private final VoxelShape[] shapes = new VoxelShape[4];
   private final String tooltipKey;

   public ChargerBlock(BlockBehaviour.Properties settings, boolean top, String rawModel, String tooltipKey) {
      super(settings);
      this.top = top;
      this.rawModel = rawModel;
      this.tooltipKey = tooltipKey;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
      float[] bounds = RawModelJson.bounds(RawModelJson.id(rawModel));
      float[] box = bounds == null ? FALLBACK_BOX : bounds;

      for(int i = 0; i < 4; ++i) {
         this.shapes[i] = rotatedBox(box, i);
      }

   }

   public String rawModel() {
      return this.rawModel;
   }

   public boolean isTop() {
      return this.top;
   }

   private static VoxelShape ltedBox(double x1, double x2, double y1, double y2, double z1, double z2) {
      return Block.box(x1, y1, z1, x2, y2, z2);
   }

   private static VoxelShape rotatedBox(float[] b, int quarterTurns) {
      double x1 = (double)b[0];
      double y1 = (double)b[1];
      double z1 = (double)b[2];
      double x2 = (double)b[3];
      double y2 = (double)b[4];
      double z2 = (double)b[5];
      VoxelShape var10000;
      switch (quarterTurns & 3) {
         case 1:
            var10000 = ltedBox(16.0D - z2, 16.0D - z1, y1, y2, x1, x2);
            break;
         case 2:
            var10000 = ltedBox(16.0D - x2, 16.0D - x1, y1, y2, 16.0D - z2, 16.0D - z1);
            break;
         case 3:
            var10000 = ltedBox(z1, z2, y1, y2, 16.0D - x2, 16.0D - x1);
            break;
         default:
            var10000 = ltedBox(x1, x2, y1, y2, z1, z2);
      }

      return var10000;
   }

   private static int quarterTurns(Direction facing) {
      byte var10000;
      switch (facing) {
         case WEST:
            var10000 = 1;
            break;
         case NORTH:
            var10000 = 2;
            break;
         case EAST:
            var10000 = 3;
            break;
         default:
            var10000 = 0;
      }

      return var10000;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapes[quarterTurns((Direction)state.getValue(FACING))];
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapes[quarterTurns((Direction)state.getValue(FACING))];
   }

   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      return Shapes.empty();
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ChargerBlockEntity(pos, state);
   }

   public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (this.top && hand == InteractionHand.MAIN_HAND) {
         if (world.isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof PowerBankItem) {
               if (player.isShiftKeyDown()) {
                  forceBuyout(world, pos, player, held);
               } else {
                  returnPowerBank(world, pos, player, held);
               }

               return InteractionResult.SUCCESS;
            } else if (!held.isEmpty()) {
               player.displayClientMessage(Component.translatable("message.mtrlte.charger.hand_not_empty"), true);
               return InteractionResult.SUCCESS;
            } else {
               rentPowerBank(world, pos, player);
               return InteractionResult.SUCCESS;
            }
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private static void forceBuyout(Level world, BlockPos pos, Player player, ItemStack held) {
      if (PowerBankItem.isDamagedTag(held)) {
         player.displayClientMessage(Component.translatable("message.mtrlte.charger.damaged_not_returnable").withStyle(ChatFormatting.RED), true);
      } else if (!PowerBankItem.isRented(held)) {
         player.displayClientMessage(Component.translatable("message.mtrlte.charger.returned_free", new Object[]{MtrBalance.get(world, player)}), true);
      } else if (PowerBankItem.isOverdueSettled(held)) {
         player.displayClientMessage(Component.translatable("message.mtrlte.charger.overdue_again", new Object[]{PowerBankItem.heldHours(held, System.currentTimeMillis()), 100}).withStyle(ChatFormatting.RED), false);
      } else {
         int fee = 100;
         int before = MtrBalance.get(world, player);
         if (before < 100) {
            player.displayClientMessage(Component.translatable("message.mtrlte.charger.buyout_no_money", new Object[]{100, before}).withStyle(ChatFormatting.RED), false);
         } else {
            int after = MtrBalance.add(world, player, -100);
            if (after != before - 100) {
               player.displayClientMessage(Component.translatable("message.mtrlte.charger.buyout_failed", new Object[]{100}).withStyle(ChatFormatting.RED), false);
            } else {
               PowerBankItem.markOverdueSettled(held);
               ding(world, pos);
               player.displayClientMessage(Component.translatable("message.mtrlte.charger.bought_out", new Object[]{100, after}), false);
            }
         }
      }
   }

   private static void rentPowerBank(Level world, BlockPos pos, Player player) {
      if (findRented(player) != null) {
         player.displayClientMessage(Component.translatable("message.mtrlte.charger.already_rented"), true);
      } else {
         int balance = MtrBalance.get(world, player);
         if (balance < 0) {
            player.displayClientMessage(Component.translatable("message.mtrlte.charger.negative_balance", new Object[]{balance}), true);
         } else {
            ItemStack powerBank = PowerBankItem.rented(ChargerObjects.powerBankItem(), System.currentTimeMillis());
            if (!player.getInventory().add(powerBank)) {
               player.drop(powerBank, false);
            }

            ding(world, pos);
            player.displayClientMessage(Component.translatable("message.mtrlte.charger.rented"), true);
            player.displayClientMessage(Component.translatable("message.mtrlte.charger.rule", new Object[]{5, 30, 2, 48, 96, 24, 100}), false);
         }
      }
   }

   private static void returnPowerBank(Level world, BlockPos pos, Player player, ItemStack held) {
      if (PowerBankItem.isDamagedTag(held)) {
         player.displayClientMessage(Component.translatable("message.mtrlte.charger.damaged_not_returnable").withStyle(ChatFormatting.RED), true);
      } else if (!PowerBankItem.isRented(held)) {
         held.shrink(1);
         ding(world, pos);
         player.displayClientMessage(Component.translatable("message.mtrlte.charger.returned_free", new Object[]{MtrBalance.get(world, player)}), true);
      } else {
         long now = System.currentTimeMillis();
         if (PowerBankItem.isOverdue(held, now)) {
            rejectOverdue(world, player, held, now);
         } else {
            long minutes = PowerBankItem.heldMinutes(held, now);
            long periods = PowerBankItem.billedPeriods(held, now);
            int fee = PowerBankItem.fee(held, now);
            int balance = MtrBalance.add(world, player, -fee);
            held.shrink(1);
            ding(world, pos);
            player.displayClientMessage(Component.translatable("message.mtrlte.charger.returned", new Object[]{minutes, periods, fee, balance}), true);
         }
      }
   }

   private static void rejectOverdue(Level world, Player player, ItemStack held, long now) {
      long hours = PowerBankItem.heldHours(held, now);
      if (PowerBankItem.isOverdueSettled(held)) {
         player.displayClientMessage(Component.translatable("message.mtrlte.charger.overdue_again", new Object[]{hours, 100}).withStyle(ChatFormatting.RED), false);
      } else {
         int before = MtrBalance.get(world, player);
         int after = MtrBalance.add(world, player, -100);
         if (after != before - 100) {
            player.displayClientMessage(Component.translatable("message.mtrlte.charger.overdue_failed", new Object[]{hours, 100}).withStyle(ChatFormatting.RED), false);
         } else {
            PowerBankItem.markOverdueSettled(held);
            player.displayClientMessage(Component.translatable("message.mtrlte.charger.overdue", new Object[]{hours, 100, after}).withStyle(ChatFormatting.RED), false);
         }
      }
   }

   private static void ding(Level world, BlockPos pos) {
      world.playSound((Player)null, pos, (SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.9F, 1.4F);
      world.playSound((Player)null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.7F, 1.2F);
   }

   private static ItemStack findRented(Player player) {
      ItemStack offHand = player.getOffhandItem();
      if (isOwing(offHand)) {
         return offHand;
      } else {
         for(int slot = 0; slot < player.getInventory().getContainerSize(); ++slot) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isOwing(stack)) {
               return stack;
            }
         }

         return null;
      }
   }

   private static boolean isOwing(ItemStack stack) {
      return stack.getItem() instanceof PowerBankItem && PowerBankItem.isRented(stack) && !PowerBankItem.isOverdueSettled(stack) && !PowerBankItem.isDamagedTag(stack);
   }

   public void appendHoverText(ItemStack stack, BlockGetter world, List<Component> tooltip, TooltipFlag context) {
      tooltip.add(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.GRAY));
   }
}
