package dev.js.mtrheadlight.xiamen.fangsu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.js.mtrheadlight.LteLog;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record FangsuPsdDef(String id, String nameZh, String nameEn, ResourceLocation model, boolean flipV, FangsuPsdDef.Kind kind, String staticGroup, List<FangsuPsdDef.Motion> motions, List<double[]> closeBoxes) {
   public static final String DIR = "fangsu_psd/";
   private static final String[] DUANMEN = new String[]{"brtduanmen", "mtrduanmen", "drlduanmen", "shlowduanmen", "shhighduanmen"};
   private static final String[] SCREENDOOR = new String[]{"low", "high"};

   private static String[] duanmenName(String file) {
      String[] var10000;
      switch (file) {
         case "brtduanmen":
            var10000 = new String[]{"\u65b9\u901f\u7aef\u95e8 BRT", "Fangsu end door BRT"};
            break;
         case "mtrduanmen":
            var10000 = new String[]{"\u65b9\u901f\u7aef\u95e8 MTR", "Fangsu end door MTR"};
            break;
         case "drlduanmen":
            var10000 = new String[]{"\u65b9\u901f\u7aef\u95e8 DRL", "Fangsu end door DRL"};
            break;
         case "shlowduanmen":
            var10000 = new String[]{"\u65b9\u901f\u7aef\u95e8 SH \u4f4e", "Fangsu end door SH low"};
            break;
         case "shhighduanmen":
            var10000 = new String[]{"\u65b9\u901f\u7aef\u95e8 SH \u9ad8", "Fangsu end door SH high"};
            break;
         default:
            var10000 = new String[]{file, file};
      }

      return var10000;
   }

   public static List<FangsuPsdDef> load() {
      List<FangsuPsdDef> out = new ArrayList(14);

      for(String file : DUANMEN) {
         try {
            loadDuanmen(file, out);
         } catch (Throwable var7) {
            LteLog.LOGGER.warn("[lte] fangsu psd: could not read {}{}.json ({})", new Object[]{"fangsu_psd/", file, var7.toString()});
         }
      }

      for(String file : SCREENDOOR) {
         try {
            loadScreendoor(file, out);
         } catch (Throwable var6) {
            LteLog.LOGGER.warn("[lte] fangsu psd: could not read {}screendoor/xiamen/{}.json ({})", new Object[]{"fangsu_psd/", file, var6.toString()});
         }
      }

      return out;
   }

   private static void loadDuanmen(String file, List<FangsuPsdDef> out) {
      JsonObject root = read("fangsu_psd/duanmen/" + file + ".json");
      String[] names = duanmenName(file);

      for(JsonElement element : root.getAsJsonArray("content")) {
         JsonObject entry = element.getAsJsonObject();
         String rawId = entry.get("id").getAsString();
         boolean right = rawId.endsWith("_right");
         JsonObject sub = entry.getAsJsonObject("subModels");
         String mainGroup = sub.get("main").getAsString();
         String doorGroup = sub.get("door").getAsString();
         float[] doorPos = floats(entry.getAsJsonArray("doorPos"));
         float[] pivot = new float[]{doorPos[0] * 16.0F + 8.0F, doorPos[1] * 16.0F, doorPos[2] * 16.0F + 8.0F};
         float angle = entry.get("doorAngle").getAsFloat() * ((float)Math.PI / 2F);
         List<double[]> close = boxes(entry.getAsJsonObject("shape").getAsJsonArray("close"));
         out.add(new FangsuPsdDef("fangsu_psd_" + rawId, names[0] + (right ? "\uff08\u53f3\uff09" : "\uff08\u5de6\uff09"), names[1] + (right ? " (right)" : " (left)"), new ResourceLocation(entry.get("model").getAsString()), !entry.has("flipV") || entry.get("flipV").getAsBoolean(), FangsuPsdDef.Kind.END_DOOR, mainGroup, List.of(new FangsuPsdDef.Motion(doorGroup, pivot, angle, 0.0F)), close));
      }

   }

   private static void loadScreendoor(String file, List<FangsuPsdDef> out) {
      JsonObject root = read("fangsu_psd/screendoor/xiamen/" + file + ".json");
      ResourceLocation model = new ResourceLocation(root.get("model").getAsString());
      boolean flipV = !root.has("flipV") || root.get("flipV").getAsBoolean();
      JsonObject doors = root.getAsJsonObject("door");
      String label = file.equals("low") ? "\u4f4e" : "\u9ad8";
      String labelEn = file.equals("low") ? "low" : "high";

      for(String side : new String[]{"left", "right"}) {
         List<FangsuPsdDef.Motion> motions = new ArrayList(2);
         List<double[]> close = new ArrayList(2);

         for(JsonElement entryElement : doors.getAsJsonArray(side)) {
            JsonObject entry = entryElement.getAsJsonObject();
            if (entry.has("doors")) {
               for(JsonElement doorElement : entry.getAsJsonArray("doors")) {
                  JsonObject door = doorElement.getAsJsonObject();
                  motions.add(new FangsuPsdDef.Motion(door.get("subModel").getAsString(), new float[]{8.0F, 0.0F, 8.0F}, 0.0F, -door.get("step").getAsFloat()));
                  close.addAll(boxes(door.getAsJsonArray("shape")));
               }
            }
         }

         if (!motions.isEmpty()) {
            out.add(new FangsuPsdDef("fangsu_psd_screendoor_" + file + "_" + side, "\u65b9\u901f\u5c4f\u853d\u95e8 \u53a6\u95e8" + label + "\uff08" + (side.equals("left") ? "\u5de6" : "\u53f3") + "\uff09", "Fangsu platform screen door Xiamen " + labelEn + (side.equals("left") ? " (left)" : " (right)"), model, flipV, FangsuPsdDef.Kind.SCREENDOOR, (String)null, List.copyOf(motions), List.copyOf(close)));
         }
      }

   }

   static JsonObject read(String path) {
      String resource = "/assets/mtrlte/" + path;

      try {
         InputStream in = FangsuPsdDef.class.getResourceAsStream(resource);

         JsonObject var3;
         try {
            if (in == null) {
               throw new IllegalStateException(resource + " is missing");
            }

            var3 = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
         } catch (Throwable var6) {
            if (in != null) {
               try {
                  in.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (in != null) {
            in.close();
         }

         return var3;
      } catch (IOException var7) {
         throw new IllegalStateException(var7.toString(), var7);
      }
   }

   private static float[] floats(JsonArray array) {
      float[] out = new float[array.size()];

      for(int i = 0; i < out.length; ++i) {
         out[i] = array.get(i).getAsFloat();
      }

      return out;
   }

   static List<double[]> boxes(JsonArray array) {
      List<double[]> out = new ArrayList(array.size());

      for(JsonElement element : array) {
         JsonArray box = element.getAsJsonArray();
         double[] values = new double[6];

         for(int i = 0; i < 6; ++i) {
            values[i] = box.get(i).getAsDouble();
         }

         out.add(new double[]{Math.min(values[0], values[3]), Math.min(values[1], values[4]), Math.min(values[2], values[5]), Math.max(values[0], values[3]), Math.max(values[1], values[4]), Math.max(values[2], values[5])});
      }

      return out;
   }

   public static enum Kind {
      END_DOOR,
      SCREENDOOR;
   }

   public static record Motion(String group, float[] pivot, float angle, float step) {
      public boolean rotating() {
         return this.angle != 0.0F;
      }
   }
}
