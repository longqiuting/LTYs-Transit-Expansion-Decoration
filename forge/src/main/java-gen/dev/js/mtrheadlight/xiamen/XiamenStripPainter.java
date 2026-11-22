package dev.js.mtrheadlight.xiamen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

public final class XiamenStripPainter {
   public static final int IMG_HEIGHT = 320;
   public static final int MAX_WIDTH = 4096;
   public static final float BAND_RATIO = 1.0F;
   public static final float CONTENT_RATIO = 0.6F;
   public static final float LINE_RATIO = 0.085F;
   public static final float CIRCLE_RATIO = 0.19F;
   public static final float RING_RATIO = 0.15F;
   public static final float INNER_RATIO = 0.58F;
   public static final float TERM_ARROW_LEN = 1.9F;
   public static final float TERM_ARROW_HALF = 1.15F;
   public static final int TERM_ARROW_TIP_OVERLAP = 2;
   public static final int END_JOINT_OVERLAP = 2;
   public static final float PAD_X_RATIO = 0.03F;
   public static final int PAD_X_MIN = 24;
   public static final float IX_RING_RATIO = 0.26F;
   public static final float IX_RING_THICK = 0.2F;
   public static final float IX_ARC_TOP_FROM = 30.0F;
   public static final float IX_ARC_TOP_TO = 150.0F;
   public static final float IX_ARC_BOTTOM_FROM = 210.0F;
   public static final float IX_ARC_BOTTOM_TO = 330.0F;
   public static final float IX_HEAD_LEN = 1.05F;
   public static final float IX_HEAD_W = 1.45F;
   public static final float IX_TAIL_W = 0.24F;
   public static final float IX_TAIL_HEAD = 0.24F;
   public static final float IX_PLATE_GAP_RATIO = 0.09F;
   public static final float IX_PLATE_H_RATIO = 0.165F;
   public static final float IX_PLATE_PAD_RATIO = 0.26F;
   public static final float IX_PLATE_TEXT_RATIO = 0.47F;
   public static final float BADGE_PAD_RATIO = 0.3F;
   public static final float BADGE_GAP_RATIO = 0.16F;
   public static final float BADGE_H_RATIO = 0.8F;
   public static final float BADGE_TEXT_RATIO = 0.52F;
   public static final float BADGE_MIN_SCALE = 0.55F;
   public static final float BADGE_AVAIL_RATIO = 0.92F;
   public static final float NAME_MAX_RATIO = 0.095F;
   public static final float NAME_AREA_TOP_RATIO = 0.035F;
   public static final float NAME_CLEARANCE_MIN = 8.0F;
   public static final float NAME_CLEARANCE_MAX = 12.0F;
   public static final float EN_RATIO = 0.46F;
   public static final float NAME_FLOOR = 7.5F;
   public static final float NAME_MIN_SCALE = 0.45F;
   private static final double DIAG = 1.0D / Math.sqrt(2.0D);
   private static final double NAME_ROTATION = (-Math.PI / 4D);
   public static final float NAME_ROTATION_DEG = 45.0F;
   private static final float NAME_GAP = 0.92F;
   public static volatile String lastFontName = "?";
   public static volatile int lastBandH;
   public static volatile int lastLineW;
   public static volatile int lastCircle;
   public static volatile int lastCurrent;
   public static volatile int lastNameSize;
   public static volatile int lastInterchangeCount;
   public static volatile int lastSwapHead;
   public static volatile int lastSwapPlate;
   public static volatile int lastSwapPlateH;
   public static volatile int lastBadgeCount;
   public static volatile float lastBadgeScale = 1.0F;
   public static volatile int lastBadgeDropped;
   public static volatile int lastNameAnchorX;
   public static volatile float lastNameRotationDeg;
   public static volatile int lastNameReserve;
   public static volatile int[] lastStationXs = new int[0];
   public static volatile int lastTerminalRing;
   public static volatile int lastTerminalArrow;
   public static volatile int lastNameShrunk;
   public static volatile int lastNameClipped;
   public static final int NAME_GREY = 10395294;
   public static final int TEXT_DARK = 1710618;
   public static final int TEXT_EN = 3815994;
   public static final int PASSED_GREY = 11711154;
   public static final int PASSED_SEGMENT_GREY = 10987688;
   public static final int PASSED_NAME_GREY = 10395294;
   public static final int CURRENT_FILL = 15073298;
   private static volatile Font cjkFont;

   private XiamenStripPainter() {
   }

   public static boolean isLight(int rgb) {
      int r = rgb >> 16 & 255;
      int g = rgb >> 8 & 255;
      int b = rgb & 255;
      return 0.299D * (double)r + 0.587D * (double)g + 0.114D * (double)b > 128.0D;
   }

   public static int contrastText(int rgb) {
      return isLight(rgb) ? -16777216 : -1;
   }

   public static int onWhiteText(int rgb) {
      return isLight(rgb) ? 1710618 : -16777216 | rgb & 16777215;
   }

   public static int currentNameText(int rgb) {
      return 15073298;
   }

   public static int darken(int rgb, double factor) {
      int r = (int)Math.round((double)(rgb >> 16 & 255) * factor);
      int g = (int)Math.round((double)(rgb >> 8 & 255) * factor);
      int b = (int)Math.round((double)(rgb & 255) * factor);
      return -16777216 | Math.min(255, Math.max(0, r)) << 16 | Math.min(255, Math.max(0, g)) << 8 | Math.min(255, Math.max(0, b));
   }

   public static XiamenStripPainter.BadgeFit fitBadgeRow(float[] textWidths, float available, float baseHeight) {
      int n = textWidths == null ? 0 : textWidths.length;
      if (n <= 0) {
         return new XiamenStripPainter.BadgeFit(1.0F, 0, new float[0], 0.0F, 0.0F, false);
      } else {
         float h = Math.max(1.0F, baseHeight);
         float pad = h * 0.3F;
         float gap = h * 0.16F;
         float avail = Math.max(0.0F, available);
         float[] natural = new float[n];

         for(int i = 0; i < n; ++i) {
            natural[i] = Math.max(1.0F, textWidths[i]) + pad * 2.0F;
         }

         for(int keep = n; keep >= 1; --keep) {
            float raw = 0.0F;

            for(int i = 0; i < keep; ++i) {
               raw += natural[i];
            }

            raw += gap * (float)(keep - 1);
            float fitScale = raw > 0.0F ? avail / raw : 1.0F;
            if (!(fitScale < 0.55F) || keep <= 1) {
               float scale = Math.max(0.55F, Math.min(1.0F, fitScale));
               float[] widths = new float[keep];
               float total = 0.0F;

               for(int i = 0; i < keep; ++i) {
                  widths[i] = natural[i] * scale;
                  total += widths[i];
               }

               float scaledGap = gap * scale;
               total += scaledGap * (float)(keep - 1);
               return new XiamenStripPainter.BadgeFit(scale, keep, widths, scaledGap, total, total > avail + 0.5F);
            }
         }

         return new XiamenStripPainter.BadgeFit(0.55F, 1, new float[]{natural[0] * 0.55F}, gap * 0.55F, natural[0] * 0.55F, true);
      }
   }

   public static BufferedImage paint(XiamenStripPainter.Model model) {
      if (model != null && model.names != null && model.names.length >= 2 && model.aspectRatio > 0.05F) {
         int height = 320;
         int width = Math.max(height, Math.round((float)height * model.aspectRatio));
         if (width > 4096) {
            height = Math.max(48, Math.round((float)height * 4096.0F / (float)width));
            width = 4096;
         }

         if (width >= 16 && height >= 16) {
            BufferedImage image = new BufferedImage(width, height, 2);
            Graphics2D g = image.createGraphics();

            try {
               g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
               g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
               g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
               lastBadgeCount = 0;
               lastBadgeScale = 1.0F;
               lastBadgeDropped = 0;
               lastNameAnchorX = 0;
               lastNameRotationDeg = 0.0F;
               lastNameReserve = 0;
               int count = model.names.length;
               boolean flip = model.direction != 0;
               int current = Math.max(0, Math.min(count - 1, model.currentIndex));
               int lineRgb = model.lineRgb & 16777215;
               String[] names = new String[count];
               String[] namesEn = new String[count];
               XiamenStripPainter.Interchange[][] swaps = new XiamenStripPainter.Interchange[count][];
               boolean[] passed = new boolean[count];

               for(int i = 0; i < count; ++i) {
                  int d = flip ? count - 1 - i : i;
                  names[i] = model.names[d];
                  namesEn[i] = model.namesEn != null && d < model.namesEn.length ? model.namesEn[d] : "";
                  swaps[i] = dataInterchanges(model, d);
                  passed[i] = d < current;
               }

               int currentIndex = flip ? count - 1 - current : current;
               g.setColor(Color.WHITE);
               g.fillRect(0, 0, width, height);
               int bandH = Math.max(8, Math.round((float)height * 0.6F));
               int bandTop = (height - bandH) / 2;
               int bandBottom = bandTop + bandH;
               int lineW = Math.max(3, Math.round((float)bandH * 0.085F));
               boolean namesBelow = model.namesBelow;
               int lineY = bandTop + Math.round((float)bandH * (namesBelow ? 0.3F : 0.64F));
               int padX = Math.max(24, Math.round((float)width * 0.03F));
               int padLeft = Math.min(padX, Math.max(2, width / 3));
               int rightEdge = Math.max(padLeft + 2, width - padLeft);
               int termArrowLen = Math.max(4, Math.round((float)lineW * 1.9F));
               int termArrowHalf = Math.max(3, Math.round((float)lineW * 1.15F));
               float stepGuess = (float)Math.max(1, rightEdge - padLeft) / (float)(count - 1);
               float circle = Math.max((float)bandH * 0.09F, Math.min((float)bandH * 0.19F, stepGuess * 0.62F));
               int radius = Math.round(circle / 2.0F);
               int ixRingD = Math.max(14, Math.round((float)bandH * 0.26F));
               float curD = circle + Math.max(1.2F, circle * 0.15F);
               int nameTop = Math.max(2, Math.round((float)height * 0.035F));
               int nameCap = Math.max(9, Math.round((float)height * 0.095F));
               int xFirst = padLeft + radius;
               int nominalXLast = Math.max(xFirst + 1, rightEdge - radius);
               float tailR = markOuterRadius(circle, curD, (float)ixRingD, swaps[count - 1] != null);
               int xLast = nominalXLast;
               float reserve = 0.0F;

               for(int iter = 0; iter < 6; ++iter) {
                  float stepTry = (float)(xLast - xFirst) / (float)(count - 1);
                  float need = namesBelow ? 0.0F : tailNameReserve(g, names, namesEn, tailR, currentIndex, stepTry, nameTop, lineY, rightEdge, padLeft, nameCap);
                  int wanted = namesBelow ? nominalXLast : (int)Math.floor((double)((float)(rightEdge - 6) - need));
                  int minXLast = xFirst + Math.max(1, Math.round((float)(nominalXLast - xFirst) * 0.6F));
                  int target = Math.max(minXLast, Math.min(nominalXLast, wanted));
                  boolean settled = target == xLast && Math.abs(need - reserve) <= 0.5F;
                  xLast = target;
                  reserve = need;
                  if (settled) {
                     break;
                  }
               }

               float step = (float)(xLast - xFirst) / (float)(count - 1);
               int[] xs = new int[count];

               for(int i = 0; i < count; ++i) {
                  xs[i] = Math.round((float)xFirst + step * (float)i);
               }

               lastNameReserve = Math.round(reserve);
               lastStationXs = (int[])xs.clone();
               float[] visR = new float[count];

               for(int i = 0; i < count; ++i) {
                  visR[i] = markOuterRadius(circle, curD, (float)ixRingD, swaps[i] != null);
               }

               float maxVisR = 0.0F;

               for(float r : visR) {
                  maxVisR = Math.max(maxVisR, r);
               }

               float[] markR = new float[count];

               for(int i = 0; i < count; ++i) {
                  markR[i] = swaps[i] == null && i == currentIndex ? (float)currentDotDiameter(curD) / 2.0F : visR[i];
               }

               int terminalIndex = flip ? 0 : count - 1;
               int termX = xs[terminalIndex];
               int tipInset = Math.max(0, Math.round(visR[terminalIndex]) - 2);
               int tipX = flip ? termX + tipInset : termX - tipInset;
               int headBaseX = flip ? tipX + termArrowLen : tipX - termArrowLen;
               int lineStartX = Math.max(0, Math.round((float)xs[0] - markR[0]) + 2);
               int lineEndX = Math.min(width, Math.round((float)xs[count - 1] + markR[count - 1]) - 2);
               int lineFrom = flip ? headBaseX : lineStartX;
               int lineTo = flip ? Math.max(headBaseX + 1, lineEndX) : headBaseX;
               g.setColor(new Color(10987688));
               g.fillRect(lineFrom, lineY - lineW / 2, Math.max(1, lineTo - lineFrom), lineW);
               int curX = xs[currentIndex];
               int futureA = flip ? lineFrom : Math.min(curX, lineTo);
               int futureB = flip ? Math.max(curX, lineFrom) : lineTo;
               g.setColor(new Color(lineRgb));
               g.fillRect(futureA, lineY - lineW / 2, Math.max(1, futureB - futureA), lineW);
               int marginV = Math.max(0, (height - bandH) / 2);
               int plateFloorY = bandBottom + Math.round((float)marginV * 0.55F) - Math.max(1, Math.round((float)height * 0.01F));
               int swapDrawn = 0;
               int plateBottomMax = 0;

               for(int i = 0; i < count; ++i) {
                  if (i != currentIndex) {
                     XiamenStripPainter.Interchange[] swap = swaps[i];
                     if (swap != null) {
                        int[] box = new int[2];
                        if (drawSwap(g, xs[i], lineY, lineRgb, swap, bandH, step, width, plateFloorY, box, passed[i], false)) {
                           ++swapDrawn;
                           plateBottomMax = Math.max(plateBottomMax, box[1]);
                        }
                     } else {
                        drawDot(g, xs[i], lineY, circle, passed[i] ? 11711154 : lineRgb, passed[i]);
                     }
                  }
               }

               XiamenStripPainter.Interchange[] currentSwap = swaps[currentIndex];
               if (currentSwap != null) {
                  int[] box = new int[2];
                  if (drawSwap(g, xs[currentIndex], lineY, lineRgb, currentSwap, bandH, step, width, plateFloorY, box, false, true)) {
                     ++swapDrawn;
                     plateBottomMax = Math.max(plateBottomMax, box[1]);
                  }
               } else {
                  drawCurrentDot(g, xs[currentIndex], lineY, curD);
               }

               lastInterchangeCount = swapDrawn;
               float belowExtra = namesBelow && plateBottomMax > 0 ? Math.max(0.0F, (float)plateBottomMax - ((float)lineY + maxVisR)) : 0.0F;
               float nameSize = drawNames(g, names, namesEn, xs, visR, currentIndex, passed, lineRgb, nameTop, bandBottom, lineY, step, namesBelow, rightEdge, width, padLeft, belowExtra, nameCap);
               drawTerminalArrow(g, tipX, headBaseX, lineY, termArrowHalf, lineRgb);
               lastBandH = bandH;
               lastLineW = lineW;
               lastCircle = Math.round(circle);
               lastCurrent = Math.round(curD);
               lastNameSize = Math.round(nameSize);
               lastTerminalRing = Math.round(circle);
               lastTerminalArrow = termArrowLen;
            } finally {
               g.dispose();
            }

            return image;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static void drawTerminalArrow(Graphics2D g, int tipX, int baseX, int lineY, int half, int rgb) {
      if (half > 0 && tipX != baseX) {
         g.setColor(new Color(rgb));
         g.fillPolygon(new int[]{tipX, baseX, baseX}, new int[]{lineY, lineY - half, lineY + half}, 3);
      }
   }

   private static void drawDot(Graphics2D g, int cx, int cy, float size, int rgb, boolean grey) {
      ring(g, cx, cy, size, grey ? 11711154 : ringColor(rgb), 0.0F, 0.0F);
   }

   private static void drawCurrentDot(Graphics2D g, int cx, int cy, float size) {
      int fd = currentDotDiameter(size);
      g.setColor(new Color(15073298));
      g.fillOval(cx - fd / 2, cy - fd / 2, fd, fd);
   }

   private static int currentDotDiameter(float size) {
      int d = Math.max(4, Math.round(size));
      int inset = Math.max(1, Math.round((float)d * 0.15F));
      return Math.max(2, d - inset * 2);
   }

   private static int ringColor(int rgb) {
      return isLight(rgb) ? darken(rgb, 0.72D) : rgb;
   }

   private static void ring(Graphics2D g, int cx, int cy, float size, int rgb, float innerStroke, float innerRatio) {
      int d = Math.max(4, Math.round(size));
      int left = cx - d / 2;
      int top = cy - d / 2;
      g.setColor(Color.WHITE);
      g.fillOval(left, top, d, d);
      g.setColor(new Color(rgb));
      g.setStroke(new BasicStroke(Math.max(1.2F, (float)d * 0.15F), 0, 1));
      g.drawOval(left, top, d - 1, d - 1);
      if (innerStroke > 0.0F && innerRatio > 0.0F) {
         float inner = (float)d * innerRatio;
         int innerLeft = Math.round((float)cx - inner / 2.0F);
         int innerTop = Math.round((float)cy - inner / 2.0F);
         g.setStroke(new BasicStroke(Math.max(1.0F, (float)d * innerStroke), 0, 1));
         g.drawOval(innerLeft, innerTop, Math.round(inner) - 1, Math.round(inner) - 1);
      }

      g.setStroke(new BasicStroke(1.0F));
   }

   private static XiamenStripPainter.Interchange[] dataInterchanges(XiamenStripPainter.Model model, int dataIndex) {
      if (model.interchanges != null && model.interchanges.length != 0) {
         int n = 0;

         for(XiamenStripPainter.Interchange ic : model.interchanges) {
            if (ic != null && ic.stationIndex == dataIndex) {
               ++n;
            }
         }

         if (n == 0) {
            return null;
         } else {
            XiamenStripPainter.Interchange[] found = new XiamenStripPainter.Interchange[n];
            int k = 0;

            for(XiamenStripPainter.Interchange ic : model.interchanges) {
               if (ic != null && ic.stationIndex == dataIndex) {
                  found[k++] = ic;
               }
            }

            return found;
         }
      } else {
         return null;
      }
   }

   private static boolean drawInterchange(Graphics2D g, int cx, int lineY, int lineRgb, XiamenStripPainter.Interchange swap, int bandH, float step, int width, int floorY, int[] box, boolean grey, boolean asCurrent) {
      int swapRgb = grey ? 11711154 : swap.rgb & 16777215;
      int ringD = Math.max(14, Math.round((float)bandH * 0.26F));
      float bodyR = (float)ringD / 2.0F;
      int gap = Math.max(3, Math.round((float)bandH * 0.09F));
      int plateH = Math.max(10, Math.round((float)bandH * 0.165F));
      int plateTop = lineY + Math.round(bodyR) + gap;
      if (plateTop + plateH > floorY) {
         plateH = Math.max(10, floorY - plateTop);
      }

      int maxByEdge = Math.max(18, 2 * (Math.min(cx, width - cx) - 2));
      int maxByStep = Math.max(18, Math.round(step * 1.5F));
      int maxW = Math.max(18, Math.min(maxByEdge, maxByStep));
      String cn = plateCjk(swap);
      String en = plateEn(swap);
      float cjkSize = 0.0F;
      float enSize = 0.0F;
      int cjkW = 0;
      int enW = 0;

      for(int attempt = 0; attempt < 6; ++attempt) {
         cjkSize = (float)plateH * (0.47F - 0.05F * (float)attempt);
         enSize = cjkSize * 0.62F;
         Font cjkFont = font().deriveFont(1, cjkSize);
         Font enFont = font().deriveFont(1, enSize);
         cjkW = cn.isEmpty() ? 0 : g.getFontMetrics(cjkFont).stringWidth(cn);
         enW = en.isEmpty() ? 0 : g.getFontMetrics(enFont).stringWidth(en);
         int need = Math.max(cjkW, enW) + Math.round((float)plateH * 0.26F * 2.0F);
         if (need <= maxW || cjkSize <= 7.0F) {
            break;
         }
      }

      if (cn.isEmpty() && en.isEmpty()) {
         return false;
      } else {
         int padX = Math.max(3, Math.round((float)plateH * 0.26F));
         int plateW = Math.max(18, Math.min(maxW, Math.max(cjkW, enW) + padX * 2));
         int px = cx - plateW / 2;
         int ringRgb = asCurrent ? 15073298 : (grey ? 11711154 : ringColor(lineRgb));
         drawSwapBody(g, cx, lineY, ringRgb, swapRgb, ringD, (float)plateTop - 2.0F);
         if (plateW > 0 && plateH > 0) {
            g.setColor(new Color(swapRgb));
            g.fill(new RoundRectangle2D.Float((float)px, (float)plateTop, (float)plateW, (float)plateH, (float)plateH * 0.34F, (float)plateH * 0.34F));
            g.setColor(new Color(contrastText(swapRgb), true));
            if (!cn.isEmpty()) {
               Font cjkFont = font().deriveFont(1, cjkSize);
               g.setFont(cjkFont);
               FontMetrics fm = g.getFontMetrics(cjkFont);
               String text = cjkW > plateW - 2 * padX ? fitText(g, cjkFont, cn, plateW - 2 * padX) : cn;
               int tw = g.getFontMetrics(cjkFont).stringWidth(text);
               float baseline = (float)plateTop + (float)plateH * (en.isEmpty() ? 0.5F : 0.44F) + (float)(fm.getAscent() - fm.getDescent()) / 2.0F - (float)plateH * 0.04F;
               g.drawString(text, (float)px + (float)(plateW - tw) / 2.0F, baseline);
            }

            if (!en.isEmpty()) {
               float size = cn.isEmpty() ? (float)plateH * 0.5F : enSize;
               Font enFont = font().deriveFont(1, size);
               g.setFont(enFont);
               FontMetrics fm = g.getFontMetrics(enFont);
               String text = enW > plateW - 2 * padX ? fitText(g, enFont, en, plateW - 2 * padX) : en;
               int tw = g.getFontMetrics(enFont).stringWidth(text);
               g.drawString(text, (float)px + (float)(plateW - tw) / 2.0F, (float)plateTop + (float)plateH * 0.86F + (float)(fm.getAscent() - fm.getDescent()) / 2.0F - (float)plateH * 0.06F);
            }

            box[0] = plateTop;
            box[1] = plateTop + plateH;
            lastSwapHead = ringD;
            lastSwapPlate = plateW;
            lastSwapPlateH = plateH;
            return true;
         } else {
            return false;
         }
      }
   }

   private static boolean drawSwap(Graphics2D g, int cx, int lineY, int lineRgb, XiamenStripPainter.Interchange[] swaps, int bandH, float step, int width, int floorY, int[] box, boolean grey, boolean asCurrent) {
      if (swaps != null && swaps.length != 0) {
         return swaps.length == 1 ? drawInterchange(g, cx, lineY, lineRgb, swaps[0], bandH, step, width, floorY, box, grey, asCurrent) : drawInterchangeRow(g, cx, lineY, lineRgb, swaps, bandH, step, width, floorY, box, grey, asCurrent);
      } else {
         return false;
      }
   }

   private static void drawSwapBody(Graphics2D g, int cx, int lineY, int ringRgb, int swapRgb, int ringD, float tipY) {
      float rOut = (float)ringD / 2.0F;
      float thick = (float)ringD * 0.2F;
      float rIn = Math.max(1.0F, rOut - thick);
      float cy = (float)lineY;
      g.setColor(Color.WHITE);
      g.fillOval(Math.round((float)cx - rOut), Math.round(cy - rOut), ringD, ringD);
      g.setColor(new Color(swapRgb));
      g.fill(annularSector((float)cx, cy, rOut, rIn, 210.0F, 330.0F));
      arrowHead(g, (float)cx, cy, rOut, rIn, 330.0F, swapRgb);
      g.setColor(new Color(ringRgb));
      g.fill(annularSector((float)cx, cy, rOut, rIn, 30.0F, 150.0F));
      arrowHead(g, (float)cx, cy, rOut, rIn, 150.0F, ringRgb);
      float tailW = (float)ringD * 0.24F;
      float shaftTop = cy + rOut - thick * 0.45F;
      float tailHead = Math.min((float)ringD * 0.24F, Math.max(3.0F, (tipY - shaftTop) * 0.42F));
      float headBaseY = tipY - tailHead;
      if (headBaseY > shaftTop + 1.0F) {
         g.setColor(new Color(swapRgb));
         g.fillRect(Math.round((float)cx - tailW / 2.0F), Math.round(shaftTop), Math.max(1, Math.round(tailW)), Math.max(1, Math.round(headBaseY - shaftTop)));
         g.fillPolygon(new int[]{Math.round((float)cx - tailW / 2.0F), Math.round((float)cx + tailW / 2.0F), cx}, new int[]{Math.round(headBaseY), Math.round(headBaseY), Math.round(tipY)}, 3);
      } else if (tipY > shaftTop) {
         g.setColor(new Color(swapRgb));
         g.fillRect(Math.round((float)cx - tailW / 2.0F), Math.round(shaftTop), Math.max(1, Math.round(tailW)), Math.max(1, Math.round(tipY - shaftTop)));
      }

   }

   private static boolean drawInterchangeRow(Graphics2D g, int cx, int lineY, int lineRgb, XiamenStripPainter.Interchange[] swaps, int bandH, float step, int width, int floorY, int[] box, boolean grey, boolean asCurrent) {
      int ringD = Math.max(14, Math.round((float)bandH * 0.26F));
      float bodyR = (float)ringD / 2.0F;
      int gap = Math.max(3, Math.round((float)bandH * 0.09F));
      int top = lineY + Math.round(bodyR) + gap;
      int badgeH = Math.max(9, Math.round((float)bandH * 0.165F * 0.8F));
      if (top + badgeH > floorY) {
         badgeH = Math.max(9, floorY - top);
      }

      float baseText = Math.max(5.0F, (float)badgeH * 0.52F);
      Font baseFont = font().deriveFont(1, baseText);
      String[] labels = new String[swaps.length];
      float[] textW = new float[swaps.length];
      int nonEmpty = 0;

      for(int i = 0; i < swaps.length; ++i) {
         String label = plateCjk(swaps[i]);
         if (label.isEmpty()) {
            label = plateEn(swaps[i]);
         }

         labels[i] = label;
         if (!label.isEmpty()) {
            ++nonEmpty;
            textW[i] = (float)g.getFontMetrics(baseFont).stringWidth(label);
         } else {
            textW[i] = 1.0F;
         }
      }

      if (nonEmpty == 0) {
         return false;
      } else {
         float avail = Math.max(8.0F, step * 0.92F);
         XiamenStripPainter.BadgeFit fit = fitBadgeRow(textW, avail, (float)badgeH);
         int firstRgb = grey ? 11711154 : swaps[0].rgb & 16777215;
         int ringRgb = asCurrent ? 15073298 : (grey ? 11711154 : ringColor(lineRgb));
         drawSwapBody(g, cx, lineY, ringRgb, firstRgb, ringD, (float)top - 2.0F);
         int totalW = Math.max(1, Math.round(fit.total));
         int x = Math.round((float)cx - fit.total / 2.0F);
         if (x < 1) {
            x = 1;
         }

         if (x + totalW > width - 1) {
            x = Math.max(1, width - 1 - totalW);
         }

         for(int i = 0; i < fit.count; ++i) {
            int w = Math.max(4, Math.round(fit.widths[i]));
            int rgb = grey ? 10395294 : swaps[i].rgb & 16777215;
            g.setColor(new Color(rgb));
            g.fill(new RoundRectangle2D.Float((float)x, (float)top, (float)w, (float)badgeH, (float)badgeH * 0.34F, (float)badgeH * 0.34F));
            String label = labels[i];
            if (!label.isEmpty()) {
               int inner = Math.max(2, w - Math.round((float)badgeH * 0.3F * fit.scale * 2.0F));
               float size = Math.max(4.0F, baseText * fit.scale);
               Font f = baseFont.deriveFont(size);

               for(int k = 0; k < 5 && size > 4.0F && g.getFontMetrics(f).stringWidth(label) > inner; ++k) {
                  size *= 0.88F;
                  f = baseFont.deriveFont(size);
               }

               g.setFont(f);
               g.setColor(new Color(contrastText(rgb), true));
               int tw = g.getFontMetrics(f).stringWidth(label);
               Shape oldClip = g.getClip();
               g.setClip(new Rectangle(x, top, w, badgeH));
               g.drawString(label, (float)x + (float)(w - tw) / 2.0F, (float)top + (float)badgeH * 0.5F + (float)(g.getFontMetrics(f).getAscent() - g.getFontMetrics(f).getDescent()) / 2.0F);
               g.setClip(oldClip);
            }

            x += w + Math.max(0, Math.round(fit.gap));
         }

         box[0] = top;
         box[1] = top + badgeH;
         lastSwapHead = ringD;
         lastSwapPlate = totalW;
         lastSwapPlateH = badgeH;
         lastBadgeCount += fit.count;
         lastBadgeDropped += swaps.length - fit.count;
         lastBadgeScale = Math.min(lastBadgeScale, fit.scale);
         return true;
      }
   }

   private static String fitText(Graphics2D g, Font font, String text, int maxWidth) {
      if (maxWidth > 4 && !text.isEmpty()) {
         float size = font.getSize2D();

         for(int i = 0; i < 8 && size > 6.0F; ++i) {
            size *= 0.9F;
            if (g.getFontMetrics(font.deriveFont(size)).stringWidth(text) <= maxWidth) {
               break;
            }
         }

         Font smaller = font.deriveFont(size);
         g.setFont(smaller);
         return text;
      } else {
         return text;
      }
   }

   private static Path2D.Float annularSector(float cx, float cy, float rOut, float rIn, float a1, float a2) {
      int steps = 28;
      Path2D.Float path = new Path2D.Float();

      for(int i = 0; i <= 28; ++i) {
         double a = Math.toRadians((double)(a1 + (a2 - a1) * (float)i / 28.0F));
         float x = (float)((double)cx + (double)rOut * Math.cos(a));
         float y = (float)((double)cy - (double)rOut * Math.sin(a));
         if (i == 0) {
            path.moveTo(x, y);
         } else {
            path.lineTo(x, y);
         }
      }

      for(int i = 28; i >= 0; --i) {
         double a = Math.toRadians((double)(a1 + (a2 - a1) * (float)i / 28.0F));
         float x = (float)((double)cx + (double)rIn * Math.cos(a));
         float y = (float)((double)cy - (double)rIn * Math.sin(a));
         path.lineTo(x, y);
      }

      path.closePath();
      return path;
   }

   private static void arrowHead(Graphics2D g, float cx, float cy, float rOut, float rIn, float angleDeg, int rgb) {
      double a = Math.toRadians((double)angleDeg);
      float rMid = (rOut + rIn) / 2.0F;
      float px = (float)((double)cx + (double)rMid * Math.cos(a));
      float py = (float)((double)cy - (double)rMid * Math.sin(a));
      float tx = (float)(-Math.sin(a));
      float ty = (float)(-Math.cos(a));
      float nx = -ty;
      float thick = rOut - rIn;
      float headLen = thick * 1.05F;
      float headW = thick * 1.45F / 2.0F;
      g.setColor(new Color(rgb));
      g.fillPolygon(new int[]{Math.round(px + tx * headLen), Math.round(px + nx * headW), Math.round(px - nx * headW)}, new int[]{Math.round(py + ty * headLen), Math.round(py + tx * headW), Math.round(py - tx * headW)}, 3);
   }

   private static String plateCjk(XiamenStripPainter.Interchange swap) {
      String number = swap.number == null ? "" : swap.number.trim();
      if (isAllDigits(number)) {
         return number + "\u53f7\u7ebf";
      } else {
         String name = swap.name == null ? "" : swap.name.trim();
         return name.isEmpty() ? number : name;
      }
   }

   private static String plateEn(XiamenStripPainter.Interchange swap) {
      String number = swap.number == null ? "" : swap.number.trim();
      return isAllDigits(number) ? "Line " + number : "";
   }

   private static boolean isAllDigits(String text) {
      if (text != null && !text.isEmpty()) {
         for(int i = 0; i < text.length(); ++i) {
            if (text.charAt(i) < '0' || text.charAt(i) > '9') {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static float drawNames(Graphics2D g, String[] names, String[] ens, int[] xs, float[] radii, int currentIndex, boolean[] passed, int lineRgb, int nameTop, int bandBottom, int lineY, float step, boolean namesBelow, int rightEdge, int imgW, int firstTextX, float belowExtra, int nameCap) {
      int count = names.length;
      float maxR = 0.0F;

      for(float r : radii) {
         maxR = Math.max(maxR, r);
      }

      if (roomTop(namesBelow, bandBottom, lineY, maxR, nameTop, belowExtra) < 7.5F) {
         return 0.0F;
      } else {
         Font base = font();
         Font ref100 = base.deriveFont(1, 100.0F);
         FontMetrics fm100 = g.getFontMetrics(ref100);
         FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
         XiamenStripPainter.NamePlan plan = planNames(ref100, frc, fm100, names, ens, step, nameCap, namesBelow);
         if (!plan.ok) {
            return 0.0F;
         } else {
            XiamenStripPainter.NameCtx ctx = new XiamenStripPainter.NameCtx(ref100, frc, Math.max(1.0F, (float)fm100.getAscent()), Math.max(0.0F, (float)fm100.getDescent()), plan, namesBelow, nameTop, lineY, bandBottom, rightEdge, firstTextX, belowExtra);
            Shape clip = namesBelow ? new Rectangle(Math.max(0, firstTextX), Math.round((float)lineY + maxR) + 1, Math.max(1, rightEdge - firstTextX), Math.max(1, bandBottom - lineY)) : new Rectangle(Math.max(0, firstTextX), nameTop, Math.max(1, rightEdge - firstTextX), Math.max(1, lineY - nameTop));
            int shrunk = 0;
            int clipped = 0;
            Shape oldClip = g.getClip();
            g.setClip(clip);

            for(int i = 0; i < count; i += plan.stride) {
               String raw = names[i];
               if (raw != null && !raw.isEmpty()) {
                  boolean isCurrent = i == currentIndex;
                  String rawEn = plan.enScale > 0.0F && ens != null && i < ens.length ? ens[i] : null;
                  XiamenStripPainter.NameFit fit = fitName(ctx, raw, rawEn, radii[i], isCurrent);
                  float s = fit.s;
                  float boost = fit.boost;
                  float enStep = fit.enStep;
                  float enOffset = fit.enOffset;
                  float clr = fit.clr;
                  String label = fit.label;
                  String en = fit.en;
                  if (fit.shrunk) {
                     ++shrunk;
                  }

                  if (fit.clipped) {
                     ++clipped;
                  }

                  Font cjkFont = base.deriveFont(1, s * boost);
                  Font enFont = base.deriveFont(0, Math.max(1.0F, fit.enSize));
                  boolean isPassed = passed != null && i < passed.length && passed[i];
                  int textRgb = isCurrent ? 15073298 : (isPassed ? 10395294 : (namesBelow ? 10395294 : 1710618));
                  Color textColor = new Color(-16777216 | textRgb, true);
                  Color enColor = new Color(-16777216 | (isCurrent ? textRgb : (isPassed ? 10395294 : 3815994)), true);
                  g.setFont(cjkFont);
                  g.setColor(textColor);
                  if (namesBelow) {
                     int textW = g.getFontMetrics(cjkFont).stringWidth(label);
                     int x = Math.round((float)xs[i] - (float)textW / 2.0F);
                     x = Math.max(x, Math.min(Math.round((float)xs[i]), firstTextX));
                     x = Math.min(x, Math.max(firstTextX, imgW - 2 - textW));
                     x = Math.max(x, 2);
                     if (i == count - 1) {
                        lastNameAnchorX = x + textW / 2;
                        lastNameRotationDeg = 0.0F;
                     }

                     float baseline = (float)lineY + radii[i] + belowExtra + clr + (float)g.getFontMetrics().getAscent();
                     g.drawString(label, (float)x, baseline);
                     if (en != null) {
                        g.setFont(enFont);
                        g.setColor(enColor);
                        int enW = g.getFontMetrics(enFont).stringWidth(en);
                        g.drawString(en, Math.min((float)(imgW - 2 - enW), Math.max((float)firstTextX, (float)xs[i] - (float)enW / 2.0F)), baseline + enOffset);
                     }
                  } else {
                     int projected = projectedWidth(g, cjkFont, enFont, label, en, enStep);
                     int baseY = Math.round((float)lineY - radii[i] - clr - s * 0.2F) - Math.round(enOffset);
                     int x = xs[i];
                     if (x + projected > rightEdge - 6) {
                        x = Math.max(firstTextX, rightEdge - 6 - projected);
                     }

                     if (i == count - 1) {
                        lastNameAnchorX = x;
                        lastNameRotationDeg = 45.0F;
                     }

                     drawRotatedName(g, label, en, x, baseY, cjkFont, enFont, enStep, enColor);
                  }
               }
            }

            g.setClip(oldClip);
            lastNameShrunk = shrunk;
            lastNameClipped = clipped;
            return plan.size;
         }
      }
   }

   private static String clipToWidth(Font ref100, FontRenderContext frc, String text, float size, float maxWidth) {
      if (text != null && !text.isEmpty() && !(maxWidth <= 2.0F)) {
         if (ref100.getStringBounds(text, frc).getWidth() * (double)size / 100.0D <= (double)maxWidth) {
            return text;
         } else {
            String ellipsis = "\u2026";

            for(int len = text.length() - 1; len >= 1; --len) {
               String cut = text.substring(0, len) + "\u2026";
               if (ref100.getStringBounds(cut, frc).getWidth() * (double)size / 100.0D <= (double)maxWidth) {
                  return cut;
               }
            }

            return "\u2026";
         }
      } else {
         return text;
      }
   }

   private static float enWidth(Graphics2D g, Font enFont, String en) {
      return en != null && !en.isEmpty() ? (float)g.getFontMetrics(enFont).stringWidth(en) : 0.0F;
   }

   private static boolean hasEnglish(String[] ens) {
      for(String en : ens) {
         if (en != null && !en.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private static void drawRotatedName(Graphics2D g, String cjk, String en, int x, int baseY, Font cjkFont, Font enFont, float lineStep, Color enColor) {
      AffineTransform old = g.getTransform();
      g.rotate((-Math.PI / 4D), (double)x, (double)baseY);
      g.setFont(cjkFont);
      g.drawString(cjk, x, baseY);
      if (en != null && !en.isEmpty()) {
         g.setFont(enFont);
         g.setColor(enColor);
         g.drawString(en, (float)x, (float)baseY + lineStep);
      }

      g.setTransform(old);
   }

   private static int projectedWidth(Graphics2D g, Font cjkFont, Font enFont, String label, String en, float enStep) {
      int textW = g.getFontMetrics(cjkFont).stringWidth(label);
      int enW = en == null ? 0 : g.getFontMetrics(enFont).stringWidth(en);
      return (int)Math.ceil((double)Math.max(textW, enW) * DIAG) + (en == null ? 0 : (int)Math.ceil((double)enStep * DIAG)) + 4;
   }

   private static float markOuterRadius(float circle, float curD, float ixRingD, boolean interchange) {
      return interchange ? Math.max(circle, ixRingD) / 2.0F : curD / 2.0F;
   }

   private static float roomTop(boolean namesBelow, int bandBottom, int lineY, float maxR, int nameTop, float belowExtra) {
      return namesBelow ? (float)(bandBottom - lineY) - maxR - 8.0F - belowExtra - 3.0F : (float)lineY - maxR - 8.0F - (float)nameTop;
   }

   private static XiamenStripPainter.NamePlan planNames(Font ref100, FontRenderContext frc, FontMetrics fm100, String[] names, String[] ens, float step, int nameCap, boolean namesBelow) {
      XiamenStripPainter.NamePlan plan = new XiamenStripPainter.NamePlan();
      double maxW100 = 0.0D;

      for(String name : names) {
         if (name != null && !name.isEmpty()) {
            maxW100 = Math.max(maxW100, ref100.getStringBounds(name, frc).getWidth());
         }
      }

      if (maxW100 <= 0.0D) {
         return plan;
      } else {
         double footprint = namesBelow ? maxW100 : maxW100 * DIAG;
         float size = 0.0F;

         for(int attempt = 0; attempt < 4; ++attempt) {
            size = (float)(100.0D * (double)step * (double)plan.stride * (double)0.92F / footprint);
            size = Math.min(size, (float)nameCap);
            if (size >= 7.5F) {
               break;
            }

            ++plan.stride;
         }

         if (size < 7.5F) {
            return plan;
         } else {
            float enScale = 0.0F;
            if (ens != null && hasEnglish(ens)) {
               int maxEn100 = 0;

               for(String en : ens) {
                  if (en != null && !en.isEmpty()) {
                     maxEn100 = Math.max(maxEn100, fm100.stringWidth(en));
                  }
               }

               if (maxEn100 > 0) {
                  double limit = (double)(step * (float)plan.stride * 0.92F);
                  double maxEnSize = limit / ((double)maxEn100 / 100.0D * (namesBelow ? 1.0D : DIAG));
                  enScale = (float)Math.min((double)0.46F, maxEnSize / (double)Math.max(1.0F, size));
                  if (enScale < 0.3F) {
                     enScale = 0.0F;
                  }
               }
            }

            plan.size = size;
            plan.enScale = enScale;
            plan.minSize = Math.max(7.5F, size * 0.45F);
            plan.ok = true;
            return plan;
         }
      }
   }

   private static XiamenStripPainter.NameFit fitName(XiamenStripPainter.NameCtx c, String raw, String rawEn, float radius, boolean isCurrent) {
      XiamenStripPainter.NameFit f = new XiamenStripPainter.NameFit();
      float size = c.plan.size;
      boolean hasEn = rawEn != null && !rawEn.isEmpty();
      f.boost = isCurrent ? 1.06F : 1.0F;
      double wCjk = c.ref100.getStringBounds(raw, c.frc).getWidth();
      double wEnRaw = hasEn ? c.ref100.getStringBounds(rawEn, c.frc).getWidth() : 0.0D;
      double wEn = wEnRaw * (double)c.plan.enScale;
      float clr0 = Math.max(8.0F, Math.min(12.0F, size * 0.35F));
      float enStep0 = size * c.plan.enScale * 1.3F;
      float availV = c.namesBelow ? c.bandBottom - c.lineY - radius - c.belowExtra - clr0 - 2.0F : c.lineY - radius - clr0 - size * 0.2F - enStep0 - c.nameTop;
      float availH = (float)c.rightEdge - 6.0F - (float)c.firstTextX;
      double needV = c.namesBelow ? ((double)(c.asc100 + c.desc100) + 130.0D * (double)c.plan.enScale) * (double)f.boost : DIAG * (wCjk + (double)c.asc100) * (double)f.boost;
      double needH = c.namesBelow ? Math.max(wCjk, wEn) * (double)f.boost + 4.0D : DIAG * Math.max(wCjk, wEn) * (double)f.boost + 2.0D;
      float s = (float)Math.min((double)size, Math.min(100.0D * (double)Math.max(1.0F, availV) / Math.max(1.0D, needV), 100.0D * (double)Math.max(1.0F, availH) / Math.max(1.0D, needH)));
      s = Math.max(c.plan.minSize, Math.min(size, s));
      f.shrunk = s < size - 0.4F;
      f.s = s;
      float clr = Math.max(8.0F, Math.min(12.0F, s * 0.35F));
      f.clr = clr;
      float runAvailV = c.namesBelow ? Float.MAX_VALUE : (c.lineY - radius - clr - s * 0.2F - c.nameTop) / (float)DIAG;
      float runAvailH = c.namesBelow ? (float)c.rightEdge - 6.0F - (float)c.firstTextX : ((float)c.rightEdge - 6.0F - (float)c.firstTextX) / (float)DIAG;
      float runAll = Math.min(runAvailV, runAvailH);
      boolean showEn = hasEn;
      float enSize = hasEn ? s * c.plan.enScale : 0.0F;
      float enStep = enSize * 1.3F;
      if (hasEn && wEnRaw > 0.0D) {
         for(int k = 0; k < 3; ++k) {
            float avail = runAll - enStep;
            if (avail <= 4.0F) {
               break;
            }

            float fit = (float)(100.0D * (double)avail / wEnRaw);
            if (fit >= enSize - 0.2F) {
               break;
            }

            enSize = Math.max(4.0F, fit);
            enStep = enSize * 1.3F;
         }

         if (enSize < 4.5F) {
            showEn = false;
            enSize = 0.0F;
            enStep = 0.0F;
         }
      }

      f.enSize = enSize;
      f.enStep = enStep;
      f.enOffset = showEn ? enStep : 0.0F;
      f.label = clipToWidth(c.ref100, c.frc, raw, s * f.boost, runAll - f.enOffset);
      f.en = showEn ? clipToWidth(c.ref100, c.frc, rawEn, enSize, Math.max(4.0F, runAll - enStep)) : null;
      f.clipped = !f.label.equals(raw) || showEn && f.en != null && !f.en.equals(rawEn);
      return f;
   }

   private static float tailNameReserve(Graphics2D g, String[] names, String[] ens, float tailRadius, int currentIndex, float step, int nameTop, int lineY, int rightEdge, int firstTextX, int nameCap) {
      int count = names.length;
      int i = count - 1;
      if (names[i] != null && !names[i].isEmpty()) {
         if (roomTop(false, 0, lineY, tailRadius, nameTop, 0.0F) < 7.5F) {
            return 0.0F;
         } else {
            Font base = font();
            Font ref100 = base.deriveFont(1, 100.0F);
            FontMetrics fm100 = g.getFontMetrics(ref100);
            FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
            XiamenStripPainter.NamePlan plan = planNames(ref100, frc, fm100, names, ens, step, nameCap, false);
            if (plan.ok && i % plan.stride == 0) {
               XiamenStripPainter.NameCtx ctx = new XiamenStripPainter.NameCtx(ref100, frc, Math.max(1.0F, (float)fm100.getAscent()), Math.max(0.0F, (float)fm100.getDescent()), plan, false, nameTop, lineY, lineY, rightEdge, firstTextX, 0.0F);
               String rawEn = plan.enScale > 0.0F && ens != null && i < ens.length ? ens[i] : null;
               XiamenStripPainter.NameFit fit = fitName(ctx, names[i], rawEn, tailRadius, i == currentIndex);
               Font cjkFont = base.deriveFont(1, fit.s * fit.boost);
               Font enFont = base.deriveFont(0, Math.max(1.0F, fit.enSize));
               return (float)projectedWidth(g, cjkFont, enFont, fit.label, fit.en, fit.enStep);
            } else {
               return 0.0F;
            }
         }
      } else {
         return 0.0F;
      }
   }

   public static Font font() {
      Font font = cjkFont;
      if (font != null) {
         return font;
      } else {
         synchronized(XiamenStripPainter.class) {
            font = cjkFont;
            if (font == null) {
               font = resolveFont();
               cjkFont = font;
            }

            return font;
         }
      }
   }

   private static Font resolveFont() {
      try {
         GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
         Font best = null;
         int bestScore = Integer.MAX_VALUE;

         for(Font f : env.getAllFonts()) {
            if (f.canDisplay('\u53a6') && f.canDisplay('\u7ebf')) {
               String key = (f.getPSName() + "|" + f.getFontName(Locale.ENGLISH) + "|" + f.getFamily(Locale.ENGLISH)).toLowerCase(Locale.ROOT);
               int score;
               if (key.contains("simhei")) {
                  score = 0;
               } else if (key.contains("yahei")) {
                  score = key.contains("light") ? 4 : 1;
               } else if (!key.contains("noto sans sc") && !key.contains("source han sans") && !key.contains("noto sans cjk")) {
                  if (!key.contains("simsun") && !key.contains("jhenghei")) {
                     score = 8;
                  } else {
                     score = 5;
                  }
               } else {
                  score = 3;
               }

               if (score < bestScore) {
                  bestScore = score;
                  best = f;
               }
            }
         }

         if (best != null) {
            lastFontName = best.getPSName();
            return best.deriveFont(1, 100.0F);
         }
      } catch (Throwable var9) {
         lastFontName = "logical";
      }

      return new Font("SansSerif", 1, 100);
   }

   public static String cjkPart(String raw) {
      String text = raw == null ? "" : raw.replace("||", "|").trim();
      if (text.isEmpty()) {
         return "";
      } else {
         int cut = text.indexOf(124);
         if (cut < 0) {
            return text;
         } else {
            String head = text.substring(0, cut).trim();
            return head.isEmpty() ? text.substring(cut + 1).trim() : head;
         }
      }
   }

   public static String enPart(String raw) {
      String text = raw == null ? "" : raw.replace("||", "|").trim();
      int cut = text.indexOf(124);
      return cut < 0 ? "" : text.substring(cut + 1).trim();
   }

   public static final class BadgeFit {
      public final float scale;
      public final int count;
      public final float[] widths;
      public final float gap;
      public final float total;
      public final boolean overflow;

      BadgeFit(float scale, int count, float[] widths, float gap, float total, boolean overflow) {
         this.scale = scale;
         this.count = count;
         this.widths = widths;
         this.gap = gap;
         this.total = total;
         this.overflow = overflow;
      }

      public String toString() {
         return String.format(Locale.ROOT, "BadgeFit{count=%d/%d, scale=%.3f, total=%.1f, gap=%.1f%s}", this.count, this.widths.length, this.scale, this.total, this.gap, this.overflow ? ", OVERFLOW" : "");
      }
   }

   public static final class Interchange {
      public int stationIndex;
      public int rgb;
      public String number;
      public String name;

      public Interchange() {
      }

      public Interchange(int stationIndex, int rgb, String number, String name) {
         this.stationIndex = stationIndex;
         this.rgb = rgb;
         this.number = number == null ? "" : number;
         this.name = name == null ? "" : name;
      }
   }

   public static final class Model {
      public String[] names;
      public String[] namesEn;
      public int currentIndex;
      public int lineRgb;
      public String lineNumber;
      public int direction;
      public boolean namesBelow;
      public float aspectRatio;
      public XiamenStripPainter.Interchange[] interchanges;
   }

   private static final class NameCtx {
      final Font ref100;
      final FontRenderContext frc;
      final float asc100;
      final float desc100;
      final XiamenStripPainter.NamePlan plan;
      final boolean namesBelow;
      final float nameTop;
      final float lineY;
      final float bandBottom;
      final int rightEdge;
      final int firstTextX;
      final float belowExtra;

      NameCtx(Font ref100, FontRenderContext frc, float asc100, float desc100, XiamenStripPainter.NamePlan plan, boolean namesBelow, int nameTop, int lineY, int bandBottom, int rightEdge, int firstTextX, float belowExtra) {
         this.ref100 = ref100;
         this.frc = frc;
         this.asc100 = asc100;
         this.desc100 = desc100;
         this.plan = plan;
         this.namesBelow = namesBelow;
         this.nameTop = (float)nameTop;
         this.lineY = (float)lineY;
         this.bandBottom = (float)bandBottom;
         this.rightEdge = rightEdge;
         this.firstTextX = firstTextX;
         this.belowExtra = belowExtra;
      }
   }

   private static final class NameFit {
      String label;
      String en;
      float s;
      float boost;
      float enSize;
      float enStep;
      float enOffset;
      float clr;
      boolean shrunk;
      boolean clipped;
   }

   private static final class NamePlan {
      int stride = 1;
      float size;
      float enScale;
      float minSize;
      boolean ok;
   }
}
