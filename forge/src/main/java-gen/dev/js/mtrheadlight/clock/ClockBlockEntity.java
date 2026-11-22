package dev.js.mtrheadlight.clock;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ClockBlockEntity extends BlockEntity {
   public static final int CATCHUP_TICKS = 26;
   private static final int CHECK_INTERVAL = 20;
   private static final int STRIKE_GAP = 8;
   private static final String TAG_CONFIG = "Clock";
   private ClockConfig config = new ClockConfig();
   private double dispPrev;
   private double dispNow;
   private boolean initialized;
   private boolean stopped;
   private double gapRemaining;
   private double gapPerTick;
   private int tickCounter;
   private int errorCounter;
   private int lastChimeHour = -1;
   private boolean chimePrimed;
   private int pendingStrikes;
   private long nextStrikeTick;

   public ClockBlockEntity(BlockPos pos, BlockState state) {
      super(ClockObjects.entityType(), pos, state);
   }

   public ClockConfig config() {
      return this.config.copy();
   }

   public void setConfig(ClockConfig cfg) {
      if (cfg != null) {
         ClockConfig copy = cfg.copy();
         copy.clamp();
         this.config = copy;
         this.setChanged();
         this.syncToClients();
      }
   }

   public void applyClientConfig(ClockConfig cfg) {
      if (cfg != null) {
         ClockConfig copy = cfg.copy();
         copy.clamp();
         this.config = copy;
      }
   }

   private void syncToClients() {
      Level world = this.level;
      if (world != null && !world.isClientSide) {
         try {
            BlockState state = world.getBlockState(this.worldPosition);
            world.sendBlockUpdated(this.worldPosition, state, state, 2);
         } catch (Throwable var4) {
            LteLog.LOGGER.debug("[lte] clock at {} could not refresh listeners", this.worldPosition, var4);
         }

         try {
            ClockNet.sendConfig(world, this.worldPosition, this.config);
         } catch (Throwable var3) {
            LteLog.LOGGER.debug("[lte] clock at {} could not push its config", this.worldPosition, var3);
         }

      }
   }

   public ClockConfig effectiveConfig() {
      Level world = this.level;
      if (world == null) {
         return this.config;
      } else {
         return world.isClientSide ? ClockGlobalCache.effectiveOr(this.config) : ClockGlobalState.effectiveOr(world.getServer(), this.config);
      }
   }

   public double displaySeconds(float tickDelta) {
      if (!this.initialized) {
         return ClockTime.displayTarget(this.level, this.effectiveConfig(), (double)tickDelta);
      } else {
         double delta = this.dispNow - this.dispPrev;
         if (delta < -43200.0D) {
            delta += 86400.0D;
         } else if (delta > 43200.0D) {
            delta -= 86400.0D;
         }

         return ClockTime.wrap(this.dispNow + delta * (double)tickDelta);
      }
   }

   public boolean isStopped() {
      return this.stopped;
   }

   public static void tick(Level world, BlockPos pos, BlockState state, ClockBlockEntity entity) {
      try {
         entity.tickOnce(world, pos, state);
      } catch (Throwable var5) {
         if (entity.errorCounter++ % 200 == 0) {
            LteLog.LOGGER.warn("[lte] clock at {} failed to tick", pos, var5);
         }
      }

   }

   private void tickOnce(Level world, BlockPos pos, BlockState state) {
      ClockConfig cfg = this.effectiveConfig();
      double actual = ClockTime.displayTarget(world, cfg, 0.0D);
      boolean stop = cfg.redstone && !state.getValue(ClockBlock.POWERED);
      if (!this.initialized) {
         this.initialized = true;
         this.dispPrev = actual;
         this.dispNow = actual;
      }

      this.dispPrev = this.dispNow;
      if (stop) {
         this.stopped = true;
         this.gapRemaining = 0.0D;
         this.gapPerTick = 0.0D;
      } else {
         if (this.stopped) {
            this.stopped = false;
            double gap = ClockTime.wrap(actual - this.dispNow);
            this.gapRemaining = gap;
            this.gapPerTick = gap / 26.0D;
         }

         if (this.gapRemaining > 0.0D) {
            this.gapRemaining = Math.max(0.0D, this.gapRemaining - this.gapPerTick);
            this.dispNow = ClockTime.wrap(actual - this.gapRemaining);
         } else {
            this.dispNow = actual;
         }
      }

      if (!world.isClientSide) {
         this.tickChime(world, pos, stop, cfg);
      }
   }

   private void tickChime(Level world, BlockPos pos, boolean stop, ClockConfig cfg) {
      if (stop) {
         this.pendingStrikes = 0;
         this.nextStrikeTick = 0L;
      }

      if (this.pendingStrikes > 0 && world.getGameTime() >= this.nextStrikeTick) {
         world.playSound((Player)null, pos, soundOf(cfg), SoundSource.BLOCKS, 1.0F, 1.0F);
         --this.pendingStrikes;
         this.nextStrikeTick = world.getGameTime() + 8L;
      }

      if (++this.tickCounter % 20 == 0) {
         double display = ClockTime.displayTarget(world, cfg, 0.0D);
         int minute = ClockTime.minute(display);
         int hour12 = ClockTime.hour12(display);
         if (!this.chimePrimed) {
            this.chimePrimed = true;
            this.lastChimeHour = minute == 0 ? hour12 : -1;
         } else if (minute != 0) {
            this.lastChimeHour = -1;
         } else if (cfg.chime && !stop && hour12 != this.lastChimeHour) {
            this.lastChimeHour = hour12;
            this.pendingStrikes = cfg.chimeMode == ClockConfig.ChimeMode.HOURLY ? hour12 : 1;
            this.nextStrikeTick = world.getGameTime() + 8L;
            LteLog.LOGGER.debug("[lte] clock at {} chiming {}x at {}:00", new Object[]{pos, this.pendingStrikes, ClockTime.hour(display)});
         }
      }
   }

   public SoundEvent soundEvent() {
      return soundOf(this.effectiveConfig());
   }

   public static SoundEvent soundOf(ClockConfig cfg) {
      String raw = cfg == null ? "minecraft:block.bell.use" : cfg.chimeSound;
      ResourceLocation id = ResourceLocation.tryParse(raw == null ? "minecraft:block.bell.use" : raw);
      if (id != null) {
         SoundEvent event = (SoundEvent)BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse((SoundEvent)null);
         if (event != null) {
            return event;
         }
      }

      return SoundEvents.BELL_BLOCK;
   }

   public void playPreview() {
      if (this.level != null && !this.level.isClientSide) {
         this.level.playSound((Player)null, this.worldPosition, this.soundEvent(), SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   public void saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      CompoundTag cfg = new CompoundTag();
      this.config.writeNbt(cfg);
      nbt.put("Clock", cfg);
   }

   public void load(CompoundTag nbt) {
      super.load(nbt);
      if (nbt.contains("Clock")) {
         ClockConfig cfg = new ClockConfig();
         cfg.readNbt(nbt.getCompound("Clock"));
         this.config = cfg;
      }

   }

   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }
}
