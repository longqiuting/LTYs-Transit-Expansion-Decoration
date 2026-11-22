package dev.js.mtrheadlight.xiamen.drinks;

import dev.js.mtrheadlight.bind.MtrBalance;
import dev.js.mtrheadlight.rawmodel.RawModelJson;
import java.util.List;
import mtr.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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

public class DrinksMachineBlock extends HorizontalDirectionalBlock implements EntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty OPEN = BooleanProperty.create("open");
   public static final int TOP_LUMINANCE = 10;
   public static final int BOTTOM_LUMINANCE = 6;
   public static final int PRICE = 3;
   public static final int OPEN_TICKS = 20;
   public static final String TOP_MODEL = "drinks_machine_top";
   public static final String BOTTOM_CLOSED_MODEL = "drinks_machine_bottom_closed";
   public static final String BOTTOM_OPENED_MODEL = "drinks_machine_bottom_opened";
   private static final float[] FALLBACK_BOX = new float[]{0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 16.0F};
   private final VoxelShape[] shapes = new VoxelShape[4];
   private final String tooltipKey;
   private final boolean top;
   private final DrinksMachineBlock.Variant variant;

   public DrinksMachineBlock(BlockBehaviour.Properties settings, boolean top, String tooltipKey) {
      this(settings, top, tooltipKey, DrinksMachineBlock.Variant.SINGLE);
   }

   public DrinksMachineBlock(BlockBehaviour.Properties settings, boolean top, String tooltipKey, DrinksMachineBlock.Variant variant) {
      super(settings);
      this.top = top;
      this.tooltipKey = tooltipKey;
      this.variant = variant == null ? DrinksMachineBlock.Variant.SINGLE : variant;
      BlockState defaults = (BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH);
      this.registerDefaultState(this.top ? defaults : (BlockState)defaults.setValue(OPEN, false));
      float[] raw = this.top ? boxOrFallback("drinks_machine_top") : boxOrFallback("drinks_machine_bottom_closed");
      float dx = this.variant.modelDx();
      float[] box = new float[]{raw[0] + dx, raw[1], raw[2], raw[3] + dx, raw[4], raw[5]};

      for(int i = 0; i < 4; ++i) {
         this.shapes[i] = rotatedBox(box, i);
      }

   }

   public DrinksMachineBlock.Variant variant() {
      return this.variant;
   }

   public float modelDx() {
      return this.variant.modelDx();
   }

   public boolean isTop() {
      return this.top;
   }

   public static String rawModel(BlockState state) {
      if (isTopState(state)) {
         return "drinks_machine_top";
      } else {
         return state.hasProperty(OPEN) && state.getValue(OPEN) ? "drinks_machine_bottom_opened" : "drinks_machine_bottom_closed";
      }
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
      if (!this.top) {
         builder.add(new Property[]{OPEN});
      }

   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapes[quarterTurns((Direction)state.getValue(FACING))];
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapes[quarterTurns((Direction)state.getValue(FACING))];
   }

   private static float[] boxOrFallback(String model) {
      float[] bounds = RawModelJson.bounds(RawModelJson.id(model));
      return bounds == null ? FALLBACK_BOX : bounds;
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
            var10000 = ltedBox(16.0D - z2, y1, x1, 16.0D - z1, y2, x2);
            break;
         case 2:
            var10000 = ltedBox(16.0D - x2, y1, 16.0D - z2, 16.0D - x1, y2, 16.0D - z1);
            break;
         case 3:
            var10000 = ltedBox(z1, y1, 16.0D - x2, z2, y2, 16.0D - x1);
            break;
         default:
            var10000 = ltedBox(x1, y1, z1, x2, y2, z2);
      }

      return var10000;
   }

   private static VoxelShape ltedBox(double x1, double y1, double z1, double x2, double y2, double z2) {
      return Block.box(x1, y1, z1, x2, y2, z2);
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

   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      return Shapes.empty();
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DrinksMachineBlockEntity(pos, state);
   }

   private static boolean free(BlockState state) {
      return state.isAir() || state.canBeReplaced();
   }

   private static boolean isTopState(BlockState state) {
      Block var2 = state.getBlock();
      if (var2 instanceof DrinksMachineBlock block) {
         if (block.top) {
            return true;
         }
      }

      return false;
   }

   private static boolean isBottomState(BlockState state) {
      Block var2 = state.getBlock();
      if (var2 instanceof DrinksMachineBlock block) {
         if (!block.top) {
            return true;
         }
      }

      return false;
   }

   private BlockState bottomState(Direction facing) {
      return (BlockState)((BlockState)DrinksMachineObjects.bottom(this.variant).defaultBlockState().setValue(FACING, facing)).setValue(OPEN, false);
   }

   private BlockState topState(Direction facing) {
      return (BlockState)DrinksMachineObjects.top(this.variant).defaultBlockState().setValue(FACING, facing);
   }

   private boolean isMyTop(BlockState state) {
      Block var3 = state.getBlock();
      if (var3 instanceof DrinksMachineBlock block) {
         if (block.top && block.variant == this.variant) {
            return true;
         }
      }

      return false;
   }

   private boolean isMyBottom(BlockState state) {
      Block var3 = state.getBlock();
      if (var3 instanceof DrinksMachineBlock block) {
         if (!block.top && block.variant == this.variant) {
            return true;
         }
      }

      return false;
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Level world = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      if (pos.getY() >= world.getMaxBuildHeight() - 1) {
         return null;
      } else {
         Direction facing = ctx.getHorizontalDirection().getOpposite();
         if (this.top) {
            return free(world.getBlockState(pos.below())) ? (BlockState)this.defaultBlockState().setValue(FACING, facing) : null;
         } else {
            return !free(world.getBlockState(pos.above())) ? null : (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, facing)).setValue(OPEN, false);
         }
      }
   }

   public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
      if (!world.isClientSide) {
         Direction facing = (Direction)state.getValue(FACING);
         if (this.top) {
            BlockPos below = pos.below();
            if (free(world.getBlockState(below))) {
               world.setBlock(below, this.bottomState(facing), 3);
            }

         } else {
            BlockPos above = pos.above();
            if (!this.isMyTop(world.getBlockState(above)) && free(world.getBlockState(above))) {
               world.setBlock(above, this.topState(facing), 3);
            }

         }
      }
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return pos.getY() < world.getMaxBuildHeight() - 1 && pos.getY() > world.getMinBuildHeight();
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
      if (this.top) {
         if (direction == Direction.DOWN && !this.isMyBottom(neighborState)) {
            return Blocks.AIR.defaultBlockState();
         }
      } else if (direction == Direction.UP && !this.isMyTop(neighborState)) {
         return Blocks.AIR.defaultBlockState();
      }

      return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
   }

   public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResult.PASS;
      } else {
         BlockPos base = this.top ? pos.below() : pos;
         BlockState baseState = world.getBlockState(base);
         if (!isBottomState(baseState)) {
            return InteractionResult.PASS;
         } else {
            ItemStack held = player.getItemInHand(hand);
            if (held.is((Item)Items.BRUSH.get())) {
               if (world.isClientSide) {
                  return InteractionResult.SUCCESS;
               } else {
                  DrinksMachineBlockEntity entity = blockEntity(world, base);
                  if (entity == null) {
                     return InteractionResult.SUCCESS;
                  } else {
                     DrinksMachinePotions.Preset next = entity.getPreset().next();
                     entity.setPreset(next);
                     player.displayClientMessage(Component.translatable("message.mtrlte.drinks_machine.preset", new Object[]{next.displayName()}), true);
                     return InteractionResult.SUCCESS;
                  }
               }
            } else if (!held.isEmpty()) {
               return InteractionResult.PASS;
            } else if (world.isClientSide) {
               return InteractionResult.SUCCESS;
            } else if (baseState.getValue(OPEN)) {
               player.displayClientMessage(Component.translatable("message.mtrlte.drinks_machine.open_busy"), true);
               return InteractionResult.SUCCESS;
            } else {
               int balance = MtrBalance.get(world, player);
               if (balance < 3) {
                  player.displayClientMessage(Component.translatable("message.mtrlte.drinks_machine.no_money", new Object[]{balance, 3}), true);
                  return InteractionResult.SUCCESS;
               } else {
                  DrinksMachineBlockEntity entity = blockEntity(world, base);
                  DrinksMachinePotions.Preset preset = entity == null ? DrinksMachinePotions.Preset.MIXED : entity.getPreset();
                  RandomSource random = world.getRandom();
                  Potion potion = preset.pick(random);
                  ItemStack bottle = new ItemStack(net.minecraft.world.item.Items.POTION);
                  PotionUtils.setPotion(bottle, potion);
                  int after = MtrBalance.add(world, player, -3);
                  if (after != balance - 3) {
                     player.displayClientMessage(Component.translatable("message.mtrlte.drinks_machine.no_money", new Object[]{balance, 3}), true);
                     return InteractionResult.SUCCESS;
                  } else {
                     Component potionName = bottle.getHoverName();
                     if (!player.getInventory().add(bottle)) {
                        player.drop(bottle, false);
                     }

                     world.playSound((Player)null, base, (SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.9F, 1.4F);
                     world.playSound((Player)null, base, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.7F, 1.2F);
                     world.setBlock(base, (BlockState)baseState.setValue(OPEN, true), 3);
                     world.scheduleTick(base, DrinksMachineObjects.bottom(this.variant), 20);
                     player.displayClientMessage(Component.translatable("message.mtrlte.drinks_machine.bought", new Object[]{potionName, after}), true);
                     return InteractionResult.SUCCESS;
                  }
               }
            }
         }
      }
   }

   public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
      if (!this.top && state.hasProperty(OPEN) && state.getValue(OPEN)) {
         world.setBlock(pos, (BlockState)state.setValue(OPEN, false), 3);
      }

   }

   private static DrinksMachineBlockEntity blockEntity(Level world, BlockPos base) {
      BlockEntity var3 = world.getBlockEntity(base);
      DrinksMachineBlockEntity var10000;
      if (var3 instanceof DrinksMachineBlockEntity entity) {
         var10000 = entity;
      } else {
         var10000 = null;
      }

      return var10000;
   }

   public void appendHoverText(ItemStack stack, BlockGetter world, List<Component> tooltip, TooltipFlag context) {
      tooltip.add(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.mtrlte.drinks_machine.price", new Object[]{3}).withStyle(ChatFormatting.GRAY));
   }

   public static enum Variant {
      SINGLE("drinks_machine", 0.0F),
      DOUBLE("drinks_machine_double", -8.0F);

      private final String idBase;
      private final float modelDx;

      private Variant(final String idBase, final float modelDx) {
         this.idBase = idBase;
         this.modelDx = modelDx;
      }

      public String idBase() {
         return this.idBase;
      }

      public float modelDx() {
         return this.modelDx;
      }
   }
}
