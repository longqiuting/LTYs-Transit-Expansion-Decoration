package dev.js.mtrheadlight.xiamen.charger;

import dev.js.mtrheadlight.bind.MtrBalance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PowerBankProjectileEntity extends ThrowableProjectile implements ItemSupplier {
   public static final double BLAST_RADIUS = 2.0D;
   public static final float BLAST_DAMAGE = 2.0F;
   private ItemStack stack = ItemStack.EMPTY;
   private boolean detonated;

   public PowerBankProjectileEntity(EntityType<? extends PowerBankProjectileEntity> type, Level world) {
      super(type, world);
   }

   public PowerBankProjectileEntity(Level world, LivingEntity owner) {
      super(ChargerObjects.projectileType(), owner, world);
   }

   public void setItem(ItemStack stack) {
      this.stack = stack.copyWithCount(1);
   }

   protected void defineSynchedData() {
   }

   public ItemStack getItem() {
      return this.stack.isEmpty() ? new ItemStack(ChargerObjects.powerBankItem()) : this.stack;
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide) {
         this.detonate();
      }

   }

   protected void onHit(HitResult hit) {
      super.onHit(hit);
      if (!this.level().isClientSide) {
         this.detonate();
      }

   }

   private void detonate() {
      if (!this.detonated) {
         this.detonated = true;
         Level world = this.level();
         Vec3 at = this.position();
         if (world instanceof ServerLevel) {
            ServerLevel server = (ServerLevel)world;
            this.settleThrow(server);
            server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.05D, at.z, 20, 0.4D, 0.15D, 0.4D, 0.02D);
            server.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.1D, at.z, 8, 0.35D, 0.1D, 0.35D, 0.01D);
            world.playSound((Player)null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.5F);
            igniteLandingTile(server, BlockPos.containing(at.x, at.y, at.z));
         }

         AABB box = this.getBoundingBox().inflate(2.0D);
         Entity owner = this.getOwner();
         double radiusSquared = 4.0D;

         for(Entity entity : world.getEntities(this, box)) {
            if (entity != owner && entity != this && entity instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)entity;
               if (!(entity.distanceToSqr(at) > 4.0D)) {
                  living.hurt(world.damageSources().thrown(this, owner), 2.0F);
               }
            }
         }

         this.discard();
      }
   }

   private void settleThrow(ServerLevel server) {
      ItemStack stack = this.stack;
      if (!stack.isEmpty() && stack.getItem() instanceof PowerBankItem) {
         PowerBankItem.markDamaged(stack);
         if (PowerBankItem.isRented(stack) && !PowerBankItem.isOverdueSettled(stack)) {
            Entity owner = this.getOwner();
            if (owner instanceof ServerPlayer) {
               ServerPlayer player = (ServerPlayer)owner;
               boolean fee = true;
               int before = MtrBalance.get(server, player);
               if (before < 100) {
                  player.displayClientMessage(Component.translatable("message.mtrlte.charger.thrown_no_money", new Object[]{100, before}).withStyle(ChatFormatting.RED), false);
               } else {
                  int after = MtrBalance.add(server, player, -100);
                  if (after != before - 100) {
                     player.displayClientMessage(Component.translatable("message.mtrlte.charger.thrown_no_money", new Object[]{100, before}).withStyle(ChatFormatting.RED), false);
                  } else {
                     PowerBankItem.markOverdueSettled(stack);
                     player.displayClientMessage(Component.translatable("message.mtrlte.charger.thrown_damaged", new Object[]{100, after}).withStyle(ChatFormatting.RED), false);
                  }
               }
            }
         }
      }
   }

   private static void igniteLandingTile(ServerLevel world, BlockPos landing) {
      for(BlockPos candidate : new BlockPos[]{landing, landing.below(), landing.above()}) {
         if (ignite(world, candidate)) {
            return;
         }
      }

   }

   private static boolean ignite(ServerLevel world, BlockPos pos) {
      BlockState current = world.getBlockState(pos);
      if (!current.isAir() && !current.canBeReplaced()) {
         return false;
      } else {
         return !Blocks.FIRE.defaultBlockState().canSurvive(world, pos) ? false : world.setBlock(pos, BaseFireBlock.getState(world, pos), 3);
      }
   }

   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      if (!this.stack.isEmpty()) {
         nbt.put("Item", this.stack.save(new CompoundTag()));
      }

   }

   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      if (nbt.contains("Item")) {
         this.stack = ItemStack.of(nbt.getCompound("Item"));
      }

   }
}
