package dev.js.mtrheadlight.xiamen.fangsu;

import java.util.ArrayList;
import java.util.List;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FangsuPsdGlassBlock extends HorizontalDirectionalBlock implements EntityBlock {
   public static final IntegerProperty FORM_LEFT = IntegerProperty.create("form_left", 0, 3);
   public static final IntegerProperty FORM_RIGHT = IntegerProperty.create("form_right", 0, 3);
   public static final BooleanProperty AUTO_LEFT = BooleanProperty.create("auto_left");
   public static final BooleanProperty AUTO_RIGHT = BooleanProperty.create("auto_right");
   private static final int MAX_FORMS = 4;
   private static final int FACINGS = 4;
   private static final int MAX_RUN = 64;
   private final FangsuPsdGlassDef def;
   private final VoxelShape[][][] shapes;
   private Item item;

   public FangsuPsdGlassBlock(FangsuPsdGlassDef def) {
      super(Properties.of().mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.GLASS).noOcclusion());
      this.def = def;
      int leftForms = Math.min(4, def.left().size());
      int rightForms = Math.min(4, def.right().size());
      this.shapes = new VoxelShape[leftForms][rightForms][4];

      for(int left = 0; left < leftForms; ++left) {
         for(int right = 0; right < rightForms; ++right) {
            for(int rotation = 0; rotation < 4; ++rotation) {
               this.shapes[left][right][rotation] = buildShape(def.leftForm(left).shape(), def.rightForm(right).shape(), rotation);
            }
         }
      }

      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.getStateDefinition().any()).setValue(FACING, Direction.NORTH)).setValue(FORM_LEFT, Math.min(3, def.defaultLeft()))).setValue(FORM_RIGHT, Math.min(3, def.defaultRight()))).setValue(AUTO_LEFT, true)).setValue(AUTO_RIGHT, true));
   }

   public FangsuPsdGlassDef def() {
      return this.def;
   }

   public int leftFormOf(BlockState state) {
      return clampForm(state.hasProperty(FORM_LEFT) ? state.getValue(FORM_LEFT) : this.def.defaultLeft(), this.def.left().size());
   }

   public int rightFormOf(BlockState state) {
      return clampForm(state.hasProperty(FORM_RIGHT) ? state.getValue(FORM_RIGHT) : this.def.defaultRight(), this.def.right().size());
   }

   public static boolean isAutoLeft(BlockState state) {
      return state.hasProperty(AUTO_LEFT) && state.getValue(AUTO_LEFT);
   }

   public static boolean isAutoRight(BlockState state) {
      return state.hasProperty(AUTO_RIGHT) && state.getValue(AUTO_RIGHT);
   }

   public String leftKeyOf(BlockState state) {
      return isAutoLeft(state) ? "auto" : this.def.leftForm(this.leftFormOf(state)).key();
   }

   public String rightKeyOf(BlockState state) {
      return isAutoRight(state) ? "auto" : this.def.rightForm(this.rightFormOf(state)).key();
   }

   private static int clampForm(int stored, int forms) {
      return Math.max(0, Math.min(Math.min(4, forms) - 1, stored));
   }

   public void setItem(Item item) {
      this.item = item;
   }

   public Item asItem() {
      return this.item == null ? Items.AIR : this.item;
   }

   private static VoxelShape buildShape(List<double[]> left, List<double[]> right, int quarters) {
      List<VoxelShape> parts = new ArrayList(left.size() + right.size());
      addBoxes(parts, left, quarters);
      addBoxes(parts, right, quarters);
      return parts.isEmpty() ? Shapes.empty() : Shapes.or(Shapes.empty(), (VoxelShape[])parts.toArray(new VoxelShape[0]));
   }

   private static void addBoxes(List<VoxelShape> parts, List<double[]> boxes, int quarters) {
      for(double[] box : boxes) {
         double x1 = box[0];
         double y1 = box[1];
         double z1 = box[2];
         double x2 = box[3];
         double y2 = box[4];
         double z2 = box[5];

         for(int i = 0; i < quarters; ++i) {
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
         if (!(x2 - x1 < 0.01D) && !(z2 - z1 < 0.01D) && !(y2 - y1 < 0.01D)) {
            parts.add(Shapes.box(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D));
         }
      }

   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapeFor(state);
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.shapeFor(state);
   }

   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      return Shapes.empty();
   }

   private VoxelShape shapeFor(BlockState state) {
      int left = Math.min(this.shapes.length - 1, this.leftFormOf(state));
      int right = Math.min(this.shapes[left].length - 1, this.rightFormOf(state));
      return this.shapes[left][right][shapeQuarterTurns(state)];
   }

   private static int shapeQuarterTurns(BlockState state) {
      byte var10000;
      switch ((Direction)state.getValue(FACING)) {
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

   private static Direction leftOf(BlockState state) {
      return ((Direction)state.getValue(FACING)).getCounterClockWise();
   }

   private static Direction rightOf(BlockState state) {
      return ((Direction)state.getValue(FACING)).getClockWise();
   }

   private static String kindAt(BlockGetter world, BlockPos pos, Direction facing) {
      BlockState state = world.getBlockState(pos);
      if (state.getBlock() instanceof FangsuPsdGlassBlock) {
         return state.hasProperty(FACING) && state.getValue(FACING) == facing ? "SCREENDOOR_GLASS" : "NULL";
      } else if (!(state.getBlock() instanceof FangsuPsdDoorBlock)) {
         return "NULL";
      } else {
         return state.hasProperty(FACING) && state.getValue(FACING) != facing ? "NULL" : "SCREENDOOR_DOOR";
      }
   }

   private int resolveAuto(BlockState state, BlockGetter world, BlockPos pos, boolean left) {
      Direction facing = (Direction)state.getValue(FACING);
      String leftBlock = kindAt(world, pos.relative(facing.getCounterClockWise()), facing);
      String rightBlock = kindAt(world, pos.relative(facing.getClockWise()), facing);

      for(FangsuPsdGlassDef.AutoRule rule : this.def.auto()) {
         if (rule.matches(leftBlock, rightBlock)) {
            String key = left ? rule.left() : rule.right();
            if (key != null) {
               int index = left ? this.def.leftIndexOf(key) : this.def.rightIndexOf(key);
               if (index >= 0) {
                  return index;
               }
            }
         }
      }

      return left ? this.def.defaultLeft() : this.def.defaultRight();
   }

   BlockState relink(BlockState state, BlockGetter world, BlockPos pos) {
      if (world != null && (isAutoLeft(state) || isAutoRight(state))) {
         BlockState out = state;
         if (isAutoLeft(state)) {
            out = (BlockState)state.setValue(FORM_LEFT, clampForm(this.resolveAuto(state, world, pos, true), this.def.left().size()));
         }

         if (isAutoRight(state)) {
            out = (BlockState)out.setValue(FORM_RIGHT, clampForm(this.resolveAuto(state, world, pos, false), this.def.right().size()));
         }

         return out;
      } else {
         return state;
      }
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
      return direction != leftOf(state) && direction != rightOf(state) ? state : this.relink(state, world, pos);
   }

   private static int refreshRun(Level world, BlockPos pos, boolean forceAuto) {
      BlockState start = world.getBlockState(pos);
      if (!(start.getBlock() instanceof FangsuPsdGlassBlock)) {
         return 0;
      } else {
         Direction facing = (Direction)start.getValue(FACING);
         int changed = applyRelink(world, pos, forceAuto) ? 1 : 0;

         for(Direction side : new Direction[]{leftOf(start), rightOf(start)}) {
            BlockPos at = pos;

            for(int i = 0; i < 64; ++i) {
               BlockPos next = at.relative(side);
               BlockState nextState = world.getBlockState(next);
               if (!(nextState.getBlock() instanceof FangsuPsdGlassBlock) || !nextState.hasProperty(FACING) || nextState.getValue(FACING) != facing) {
                  break;
               }

               if (applyRelink(world, next, forceAuto)) {
                  ++changed;
               }

               at = next;
            }
         }

         return changed;
      }
   }

   private static boolean applyRelink(Level world, BlockPos pos, boolean forceAuto) {
      BlockState state = world.getBlockState(pos);
      Block base = state.getBlock();
      if (base instanceof FangsuPsdGlassBlock glass) {
         BlockState baseLted = forceAuto ? (BlockState)((BlockState)state.setValue(AUTO_LEFT, true)).setValue(AUTO_RIGHT, true) : state;
         BlockState relinked = glass.relink(baseLted, world, pos);
         if (relinked == state) {
            return false;
         } else {
            world.setBlock(pos, relinked, 3);
            return true;
         }
      } else {
         return false;
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockState placed = (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(FACING, FangsuPsdPlacement.facingOf(ctx))).setValue(AUTO_LEFT, true)).setValue(AUTO_RIGHT, true);
      return this.relink(placed, ctx.getLevel(), ctx.getClickedPos());
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, FORM_LEFT, FORM_RIGHT, AUTO_LEFT, AUTO_RIGHT});
   }

   public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!FangsuPsdInteract.isBrush(player.getItemInHand(hand))) {
         return InteractionResult.PASS;
      } else if (world.isClientSide) {
         return InteractionResult.SUCCESS;
      } else if (player.isShiftKeyDown()) {
         int changed = refreshRun(world, pos, true);
         world.playSound((Player)null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, 1.4F);
         player.displayClientMessage(Component.translatable("block.mtrlte.fangsu_psd_glass_auto.reconnect", new Object[]{changed}), true);
         return InteractionResult.CONSUME;
      } else {
         boolean leftHalf = hitLeftHalf(state, hit, pos);
         BlockState next = this.cycleHalf(state, leftHalf);
         world.setBlock(pos, next, 3);
         refreshRun(world, pos, false);
         world.playSound((Player)null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, 1.0F + (leftHalf ? 0.0F : 0.1F));
         player.displayClientMessage(Component.translatable(leftHalf ? "block.mtrlte.fangsu_psd_glass_half.left" : "block.mtrlte.fangsu_psd_glass_half.right").append(Component.literal(": ")).append(Component.translatable(FangsuPsdGlassDef.langKey(leftHalf ? this.leftKeyOf(next) : this.rightKeyOf(next)))), true);
         return InteractionResult.CONSUME;
      }
   }

   private static boolean hitLeftHalf(BlockState state, BlockHitResult hit, BlockPos pos) {
      Direction left = leftOf(state);
      Vec3 at = hit.getLocation();
      double along = (at.x - ((double)pos.getX() + 0.5D)) * (double)left.getStepX() + (at.z - ((double)pos.getZ() + 0.5D)) * (double)left.getStepZ();
      return along >= 0.0D;
   }

   private BlockState cycleHalf(BlockState state, boolean leftHalf) {
      int count = Math.min(4, (leftHalf ? this.def.left() : this.def.right()).size());
      IntegerProperty form = leftHalf ? FORM_LEFT : FORM_RIGHT;
      BooleanProperty auto = leftHalf ? AUTO_LEFT : AUTO_RIGHT;
      if (count <= 0) {
         return state;
      } else {
         boolean currentlyAuto = leftHalf ? isAutoLeft(state) : isAutoRight(state);
         if (currentlyAuto) {
            return (BlockState)((BlockState)state.setValue(auto, false)).setValue(form, 0);
         } else {
            int current = leftHalf ? this.leftFormOf(state) : this.rightFormOf(state);
            return current + 1 < count ? (BlockState)((BlockState)state.setValue(auto, false)).setValue(form, current + 1) : (BlockState)state.setValue(auto, true);
         }
      }
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new FangsuPsdGlassBlock.FangsuPsdGlassEntity(pos, state);
   }

   public static class FangsuPsdGlassEntity extends BlockEntityMapper {
      public FangsuPsdGlassEntity(BlockPos pos, BlockState state) {
         super(FangsuPsdBlocks.glassEntityType(), pos, state == null ? Blocks.AIR.defaultBlockState() : state);
      }

      public void writeCompoundTag(CompoundTag nbt) {
      }
   }
}
