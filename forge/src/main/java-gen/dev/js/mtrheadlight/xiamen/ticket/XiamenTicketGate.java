package dev.js.mtrheadlight.xiamen.ticket;

import mtr.block.BlockTicketBarrier;
import mtr.block.IBlock;
import mtr.data.TicketSystem;
import mtr.data.TicketSystem.EnumTicketBarrierOpen;
import mtr.mappings.Utilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class XiamenTicketGate extends BlockTicketBarrier implements EntityBlock {
   private final boolean entrance;
   private final String rawModel;
   private static final double COLLISION_TOP = 24.0D;

   public XiamenTicketGate(boolean isEntrance, String rawModel) {
      super(isEntrance);
      this.entrance = isEntrance;
      this.rawModel = rawModel;
   }

   public static Direction sideOf(Direction facing) {
      return facing.getCounterClockWise();
   }

   public boolean entrance() {
      return this.entrance;
   }

   public String rawModel() {
      return this.rawModel;
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection())).setValue(OPEN, EnumTicketBarrierOpen.CLOSED);
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return IBlock.getVoxelShapeByDirection(12.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D, (Direction)state.getValue(FACING));
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      Direction facing = (Direction)state.getValue(FACING);
      VoxelShape frame = IBlock.getVoxelShapeByDirection(12.0D, 0.0D, 0.0D, 16.0D, 24.0D, 16.0D, facing);
      return ((TicketSystem.EnumTicketBarrierOpen)state.getValue(OPEN)).isOpen() ? frame : Shapes.or(frame, IBlock.getVoxelShapeByDirection(0.0D, 0.0D, 7.0D, 16.0D, 24.0D, 9.0D, facing));
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new XiamenTicketGateBlockEntity(pos, state);
   }

   public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
      if (!world.isClientSide) {
         if (entity instanceof Player) {
            Player player = (Player)entity;
            Direction facing = (Direction)state.getValue(FACING);
            Vec3 local = player.position().subtract((double)pos.getX() + 0.5D, 0.0D, (double)pos.getZ() + 0.5D).yRot((float)Math.toRadians((double)facing.toYRot()));
            TicketSystem.EnumTicketBarrierOpen open = (TicketSystem.EnumTicketBarrierOpen)state.getValue(OPEN);
            if (open.isOpen()) {
               if (local.z > 0.0D) {
                  world.setBlockAndUpdate(pos, (BlockState)state.setValue(OPEN, EnumTicketBarrierOpen.CLOSED));
               }

            } else if (!(local.z >= 0.0D)) {
               TicketSystem.EnumTicketBarrierOpen result = TicketSystem.passThrough(world, pos, player, this.entrance, !this.entrance, XiamenTicketGateSounds.CRT_TICKET, XiamenTicketGateSounds.CRT_TICKET, XiamenTicketGateSounds.CRT_TICKET, XiamenTicketGateSounds.CRT_TICKET, XiamenTicketGateSounds.CRT_TICKET_ERROR, false);
               world.setBlockAndUpdate(pos, (BlockState)state.setValue(OPEN, result));
               if (result != EnumTicketBarrierOpen.CLOSED && !world.getBlockTicks().hasScheduledTick(pos, this)) {
                  Utilities.scheduleBlockTick(world, pos, this, 40);
               }

            }
         }
      }
   }
}
