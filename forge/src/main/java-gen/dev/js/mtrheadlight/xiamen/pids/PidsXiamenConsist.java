package dev.js.mtrheadlight.xiamen.pids;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class PidsXiamenConsist {
   public static final int SCALE = 4;
   public static final int STRIP_X0 = 179;
   public static final int STRIP_Y0 = 0;
   public static final int STRIP_W = 333;
   public static final int STRIP_H = 33;
   public static final int BAND_INK_X0_DEFAULT = 16;
   public static final float BAND_PAD = 3.0F;
   public static final int ROW_INSET = 31;
   public static final int BAND_CAP_DEFAULT = 2;
   public static final int CARD_SRC_X = 210;
   public static final int CARD_SRC_Y = 3;
   public static final int CARD_SRC_W = 41;
   public static final int CARD_SRC_H = 20;
   public static final int FLAKE_SRC_X = 228;
   public static final int FLAKE_SRC_Y = 23;
   public static final int FLAKE_SRC_W = 7;
   public static final int FLAKE_SRC_H = 7;
   public static final float CARD_W = 41.0F;
   public static final float CARD_H = 20.0F;
   public static final float CARD_TOP = 3.0F;
   public static final float GAP = 5.6F;
   public static final float MIN_CARD_W = 18.0F;
   public static final float GAP_MIN = 2.0F;
   public static final float GAP_HARD_MIN = 1.0F;
   public static final float FLAKE_TOP = 23.0F;
   public static final float FLAKE_W = 7.0F;
   public static final float FLAKE_GAP = 2.0F;
   public static final float FLAKE_PAD = 1.0F;
   public static final float CAR_TEXT_TOP = 24.0F;
   public static final float CAR_TEXT_INK_H = 6.0F;
   public static final float CAR_TEXT_GAP = 1.0F;
   public static final float FLAKE_MIN_W = 4.0F;
   public static final float CAR_TEXT_MIN_W = 5.0F;
   public static final int MAX_CARS = 16;
   public static final int CONTENT_VERSION = 8;
   private static final int KEY_FULL = 96;
   private static int GEOM_LEFT = 35;
   private static int GEOM_CAP = 2;
   private static int GEOM_BODY = 313;
   private static int GEOM_INK_X0 = 16;
   private static BufferedImage GEOM_FOR;
   private static BufferedImage CACHED_CARD_KEYED;
   private static BufferedImage CACHED_CARD_SRC;
   private static BufferedImage CACHED_FLAKE_KEYED;
   private static BufferedImage CACHED_FLAKE_SRC;

   public static float rowAvail() {
      return 302.0F - tailPad();
   }

   public static float tailPad() {
      return Math.max(3.0F + (float)Math.max(1, GEOM_CAP), (float)(31 - bandInkInset()));
   }

   public static int opaqueBelowStrip(BufferedImage overlay) {
      if (overlay == null) {
         return 0;
      } else {
         int from = Math.min(overlay.getHeight(), 132);
         int count = 0;

         for(int y = from; y < overlay.getHeight(); ++y) {
            for(int x = 0; x < overlay.getWidth(); ++x) {
               if ((overlay.getRGB(x, y) >>> 24 & 255) != 0) {
                  ++count;
               }
            }
         }

         return count;
      }
   }

   private PidsXiamenConsist() {
   }

   public static float[] strip() {
      return new float[]{179.0F, 0.0F, 333.0F, 33.0F};
   }

   public static int overlayWidth() {
      return 1332;
   }

   public static int overlayHeight() {
      return 132;
   }

   public static float[] stripNormalized() {
      return new float[]{0.34960938F, 0.0F, 0.6503906F, 0.14042553F};
   }

   public static int[] bandSegments(BufferedImage band) {
      if (band == null) {
         return new int[]{GEOM_LEFT, Math.max(1, GEOM_BODY - GEOM_LEFT - GEOM_CAP), GEOM_CAP};
      } else {
         segments(band);
         int bodyW = GEOM_BODY;
         int left = Math.min(bodyW - 2, GEOM_LEFT);
         int cap = Math.max(1, Math.min(bodyW - left - 1, GEOM_CAP));
         return new int[]{left, Math.max(1, bodyW - left - cap), cap};
      }
   }

   public static int bandBodyWidth(BufferedImage band) {
      if (band != null) {
         segments(band);
      }

      return GEOM_BODY;
   }

   public static float contentWidth(int cars) {
      PidsXiamenConsist.Card[] cards = layout(cars);
      return cards.length == 0 ? 0.0F : contentWidthOf(cards.length, cards[0].w(), cards.length > 1 ? cards[1].x0() - cards[0].x1() : 0.0F);
   }

   private static float contentWidthOf(int n, float cw, float gap) {
      if (n <= 0) {
         return 0.0F;
      } else {
         float span = (float)n * cw + (float)(n - 1) * gap;
         float tail = tailPad();
         float natural = 31.0F + span + tail;
         float minimal = 72.0F + tail;
         return Math.min(333.0F, Math.max(minimal, natural));
      }
   }

   public static int bandInkInset() {
      return Math.max(0, Math.min(Math.max(0, GEOM_BODY - 2), GEOM_INK_X0));
   }

   public static float bandX0(int cars) {
      return bandX0Of(contentWidth(cars));
   }

   private static float bandX0Of(float contentW) {
      return 179.0F + ((float)(333 - bandInkInset()) - contentW) * 0.5F;
   }

   public static float[] bandMargins(int cars) {
      float x0 = bandX0(cars);
      float inkLeft = x0 + (float)bandInkInset();
      float inkRight = x0 + contentWidth(cars);
      return new float[]{inkLeft - 179.0F, 512.0F - inkRight};
   }

   public static float bandRight(int cars) {
      return bandX0(cars) + contentWidth(cars);
   }

   public static PidsXiamenConsist.Card[] layout(int cars) {
      if (cars <= 0) {
         return new PidsXiamenConsist.Card[0];
      } else {
         int n = Math.min(16, cars);
         float avail = rowAvail();
         float gap = 5.6F;
         float cw = widthFor(n, gap);
         if (cw < 41.0F) {
            if (cw < 18.0F) {
               gap = Math.max(2.0F, gapFor(n, 18.0F));
               cw = widthFor(n, gap);
            }

            if (cw < 18.0F) {
               gap = Math.max(1.0F, gapFor(n, 18.0F));
               cw = widthFor(n, gap);
            }
         }

         cw = Math.min(41.0F, cw);
         if (n > 1 && (float)n * cw + (float)(n - 1) * gap > avail) {
            cw = Math.max(1.0F, (avail - (float)(n - 1) * gap) / (float)n);
         }

         float step = cw + gap;
         float contentW = contentWidthOf(n, cw, gap);
         float x0 = bandX0Of(contentW) + 31.0F;
         PidsXiamenConsist.Card[] out = new PidsXiamenConsist.Card[n];

         for(int i = 0; i < n; ++i) {
            float cx = x0 + (float)i * step;
            int flakes = i != 0 && i != n - 1 ? 2 : 1;
            float flakeW = flakeSizeFor(cw, flakes);
            float snowW = (float)flakes * flakeW + (float)(flakes - 1) * 2.0F;
            float flakeX0 = Math.max(cx + 0.5F, cx + cw - 1.0F - snowW);
            float textRight = flakeX0 - 1.0F;
            float textW = Math.max(2.0F, textRight - cx - 0.5F);
            out[i] = new PidsXiamenConsist.Card(i, cx, cw, flakes, flakeW, flakeX0, snowW, textRight, textW);
         }

         return out;
      }
   }

   public static float flakeSizeFor(float slotW, int flakes) {
      float budget = slotW - 1.0F - 1.0F - 5.0F;

      for(float fs = 7.0F; fs >= 4.0F; --fs) {
         if ((float)flakes * fs + (float)(flakes - 1) * 2.0F <= budget + 1.0E-4F) {
            return fs;
         }
      }

      return 4.0F;
   }

   private static float widthFor(int n, float gap) {
      return n <= 0 ? 41.0F : (rowAvail() - (float)(n - 1) * gap) / (float)n;
   }

   private static float gapFor(int n, float w) {
      return n <= 1 ? 5.6F : (rowAvail() - (float)n * w) / (float)(n - 1);
   }

   public static boolean insideBand(PidsXiamenConsist.Card card) {
      boolean cardOk = card.x0() >= 179.0F && card.x1() <= 512.0F;
      float flakeTop = 23.0F + (7.0F - card.flakeW());
      boolean flakeOk = flakeTop >= 2.0F && flakeTop + card.flakeW() <= 31.0F && card.flakeX0() >= card.x0() && card.flakeX0() + card.snowW() <= card.x1() && card.flakeX0() >= 179.0F && card.flakeX0() + card.snowW() <= 512.0F;
      boolean textOk = card.textRight() <= 512.0F && card.textRight() <= card.flakeX0() - 1.0F + 1.0E-4F;
      return cardOk && flakeOk && textOk;
   }

   public static long hash(int cars, BufferedImage... assets) {
      long h = 8000024L + (long)cars;

      for(BufferedImage asset : assets) {
         h = h * 31L + (long)System.identityHashCode(asset);
      }

      return h;
   }

   public static BufferedImage paint(BufferedImage band, BufferedImage card, BufferedImage flake, int cars) {
      int n = Math.min(16, Math.max(0, cars));
      if (n <= 0 || band != null && band.getWidth() >= 8 && band.getHeight() >= 4 && card != null && card.getWidth() >= 2 && card.getHeight() >= 2 && flake != null && flake.getWidth() >= 2 && flake.getHeight() >= 2) {
         try {
            return paintUnsafe(band, card, flake, n);
         } catch (Throwable var6) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static BufferedImage paintUnsafe(BufferedImage band, BufferedImage card, BufferedImage flake, int cars) {
      int w = overlayWidth();
      int h = overlayHeight();
      int[] pixels = new int[w * h];
      Arrays.fill(pixels, 0);
      if (cars > 0) {
         paintBand(pixels, band, dev(bandX0(cars) - 179.0F), Math.round(contentWidth(cars) * 4.0F));
         BufferedImage cardKeyed = keyed(card, true);
         BufferedImage flakeKeyed = keyed(flake, false);

         for(PidsXiamenConsist.Card c : layout(cars)) {
            blitSqueezed(pixels, w, h, cardKeyed, dev(c.x0() - 179.0F), dev(3.0F), Math.max(1, Math.round(c.w() * 4.0F)), Math.max(1, Math.round(80.0F)));
            int flakeDev = Math.max(1, Math.round(c.flakeW() * 4.0F));
            int flakeTop = dev(23.0F + (7.0F - c.flakeW()) - 0.0F);

            for(int k = 0; k < c.flakes(); ++k) {
               float fx = c.flakeX0() + (float)k * (c.flakeW() + 2.0F);
               blitSqueezed(pixels, w, h, flakeKeyed, dev(fx - 179.0F), flakeTop, flakeDev, flakeDev);
            }
         }
      }

      BufferedImage out = new BufferedImage(w, h, 2);
      out.setRGB(0, 0, w, h, pixels, 0, w);
      return out;
   }

   private static int dev(float panelPx) {
      return Math.round(panelPx * 4.0F);
   }

   private static void blitSqueezed(int[] dst, int dstWidth, int dstHeight, BufferedImage src, int x0, int y0, int targetW, int targetH) {
      if (targetW > 0 && targetH > 0) {
         int sw = src.getWidth();
         int sh = src.getHeight();

         for(int dx = 0; dx < targetW; ++dx) {
            int px = x0 + dx;
            if (px >= 0 && px < dstWidth) {
               int sx = Math.min(sw - 1, (int)((long)dx * (long)sw / (long)targetW));

               for(int dy = 0; dy < targetH; ++dy) {
                  int py = y0 + dy;
                  if (py >= 0 && py < dstHeight) {
                     int argb = src.getRGB(sx, Math.min(sh - 1, (int)((long)dy * (long)sh / (long)targetH)));
                     int a = argb >>> 24 & 255;
                     if (a != 0) {
                        int i = py * dstWidth + px;
                        dst[i] = a >= 255 ? argb : over(dst[i], argb, a);
                     }
                  }
               }
            }
         }

      }
   }

   private static int over(int dstArgb, int srcArgb, int a) {
      int inv = 255 - a;
      int r = ((srcArgb >> 16 & 255) * a + (dstArgb >> 16 & 255) * inv) / 255;
      int g = ((srcArgb >> 8 & 255) * a + (dstArgb >> 8 & 255) * inv) / 255;
      int b = ((srcArgb & 255) * a + (dstArgb & 255) * inv) / 255;
      return -16777216 | r << 16 | g << 8 | b;
   }

   private static void segments(BufferedImage band) {
      if (GEOM_FOR != band || GEOM_LEFT <= 0) {
         int left = leftSegment(band);
         int bodyRightCol = Math.max(left + 1, bodyRight(band));
         int span = Math.max(2, bodyRightCol + 1 - left);
         GEOM_LEFT = left;
         GEOM_BODY = bodyRightCol + 1;
         GEOM_CAP = Math.max(1, Math.min(Math.min(8, span - 1), edgeRun(band, bodyRightCol) + 1));
         GEOM_INK_X0 = Math.max(0, Math.min(GEOM_LEFT - 1, inkX0(band)));
         GEOM_FOR = band;
      }
   }

   private static int inkX0(BufferedImage band) {
      int first = band.getWidth();

      for(int y = 0; y < band.getHeight() && first > 0; ++y) {
         for(int x = 0; x < first; ++x) {
            if (!isCyan(band.getRGB(x, y))) {
               first = x;
               break;
            }
         }
      }

      return first >= band.getWidth() ? 0 : first;
   }

   private static void paintBand(int[] pixels, BufferedImage band, int dstX0, int dstBandW) {
      int dstW = overlayWidth();
      int dstH = overlayHeight();
      int target = Math.min(dstW - dstX0, dstBandW);
      if (target > 0 && dstX0 < dstW) {
         int srcH = band.getHeight();
         float scale = (float)dstH / (float)srcH;
         segments(band);
         int leftW = GEOM_LEFT;
         int capW = GEOM_CAP;
         int bodyW = GEOM_BODY;
         int midSrc = Math.max(1, bodyW - leftW - capW);
         int capSrcX0 = bodyW - capW;
         int dstLeft = Math.min(target, Math.round((float)leftW * scale));
         int dstCap = Math.min(target - dstLeft, Math.round((float)capW * scale));
         int dstMid = Math.max(1, target - dstLeft - dstCap);

         for(int y = 0; y < dstH; ++y) {
            int sy = Math.min(srcH - 1, (int)((float)y / scale));
            int row = y * dstW;

            for(int x = 0; x < target; ++x) {
               int sx;
               if (x < dstLeft) {
                  sx = Math.min(leftW - 1, (int)((float)x / scale));
               } else if (x >= target - dstCap) {
                  sx = capSrcX0 + Math.min(capW - 1, (int)((float)(x - (target - dstCap)) / scale));
               } else {
                  sx = leftW + Math.min(midSrc - 1, (int)((float)(x - dstLeft) * (float)midSrc / (float)dstMid));
               }

               int argb = band.getRGB(sx, sy);
               if (!isCyan(argb)) {
                  int px = dstX0 + x;
                  if (px >= 0 && px < dstW) {
                     pixels[row + px] = argb | -16777216;
                  }
               }
            }
         }

      }
   }

   private static int leftSegment(BufferedImage band) {
      int maxFirst = 0;

      for(int y = 2; y < band.getHeight() - 2; ++y) {
         for(int x = 0; x < band.getWidth(); ++x) {
            if (!isCyan(band.getRGB(x, y))) {
               maxFirst = Math.max(maxFirst, x);
               break;
            }
         }
      }

      return Math.min(band.getWidth() - 2, maxFirst + 3);
   }

   static int bodyRight(BufferedImage band) {
      int right = -1;

      for(int y = 3; y < band.getHeight() - 2; ++y) {
         for(int x = band.getWidth() - 1; x >= 0; --x) {
            if (isNavy(band.getRGB(x, y))) {
               right = Math.max(right, x);
               break;
            }
         }
      }

      return right < 0 ? band.getWidth() - 1 : right;
   }

   private static int edgeRun(BufferedImage band, int bodyRight) {
      int y = band.getHeight() / 2;
      int interior = band.getRGB(Math.max(0, bodyRight - 20), y) & 255;
      int run = 1;

      for(int x = bodyRight - 1; x >= 0 && run < 8 && Math.abs((band.getRGB(x, y) & 255) - interior) > 3; --x) {
         ++run;
      }

      return run;
   }

   private static boolean isCyan(int argb) {
      int r = argb >> 16 & 255;
      int gg = argb >> 8 & 255;
      int b = argb & 255;
      return r < 110 && gg > 150 && b > 150;
   }

   private static boolean isNavy(int argb) {
      int r = argb >> 16 & 255;
      int gg = argb >> 8 & 255;
      int b = argb & 255;
      return b > 150 && r < 60 && gg < 60;
   }

   private static BufferedImage keyed(BufferedImage src, boolean card) {
      BufferedImage cached = card ? CACHED_CARD_KEYED : CACHED_FLAKE_KEYED;
      if (cached != null) {
         BufferedImage cachedFor = card ? CACHED_CARD_SRC : CACHED_FLAKE_SRC;
         if (cachedFor == src) {
            return cached;
         }
      }

      int w = src.getWidth();
      int h = src.getHeight();
      BufferedImage out = new BufferedImage(w, h, 2);

      for(int y = 0; y < h; ++y) {
         for(int x = 0; x < w; ++x) {
            int argb = src.getRGB(x, y);
            int r = argb >> 16 & 255;
            int g = argb >> 8 & 255;
            int a = Math.min(255, Math.max(r, g) * 255 / 96);
            out.setRGB(x, y, a << 24 | argb & 16777215);
         }
      }

      if (card) {
         CACHED_CARD_KEYED = out;
         CACHED_CARD_SRC = src;
      } else {
         CACHED_FLAKE_KEYED = out;
         CACHED_FLAKE_SRC = src;
      }

      return out;
   }

   public static record Card(int index, float x0, float slotW, int flakes, float flakeW, float flakeX0, float snowW, float textRight, float textW) {
      public float w() {
         return this.slotW;
      }

      public float x1() {
         return this.x0 + this.slotW;
      }
   }
}
