package dev.js.mtrheadlight.xiamen.pids;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import dev.js.mtrheadlight.LteLog;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

final class PidsXiamenConsistTextures {
   private static final long TTL_MS = 30000L;
   private static final Map<String, PidsXiamenConsistTextures.Entry> ENTRIES = new HashMap();
   private static int generation;
   private static BufferedImage band;
   private static BufferedImage card;
   private static BufferedImage flake;
   private static boolean warned;
   private static final ResourceLocation BAND_ID = new ResourceLocation("mtrlte", "textures/gui/pids/consist_band.png");
   private static final ResourceLocation CARD_ID = new ResourceLocation("mtrlte", "textures/gui/pids/consist_card.png");
   private static final ResourceLocation FLAKE_ID = new ResourceLocation("mtrlte", "textures/gui/pids/consist_flake.png");

   private PidsXiamenConsistTextures() {
   }

   static ResourceLocation texture(int cars) {
      int n = Math.max(0, Math.min(16, cars));
      BufferedImage bandImage = n > 0 ? asset(BAND_ID) : null;
      BufferedImage cardImage = n > 0 ? asset(CARD_ID) : null;
      BufferedImage flakeImage = n > 0 ? asset(FLAKE_ID) : null;
      if (n <= 0 || bandImage != null && cardImage != null && flakeImage != null) {
         String key = "consist|" + generation + "|" + n;
         long hash = PidsXiamenConsist.hash(n, bandImage, cardImage, flakeImage);
         PidsXiamenConsistTextures.Entry entry = (PidsXiamenConsistTextures.Entry)ENTRIES.get(key);
         if (entry != null && stillRegistered(entry) && entry.hash == hash) {
            entry.lastUse = System.currentTimeMillis();
            return entry.id;
         } else {
            if (entry != null) {
               release(key);
            }

            try {
               int w = PidsXiamenConsist.overlayWidth();
               int h = PidsXiamenConsist.overlayHeight();
               BufferedImage image = PidsXiamenConsist.paint(bandImage, cardImage, flakeImage, n);
               if (image == null) {
                  return null;
               } else {
                  NativeImage nativeImage = new NativeImage(Format.RGBA, w, h, true);
                  DynamicTexture texture = new DynamicTexture(nativeImage);
                  texture.setFilter(false, false);
                  ResourceLocation id = Minecraft.getInstance().getTextureManager().register("pids_consist_" + generation + "_" + n, texture);
                  PidsXiamenConsistTextures.Entry created = new PidsXiamenConsistTextures.Entry();
                  created.id = id;
                  created.texture = texture;
                  created.hash = hash;
                  created.lastUse = System.currentTimeMillis();
                  upload(created, image);
                  ENTRIES.put(key, created);
                  return id;
               }
            } catch (Throwable var16) {
               if (!warned) {
                  warned = true;
                  LteLog.LOGGER.warn("[lte] xiamen pids consist: cannot build the overlay texture", var16);
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static void upload(PidsXiamenConsistTextures.Entry entry, BufferedImage image) {
      NativeImage target = entry.texture.getPixels();
      if (target != null && target.getWidth() == image.getWidth() && target.getHeight() == image.getHeight()) {
         int w = image.getWidth();
         int h = image.getHeight();
         int[] pixels = image.getRGB(0, 0, w, h, (int[])null, 0, w);

         for(int y = 0; y < h; ++y) {
            int row = y * w;

            for(int x = 0; x < w; ++x) {
               int argb = pixels[row + x];
               target.setPixelRGBA(x, y, argb & -16711936 | (argb & 255) << 16 | argb >> 16 & 255);
            }
         }

         entry.texture.upload();
      }
   }

   private static BufferedImage asset(ResourceLocation id) {
      if (id == BAND_ID) {
         if (band == null) {
            band = read(id);
         }

         return band;
      } else if (id == CARD_ID) {
         if (card == null) {
            card = read(id);
         }

         return card;
      } else {
         if (flake == null) {
            flake = read(id);
         }

         return flake;
      }
   }

   private static BufferedImage read(ResourceLocation id) {
      try {
         Minecraft client = Minecraft.getInstance();
         ResourceManager manager = client == null ? null : client.getResourceManager();
         if (manager == null) {
            return null;
         } else {
            Optional<Resource> resource = manager.getResource(id);
            if (resource.isEmpty()) {
               return null;
            } else {
               InputStream in = ((Resource)resource.get()).open();

               BufferedImage var5;
               try {
                  var5 = ImageIO.read(in);
               } catch (Throwable var8) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }
                  }

                  throw var8;
               }

               if (in != null) {
                  in.close();
               }

               return var5;
            }
         }
      } catch (Throwable var9) {
         if (!warned) {
            warned = true;
            LteLog.LOGGER.warn("[lte] xiamen pids consist: cannot read {}", id, var9);
         }

         return null;
      }
   }

   private static boolean stillRegistered(PidsXiamenConsistTextures.Entry entry) {
      try {
         Minecraft client = Minecraft.getInstance();
         return client != null && client.getTextureManager().getTexture(entry.id) == entry.texture;
      } catch (Throwable var2) {
         return false;
      }
   }

   static void sweep() {
      if (!ENTRIES.isEmpty()) {
         long now = System.currentTimeMillis();
         Iterator<Map.Entry<String, PidsXiamenConsistTextures.Entry>> it = ENTRIES.entrySet().iterator();

         while(it.hasNext()) {
            PidsXiamenConsistTextures.Entry entry = (PidsXiamenConsistTextures.Entry)((Map.Entry)it.next()).getValue();
            if (now - entry.lastUse > 30000L) {
               it.remove();
               destroy(entry);
            }
         }

      }
   }

   static void invalidate() {
      band = null;
      card = null;
      flake = null;
      ++generation;

      for(PidsXiamenConsistTextures.Entry entry : ENTRIES.values()) {
         destroy(entry);
      }

      ENTRIES.clear();
   }

   private static void release(String key) {
      PidsXiamenConsistTextures.Entry entry = (PidsXiamenConsistTextures.Entry)ENTRIES.remove(key);
      if (entry != null) {
         destroy(entry);
      }

   }

   private static void destroy(PidsXiamenConsistTextures.Entry entry) {
      try {
         Minecraft client = Minecraft.getInstance();
         if (entry.id != null && client != null) {
            client.getTextureManager().release(entry.id);
         }
      } catch (Throwable var2) {
      }

   }

   private static final class Entry {
      ResourceLocation id;
      DynamicTexture texture;
      long hash = Long.MIN_VALUE;
      long lastUse;
   }
}
