package dev.js.mtrheadlight.xiamen.charger;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class PowerBankItem extends Item {
   public static final String NBT_RENTED_AT = "rentedAt";
   public static final String NBT_OVERDUE_SETTLED = "expiredFine";
   public static final String NBT_DAMAGED = "damaged";
   public static final int GRACE_MINUTES = 5;
   public static final long GRACE_MILLIS = 300000L;
   public static final int PERIOD_MINUTES = 30;
   public static final long PERIOD_MILLIS = 1800000L;
   public static final int FEE_PER_PERIOD = 2;
   public static final int MAX_PERIODS = 48;
   public static final int MAX_FEE = 96;
   public static final int OVERDUE_HOURS = 24;
   public static final long OVERDUE_MILLIS = 86400000L;
   public static final int OVERDUE_FEE = 100;

   public PowerBankItem(Item.Properties settings) {
      super(settings);
   }

   public static ItemStack rented(Item item, long nowMillis) {
      ItemStack stack = new ItemStack(item);
      stack.getOrCreateTag().putLong("rentedAt", nowMillis);
      return stack;
   }

   public static boolean isRented(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag().contains("rentedAt");
   }

   public static long rentedAt(ItemStack stack) {
      return !isRented(stack) ? 0L : stack.getTag().getLong("rentedAt");
   }

   public static long heldMillis(ItemStack stack, long nowMillis) {
      return !isRented(stack) ? 0L : Math.max(0L, nowMillis - rentedAt(stack));
   }

   public static long heldMinutes(ItemStack stack, long nowMillis) {
      return heldMillis(stack, nowMillis) / 60000L;
   }

   public static long heldHours(ItemStack stack, long nowMillis) {
      return heldMillis(stack, nowMillis) / 3600000L;
   }

   public static long billedPeriods(ItemStack stack, long nowMillis) {
      long billable = heldMillis(stack, nowMillis) - 300000L;
      if (billable <= 0L) {
         return 0L;
      } else {
         long periods = (billable + 1800000L - 1L) / 1800000L;
         return Math.min(periods, 48L);
      }
   }

   public static int fee(ItemStack stack, long nowMillis) {
      return (int)Math.min(2147483647L, billedPeriods(stack, nowMillis) * 2L);
   }

   public static boolean isOverdue(ItemStack stack, long nowMillis) {
      return isRented(stack) && heldMillis(stack, nowMillis) > 86400000L;
   }

   public static boolean isOverdueSettled(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean("expiredFine");
   }

   public static void markOverdueSettled(ItemStack stack) {
      stack.getOrCreateTag().putBoolean("expiredFine", true);
   }

   public static boolean isDamaged(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean("damaged");
   }

   public static void markDamaged(ItemStack stack) {
      stack.getOrCreateTag().putBoolean("damaged", true);
   }

   public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
      ItemStack stack = user.getItemInHand(hand);
      world.playSound((Player)null, user.getX(), user.getY(), user.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
      if (!world.isClientSide) {
         PowerBankProjectileEntity projectile = new PowerBankProjectileEntity(world, user);
         ItemStack thrown = stack.copyWithCount(1);
         markDamaged(thrown);
         if (user.getAbilities().instabuild) {
            markDamaged(stack);
         }

         projectile.setItem(thrown);
         projectile.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, 1.5F, 1.0F);
         world.addFreshEntity(projectile);
      }

      if (!user.getAbilities().instabuild) {
         stack.shrink(1);
      }

      return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
   }

   public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
      tooltip.add(Component.translatable("tooltip.mtrlte.power_bank.rent").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.mtrlte.power_bank.buyout").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.mtrlte.power_bank.throw").withStyle(ChatFormatting.GRAY));
      if (isRented(stack)) {
         tooltip.add(Component.translatable("tooltip.mtrlte.power_bank.held", new Object[]{heldMinutes(stack, System.currentTimeMillis())}).withStyle(ChatFormatting.AQUA));
      }

      if (isDamaged(stack)) {
         tooltip.add(Component.translatable("tooltip.mtrlte.power_bank.damaged").withStyle(ChatFormatting.RED));
      } else if (isOverdueSettled(stack)) {
         tooltip.add(Component.translatable("tooltip.mtrlte.power_bank.owned").withStyle(ChatFormatting.GOLD));
      }

      tooltip.add(Component.translatable("tooltip.mtrlte.power_bank.warning").withStyle(ChatFormatting.RED));
   }
}
