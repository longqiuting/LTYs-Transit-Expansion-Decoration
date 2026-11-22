package dev.js.mtrheadlight.xiamen.pids;

import java.util.Arrays;
import java.util.List;
import mtr.data.IPIDS;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PidsXiamenBlockEntity extends IPIDS.TileEntityPIDS {
   private static final String ADVERTISEMENT_INDEX_TAG = "xiamen_advertisement_index";
   private static final String AD_CATEGORIES_TAG = "xiamen_ad_categories";
   private int advertisementIndex;
   private long[] adCategories;
   private final PidsXiamenBlock.Kind kind;

   public PidsXiamenBlockEntity(BlockPos pos, BlockState state) {
      super(PidsXiamenObjects.entityType(), pos, state);
      this.kind = kindOf(state);
   }

   private static PidsXiamenBlock.Kind kindOf(BlockState state) {
      if (state != null) {
         Block block = state.getBlock();
         if (block instanceof PidsXiamenBlock pidsBlock) {
            return pidsBlock.kind();
         }
      }

      return PidsXiamenBlock.Kind.METRO;
   }

   protected PidsXiamenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, PidsXiamenBlock.Kind kind) {
      super(type, pos, state);
      this.kind = kind == null ? PidsXiamenBlock.Kind.METRO : kind;
   }

   public int getMaxArrivals() {
      return PidsXiamenLayout.rowsOf(this.kind == PidsXiamenBlock.Kind.BRT);
   }

   public int getLinesPerArrival() {
      return 1;
   }

   public int getAdvertisementIndex() {
      return this.advertisementIndex;
   }

   public void setAdvertisementIndex(int index) {
      this.advertisementIndex = Math.max(0, index);
      this.setChanged();
   }

   public long[] getAdCategories() {
      long[] value = this.adCategories;
      return value == null ? new long[0] : (long[])value.clone();
   }

   public List<PidsXiamenAds.Ad> getAds() {
      return PidsXiamenAds.adsFor(this.adCategories);
   }

   public void setAdCategories(long[] ids) {
      if (ids != null && ids.length != 0) {
         long[] clean = Arrays.stream(ids).distinct().toArray();
         this.adCategories = clean.length == 0 ? null : clean;
      } else {
         this.adCategories = null;
      }

      this.setChanged();
   }

   public void readCompoundTag(CompoundTag nbt) {
      super.readCompoundTag(nbt);
      this.advertisementIndex = nbt.contains("xiamen_advertisement_index") ? nbt.getInt("xiamen_advertisement_index") : 0;
      this.adCategories = nbt.contains("xiamen_ad_categories") ? nbt.getLongArray("xiamen_ad_categories") : null;
   }

   public void writeCompoundTag(CompoundTag nbt) {
      super.writeCompoundTag(nbt);
      nbt.putInt("xiamen_advertisement_index", this.advertisementIndex);
      if (this.adCategories != null && this.adCategories.length > 0) {
         nbt.putLongArray("xiamen_ad_categories", this.adCategories);
      }

   }
}
