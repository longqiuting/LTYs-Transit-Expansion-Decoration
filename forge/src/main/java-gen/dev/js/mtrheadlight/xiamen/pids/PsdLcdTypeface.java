package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.xiamen.XiamenStripPainter;
import java.awt.Font;

public final class PsdLcdTypeface {
   private static Font cachedCjk;
   private static Font cachedLatin;

   private PsdLcdTypeface() {
   }

   public static Font cjk() {
      if (cachedCjk == null) {
         Font base = XiamenStripPainter.font();
         cachedCjk = base == null ? new Font("SansSerif", 1, 100) : base.deriveFont(1, 100.0F);
      }

      return cachedCjk;
   }

   public static Font latin() {
      if (cachedLatin == null) {
         Font base = XiamenStripPainter.font();
         cachedLatin = base == null ? new Font("SansSerif", 0, 100) : base.deriveFont(0, 100.0F);
      }

      return cachedLatin;
   }

   public static Font digits() {
      return cjk();
   }

   public static String name(Font font) {
      return font == null ? "-" : font.getPSName() + "@" + Math.round(font.getSize2D()) + (font.isBold() ? "/bold" : "/plain");
   }
}
