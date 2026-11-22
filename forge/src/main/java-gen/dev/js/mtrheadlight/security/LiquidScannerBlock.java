package dev.js.mtrheadlight.security;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class LiquidScannerBlock extends HorizontalDirectionalBlock implements EntityBlock {
   public static final EnumProperty<LiquidScannerBlock.Result> RESULT = EnumProperty.create("result", LiquidScannerBlock.Result.class);
   public static final int LAMP_TICKS = 60;
   private static final double MIN_X = 3.0D;
   private static final double MAX_X = 13.0D;
   private static final double MIN_Z = 2.0D;
   private static final double MAX_Z = 14.0D;
   private static final double MIN_Y = 0.0D;
   private static final double MAX_Y = 8.66875D;
   private static final VoxelShape SHAPE_NORTH = rotatedBox(90);
   private static final VoxelShape SHAPE_EAST = rotatedBox(180);
   private static final VoxelShape SHAPE_SOUTH = rotatedBox(270);
   private static final VoxelShape SHAPE_WEST = rotatedBox(0);
   public static final ResourceLocation LIQUID_LAVA = new ResourceLocation("mtrlte", "liquid/lava");
   public static final ResourceLocation LIQUID_WATER = new ResourceLocation("mtrlte", "liquid/water");
   public static final ResourceLocation LIQUID_MILK = new ResourceLocation("mtrlte", "liquid/milk");
   public static final ResourceLocation LIQUID_HONEY = new ResourceLocation("mtrlte", "liquid/honey");
   public static final ResourceLocation LIQUID_COD = new ResourceLocation("mtrlte", "liquid/cod");
   public static final ResourceLocation LIQUID_SALMON = new ResourceLocation("mtrlte", "liquid/salmon");
   public static final ResourceLocation LIQUID_PUFFERFISH = new ResourceLocation("mtrlte", "liquid/pufferfish");
   public static final ResourceLocation LIQUID_TROPICAL_FISH = new ResourceLocation("mtrlte", "liquid/tropical_fish");
   public static final ResourceLocation LIQUID_AXOLOTL = new ResourceLocation("mtrlte", "liquid/axolotl");
   public static final ResourceLocation LIQUID_TADPOLE = new ResourceLocation("mtrlte", "liquid/tadpole");
   public static final List<ResourceLocation> EXTRA_LIQUIDS = List.of(LIQUID_LAVA, LIQUID_WATER, LIQUID_MILK, LIQUID_HONEY, LIQUID_COD, LIQUID_SALMON, LIQUID_PUFFERFISH, LIQUID_TROPICAL_FISH, LIQUID_AXOLOTL, LIQUID_TADPOLE);

   public LiquidScannerBlock() {
      super(Properties.of().mapColor(MapColor.METAL).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.getStateDefinition().any()).setValue(FACING, Direction.NORTH)).setValue(RESULT, LiquidScannerBlock.Result.NONE));
   }

   private static VoxelShape box(double x1, double x2, double z1, double z2) {
      return Block.box(x1, 0.0D, z1, x2, 8.66875D, z2);
   }

   private static VoxelShape rotatedBox(int yRotation) {
      switch ((yRotation % 360 + 360) % 360) {
         case 0:
         default:
            return box(3.0D, 13.0D, 2.0D, 14.0D);
         case 90:
            return box(2.0D, 14.0D, 3.0D, 13.0D);
         case 180:
            return box(3.0D, 13.0D, 2.0D, 14.0D);
         case 270:
            return box(2.0D, 14.0D, 3.0D, 13.0D);
      }
   }

   private static VoxelShape shapeFor(Direction facing) {
      switch (facing) {
         case EAST:
            return SHAPE_EAST;
         case SOUTH:
            return SHAPE_SOUTH;
         case WEST:
            return SHAPE_WEST;
         case NORTH:
         default:
            return SHAPE_NORTH;
      }
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, RESULT});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction facing = ctx.getHorizontalDirection().getOpposite();
      return (BlockState)this.defaultBlockState().setValue(FACING, facing);
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return shapeFor((Direction)state.getValue(FACING));
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return shapeFor((Direction)state.getValue(FACING));
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new LiquidScannerBlockEntity(pos, state);
   }

   public static ResourceLocation liquidId(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         Item item = stack.getItem();
         if (item != Items.POTION && item != Items.SPLASH_POTION && item != Items.LINGERING_POTION) {
            if (item == Items.LAVA_BUCKET) {
               return LIQUID_LAVA;
            } else if (item == Items.WATER_BUCKET) {
               return LIQUID_WATER;
            } else if (item == Items.MILK_BUCKET) {
               return LIQUID_MILK;
            } else if (item == Items.HONEY_BOTTLE) {
               return LIQUID_HONEY;
            } else if (item == Items.COD_BUCKET) {
               return LIQUID_COD;
            } else if (item == Items.SALMON_BUCKET) {
               return LIQUID_SALMON;
            } else if (item == Items.PUFFERFISH_BUCKET) {
               return LIQUID_PUFFERFISH;
            } else if (item == Items.TROPICAL_FISH_BUCKET) {
               return LIQUID_TROPICAL_FISH;
            } else if (item == Items.AXOLOTL_BUCKET) {
               return LIQUID_AXOLOTL;
            } else {
               return item == Items.TADPOLE_BUCKET ? LIQUID_TADPOLE : null;
            }
         } else {
            Potion potion = PotionUtils.getPotion(stack);
            return potion == null ? null : BuiltInRegistries.POTION.getKey(potion);
         }
      } else {
         return null;
      }
   }

   public static boolean isLiquid(ItemStack stack) {
      return liquidId(stack) != null;
   }

   public static boolean isConfiscatedByDefault(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem() == Items.LAVA_BUCKET;
   }

   public static boolean isHarmful(ItemStack stack) {
      return PotionUtils.getMobEffects(stack).stream().map(MobEffectInstance::getEffect).anyMatch((effect) -> effect.getCategory() == MobEffectCategory.HARMFUL);
   }

   private void showLamp(Level world, BlockPos pos, BlockState state, LiquidScannerBlock.Result result) {
      if (world instanceof ServerLevel) {
         world.setBlock(pos, (BlockState)state.setValue(RESULT, result), 3);
         world.scheduleTick(pos, this, 60);
      }
   }

   public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
      if (state.getValue(RESULT) != LiquidScannerBlock.Result.NONE) {
         world.setBlock(pos, (BlockState)state.setValue(RESULT, LiquidScannerBlock.Result.NONE), 3);
      }

   }

   public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      ItemStack held = player.getItemInHand(hand);
      if (held.is((Item)mtr.Items.BRUSH.get())) {
         if (!world.isClientSide && player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            BlockEntity var15 = world.getBlockEntity(pos);
            LiquidScannerBlockEntity var10002;
            if (var15 instanceof LiquidScannerBlockEntity) {
               LiquidScannerBlockEntity entity = (LiquidScannerBlockEntity)var15;
               var10002 = entity;
            } else {
               var10002 = null;
            }

            LiquidScannerNet.sendOpen(serverPlayer, pos, var10002);
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.SUCCESS;
         }
      } else if (!isLiquid(held)) {
         return InteractionResult.PASS;
      } else if (world.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockEntity scanned = world.getBlockEntity(pos);
         LiquidScannerBlockEntity var10000;
         if (scanned instanceof LiquidScannerBlockEntity) {
            LiquidScannerBlockEntity be = (LiquidScannerBlockEntity)scanned;
            var10000 = be;
         } else {
            var10000 = null;
         }

         LiquidScannerBlockEntity entity = var10000;
         boolean confiscate = entity == null ? isHarmful(held) || isConfiscatedByDefault(held) : entity.shouldConfiscate(held);
         if (confiscate) {
            held.shrink(1);
            world.playSound((Player)null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 0.7F);
            player.displayClientMessage(Component.translatable("message.mtrlte.liquid_scanner.confiscated"), true);
            this.showLamp(world, pos, state, LiquidScannerBlock.Result.RED);
            return InteractionResult.CONSUME;
         } else {
            ItemStack scanned = held.copyWithCount(1);
            held.shrink(1);
            if (!player.getInventory().add(scanned)) {
               player.drop(scanned, false);
            }

            world.playSound((Player)null, pos, (SoundEvent)SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1.0F, 1.4F);
            player.displayClientMessage(Component.translatable("message.mtrlte.liquid_scanner.approved"), true);
            this.showLamp(world, pos, state, LiquidScannerBlock.Result.GREEN);
            return InteractionResult.CONSUME;
         }
      }
   }

   public static enum Result implements StringRepresentable {
      NONE("none"),
      GREEN("green"),
      RED("red");

      private final String id;

      private Result(final String id) {
         this.id = id;
      }

      public String getSerializedName() {
         return this.id;
      }
   }
}
