package dev.js.mtrheadlight.xiamen.pids;

public final class TextFit {
   public static final float MIN_SCALE = 0.62F;
   public static final String ELLIPSIS = "\u2026";
   private static final int STEPS = 14;

   private TextFit() {
   }

   public static float fitFontSize(String text, TextFit.Metrics font, float nominalSize, float maxWidth, float minScale) {
      if (font != null && text != null && !text.isEmpty() && !(nominalSize <= 0.0F)) {
         if (maxWidth > 0.0F && !Float.isInfinite(maxWidth)) {
            if (font.width(text, nominalSize) <= maxWidth) {
               return nominalSize;
            } else {
               float floorScale = Math.max(0.01F, Math.min(1.0F, minScale <= 0.0F ? 0.62F : minScale));
               float low = nominalSize * floorScale;
               if (font.width(text, low) > maxWidth) {
                  return low;
               } else {
                  float lo = low;
                  float hi = nominalSize;

                  for(int i = 0; i < 14; ++i) {
                     float mid = (lo + hi) * 0.5F;
                     if (mid <= lo || mid >= hi) {
                        break;
                     }

                     if (font.width(text, mid) <= maxWidth) {
                        lo = mid;
                     } else {
                        hi = mid;
                     }
                  }

                  return lo;
               }
            }
         } else {
            return nominalSize;
         }
      } else {
         return nominalSize;
      }
   }

   public static String ellipsize(String text, TextFit.Metrics font, float size, float maxWidth) {
      if (font != null && text != null && !text.isEmpty() && maxWidth > 0.0F) {
         if (font.width(text, size) <= maxWidth) {
            return text;
         } else {
            int codePoints = text.codePointCount(0, text.length());

            for(int keep = codePoints - 1; keep >= 1; --keep) {
               String cut = text.substring(0, text.offsetByCodePoints(0, keep)) + "\u2026";
               if (font.width(cut, size) <= maxWidth) {
                  return cut;
               }
            }

            return font.width("\u2026", size) <= maxWidth ? "\u2026" : "";
         }
      } else {
         return text;
      }
   }

   public static TextFit.Fitted fit(String text, TextFit.Metrics font, float nominalSize, float maxWidth, float minScale) {
      float size = fitFontSize(text, font, nominalSize, maxWidth, minScale);
      if (text != null && !text.isEmpty() && font != null) {
         String fitted = ellipsize(text, font, size, maxWidth);
         boolean shrunk = size < nominalSize - 0.01F;
         boolean truncated = !fitted.equals(text);
         return new TextFit.Fitted(size, fitted, shrunk, truncated);
      } else {
         return new TextFit.Fitted(size, text, false, false);
      }
   }

   public static record Fitted(float size, String text, boolean shrunk, boolean truncated) {
      public boolean untouched() {
         return !this.shrunk && !this.truncated;
      }
   }

   public interface Metrics {
      float width(String var1, float var2);
   }
}
