package dev.js.mtrheadlight.xiamen;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class XiamenDirectionPanel {
   public static final int IMG_HEIGHT = 256;
   public static final int MAX_WIDTH = 8192;
   public static final float PAD_RATIO = 0.055F;
   public static final float CJK_RATIO = 0.385F;
   public static final float EN_RATIO = 0.41F;
   public static final int TEXT_DARK = 1710618;
   public static volatile int lastWidth;
   public static volatile int lastHeight;
   public static volatile int lastCjkSize;
   public static volatile int lastEnSize;
   public static volatile boolean lastHasLeft;
   public static volatile boolean lastHasRight;

   private XiamenDirectionPanel() {
   }

   public static BufferedImage paint(String cjk, String en, float aspectRatio) {
      return paint(cjk, en, aspectRatio, false, false);
   }

   public static BufferedImage paint(String cjk, String en, float aspectRatio, boolean hasLeft, boolean hasRight) {
      if (!(aspectRatio > 0.05F)) {
         return null;
      } else {
         int height = 256;
         int width = Math.max(height, Math.round((float)height * aspectRatio));
         if (width > 8192) {
            height = Math.max(48, Math.round((float)height * 8192.0F / (float)width));
            width = 8192;
         }

         if (width >= 16 && height >= 16) {
            BufferedImage image = new BufferedImage(width, height, 2);
            Graphics2D g = image.createGraphics();

            BufferedImage left;
            try {
               g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
               g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
               g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
               g.setColor(Color.WHITE);
               g.fillRect(0, 0, width, height);
               int pad = Math.max(2, Math.round((float)height * 0.055F));
               String line = cjk == null ? "" : cjk.trim();
               String enText = en == null ? "" : en.trim();
               if (!line.isEmpty() || !enText.isEmpty()) {
                  int avail = Math.max(8, width - pad * 2);
                  Font ref = XiamenStripPainter.font().deriveFont(1, 100.0F);
                  int cjkW100 = line.isEmpty() ? 0 : g.getFontMetrics(ref).stringWidth(line);
                  int enW100 = enText.isEmpty() ? 0 : g.getFontMetrics(ref).stringWidth(enText);
                  float cjkSize = Math.max(7.0F, (float)height * 0.385F);
                  if (cjkW100 > 0) {
                     cjkSize = Math.min(cjkSize, 100.0F * (float)avail / (float)cjkW100);
                  }

                  float enSize = cjkSize * 0.41F;
                  if (enW100 > 0) {
                     enSize = Math.min(enSize, 100.0F * (float)avail / (float)enW100);
                  }

                  if (line.isEmpty()) {
                     cjkSize = Math.max(7.0F, enSize / 0.41F);
                  }

                  cjkSize = Math.max(7.0F, cjkSize);
                  enSize = Math.max(5.0F, enSize);
                  Font cjkFont = ref.deriveFont(cjkSize);
                  Font enFont = ref.deriveFont(0, enSize);
                  FontMetrics cjkFm = g.getFontMetrics(cjkFont);
                  FontMetrics enFm = g.getFontMetrics(enFont);
                  int totalH = (line.isEmpty() ? 0 : cjkFm.getHeight()) + (enText.isEmpty() ? 0 : enFm.getHeight());
                  int availH = Math.max(8, height - pad * 2);
                  if (totalH > availH) {
                     float k = (float)availH / (float)totalH;
                     cjkSize = Math.max(6.0F, cjkSize * k);
                     enSize = Math.max(5.0F, enSize * k);
                     cjkFont = ref.deriveFont(cjkSize);
                     enFont = ref.deriveFont(0, enSize);
                     cjkFm = g.getFontMetrics(cjkFont);
                     enFm = g.getFontMetrics(enFont);
                     totalH = (line.isEmpty() ? 0 : cjkFm.getHeight()) + (enText.isEmpty() ? 0 : enFm.getHeight());
                  }

                  int cjkW = line.isEmpty() ? 0 : cjkFm.stringWidth(line);
                  int enW = enText.isEmpty() ? 0 : enFm.stringWidth(enText);
                  int blockTop = (height - totalH) / 2;
                  g.setColor(new Color(1710618));
                  int y = blockTop;
                  if (!line.isEmpty()) {
                     g.setFont(cjkFont);
                     g.drawString(line, (float)pad + (float)(avail - cjkW) / 2.0F, (float)(blockTop + cjkFm.getAscent()));
                     y = blockTop + cjkFm.getHeight();
                  }

                  if (!enText.isEmpty()) {
                     g.setFont(enFont);
                     g.drawString(enText, (float)pad + (float)(avail - enW) / 2.0F, (float)(y + enFm.getAscent()));
                  }

                  lastWidth = width;
                  lastHeight = height;
                  lastCjkSize = Math.round(cjkSize);
                  lastEnSize = Math.round(enSize);
                  lastHasLeft = hasLeft;
                  lastHasRight = hasRight;
                  return image;
               }

               lastHasLeft = hasLeft;
               lastHasRight = hasRight;
               left = image;
            } finally {
               g.dispose();
            }

            return left;
         } else {
            return null;
         }
      }
   }
}
