package dev.js.mtrheadlight.xiamen.dep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.js.mtrheadlight.LteLog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import dev.js.mtrheadlight.forge.platform.Env;

public final class DepOnWallOrient {
   private static final String FILE_NAME = "mtrlte-dep-orient.json";
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   public static final DepOnWallOrient.Mode DEFAULT = DepOnWallOrient.Mode.MIRROR;
   private static final int CONFIG_VERSION = 2;
   private static DepOnWallOrient.Mode current;

   private DepOnWallOrient() {
   }

   public static DepOnWallOrient.Mode mode() {
      if (current == null) {
         current = read();
      }

      return current;
   }

   public static boolean flipped() {
      return mode() != DepOnWallOrient.Mode.NORMAL;
   }

   public static void set(DepOnWallOrient.Mode mode) {
      if (mode != null) {
         current = mode;
         write(mode);
         LteLog.LOGGER.info("[lte] dep orientation = {}", mode.id());
      }
   }

   public static DepOnWallOrient.Mode cycle() {
      DepOnWallOrient.Mode next = mode().next();
      set(next);
      return next;
   }

   private static Path path() {
      return Env.configDir().resolve("mtrlte-dep-orient.json");
   }

   private static DepOnWallOrient.Mode read() {
      Path path = path();
      if (!Files.exists(path, new LinkOption[0])) {
         return DEFAULT;
      } else {
         try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            int version = object.has("version") ? object.get("version").getAsInt() : 0;
            if (version < 2) {
               LteLog.LOGGER.info("[lte] dep orientation: {} is v{} (need v{}), using default {}", new Object[]{path, version, 2, DEFAULT.id()});
               return DEFAULT;
            } else {
               DepOnWallOrient.Mode mode = object.has("mode") ? DepOnWallOrient.Mode.byId(object.get("mode").getAsString()) : null;
               if (mode == null) {
                  LteLog.LOGGER.warn("[lte] dep orientation: {} has no usable \"mode\", using {}", path, DEFAULT.id());
                  return DEFAULT;
               } else {
                  LteLog.LOGGER.info("[lte] dep orientation loaded: {}", mode.id());
                  return mode;
               }
            }
         } catch (RuntimeException | IOException var5) {
            LteLog.LOGGER.warn("[lte] dep orientation: could not read {}", path, var5);
            return DEFAULT;
         }
      }
   }

   private static void write(DepOnWallOrient.Mode mode) {
      Path path = path();

      try {
         Files.createDirectories(path.getParent());
         JsonObject object = new JsonObject();
         object.addProperty("version", 2);
         object.addProperty("mode", mode.id());
         object.addProperty("_comment", "mtrlte:dep_on_wall \u7684\u8bfb\u6570\u65b9\u5411\uff1b\u6e38\u620f\u5185\u7528 /mtr_lte dep \u5207\u6362");
         Files.writeString(path, GSON.toJson(object), StandardCharsets.UTF_8);
      } catch (IOException var3) {
         LteLog.LOGGER.warn("[lte] dep orientation: could not write {}", path, var3);
      }

   }

   public static String statusLine() {
      DepOnWallOrient.Mode mode = mode();
      return "LTE\uff1a\u5012\u6570\u5668\u65b9\u5411 = " + mode.id() + "\uff08" + mode.label() + "\uff09 \u2014 \u4e0b\u4e00\u6863\uff1a" + mode.next().id() + "\uff1b\u5207\u6362\uff1a/mtr_lte dep\uff08\u4f9d\u6b21 normal \u2192 mirror \u2192 flip \u2192 rotate\uff09";
   }

   public static enum Mode {
      NORMAL("normal", "\u57fa\u51c6\uff08+X = \u73a9\u5bb6\u53f3\u624b\uff09", 1, 1),
      MIRROR("mirror", "\u5de6\u53f3\u7ffb", -1, 1),
      FLIP_Y("flip", "\u4e0a\u4e0b\u7ffb", 1, -1),
      ROTATE("rotate", "\u7ffb 180\u00b0\uff08\u4e0a\u4e0b\u5de6\u53f3\u90fd\u7ffb\uff09", -1, -1);

      private final String id;
      private final String label;
      private final float scaleX;
      private final float scaleY;

      private Mode(final String id, final String label, final int scaleX, final int scaleY) {
         this.id = id;
         this.label = label;
         this.scaleX = (float)scaleX;
         this.scaleY = (float)scaleY;
      }

      public String id() {
         return this.id;
      }

      public String label() {
         return this.label;
      }

      void apply(PoseStack matrices) {
         if (this.scaleX < 0.0F || this.scaleY < 0.0F) {
            matrices.scale(this.scaleX, this.scaleY, 1.0F);
         }

      }

      public DepOnWallOrient.Mode next() {
         DepOnWallOrient.Mode[] all = values();
         return all[(this.ordinal() + 1) % all.length];
      }

      public static DepOnWallOrient.Mode byId(String raw) {
         if (raw == null) {
            return null;
         } else {
            String key = raw.trim().toLowerCase(Locale.ROOT);

            for(DepOnWallOrient.Mode mode : values()) {
               if (mode.id.equals(key) || mode.name().toLowerCase(Locale.ROOT).equals(key)) {
                  return mode;
               }
            }

            DepOnWallOrient.Mode var10000;
            switch (key) {
               case "none":
               case "off":
               case "base":
               case "normal":
               case "default":
                  var10000 = NORMAL;
                  break;
               case "x":
               case "h":
               case "horizontal":
               case "mirror_x":
                  var10000 = MIRROR;
                  break;
               case "y":
               case "v":
               case "vertical":
               case "flip_y":
               case "flipy":
                  var10000 = FLIP_Y;
                  break;
               case "180":
               case "rot180":
               case "rotate180":
               case "both":
                  var10000 = ROTATE;
                  break;
               default:
                  var10000 = null;
            }

            return var10000;
         }
      }
   }
}
