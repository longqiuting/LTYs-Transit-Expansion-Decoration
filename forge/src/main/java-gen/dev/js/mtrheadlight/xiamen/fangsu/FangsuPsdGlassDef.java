package dev.js.mtrheadlight.xiamen.fangsu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.js.mtrheadlight.LteLog;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record FangsuPsdGlassDef(String id, String nameZh, String nameEn, ResourceLocation model, boolean flipV, List<FangsuPsdGlassDef.Form> left, List<FangsuPsdGlassDef.Form> right, int defaultLeft, int defaultRight, List<FangsuPsdGlassDef.AutoRule> auto) {
   public static final String AUTO_KEY = "auto";
   public static final String SCREENDOOR_DOOR = "SCREENDOOR_DOOR";
   public static final String SCREENDOOR_GLASS = "SCREENDOOR_GLASS";
   public static final String NULL = "NULL";
   private static final String[] FILES = new String[]{"high"};
   private static final String DEFAULT_KEY = "glass";

   public static String value(String key, String leftBlock, String rightBlock) {
      String var10000;
      switch (key) {
         case "LEFT_BLOCK":
            var10000 = leftBlock;
            break;
         case "RIGHT_BLOCK":
            var10000 = rightBlock;
            break;
         default:
            var10000 = "NULL";
      }

      return var10000;
   }

   public static String langKey(String formKey) {
      return "block.mtrlte.fangsu_psd_glass_form." + formKey;
   }

   public FangsuPsdGlassDef.Form leftForm(int index) {
      return at(this.left, index);
   }

   public FangsuPsdGlassDef.Form rightForm(int index) {
      return at(this.right, index);
   }

   public int leftIndexOf(String key) {
      return indexOf(this.left, key);
   }

   public int rightIndexOf(String key) {
      return indexOf(this.right, key);
   }

   private static int indexOf(List<FangsuPsdGlassDef.Form> forms, String key) {
      if (key == null) {
         return -1;
      } else {
         for(int i = 0; i < forms.size(); ++i) {
            if (((FangsuPsdGlassDef.Form)forms.get(i)).key().equals(key)) {
               return i;
            }
         }

         return -1;
      }
   }

   private static FangsuPsdGlassDef.Form at(List<FangsuPsdGlassDef.Form> forms, int index) {
      return (FangsuPsdGlassDef.Form)forms.get(Math.max(0, Math.min(forms.size() - 1, index)));
   }

   public static List<FangsuPsdGlassDef> load() {
      List<FangsuPsdGlassDef> out = new ArrayList(FILES.length);

      for(String file : FILES) {
         try {
            FangsuPsdGlassDef def = loadOne(file);
            if (def != null) {
               out.add(def);
            }
         } catch (Throwable var6) {
            LteLog.LOGGER.warn("[lte] fangsu psd glass: could not read {}screendoor/xiamen/{}.json ({})", new Object[]{"fangsu_psd/", file, var6.toString()});
         }
      }

      return out;
   }

   private static FangsuPsdGlassDef loadOne(String file) {
      JsonObject root = FangsuPsdDef.read("fangsu_psd/screendoor/xiamen/" + file + ".json");
      if (!root.has("glass")) {
         return null;
      } else {
         JsonObject glass = root.getAsJsonObject("glass");
         List<FangsuPsdGlassDef.Form> left = forms(glass, "left");
         List<FangsuPsdGlassDef.Form> right = forms(glass, "right");
         if (!left.isEmpty() && !right.isEmpty()) {
            ResourceLocation model = new ResourceLocation(root.get("model").getAsString());
            boolean flipV = !root.has("flipV") || root.get("flipV").getAsBoolean();
            String label = file.equals("low") ? "\u4f4e" : "\u9ad8";
            String labelEn = file.equals("low") ? "low" : "high";
            return new FangsuPsdGlassDef("fangsu_psd_screendoor_" + file + "_glass", "\u65b9\u901f\u5c4f\u853d\u95e8\u73bb\u7483 \u53a6\u95e8" + label, "Fangsu platform screen door glass Xiamen " + labelEn, model, flipV, left, right, defaultOf(left), defaultOf(right), autoRules(glass));
         } else {
            return null;
         }
      }
   }

   private static List<FangsuPsdGlassDef.AutoRule> autoRules(JsonObject glass) {
      List<FangsuPsdGlassDef.AutoRule> rules = new ArrayList(8);
      if (glass.has("auto") && glass.get("auto").isJsonArray()) {
         Iterator var2 = glass.getAsJsonArray("auto").iterator();

         while(true) {
            JsonObject entry;
            List<FangsuPsdGlassDef.Condition> conditions;
            boolean always;
            label69:
            while(true) {
               if (!var2.hasNext()) {
                  return List.copyOf(rules);
               }

               JsonElement element = (JsonElement)var2.next();
               if (element.isJsonObject()) {
                  entry = element.getAsJsonObject();
                  if (entry.has("if")) {
                     conditions = new ArrayList(2);
                     always = false;
                     JsonElement condition = entry.get("if");
                     if (condition.isJsonPrimitive()) {
                        String key = condition.getAsString();
                        if ("TRUE".equals(key)) {
                           always = true;
                        } else if (entry.has("is")) {
                           conditions.add(new FangsuPsdGlassDef.Condition(key, entry.get("is").getAsString(), false));
                        } else if (entry.has("not")) {
                           conditions.add(new FangsuPsdGlassDef.Condition(key, entry.get("not").getAsString(), true));
                        } else {
                           always = true;
                        }
                        break;
                     }

                     if (condition.isJsonObject()) {
                        Iterator key = condition.getAsJsonObject().entrySet().iterator();

                        while(true) {
                           if (!key.hasNext()) {
                              break label69;
                           }

                           Map.Entry<String, JsonElement> test = (Map.Entry)key.next();
                           if (((JsonElement)test.getValue()).isJsonObject()) {
                              JsonObject value = ((JsonElement)test.getValue()).getAsJsonObject();
                              if (value.has("is")) {
                                 conditions.add(new FangsuPsdGlassDef.Condition((String)test.getKey(), value.get("is").getAsString(), false));
                              } else if (value.has("not")) {
                                 conditions.add(new FangsuPsdGlassDef.Condition((String)test.getKey(), value.get("not").getAsString(), true));
                              }
                           }
                        }
                     }
                  }
               }
            }

            rules.add(new FangsuPsdGlassDef.AutoRule(List.copyOf(conditions), always, entry.has("left") ? entry.get("left").getAsString() : null, entry.has("right") ? entry.get("right").getAsString() : null));
         }
      } else {
         return List.copyOf(rules);
      }
   }

   private static List<FangsuPsdGlassDef.Form> forms(JsonObject glass, String side) {
      List<FangsuPsdGlassDef.Form> forms = new ArrayList(4);
      if (!glass.has(side)) {
         return forms;
      } else {
         for(JsonElement element : glass.getAsJsonArray(side)) {
            JsonObject entry = element.getAsJsonObject();
            if (entry.has("id") && entry.has("subModel") && entry.has("shape")) {
               forms.add(new FangsuPsdGlassDef.Form(entry.get("id").getAsString(), entry.get("subModel").getAsString(), FangsuPsdDef.boxes(entry.getAsJsonArray("shape"))));
            }
         }

         return List.copyOf(forms);
      }
   }

   private static int defaultOf(List<FangsuPsdGlassDef.Form> forms) {
      for(int i = 0; i < forms.size(); ++i) {
         if ("glass".equals(((FangsuPsdGlassDef.Form)forms.get(i)).key())) {
            return i;
         }
      }

      return 0;
   }

   public static record AutoRule(List<FangsuPsdGlassDef.Condition> conditions, boolean always, String left, String right) {
      public boolean matches(String leftBlock, String rightBlock) {
         if (this.always) {
            return true;
         } else if (this.conditions.isEmpty()) {
            return false;
         } else {
            Iterator var3 = this.conditions.iterator();

            while(true) {
               if (!var3.hasNext()) {
                  return true;
               }

               FangsuPsdGlassDef.Condition condition = (FangsuPsdGlassDef.Condition)var3.next();
               String actual = FangsuPsdGlassDef.value(condition.key(), leftBlock, rightBlock);
               if (condition.negate()) {
                  if (condition.value().equals(actual)) {
                     break;
                  }
               } else if (!condition.value().equals(actual)) {
                  break;
               }
            }

            return false;
         }
      }
   }

   public static record Condition(String key, String value, boolean negate) {
   }

   public static record Form(String key, String group, List<double[]> shape) {
   }
}
