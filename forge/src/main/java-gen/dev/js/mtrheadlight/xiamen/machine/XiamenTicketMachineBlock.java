package dev.js.mtrheadlight.xiamen.machine;

import mtr.Items;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class XiamenTicketMachineBlock extends XiamenMachineBlock {
   public static final EnumProperty<XiamenTicketMachineBlock.TicketUi> UI = EnumProperty.create("ui", XiamenTicketMachineBlock.TicketUi.class);
   private static final double[] LOWER_BOX = new double[]{0.0D, 16.0D, 0.0D, 16.0D};
   private static final double[] UPPER_BOX = new double[]{0.0D, 16.0D, 0.0D, 14.31D};

   public XiamenTicketMachineBlock(BlockBehaviour.Properties settings) {
      this(settings, LOWER_BOX, UPPER_BOX, "tooltip.mtrlte.ticket_machine_xiamen");
   }

   // 同一条交互框架，但几何包围盒 / 物品提示换成子类自己的那份（港铁那台 z 只到 15.5）。
   protected XiamenTicketMachineBlock(BlockBehaviour.Properties settings, double[] lowerBox, double[] upperBox, String tooltipKey) {
      super(settings.lightLevel((state) -> state.hasProperty(UI) && ((XiamenTicketMachineBlock.TicketUi)state.getValue(UI)).poweredOff() ? 0 : screenGlowOf(state)), lowerBox, upperBox, tooltipKey);
   }

   // 方速界面标题：默认「厦门地铁售票机」，港铁那台覆写。
   protected String screenTitleKey() {
      return "screen.mtrlte.ticket_machine";
   }

   public static XiamenTicketMachineBlock.TicketUi defaultUi() {
      return FangsuTicketUi.loaded() ? XiamenTicketMachineBlock.TicketUi.FANGSU : XiamenTicketMachineBlock.TicketUi.VANILLA;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{UI});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockState placed = super.getStateForPlacement(ctx);
      return placed == null ? null : (BlockState)placed.setValue(UI, defaultUi());
   }

   public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResult.PASS;
      } else {
         BlockPos base = lowerPos(state, pos);
         if (player.isHolding((Item)Items.BRUSH.get())) {
            if (world.isClientSide) {
               return InteractionResult.SUCCESS;
            } else {
               XiamenTicketMachineBlock.TicketUi next = ((XiamenTicketMachineBlock.TicketUi)state.getValue(UI)).next();
               if (next == XiamenTicketMachineBlock.TicketUi.FANGSU && !FangsuTicketUi.loaded()) {
                  setUi(world, base, XiamenTicketMachineBlock.TicketUi.VANILLA);
                  player.displayClientMessage(Component.translatable("message.mtrlte.ticket_machine.no_fangsu"), true);
                  return InteractionResult.SUCCESS;
               } else {
                  setUi(world, base, next);
                  player.displayClientMessage(next.poweredOff() ? Component.translatable("message.mtrlte.ticket_machine.eco") : Component.translatable("message.mtrlte.ticket_machine.ui", new Object[]{Component.translatable("message.mtrlte.ticket_machine.ui." + next.getSerializedName())}), true);
                  return InteractionResult.SUCCESS;
               }
            }
         } else if (((XiamenTicketMachineBlock.TicketUi)state.getValue(UI)).poweredOff()) {
            if (!world.isClientSide) {
               player.displayClientMessage(Component.translatable("message.mtrlte.ticket_machine.powered_off"), true);
            }

            return InteractionResult.SUCCESS;
         } else if (state.getValue(UI) == XiamenTicketMachineBlock.TicketUi.FANGSU) {
            if (world.isClientSide) {
               if (FangsuTicketUi.open(Component.translatable(this.screenTitleKey()), base)) {
                  return InteractionResult.SUCCESS;
               } else {
                  player.displayClientMessage(Component.translatable("message.mtrlte.ticket_machine.client_missing"), true);
                  return InteractionResult.SUCCESS;
               }
            } else {
               if (!FangsuTicketUi.loaded()) {
                  openVanilla(world, player);
               }

               return InteractionResult.SUCCESS;
            }
         } else if (world.isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            openVanilla(world, player);
            return InteractionResult.SUCCESS;
         }
      }
   }

   private static void openVanilla(Level world, Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         PacketTrainDataGuiServer.openTicketMachineScreenS2C(world, serverPlayer);
      }

   }

   private static void setUi(Level world, BlockPos base, XiamenTicketMachineBlock.TicketUi ui) {
      for(BlockPos p : new BlockPos[]{base, base.above()}) {
         BlockState s = world.getBlockState(p);
         if (s.getBlock() instanceof XiamenTicketMachineBlock) {
            world.setBlock(p, (BlockState)s.setValue(UI, ui), 3);
         }
      }

   }

   public static enum TicketUi implements StringRepresentable {
      FANGSU("fangsu"),
      VANILLA("vanilla"),
      ECO("eco");

      private final String name;

      private TicketUi(final String name) {
         this.name = name;
      }

      public String getSerializedName() {
         return this.name;
      }

      public XiamenTicketMachineBlock.TicketUi next() {
         XiamenTicketMachineBlock.TicketUi var10000;
         switch (this.ordinal()) {
            case 0:
               var10000 = VANILLA;
               break;
            case 1:
               var10000 = ECO;
               break;
            case 2:
               var10000 = FANGSU;
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      public boolean poweredOff() {
         return this == ECO;
      }
   }
}
