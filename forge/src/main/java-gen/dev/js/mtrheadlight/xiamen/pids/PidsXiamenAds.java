package dev.js.mtrheadlight.xiamen.pids;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.js.mtrheadlight.LteLog;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class PidsXiamenAds {
   public static final ResourceLocation CONFIG_ID = new ResourceLocation("mtrlte", "pids_xiamen_ads.json");
   public static final ResourceLocation TIANJIN_CONFIG_ID = new ResourceLocation("tjmetro", "pids_tianjin_ads.json");
   public static final String NAMESPACE = "mtrlte";
   public static final long DEFAULT_CATEGORY = (long)"mtrlte".hashCode();
   private static volatile List<PidsXiamenAds.Category> cache;
   private static volatile List<PidsXiamenAds.Ad> allAdsCache;
   private static volatile float aspectRatio = 1.82F;

   private PidsXiamenAds() {
   }

   public static float aspectRatio() {
      return aspectRatio;
   }

   public static void invalidate() {
      cache = null;
      allAdsCache = null;
   }

   public static List<PidsXiamenAds.Category> categories() {
      List<PidsXiamenAds.Category> local = cache;
      if (local != null) {
         return local;
      } else {
         local = load();
         cache = local;
         return local;
      }
   }

   public static PidsXiamenAds.Category category(long id) {
      for(PidsXiamenAds.Category category : categories()) {
         if (category.id() == id) {
            return category;
         }
      }

      return null;
   }

   public static PidsXiamenAds.Category defaultCategory() {
      List<PidsXiamenAds.Category> list = categories();
      if (list.isEmpty()) {
         return null;
      } else {
         for(PidsXiamenAds.Category category : list) {
            if (category.id() == DEFAULT_CATEGORY) {
               return category;
            }
         }

         return (PidsXiamenAds.Category)list.get(0);
      }
   }

   public static List<PidsXiamenAds.Ad> adsFor(long[] selected) {
      if (selected != null && selected.length != 0) {
         List<PidsXiamenAds.Ad> out = new ArrayList();

         for(long id : selected) {
            PidsXiamenAds.Category category = category(id);
            if (category != null) {
               out.addAll(category.ads());
            }
         }

         if (out.isEmpty()) {
            PidsXiamenAds.Category fallback = defaultCategory();
            return fallback == null ? Collections.emptyList() : fallback.ads();
         } else {
            return Collections.unmodifiableList(out);
         }
      } else {
         PidsXiamenAds.Category fallback = defaultCategory();
         return fallback == null ? Collections.emptyList() : fallback.ads();
      }
   }

   public static List<PidsXiamenAds.Ad> ads() {
      return adsFor((long[])null);
   }

   public static List<PidsXiamenAds.Ad> allAds() {
      List<PidsXiamenAds.Ad> local = allAdsCache;
      if (local != null) {
         return local;
      } else {
         List<PidsXiamenAds.Ad> out = new ArrayList();
         Set<ResourceLocation> seen = new LinkedHashSet();

         for(PidsXiamenAds.Category category : categories()) {
            for(PidsXiamenAds.Ad ad : category.ads()) {
               if (ad.image() != null && seen.add(ad.image())) {
                  out.add(ad);
               }
            }
         }

         local = Collections.unmodifiableList(out);
         allAdsCache = local;
         return local;
      }
   }

   public static PidsXiamenAds.Ad adWithImage(String image) {
      if (image != null && !image.isEmpty()) {
         for(PidsXiamenAds.Ad ad : allAds()) {
            if (ad.image() != null && ad.image().toString().equals(image)) {
               return ad;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static int count() {
      return ads().size();
   }

   public static PidsXiamenAds.Ad get(int index) {
      List<PidsXiamenAds.Ad> list = ads();
      if (list.isEmpty()) {
         return null;
      } else {
         int i = (index % list.size() + list.size()) % list.size();
         return (PidsXiamenAds.Ad)list.get(i);
      }
   }

   private static List<PidsXiamenAds.Category> load() {
      Minecraft client = Minecraft.getInstance();
      if (client == null) {
         return fallback();
      } else {
         Map<Long, PidsXiamenAds.Category> merged = new LinkedHashMap();
         int files = 0;

         for(ResourceLocation id : new ResourceLocation[]{CONFIG_ID, TIANJIN_CONFIG_ID}) {
            try {
               for(Resource resource : client.getResourceManager().getResourceStack(id)) {
                  ++files;

                  try {
                     InputStream in = resource.open();

                     try {
                        parseInto(merged, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
                     } catch (Throwable var13) {
                        if (in != null) {
                           try {
                              in.close();
                           } catch (Throwable var12) {
                              var13.addSuppressed(var12);
                           }
                        }

                        throw var13;
                     }

                     if (in != null) {
                        in.close();
                     }
                  } catch (Throwable var14) {
                     LteLog.LOGGER.warn("[lte] xiamen pids ads: bad file inside package ({}){}", resource.sourcePackId(), var14);
                  }
               }
            } catch (Throwable var15) {
               LteLog.LOGGER.debug("[lte] could not read {} - skipping", id, var15);
            }
         }

         if (merged.isEmpty()) {
            return fallback();
         } else {
            List<PidsXiamenAds.Category> out = Collections.unmodifiableList(new ArrayList(merged.values()));
            LteLog.LOGGER.info("[lte] xiamen pids ads: {} categor(ies) from {} file(s){}", new Object[]{out.size(), files, describe(out)});
            return out;
         }
      }
   }

   private static String describe(List<PidsXiamenAds.Category> categories) {
      Minecraft client = Minecraft.getInstance();
      StringBuilder out = new StringBuilder();

      for(PidsXiamenAds.Category category : categories) {
         out.append(' ').append(category.key()).append('(').append(category.ads().size()).append(')');

         for(PidsXiamenAds.Ad ad : category.ads()) {
            String state = "?";

            try {
               if (client != null) {
                  state = client.getResourceManager().getResource(ad.image()).isPresent() ? "ok" : "MISSING";
               }
            } catch (Throwable var9) {
               state = "?";
            }

            out.append(' ').append(ad.image()).append('[').append(state).append(']');
         }
      }

      return out.toString();
   }

   static void parseInto(Map<Long, PidsXiamenAds.Category> out, JsonElement root) {
      if (root != null && root.isJsonObject()) {
         JsonObject top = root.getAsJsonObject();
         readAspectRatio(top);

         for(Map.Entry<String, JsonElement> entry : top.entrySet()) {
            String key = (String)entry.getKey();
            if (key != null && !key.isBlank() && !key.startsWith("_")) {
               JsonElement sectionElement = (JsonElement)entry.getValue();
               if (sectionElement != null && sectionElement.isJsonObject()) {
                  JsonObject section = sectionElement.getAsJsonObject();
                  JsonElement arrayElement = section.get("advertisements");
                  if (arrayElement != null && arrayElement.isJsonArray()) {
                     long id = (long)key.hashCode();
                     PidsXiamenAds.Category existing = (PidsXiamenAds.Category)out.get(id);
                     List<PidsXiamenAds.Ad> ads = existing == null ? new ArrayList() : new ArrayList(existing.ads());

                     for(PidsXiamenAds.Ad ad : parseAds(key, arrayElement.getAsJsonArray())) {
                        boolean seen = false;

                        for(PidsXiamenAds.Ad kept : ads) {
                           if (sameAd(kept, ad)) {
                              seen = true;
                              break;
                           }
                        }

                        if (!seen) {
                           ads.add(ad);
                        }
                     }

                     out.put(id, new PidsXiamenAds.Category(id, key, existing == null ? translatable(section, "name", key) : existing.name(), existing == null ? translatable(section, "description", key) : existing.description(), existing == null ? color(section) : existing.color(), Collections.unmodifiableList(ads)));
                  } else {
                     LteLog.LOGGER.warn("[lte] xiamen pids ads: bad category {} in {}: missing advertisement array. Skipping!", key, CONFIG_ID);
                  }
               }
            }
         }

      }
   }

   private static void readAspectRatio(JsonObject top) {
      try {
         JsonElement aspect = top.get("_comment_aspectRatio");
         if (aspect != null && aspect.getAsJsonPrimitive().isNumber()) {
            aspectRatio = aspect.getAsFloat();
         }
      } catch (Throwable var2) {
      }

   }

   private static List<PidsXiamenAds.Ad> parseAds(String key, JsonArray array) {
      List<PidsXiamenAds.Ad> out = new ArrayList(array.size());

      for(int i = 0; i < array.size(); ++i) {
         try {
            JsonElement entryElement = array.get(i);
            if (entryElement != null && entryElement.isJsonObject()) {
               JsonObject entry = entryElement.getAsJsonObject();
               JsonElement imageElement = entry.get("image");
               if (imageElement != null && !imageElement.isJsonNull()) {
                  ResourceLocation image = new ResourceLocation(imageElement.getAsString());
                  JsonElement textElement = entry.get("text");
                  Component text = textElement != null && !textElement.isJsonNull() ? Component.translatable(textElement.getAsString()) : Component.empty();
                  JsonElement urlElement = entry.get("url");
                  String url = urlElement != null && !urlElement.isJsonNull() && !urlElement.getAsString().isEmpty() ? urlElement.getAsString() : null;
                  out.add(new PidsXiamenAds.Ad(image, text, url));
               }
            }
         } catch (Throwable var12) {
            LteLog.LOGGER.warn("[lte] xiamen pids ads: bad advertisement #{} in category {} of {}", new Object[]{i, key, CONFIG_ID});
         }
      }

      return out;
   }

   private static Component translatable(JsonObject section, String field, String fallback) {
      try {
         JsonElement element = section.get(field);
         if (element != null && element.isJsonPrimitive()) {
            return Component.translatable(element.getAsString());
         }
      } catch (Throwable var4) {
      }

      return Component.literal(fallback);
   }

   private static int color(JsonObject section) {
      try {
         JsonElement element = section.get("color");
         if (element != null && element.isJsonPrimitive()) {
            String hex = element.getAsString().replace("#", "").trim();
            return -16777216 | Integer.parseInt(hex, 16) & 16777215;
         }
      } catch (Throwable var3) {
      }

      return -16738235;
   }

   private static boolean sameAd(PidsXiamenAds.Ad a, PidsXiamenAds.Ad b) {
      if (a != null && b != null && a.image() != null && b.image() != null) {
         if (!a.image().equals(b.image())) {
            return false;
         } else {
            String ta = a.text() == null ? "" : a.text().getString();
            String tb = b.text() == null ? "" : b.text().getString();
            return ta.equals(tb);
         }
      } else {
         return a == b;
      }
   }

   private static List<PidsXiamenAds.Category> fallback() {
      List<PidsXiamenAds.Ad> ads = new ArrayList(1);
      ads.add(new PidsXiamenAds.Ad(new ResourceLocation("mtrlte", "advertisements/xiamen_scenery.png"), Component.translatable("ticker.mtrlte.ad_welcome"), (String)null));
      PidsXiamenAds.Category only = new PidsXiamenAds.Category(DEFAULT_CATEGORY, "mtrlte", Component.translatable("advertisement.mtrlte.name"), Component.translatable("advertisement.mtrlte.description"), -16738235, Collections.unmodifiableList(ads));
      return Collections.singletonList(only);
   }

   public static record Ad(ResourceLocation image, Component text, String url) {
   }

   public static record Category(long id, String key, Component name, Component description, int color, List<PidsXiamenAds.Ad> ads) {
   }
}
