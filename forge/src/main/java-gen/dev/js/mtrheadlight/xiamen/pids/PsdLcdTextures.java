package dev.js.mtrheadlight.xiamen.pids;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import dev.js.mtrheadlight.LteLog;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

final class PsdLcdTextures {
   private static final long TTL_MS = 30000L;
   private static final Map<String, PsdLcdTextures.Entry> ENTRIES = new HashMap();
   private static boolean warned;

   private PsdLcdTextures() {
   }

   static PsdLcdTextures.Entry acquire(String key, int width, int height) {
      long now = System.currentTimeMillis();
      PsdLcdTextures.Entry existing = (PsdLcdTextures.Entry)ENTRIES.get(key);
      if (existing != null && existing.width == width && existing.height == height && stillRegistered(existing)) {
         existing.lastUse = now;
         return existing;
      } else {
         if (existing != null) {
            release(key);
         }

         try {
            NativeImage image = new NativeImage(Format.RGBA, width, height, true);
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation id = Minecraft.getInstance().getTextureManager().register("psd_lcd_" + key.replaceAll("[^A-Za-z0-9_]", "_"), texture);
            PsdLcdTextures.Entry entry = new PsdLcdTextures.Entry();
            entry.id = id;
            entry.texture = texture;
            entry.width = width;
            entry.height = height;
            entry.lastUse = now;
            ENTRIES.put(key, entry);
            return entry;
         } catch (Throwable var10) {
            if (!warned) {
               warned = true;
               LteLog.LOGGER.warn("[lte] psd lcd: cannot create dynamic texture", var10);
            }

            return null;
         }
      }
   }

   static void upload(PsdLcdTextures.Entry entry, BufferedImage image) {
      if (entry != null && image != null && entry.texture != null) {
         NativeImage target = entry.texture.getPixels();
         if (target != null && target.getWidth() == image.getWidth() && target.getHeight() == image.getHeight()) {
            int w = image.getWidth();
            int h = image.getHeight();
            int[] pixels = image.getRGB(0, 0, w, h, (int[])null, 0, w);
            BufferedImage previous = entry.last;
            boolean full = previous == null || previous.getWidth() != w || previous.getHeight() != h;
            int[] old = full ? null : previous.getRGB(0, 0, w, h, (int[])null, 0, w);

            for(int y = 0; y < h; ++y) {
               int row = y * w;

               for(int x = 0; x < w; ++x) {
                  int argb = pixels[row + x];
                  if (full || old[row + x] != argb) {
                     target.setPixelRGBA(x, y, argb & -16711936 | (argb & 255) << 16 | argb >> 16 & 255);
                  }
               }
            }

            entry.texture.upload();
            entry.last = image;
         }
      }
   }

   private static boolean stillRegistered(PsdLcdTextures.Entry entry) {
      try {
         Minecraft client = Minecraft.getInstance();
         return client != null && client.getTextureManager().getTexture(entry.id) == entry.texture;
      } catch (Throwable var2) {
         return false;
      }
   }

   static void invalidateTicker(String key) {
      PsdLcdTextures.Entry entry = (PsdLcdTextures.Entry)ENTRIES.get(key + "|ticker");
      if (entry != null) {
         entry.lastPhasePx = Integer.MIN_VALUE;
      }

   }

   static void sweep() {
      if (!ENTRIES.isEmpty()) {
         long now = System.currentTimeMillis();
         Iterator<Map.Entry<String, PsdLcdTextures.Entry>> it = ENTRIES.entrySet().iterator();

         while(it.hasNext()) {
            PsdLcdTextures.Entry entry = (PsdLcdTextures.Entry)((Map.Entry)it.next()).getValue();
            if (now - entry.lastUse > 30000L) {
               it.remove();
               destroy(entry);
            }
         }

      }
   }

   private static void release(String key) {
      PsdLcdTextures.Entry entry = (PsdLcdTextures.Entry)ENTRIES.remove(key);
      if (entry != null) {
         destroy(entry);
      }

   }

   private static void destroy(PsdLcdTextures.Entry entry) {
      try {
         if (entry.id != null && Minecraft.getInstance() != null) {
            Minecraft.getInstance().getTextureManager().release(entry.id);
         }
      } catch (Throwable var2) {
      }

   }

   static void clear() {
      for(PsdLcdTextures.Entry entry : ENTRIES.values()) {
         destroy(entry);
      }

      ENTRIES.clear();
   }

   static final class Entry {
      ResourceLocation id;
      DynamicTexture texture;
      int width;
      int height;
      String hash = "";
      BufferedImage last;
      int lastPhasePx = Integer.MIN_VALUE;
      long lastUse;
   }
}
