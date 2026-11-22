package dev.js.mtrheadlight.xiamen.eyecandy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.js.mtrheadlight.LteLog;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record EyecandyEntry(EyecandyEntry.Pack pack, String key, String displayName, ResourceLocation modelId, ResourceLocation texture, boolean flipV, float[] scale, float[] rotation, float[] translation, boolean[] mirror, int glow, ResourceLocation blockId, ResourceLocation bakedModel, int yaw) {
   public static List<EyecandyEntry> loadAll(EyecandyEntry.Pack pack) {
      List<EyecandyEntry> out = new ArrayList();

      try {
         InputStream in = EyecandyEntry.class.getResourceAsStream(pack.descriptor());

         Object var10;
         label63: {
            try {
               if (in == null) {
                  var10 = out;
                  break label63;
               }

               JsonObject root = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();

               for(Map.Entry<String, JsonElement> item : root.entrySet()) {
                  JsonObject json = ((JsonElement)item.getValue()).getAsJsonObject();
                  out.add(new EyecandyEntry(pack, (String)item.getKey(), string(json, "name", (String)item.getKey()), modelId(string(json, "model", "")), textureId(string(json, "textureId", "")), bool(json, "flipV", true), triple(json, "scale", 1.0F), triple(json, "rotation", 0.0F), triple(json, "translation", 0.0F), bools(json, "mirror"), glow(json), new ResourceLocation("mtrlte", "eyecandy_" + ((String)item.getKey()).toLowerCase(Locale.ROOT)), bakedModel(json), intValue(json, "yaw", 0)));
               }
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

            return out;
         }

         if (in != null) {
            in.close();
         }

         return (List<EyecandyEntry>)var10;
      } catch (Throwable var9) {
         LteLog.LOGGER.warn("[lte] eyecandy: could not parse {} ({})", pack.descriptor(), var9.toString());
         out.clear();
         return out;
      }
   }

   private static int intValue(JsonObject json, String key, int fallback) {
      JsonElement element = json.get(key);
      return element != null && !element.isJsonNull() ? element.getAsInt() : fallback;
   }

   private static ResourceLocation bakedModel(JsonObject json) {
      String raw = string(json, "baked", "");
      return raw != null && !raw.isEmpty() ? new ResourceLocation("mtrlte", raw) : null;
   }

   private static ResourceLocation modelId(String raw) {
      return new ResourceLocation("mtrlte", pathOnly(raw));
   }

   private static ResourceLocation textureId(String raw) {
      String path = pathOnly(raw);
      if (!path.startsWith("textures/")) {
         path = "textures/" + path;
      }

      if (!path.endsWith(".png")) {
         path = path + ".png";
      }

      return new ResourceLocation("mtrlte", path);
   }

   private static String pathOnly(String raw) {
      int colon = raw.indexOf(58);
      return colon < 0 ? raw : raw.substring(colon + 1);
   }

   private static String string(JsonObject json, String key, String fallback) {
      return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
   }

   private static boolean bool(JsonObject json, String key, boolean fallback) {
      return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsBoolean() : fallback;
   }

   private static int glow(JsonObject json) {
      if (json.has("glow") && !json.get("glow").isJsonNull()) {
         try {
            return Math.max(0, Math.min(15, json.get("glow").getAsInt()));
         } catch (RuntimeException var2) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   private static float[] triple(JsonObject json, String key, float fallback) {
      float[] out = new float[]{fallback, fallback, fallback};
      if (json.has(key) && json.get(key).isJsonArray()) {
         JsonArray array = json.getAsJsonArray(key);

         for(int i = 0; i < 3 && i < array.size(); ++i) {
            JsonElement value = array.get(i);
            out[i] = value != null && !value.isJsonNull() ? value.getAsFloat() : fallback;
         }

         return out;
      } else {
         return out;
      }
   }

   private static boolean[] bools(JsonObject json, String key) {
      boolean[] out = new boolean[]{false, false, false};
      if (json.has(key) && json.get(key).isJsonArray()) {
         JsonArray array = json.getAsJsonArray(key);

         for(int i = 0; i < 3 && i < array.size(); ++i) {
            JsonElement value = array.get(i);
            out[i] = value != null && !value.isJsonNull() && value.getAsBoolean();
         }

         return out;
      } else {
         return out;
      }
   }

   public static enum Pack {
      XIAMEN("/assets/mtrlte/eyecandies/first.json"),
      NANPUTUO("/assets/mtrlte/eyecandies/nanputuo.json");

      private final String descriptor;

      private Pack(final String descriptor) {
         this.descriptor = descriptor;
      }

      public String descriptor() {
         return this.descriptor;
      }
   }
}
