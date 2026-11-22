package dev.js.mtrheadlight.security;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LiquidScannerBlockEntity extends BlockEntity {
   private static final String TAG_CONFIGURED = "configured";
   private static final String TAG_CONFISCATE = "confiscate";
   private static final String TAG_LIST_VERSION = "ListVersion";
   private static final int LIST_VERSION = 1;
   private boolean configured;
   private final Set<ResourceLocation> confiscate = new LinkedHashSet();

   public LiquidScannerBlockEntity(BlockPos pos, BlockState state) {
      super(SecurityObjects.liquidScannerEntityType(), pos, state);
   }

   public boolean isConfigured() {
      return this.configured;
   }

   public Set<ResourceLocation> confiscate() {
      return Collections.unmodifiableSet(this.confiscate);
   }

   public boolean shouldConfiscate(ItemStack stack) {
      if (!this.configured) {
         return LiquidScannerBlock.isHarmful(stack) || LiquidScannerBlock.isConfiscatedByDefault(stack);
      } else {
         ResourceLocation id = LiquidScannerBlock.liquidId(stack);
         return id != null && this.confiscate.contains(id);
      }
   }

   public static ResourceLocation potionId(ItemStack stack) {
      return LiquidScannerBlock.liquidId(stack);
   }

   public void setConfiscate(Collection<ResourceLocation> ids) {
      this.confiscate.clear();
      if (ids != null) {
         this.confiscate.addAll(ids);
      }

      this.configured = true;
      this.setChanged();
   }

   public void saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putBoolean("configured", this.configured);
      nbt.putInt("ListVersion", 1);
      ListTag list = new ListTag();

      for(ResourceLocation id : this.confiscate) {
         list.add(StringTag.valueOf(id.toString()));
      }

      nbt.put("confiscate", list);
   }

   public void load(CompoundTag nbt) {
      super.load(nbt);
      this.configured = nbt.getBoolean("configured");
      this.confiscate.clear();
      ListTag list = nbt.getList("confiscate", 8);

      for(int i = 0; i < list.size(); ++i) {
         ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
         if (id != null) {
            this.confiscate.add(id);
         }
      }

      if (this.configured && nbt.getInt("ListVersion") < 1) {
         this.confiscate.add(LiquidScannerBlock.LIQUID_LAVA);
      }

   }
}
