package dev.js.mtrheadlight.xiamen.drinks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DrinksMachineBlockEntity extends BlockEntity {
   private static final String NBT_PRESET = "PotionPreset";
   private DrinksMachinePotions.Preset preset = DrinksMachinePotions.Preset.MIXED;

   public DrinksMachineBlockEntity(BlockPos pos, BlockState state) {
      super(DrinksMachineObjects.blockEntityType(), pos, state == null ? Blocks.AIR.defaultBlockState() : state);
   }

   public DrinksMachinePotions.Preset getPreset() {
      return this.preset;
   }

   public void setPreset(DrinksMachinePotions.Preset preset) {
      this.preset = preset;
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }

   }

   public void load(CompoundTag nbt) {
      super.load(nbt);
      if (nbt.contains("PotionPreset")) {
         this.preset = DrinksMachinePotions.Preset.byId(nbt.getString("PotionPreset"));
      }

   }

   protected void saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putString("PotionPreset", this.preset.id());
   }
}
