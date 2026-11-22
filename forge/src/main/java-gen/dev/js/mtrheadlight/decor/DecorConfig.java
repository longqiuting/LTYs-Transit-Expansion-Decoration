package dev.js.mtrheadlight.decor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.js.mtrheadlight.LteLog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import dev.js.mtrheadlight.forge.platform.Env;

public final class DecorConfig {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   public static final String CONFIG_FILE = "mtrlte_decor.json";
   private static final String LEGACY_FILE = "mtrlte.json";
   private static final String[] LEGACY_NAMES = new String[]{"mtr-external-lights.json", "mtr-headlight.json"};
   public static final int CONFIG_VERSION = 1;
   public static final String DOOR_LIGHT_ORANGE = "orange";
   public static final String DOOR_LIGHT_GREEN = "green";
   public static final String DOOR_LIGHT_BLUE = "blue";
   private static final int FALLBACK_RGB = 8421504;
   public int configVersion = 1;
   public boolean xiamenRouteStrip = true;
   public boolean routeStripNamesBelow = false;
   public Map<String, String> lineColors = new LinkedHashMap();
   public String doorLightColor = "orange";
   public Map<String, String> doorLightByLine = new LinkedHashMap();
   public boolean fangsuDoorSound = true;
   private static volatile DecorConfig live = new DecorConfig();

   private DecorConfig() {
   }

   public static DecorConfig get() {
      return live;
   }

   public static DecorConfig defaults() {
      return new DecorConfig();
   }

   public static Path path() {
      return Env.configDir().resolve("mtrlte_decor.json");
   }

   public static DecorConfig load() {
      return load(Env.configDir());
   }

   public static DecorConfig load(Path configDir) {
      Path path = configDir.resolve("mtrlte_decor.json");
      if (Files.isRegularFile(path, new LinkOption[0])) {
         try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            DecorConfig cfg = (DecorConfig)GSON.fromJson(json, DecorConfig.class);
            if (cfg != null) {
               cfg.configVersion = 1;
               live = cfg.sanitized();
               return live;
            }
         } catch (RuntimeException | IOException var4) {
            LteLog.LOGGER.warn("[lte] could not read {} - using defaults", path, var4);
         }
      }

      DecorConfig fresh = migrateLegacy(configDir).sanitized();
      live = fresh;
      fresh.save(configDir);
      return fresh;
   }

   private static DecorConfig migrateLegacy(Path configDir) {
      JsonObject legacy = readLegacyObject(configDir);
      DecorConfig cfg = new DecorConfig();
      if (legacy == null) {
         return cfg;
      } else {
         int migrated = 0;
         JsonElement strip = legacy.get("xiamenRouteStrip");
         if (strip != null && strip.isJsonPrimitive() && strip.getAsJsonPrimitive().isBoolean()) {
            cfg.xiamenRouteStrip = strip.getAsBoolean();
            ++migrated;
         }

         JsonElement namesBelow = legacy.get("routeStripNamesBelow");
         if (namesBelow != null && namesBelow.isJsonPrimitive() && namesBelow.getAsJsonPrimitive().isBoolean()) {
            cfg.routeStripNamesBelow = namesBelow.getAsBoolean();
            ++migrated;
         }

         JsonElement colors = legacy.get("lineColors");
         if (colors != null && colors.isJsonObject()) {
            for(Map.Entry<String, JsonElement> e : colors.getAsJsonObject().entrySet()) {
               JsonElement v = (JsonElement)e.getValue();
               if (v != null && v.isJsonPrimitive()) {
                  cfg.lineColors.put((String)e.getKey(), v.getAsString());
               }
            }

            ++migrated;
         }

         JsonElement doorColor = legacy.get("doorLightColor");
         if (doorColor != null && doorColor.isJsonPrimitive()) {
            cfg.doorLightColor = doorColor.getAsString();
            ++migrated;
         }

         JsonElement doorByLine = legacy.get("doorLightByLine");
         if (doorByLine != null && doorByLine.isJsonObject()) {
            for(Map.Entry<String, JsonElement> e : doorByLine.getAsJsonObject().entrySet()) {
               JsonElement v = (JsonElement)e.getValue();
               if (v != null && v.isJsonPrimitive()) {
                  cfg.doorLightByLine.put((String)e.getKey(), v.getAsString());
               }
            }

            ++migrated;
         }

         if (migrated > 0) {
            LteLog.LOGGER.info("[lte] decor config: moved {} legacy key(s) from {} to {} (route strip / line colours / door lights)", new Object[]{migrated, "mtrlte.json", "mtrlte_decor.json"});
         }

         return cfg;
      }
   }

   private static JsonObject readLegacyObject(Path dir) {
      JsonObject primary = readObject(dir.resolve("mtrlte.json"));
      if (primary != null) {
         return primary;
      } else {
         for(String name : LEGACY_NAMES) {
            JsonObject o = readObject(dir.resolve(name));
            if (o != null) {
               return o;
            }
         }

         return null;
      }
   }

   private static JsonObject readObject(Path path) {
      try {
         if (!Files.isRegularFile(path, new LinkOption[0])) {
            return null;
         } else {
            JsonElement e = (JsonElement)GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonElement.class);
            return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
         }
      } catch (RuntimeException | IOException var2) {
         return null;
      }
   }

   public void save() {
      this.save(Env.configDir());
   }

   public void save(Path configDir) {
      Path path = configDir.resolve("mtrlte_decor.json");

      try {
         Files.createDirectories(path.getParent());
         Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
      } catch (IOException var4) {
         LteLog.LOGGER.warn("[lte] could not write {}", path, var4);
      }

   }

   private DecorConfig sanitized() {
      if (this.lineColors == null) {
         this.lineColors = new LinkedHashMap();
      }

      if (this.doorLightByLine == null) {
         this.doorLightByLine = new LinkedHashMap();
      }

      this.lineColors = sanitizeColorMap(this.lineColors);
      this.doorLightByLine = sanitizeDoorLightMap(this.doorLightByLine);
      this.doorLightColor = normalizeDoorLightColor(this.doorLightColor);
      return this;
   }

   public static int parseColor(String text, int fallback) {
      if (text == null) {
         return fallback;
      } else {
         String s = text.trim();
         if (s.startsWith("#")) {
            s = s.substring(1);
         }

         if (s.length() != 6) {
            return fallback;
         } else {
            try {
               return Integer.parseInt(s, 16) & 16777215;
            } catch (NumberFormatException var4) {
               return fallback;
            }
         }
      }
   }

   public static String formatColor(int rgb) {
      return String.format("#%06X", rgb & 16777215);
   }

   public static int fallbackRgb() {
      return 8421504;
   }

   public static String normalizeDoorLightColor(String raw) {
      if (raw != null) {
         String s = raw.trim().toLowerCase(Locale.ROOT);
         if ("green".equals(s)) {
            return "green";
         }

         if ("blue".equals(s)) {
            return "blue";
         }

         if ("orange".equals(s)) {
            return "orange";
         }
      }

      return "orange";
   }

   private static Map<String, String> sanitizeColorMap(Map<String, String> raw) {
      Map<String, String> out = new LinkedHashMap();
      if (raw != null) {
         raw.forEach((key, value) -> {
            if (key != null && value != null) {
               String k = key.trim();
               if (!k.isEmpty()) {
                  int parsed = parseColor(value, -1);
                  if (parsed >= 0) {
                     out.put(k, formatColor(parsed));
                  }

               }
            }
         });
      }

      return out;
   }

   private static Map<String, String> sanitizeDoorLightMap(Map<String, String> raw) {
      Map<String, String> out = new LinkedHashMap();
      if (raw != null) {
         raw.forEach((key, value) -> {
            if (key != null && value != null) {
               String k = key.trim();
               if (!k.isEmpty()) {
                  out.put(k, normalizeDoorLightColor(value));
               }

            }
         });
      }

      return out;
   }
}
