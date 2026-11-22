package dev.js.mtrheadlight.xiamen.pids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;

public final class PidsAdSet {
   public static final int MAX_IMAGES = 64;
   private static final String ENABLED_TAG = "enabled";
   private static final String IMAGES_TAG = "images";
   private boolean enabled;
   private final List<String> images = new ArrayList();

   public PidsAdSet() {
   }

   public PidsAdSet(boolean enabled, List<String> images) {
      this.enabled = enabled;
      if (images != null) {
         for(String image : images) {
            this.add(image);
         }
      }

   }

   public boolean enabled() {
      return this.enabled;
   }

   public List<String> images() {
      return Collections.unmodifiableList(this.images);
   }

   public int size() {
      return this.images.size();
   }

   public boolean contains(String image) {
      return image != null && this.images.contains(image);
   }

   public void setEnabled(boolean on) {
      this.enabled = on;
   }

   public void add(String image) {
      if (image != null && !image.isBlank() && this.images.size() < 64) {
         if (!this.images.contains(image)) {
            this.images.add(image);
         }

      }
   }

   public boolean remove(String image) {
      return image != null && this.images.remove(image);
   }

   public void clear() {
      this.images.clear();
   }

   public PidsAdSet copy() {
      return new PidsAdSet(this.enabled, this.images);
   }

   public void copyFrom(PidsAdSet other) {
      this.enabled = other != null && other.enabled;
      this.images.clear();
      if (other != null) {
         for(String image : other.images) {
            this.add(image);
         }
      }

   }

   public List<PidsXiamenAds.Ad> resolve() {
      if (this.enabled && !this.images.isEmpty()) {
         List<PidsXiamenAds.Ad> out = new ArrayList(this.images.size());

         for(String image : this.images) {
            PidsXiamenAds.Ad ad = PidsXiamenAds.adWithImage(image);
            if (ad != null && ad.image() != null) {
               out.add(ad);
            }
         }

         return out.isEmpty() ? null : Collections.unmodifiableList(out);
      } else {
         return null;
      }
   }

   public CompoundTag writeNbt(CompoundTag nbt) {
      nbt.putBoolean("enabled", this.enabled);
      ListTag list = new ListTag();

      for(String image : this.images) {
         list.add(StringTag.valueOf(image));
      }

      nbt.put("images", list);
      return nbt;
   }

   public static PidsAdSet readNbt(CompoundTag nbt) {
      PidsAdSet out = new PidsAdSet();
      if (nbt == null) {
         return out;
      } else {
         out.enabled = nbt.getBoolean("enabled");
         ListTag list = nbt.getList("images", 8);

         for(int i = 0; i < list.size(); ++i) {
            out.add(list.getString(i));
         }

         return out;
      }
   }

   public void writeBuf(FriendlyByteBuf buf) {
      buf.writeBoolean(this.enabled);
      buf.writeVarInt(this.images.size());

      for(String image : this.images) {
         buf.writeUtf(image);
      }

   }

   public static PidsAdSet readBuf(FriendlyByteBuf buf) {
      PidsAdSet out = new PidsAdSet();
      out.enabled = buf.readBoolean();
      int count = Math.max(0, Math.min(buf.readVarInt(), 64));

      for(int i = 0; i < count; ++i) {
         out.add(buf.readUtf(256));
      }

      return out;
   }
}
