package dev.js.mtrheadlight.xiamen.eyecandy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EyecandyBlockEntity extends BlockEntity {
   public static final int BLINK_PHASE_TICKS = 12;
   public static final int BLINK_FLASHES = 3;
   public static final int BLINK_TOTAL_TICKS = 72;
   static final int SCAN_INTERVAL_TICKS = 1;
   private boolean doorOpen;
   private boolean initialised;
   private boolean blinking;
   private long blinkStart;
   private int scanCountdown;
   private final PsdDoorProbe.Tracker doorTracker = new PsdDoorProbe.Tracker();
   private boolean lit;

   public EyecandyBlockEntity(BlockPos pos, BlockState state) {
      super(EyecandyBlocks.entityType(), pos, state == null ? Blocks.AIR.defaultBlockState() : state);
   }

   public static void tick(Level world, BlockPos pos, BlockState state, EyecandyBlockEntity entity) {
      entity.tick(world, pos);
   }

   public static boolean blinkLit(long elapsed) {
      return elapsed / 12L % 2L == 0L;
   }

   private void tick(Level world, BlockPos pos) {
      long now = world.getGameTime();
      if (this.blinking) {
         long elapsed = now - this.blinkStart;
         if (elapsed >= 72L) {
            this.blinking = false;
            this.lit = this.doorOpen;
         } else {
            this.lit = blinkLit(elapsed);
         }
      } else {
         this.lit = this.initialised && this.doorOpen;
      }

      if (--this.scanCountdown <= 0) {
         this.scanCountdown = 1;
         PsdDoorProbe.Scan scan = this.doorTracker.scan(world, pos);
         if (scan.anyDoor) {
            boolean target = scan.open && !scan.closing;
            if (!this.initialised) {
               this.initialised = true;
               this.doorOpen = target;
               this.lit = target;
            } else if (target != this.doorOpen) {
               this.doorOpen = target;
               this.startBlink(now);
            }
         }
      }
   }

   private void startBlink(long now) {
      this.blinking = true;
      this.blinkStart = now;
      this.lit = true;
   }

   public boolean isLit() {
      return this.lit;
   }

   public boolean isDoorOpen() {
      return this.doorOpen;
   }

   public boolean isBlinking() {
      return this.blinking;
   }
}
