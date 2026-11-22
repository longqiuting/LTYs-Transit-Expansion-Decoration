package dev.js.mtrheadlight.xiamen;

import java.util.List;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.IBlock;
import mtr.block.IBlock.EnumSide;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

public class PsdAPGItem extends BlockItem {
   private final PsdAPGItem.Kind kind;

   public PsdAPGItem(Block block, PsdAPGItem.Kind kind, Item.Properties settings) {
      super(block, settings);
      this.kind = kind;
   }

   public InteractionResult useOn(UseOnContext context) {
      int horizontalBlocks = this.kind.door ? 2 : 1;
      int checkHeight = this.kind.topPanel ? 3 : 2;
      Block baseBlock = this.getBlock();
      if (blocksNotReplaceable(context, horizontalBlocks, checkHeight, baseBlock)) {
         return InteractionResult.FAIL;
      } else {
         Level world = context.getLevel();
         Direction playerFacing = context.getHorizontalDirection();
         BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

         for(int x = 0; x < horizontalBlocks; ++x) {
            BlockPos newPos = pos.relative(playerFacing.getClockWise(), x);

            for(int y = 0; y < 2; ++y) {
               BlockState state = (BlockState)((BlockState)baseBlock.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, playerFacing)).setValue(IBlock.HALF, y == 1 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
               if (this.kind.door) {
                  world.setBlockAndUpdate(newPos.above(y), (BlockState)((BlockState)state.setValue(IBlock.SIDE, x == 0 ? EnumSide.LEFT : EnumSide.RIGHT)).setValue(BlockPSDAPGDoorBase.UNLOCKED, true));
               } else {
                  world.setBlockAndUpdate(newPos.above(y), (BlockState)state.setValue(IBlock.SIDE_EXTENDED, EnumSide.SINGLE));
               }
            }

            if (this.kind.topPanel) {
               world.setBlockAndUpdate(newPos.above(2), XiamenPsdBlocks.Top.actualState(world, newPos.above(2)));
            }
         }

         context.getItemInHand().shrink(1);
         return InteractionResult.SUCCESS;
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
      tooltip.add(Component.translatable(this.kind.translationKey()).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.mtrlte.texture_source").withStyle(ChatFormatting.DARK_GRAY));
   }

   public static boolean blocksNotReplaceable(UseOnContext context, int width, int height, Block blacklistBlock) {
      Direction facing = context.getHorizontalDirection();
      Level world = context.getLevel();
      BlockPos startingPos = context.getClickedPos().relative(context.getClickedFace());

      for(int x = 0; x < width; ++x) {
         BlockPos offsetPos = startingPos.relative(facing.getClockWise(), x);
         if (blacklistBlock != null) {
            boolean blacklistedBelow = world.getBlockState(offsetPos.below()).is(blacklistBlock);
            boolean blacklistedAbove = world.getBlockState(offsetPos.above(height)).is(blacklistBlock);
            if (blacklistedBelow || blacklistedAbove) {
               return true;
            }
         }

         for(int y = 0; y < height; ++y) {
            if (!world.getBlockState(offsetPos.above(y)).canBeReplaced()) {
               return true;
            }
         }
      }

      return false;
   }

   public static enum Kind {
      DOOR("psd_apg_door", true, true),
      GLASS("psd_apg_glass", false, true),
      END("psd_apg_glass_end", false, true),
      APG_DOOR("apg_door", true, false),
      APG_GLASS("apg_glass", false, false),
      APG_END("apg_glass_end", false, false);

      private final String name;
      private final boolean door;
      private final boolean topPanel;

      private Kind(String name, boolean door, boolean topPanel) {
         this.name = name;
         this.door = door;
         this.topPanel = topPanel;
      }

      public String translationKey() {
         return "tooltip.mtrlte." + this.name;
      }
   }
}
