package dev.js.mtrheadlight.xiamen.pids;

import java.util.List;
import mtr.block.BlockPoleCheckBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PidsXiamenPoleBlock extends BlockPoleCheckBase {
   private static final VoxelShape COLLISION = Block.box(7.5D, 0.0D, 7.5D, 8.5D, 16.0D, 8.5D);
   private static final VoxelShape OUTLINE = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);

   public PidsXiamenPoleBlock(BlockBehaviour.Properties settings) {
      super(settings);
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return OUTLINE;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return COLLISION;
   }

   protected boolean isBlock(Block block) {
      return block instanceof PidsXiamenBlock || block instanceof PidsXiamenPoleBlock;
   }

   protected Component getTooltipBlockText() {
      return Component.translatable("block.mtrlte.pids_xiamen_pole");
   }

   public void appendHoverText(ItemStack stack, BlockGetter world, List<Component> tooltip, TooltipFlag context) {
      tooltip.add(Component.translatable("tooltip.mtrlte.pids_xiamen_pole").withStyle(ChatFormatting.GRAY));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{HorizontalDirectionalBlock.FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return (BlockState)this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, ctx.getHorizontalDirection().getOpposite());
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return true;
   }
}
