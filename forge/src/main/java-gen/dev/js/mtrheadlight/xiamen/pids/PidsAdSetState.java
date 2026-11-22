package dev.js.mtrheadlight.xiamen.pids;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class PidsAdSetState extends SavedData {
   public static final String ID = "mtrlte_pids_ad_set";
   private final PidsAdSet data = new PidsAdSet();

   public PidsAdSetState() {
   }

   public PidsAdSetState(CompoundTag nbt) {
      this.data.copyFrom(PidsAdSet.readNbt(nbt));
   }

   public static PidsAdSetState get(MinecraftServer server) {
      if (server == null) {
         return new PidsAdSetState();
      } else {
         ServerLevel overworld = server.overworld();
         return overworld == null ? new PidsAdSetState() : (PidsAdSetState)overworld.getDataStorage().computeIfAbsent(PidsAdSetState::new, PidsAdSetState::new, "mtrlte_pids_ad_set");
      }
   }

   public PidsAdSet get() {
      return this.data.copy();
   }

   public void set(PidsAdSet set) {
      this.data.copyFrom(set);
      this.setDirty();
   }

   public CompoundTag save(CompoundTag nbt) {
      return this.data.writeNbt(nbt);
   }
}
