package dev.js.mtrheadlight.xiamen.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public class XiamenBalanceMachineBlock extends XiamenMachineBlock implements EntityBlock {
   private static final double[] LOWER_BOX = new double[]{1.0D, 15.0D, 0.0D, 13.42D};
   private static final double[] UPPER_BOX = new double[]{1.0D, 15.0D, 0.0D, 11.7D};

   public XiamenBalanceMachineBlock(BlockBehaviour.Properties settings) {
      super(screenGlow(settings), LOWER_BOX, UPPER_BOX, "tooltip.mtrlte.balance_machine_xiamen");
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new XiamenBalanceMachineBlockEntity(pos, state);
   }

   public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResult.PASS;
      } else if (world.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         world.playSound((Player)null, pos, SoundEvent.createVariableRangeEvent(new ResourceLocation("mtrlte", "ticket_processor_entry")), SoundSource.BLOCKS, 0.7F, 1.0F);
         player.displayClientMessage(Component.translatable("message.mtrlte.balance_machine", new Object[]{balanceOf(world, player)}), true);
         return InteractionResult.SUCCESS;
      }
   }

   private static int balanceOf(Level world, Player player) {
      try {
         Scoreboard scoreboard = world.getScoreboard();
         Objective objective = scoreboard.getObjective("mtr_balance");
         if (objective == null) {
            return 0;
         } else {
            Score score = scoreboard.getOrCreatePlayerScore(player.getGameProfile().getName(), objective);
            return score == null ? 0 : score.getScore();
         }
      } catch (Throwable var5) {
         return 0;
      }
   }
}
