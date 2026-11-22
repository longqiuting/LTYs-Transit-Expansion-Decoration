package dev.js.mtrheadlight.xiamen.fangsu;

final class FangsuPsdProgress {
   static final float MOVE_TICKS = 7.0F;
   private double progress;
   private double lastSample = Double.NaN;
   private long origin = Long.MIN_VALUE;

   float update(float target, long ticks, float tickDelta) {
      if (this.origin == Long.MIN_VALUE) {
         this.origin = ticks;
      }

      double now = (double)(ticks - this.origin) + (double)tickDelta;
      float clamped = Math.max(0.0F, Math.min(1.0F, target));
      if (Double.isNaN(this.lastSample)) {
         this.lastSample = now;
         this.progress = (double)clamped;
         return (float)this.progress;
      } else {
         double dt = Math.max(0.0D, Math.min(4.0D, now - this.lastSample));
         this.lastSample = now;
         double step = dt / 7.0D;
         double delta = (double)clamped - this.progress;
         if (Math.abs(delta) <= step) {
            this.progress = (double)clamped;
         } else {
            this.progress += Math.signum(delta) * step;
         }

         return (float)this.progress;
      }
   }
}
