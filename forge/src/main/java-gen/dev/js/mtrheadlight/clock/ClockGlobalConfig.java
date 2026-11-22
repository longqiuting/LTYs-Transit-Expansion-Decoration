package dev.js.mtrheadlight.clock;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public final class ClockGlobalConfig {
   private static final String TAG_ENABLED = "Enabled";
   private static final String TAG_CLOCK = "Clock";
   public boolean enabled;
   public ClockConfig config;

   public ClockGlobalConfig() {
      this.enabled = false;
      this.config = new ClockConfig();
   }

   public ClockGlobalConfig(ClockConfig config, boolean enabled) {
      this.enabled = enabled;
      this.config = config == null ? new ClockConfig() : config.copy();
   }

   public ClockGlobalConfig copy() {
      return new ClockGlobalConfig(this.config, this.enabled);
   }

   public void clamp() {
      if (this.config == null) {
         this.config = new ClockConfig();
      }

      this.config.clamp();
   }

   public void writeNbt(CompoundTag nbt) {
      nbt.putBoolean("Enabled", this.enabled);
      CompoundTag clock = new CompoundTag();
      this.config.writeNbt(clock);
      nbt.put("Clock", clock);
   }

   public static ClockGlobalConfig readNbt(CompoundTag nbt) {
      ClockGlobalConfig out = new ClockGlobalConfig();
      if (nbt == null) {
         return out;
      } else {
         out.enabled = nbt.getBoolean("Enabled");
         if (nbt.contains("Clock")) {
            out.config.readNbt(nbt.getCompound("Clock"));
         }

         out.clamp();
         return out;
      }
   }

   public void writeBuf(FriendlyByteBuf buf) {
      buf.writeBoolean(this.enabled);
      this.config.writeBuf(buf);
   }

   public static ClockGlobalConfig readBuf(FriendlyByteBuf buf) {
      ClockGlobalConfig out = new ClockGlobalConfig();
      out.enabled = buf.readBoolean();
      out.config = ClockConfig.readBuf(buf);
      out.clamp();
      return out;
   }
}
