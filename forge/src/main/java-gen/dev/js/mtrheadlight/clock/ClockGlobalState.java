package dev.js.mtrheadlight.clock;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class ClockGlobalState extends SavedData {
   public static final String ID = "mtrlte_clock_global";
   private final ClockGlobalConfig data = new ClockGlobalConfig();

   public ClockGlobalState() {
   }

   public ClockGlobalState(CompoundTag nbt) {
      ClockGlobalConfig loaded = ClockGlobalConfig.readNbt(nbt);
      this.data.enabled = loaded.enabled;
      this.data.config = loaded.config;
   }

   public static ClockGlobalState get(MinecraftServer server) {
      if (server == null) {
         return new ClockGlobalState();
      } else {
         ServerLevel overworld = server.overworld();
         return overworld == null ? new ClockGlobalState() : (ClockGlobalState)overworld.getDataStorage().computeIfAbsent(ClockGlobalState::new, ClockGlobalState::new, "mtrlte_clock_global");
      }
   }

   public ClockGlobalConfig get() {
      return this.data.copy();
   }

   public boolean enabled() {
      return this.data.enabled;
   }

   public void set(ClockGlobalConfig cfg) {
      if (cfg != null) {
         ClockGlobalConfig copy = cfg.copy();
         copy.clamp();
         this.data.enabled = copy.enabled;
         this.data.config = copy.config;
         this.setDirty();
      }
   }

   public static ClockConfig effectiveOr(MinecraftServer server, ClockConfig own) {
      if (server == null) {
         return own;
      } else {
         ClockGlobalState state = get(server);
         return !state.data.enabled ? own : state.data.config.copy();
      }
   }

   public CompoundTag save(CompoundTag nbt) {
      this.data.writeNbt(nbt);
      return nbt;
   }
}
