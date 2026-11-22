package dev.js.mtrheadlight.xiamen;

import dev.js.mtrheadlight.decor.DecorConfig;
import java.awt.Color;
import java.util.Locale;
import mtr.client.ClientData;
import mtr.data.Route;

public final class XiamenRouteStyle {
   public static final int FALLBACK_RGB = 8421504;
   public static final int NAME_GREY = 10395294;
   public static final int TEXT_DARK = 1710618;

   private XiamenRouteStyle() {
   }

   public static String shortName(String raw) {
      if (raw == null) {
         return "";
      } else {
         int cut = raw.indexOf("||");
         String head = cut >= 0 ? raw.substring(0, cut) : raw;
         return head.trim();
      }
   }

   public static String lineNumber(Route route, String routeName) {
      String digits = firstDigits(shortName(routeName));
      if (!digits.isEmpty()) {
         return digits;
      } else {
         if (route != null && route.lightRailRouteNumber != null) {
            String trimmed = route.lightRailRouteNumber.trim();
            if (!trimmed.isEmpty()) {
               return trimmed;
            }
         }

         return digits;
      }
   }

   private static String firstDigits(String text) {
      if (text == null) {
         return "";
      } else {
         int start = -1;

         int cp;
         for(int i = 0; i < text.length(); i += Character.charCount(cp)) {
            cp = text.codePointAt(i);
            boolean digit = cp >= 48 && cp <= 57;
            if (digit && start < 0) {
               start = i;
            } else if (!digit && start >= 0) {
               return text.substring(start, i);
            }
         }

         return start >= 0 ? text.substring(start) : "";
      }
   }

   public static Route findRoute(long platformId, String routeName) {
      String wanted = shortName(routeName);

      try {
         for(Route route : ClientData.ROUTES) {
            if (route != null && !route.isHidden && route.name != null && wanted.equals(shortName(route.name)) && route.containsPlatformId(platformId)) {
               return route;
            }
         }
      } catch (Throwable var6) {
      }

      return null;
   }

   public static Route findRouteByName(String routeName) {
      String wanted = shortName(routeName);
      if (wanted.isEmpty()) {
         return null;
      } else {
         try {
            for(Route route : ClientData.ROUTES) {
               if (route != null && !route.isHidden && route.name != null && wanted.equals(shortName(route.name))) {
                  return route;
               }
            }
         } catch (Throwable var4) {
         }

         return null;
      }
   }

   public static int lineColor(DecorConfig cfg, Route route, String routeName) {
      Integer override = overrideColor(cfg, route, routeName);
      if (override != null) {
         return override;
      } else {
         return route != null && (route.color & 16777215) != 0 ? route.color & 16777215 : 8421504;
      }
   }

   public static Integer overrideColor(DecorConfig cfg, Route route, String routeName) {
      if (cfg != null && cfg.lineColors != null && !cfg.lineColors.isEmpty()) {
         String name = shortName(routeName);
         String number = lineNumber(route, routeName);
         if (route != null) {
            Integer byId = lookup(cfg, String.valueOf(route.id));
            if (byId != null) {
               return byId;
            }
         }

         Integer byName = lookup(cfg, name);
         if (byName != null) {
            return byName;
         } else {
            if (!number.isEmpty()) {
               Integer byNumber = lookup(cfg, number);
               if (byNumber != null) {
                  return byNumber;
               }

               Integer bySuffix = lookup(cfg, number + "\u53f7\u7ebf");
               if (bySuffix != null) {
                  return bySuffix;
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private static Integer lookup(DecorConfig cfg, String key) {
      if (key != null && !key.isEmpty()) {
         String value = (String)cfg.lineColors.get(key);
         if (value == null) {
            return null;
         } else {
            int rgb = DecorConfig.parseColor(value, -1);
            return rgb >= 0 ? rgb : null;
         }
      } else {
         return null;
      }
   }

   public static boolean isLight(int rgb) {
      return XiamenStripPainter.isLight(rgb);
   }

   public static int contrastText(int rgb) {
      return XiamenStripPainter.contrastText(rgb);
   }

   public static int onWhiteText(int rgb) {
      return XiamenStripPainter.onWhiteText(rgb);
   }

   public static int currentNameText(int rgb) {
      return XiamenStripPainter.currentNameText(rgb);
   }

   public static int darken(int rgb, double factor) {
      return XiamenStripPainter.darken(rgb, factor);
   }

   public static String doorLightColor(DecorConfig cfg, String lineNumber) {
      if (cfg == null) {
         return "orange";
      } else {
         if (lineNumber != null && cfg.doorLightByLine != null && !cfg.doorLightByLine.isEmpty()) {
            String key = lineNumber.trim();
            String value = (String)cfg.doorLightByLine.get(key);
            if (value == null && !key.isEmpty()) {
               value = (String)cfg.doorLightByLine.get(key + "\u53f7\u7ebf");
            }

            if (value != null) {
               return DecorConfig.normalizeDoorLightColor(value);
            }

            String lower = key.toLowerCase(Locale.ROOT);
            if (cfg.doorLightByLine.containsKey(lower)) {
               return DecorConfig.normalizeDoorLightColor((String)cfg.doorLightByLine.get(lower));
            }
         }

         return DecorConfig.normalizeDoorLightColor(cfg.doorLightColor);
      }
   }

   public static Color awt(int rgb) {
      return new Color(rgb & 16777215);
   }
}
