package dev.js.mtrheadlight.xiamen.pids;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.js.mtrheadlight.LteLog;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import dev.js.mtrheadlight.forge.platform.Env;

public final class PidsXiamenSettings {
   private static final String FILE_NAME = "lte_xiamen_pids.json";
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static volatile boolean localTime = true;
   private static volatile boolean arriving = true;
   private static volatile boolean advertisements = true;
   private static volatile boolean lineStrip = true;
   private static volatile String adOverride;
   private static volatile boolean loaded;

   private PidsXiamenSettings() {
   }

   private static Path file() {
      return Env.configDir().resolve("lte_xiamen_pids.json");
   }

   public static boolean localTime() {
      load();
      return localTime;
   }

   public static boolean arriving() {
      load();
      return arriving;
   }

   public static boolean advertisements() {
      load();
      return advertisements;
   }

   public static void setLocalTime(boolean on) {
      localTime = on;
      save();
   }

   public static void setArriving(boolean on) {
      arriving = on;
      save();
   }

   public static void setAdvertisements(boolean on) {
      advertisements = on;
      save();
   }

   public static boolean lineStrip() {
      load();
      return lineStrip;
   }

   public static void setLineStrip(boolean on) {
      lineStrip = on;
      save();
   }

   public static String adOverrideImage() {
      load();
      return adOverride;
   }

   public static void setAdOverrideImage(String image) {
      adOverride = image != null && !image.isEmpty() ? image : null;
      save();
   }

   public static PidsXiamenAds.Ad adOverride() {
      load();
      return PidsXiamenAds.adWithImage(adOverride);
   }

   public static List<PidsXiamenAds.Ad> applyAdOverride(List<PidsXiamenAds.Ad> own) {
      List<PidsXiamenAds.Ad> set = PidsAdSetCache.activeAds();
      if (set != null && !set.isEmpty()) {
         return set;
      } else {
         PidsXiamenAds.Ad ad = adOverride();
         return ad == null ? own : Collections.singletonList(ad);
      }
   }

   private static void load() {
      if (!loaded) {
         loaded = true;

         try {
            Path path = file();
            if (Files.isRegularFile(path, new LinkOption[0])) {
               Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);

               label77: {
                  try {
                     JsonObject json = (JsonObject)GSON.fromJson(reader, JsonObject.class);
                     if (json == null) {
                        break label77;
                     }

                     localTime = bool(json, "localTime", localTime);
                     arriving = bool(json, "arriving", arriving);
                     advertisements = bool(json, "advertisements", advertisements);
                     lineStrip = bool(json, "lineStrip", lineStrip);
                     adOverride = string(json, "adOverride", adOverride);
                  } catch (Throwable var5) {
                     if (reader != null) {
                        try {
                           reader.close();
                        } catch (Throwable var4) {
                           var5.addSuppressed(var4);
                        }
                     }

                     throw var5;
                  }

                  if (reader != null) {
                     reader.close();
                  }

                  LteLog.LOGGER.info("[lte] xiamen pids settings: localTime={} arriving={} ads={} lineStrip={} adOverride={}", new Object[]{localTime, arriving, advertisements, lineStrip, adOverride == null ? "-" : adOverride});
                  return;
               }

               if (reader != null) {
                  reader.close();
               }

            }
         } catch (Throwable var6) {
            LteLog.LOGGER.warn("[lte] xiamen pids settings: could not read {}", "lte_xiamen_pids.json", var6);
         }
      }
   }

   private static boolean bool(JsonObject json, String key, boolean fallback) {
      try {
         return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : fallback;
      } catch (Throwable var4) {
         return fallback;
      }
   }

   private static String string(JsonObject json, String key, String fallback) {
      try {
         if (json.has(key) && !json.get(key).isJsonNull()) {
            String value = json.get(key).getAsString();
            return value != null && !value.isBlank() ? value : fallback;
         } else {
            return fallback;
         }
      } catch (Throwable var4) {
         return fallback;
      }
   }

   public static void save() {
      try {
         Path path = file();
         Path parent = path.getParent();
         if (parent != null) {
            Files.createDirectories(parent);
         }

         JsonObject json = new JsonObject();
         json.addProperty("localTime", localTime);
         json.addProperty("arriving", arriving);
         json.addProperty("advertisements", advertisements);
         json.addProperty("lineStrip", lineStrip);
         if (adOverride != null && !adOverride.isBlank()) {
            json.addProperty("adOverride", adOverride);
         }

         Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);

         try {
            GSON.toJson(json, writer);
         } catch (Throwable var7) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (writer != null) {
            writer.close();
         }
      } catch (Throwable var8) {
         LteLog.LOGGER.warn("[lte] xiamen pids settings: could not write {}", "lte_xiamen_pids.json", var8);
      }

   }
}
