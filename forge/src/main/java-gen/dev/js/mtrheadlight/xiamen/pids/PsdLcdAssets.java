package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

final class PsdLcdAssets {
   private static final String DIR = "textures/gui/psd_lcd/";
   private static PsdLcdPainter.Assets cached;
   private static final Map<String, BufferedImage> AD_CACHE_MAP = new LinkedHashMap<String, BufferedImage>(8, 0.75F, true) {
      protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
         return this.size() > 8;
      }
   };

   private PsdLcdAssets() {
   }

   static PsdLcdPainter.Assets get() {
      PsdLcdPainter.Assets local = cached;
      if (local != null) {
         return local;
      } else {
         PsdLcdPainter.Assets loaded = new PsdLcdPainter.Assets();
         loaded.logo = image("xiamen_logo.png");
         loaded.nose = image("train_nose.png");
         loaded.carEnd = image("car_end.png");
         loaded.carMiddle = image("car_middle.png");
         loaded.cjk = PsdLcdTypeface.cjk();
         loaded.latin = PsdLcdTypeface.latin();
         loaded.digits = PsdLcdTypeface.digits();
         cached = loaded;
         LteLog.LOGGER.info("[lte] psd lcd: cjk={} latin={} digits={} logo={}x{}", new Object[]{PsdLcdTypeface.name(loaded.cjk), PsdLcdTypeface.name(loaded.latin), PsdLcdTypeface.name(loaded.digits), loaded.logo == null ? "?" : String.valueOf(loaded.logo.getWidth()), loaded.logo == null ? "?" : String.valueOf(loaded.logo.getHeight())});
         return loaded;
      }
   }

   private static BufferedImage image(String name) {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client == null) {
            return null;
         } else {
            ResourceLocation id = new ResourceLocation("mtrlte", "textures/gui/psd_lcd/" + name);
            return load(id);
         }
      } catch (Throwable var3) {
         LteLog.LOGGER.warn("[lte] psd lcd: cannot load texture {}", name, var3);
         return null;
      }
   }

   static BufferedImage ad(ResourceLocation id) {
      if (id == null) {
         return null;
      } else {
         String key = id.toString();
         synchronized(AD_CACHE_MAP) {
            if (AD_CACHE_MAP.containsKey(key)) {
               return (BufferedImage)AD_CACHE_MAP.get(key);
            } else {
               BufferedImage loaded = load(id);
               AD_CACHE_MAP.put(key, loaded);
               return loaded;
            }
         }
      }
   }

   static void invalidateAds() {
      synchronized(AD_CACHE_MAP) {
         AD_CACHE_MAP.clear();
      }
   }

   private static BufferedImage load(ResourceLocation id) {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client == null) {
            return null;
         } else {
            Optional<Resource> resource = client.getResourceManager().getResource(id);
            if (resource.isEmpty()) {
               LteLog.LOGGER.debug("[lte] psd lcd: ad image missing {}", id);
               return null;
            } else {
               InputStream in = ((Resource)resource.get()).open();

               BufferedImage var4;
               try {
                  var4 = ImageIO.read(in);
               } catch (Throwable var7) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }
                  }

                  throw var7;
               }

               if (in != null) {
                  in.close();
               }

               return var4;
            }
         }
      } catch (Throwable var8) {
         LteLog.LOGGER.warn("[lte] psd lcd: cannot load ad image {}", id, var8);
         return null;
      }
   }
}
