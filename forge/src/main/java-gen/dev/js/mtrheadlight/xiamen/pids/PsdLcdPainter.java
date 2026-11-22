package dev.js.mtrheadlight.xiamen.pids;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.List;

public final class PsdLcdPainter {
   public static final float DESIGN_W = 6.91F;
   public static final float MIN_CONTENT_ASPECT = 3.9F;
   private static final float CARD_X0 = 0.002F;
   private static final float CARD_X1 = 0.0995F;
   private static final float CARD_Y0 = 0.0193F;
   private static final float CARD_Y1 = 0.9756F;
   private static final float CARD_R = 0.048F;
   private static final float CARD_SW = 0.009F;
   private static final float CARD_TEXT_CX = 0.0477F;
   private static final float LOGO_CX = 0.0499F;
   private static final float LOGO_TOP = 0.0405F;
   private static final float LOGO_H = 0.298F;
   private static final float DATE_TOP = 0.4223F;
   private static final float DATE_INK = 0.0436F;
   private static final float WDZH_TOP = 0.5558F;
   private static final float WDZH_INK = 0.0834F;
   private static final float WDEN_TOP = 0.6919F;
   private static final float WDEN_INK = 0.068F;
   private static final float CLOCK_TOP = 0.8395F;
   private static final float CLOCK_INK = 0.068F;
   private static final float ROW1_CY = 0.3723F;
   private static final float ROW_DY = 0.295F;
   private static final float LBL_X = 0.11F;
   private static final float LBLZH_TOP = -0.0539F;
   private static final float LBLZH_INK = 0.0591F;
   private static final float LBLEN_TOP = 0.0398F;
   private static final float LBLEN_INK = 0.0411F;
   public static final float LABEL_GAP_RATIO = 0.6F;
   public static final float MIN_LABEL_GAP_PX = 4.0F;
   private static final float DEST_CX = 0.2212F;
   private static final float DEST_TOP = -0.0976F;
   private static final float DEST_INK = 0.1014F;
   private static final float DESTEN_TOP = 0.0462F;
   private static final float DESTEN_INK = 0.0513F;
   private static final float NUM_RIGHT = 0.34F;
   private static final float NUM_TOP = -0.1066F;
   private static final float NUM_INK = 0.1772F;
   private static final float UNIT_X = 0.366F;
   private static final float UNITZH_TOP = -0.0077F;
   private static final float UNITZH_INK = 0.043F;
   private static final float UNITEN_TOP = 0.0397F;
   private static final float UNITEN_INK = 0.0373F;
   private static final float BADGE_X0 = 0.4392F;
   private static final float BADGE_X1 = 0.5367F;
   private static final float BADGE_Y0 = 0.0565F;
   private static final float BADGE_Y1 = 0.3273F;
   private static final float BADGE_R = 0.03F;
   private static final float BADGE_CX = 0.488F;
   private static final float LINE_ZH_TOP = 0.0988F;
   private static final float LINE_ZH_INK = 0.1142F;
   private static final float LINE_EN_TOP = 0.2388F;
   private static final float LINE_EN_INK = 0.0757F;
   private static final float STA_CX = 0.6075F;
   private static final float STA_ZH_TOP = 0.0693F;
   private static final float STA_ZH_INK = 0.1643F;
   private static final float STA_EN_TOP = 0.2593F;
   private static final float STA_EN_INK = 0.0552F;
   private static final float DEST_COL_L = 0.1629F;
   private static final float DEST_COL_R = 0.2981F;
   private static final float STA_COL_L = 0.5437F;
   private static final float STA_COL_R = 0.7127F;
   private static final float NUM_COL_L = 0.3051F;
   private static final float NUM_COL_R = 0.4322F;
   private static final float CAR_ROW_X1 = 0.6894F;
   private static final float CAR_ROW_X0 = 0.3158F;
   private static final float CAR_ROW_GUARD_X = 0.4392F;
   private static final float CAR_Y0 = 0.4492F;
   private static final float CAR_Y1 = 0.687F;
   private static final float CAR_W = 0.469F;
   private static final float CAR_GAP_RATIO = 0.1F;
   private static final float NOSE_H_RATIO = 1.035F;
   private static final float CAR_MIN_SCALE = 0.32F;
   private static final float CAR_ELLIPSIS_W = 0.55F;
   private static final int CAR_CAP = 16;
   private static final float CARD_PX_W = 2144.0F;
   private static final float CARD_PX_H = 1024.0F;
   private static final float NOSE_PX_W = 74.0F;
   private static final float NOSE_PX_H = 180.0F;
   private static final float CAR_TEXT_LEFT = 0.32509327F;
   private static final float CAR_TEXT_INK_H = 0.20019531F;
   private static final float CAR_TEXT_CY = 0.7680664F;
   private static final float NOTE_X = 0.395F;
   private static final float NOTE_TOP = 0.7F;
   private static final float NOTE_INK = 0.042F;
   private static final float TICK_X0 = 0.0999F;
   private static final float TICK_X1 = 0.7171F;
   private static final float TICK_Y0 = 0.828F;
   private static final float TICK_Y1 = 0.9756F;
   private static final float TICK_SW = 0.0075F;
   private static final float TICK_SIZE_RATIO = 0.52F;
   private static final float AD_X0 = 0.7197F;
   public static final int COLOR_BG = -16644384;
   public static final int COLOR_WHITE = -1;
   public static final int COLOR_CARD_STROKE = -723201;
   public static final int COLOR_TICK_STROKE = -1643777;
   private static final FontRenderContext FRC = new FontRenderContext((AffineTransform)null, true, true);
   private static String widthCacheText = "";
   private static String widthCacheFont = "";
   private static float widthCacheInk = -1.0F;
   private static float widthCacheWidth = 0.0F;

   private PsdLcdPainter() {
   }

   public static BufferedImage paint(PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, int width, int height) {
      return paint(c, assets, (BufferedImage)null, width, height);
   }

   public static BufferedImage paint(PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, BufferedImage ad, int width, int height) {
      return paint(c, assets, ad, width, height, (Float)null);
   }

   public static BufferedImage paint(PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, BufferedImage ad, int width, int height, Float tickerPhase) {
      BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height), 2);
      Graphics2D g = image.createGraphics();

      try {
         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
         g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
         g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
         paintInto(g, c, assets, ad, (float)width, (float)height, tickerPhase);
      } finally {
         g.dispose();
      }

      return image;
   }

   public static void paintInto(Graphics2D g, PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, float width, float height) {
      paintInto(g, c, assets, (BufferedImage)null, width, height, (Float)null);
   }

   public static void paintInto(Graphics2D g, PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, BufferedImage ad, float width, float height) {
      paintInto(g, c, assets, ad, width, height, (Float)null);
   }

   public static void paintInto(Graphics2D g, PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, BufferedImage ad, float width, float height, Float tickerPhase) {
      if (g != null && c != null) {
         float h = layoutHeight(width, height);
         float offY = (height - h) * 0.5F;
         g.setColor(argb(-16644384));
         g.fill(new Rectangle2D.Float(0.0F, 0.0F, width, height));
         adBlock(g, c, assets, ad, width, height, h, offY);
         AffineTransform savedTransform = g.getTransform();
         if (offY > 0.01F) {
            g.translate(0.0D, (double)offY);
         }

         try {
            card(g, width, h);
            logo(g, assets, width, h);
            cardTexts(g, c, assets, width, h);
            PsdLcdPainter.ArrivalColumns arrivalCols = arrivalColumns(c.boundLabel(), c.boundSub(), c.nextLabel(), c.nextSub(), assets, width, h);
            row(g, c, assets, width, h, 0.3723F, arrivalCols, c.boundLabel(), c.boundSub(), c.dest1(), c.dest1En(), c.count1(), c.unitZh(), c.unitEn());
            row(g, c, assets, width, h, 0.6673F, arrivalCols, c.nextLabel(), c.nextSub(), c.dest2(), c.dest2En(), c.count2(), c.unitZh2(), c.unitEn2());
            badge(g, c, assets, width, h);
            station(g, c, assets, width, h);
            cars(g, c, assets, width, h);
            paintTicker(g, c, assets, width, height, tickerPhase);
         } finally {
            g.setTransform(savedTransform);
         }

      }
   }

   public static float layoutHeight(float width, float height) {
      float byWidth = width / 3.9F;
      return Math.min(height, Math.max(1.0F, byWidth));
   }

   public static int segments(float width) {
      return Math.max(1, Math.round(width / 1040.0F));
   }

   public static float[] adRegion(float width, float height) {
      return new float[]{(float)Math.round(0.7197F * width), 0.0F, (float)Math.round(width), (float)Math.round(height)};
   }

   public static float adLeft() {
      return 0.7197F;
   }

   public static float[] adSourceUv() {
      return new float[]{clamp01(0.0F), clamp01(0.0F), clamp01(1.0F), clamp01(1.0F)};
   }

   private static void adBlock(Graphics2D g, PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, BufferedImage ad, float width, float height, float h, float offY) {
      float[] region = adRegion(width, height);
      float x0 = region[0];
      float y0 = region[1];
      float x1 = region[2];
      float y1 = region[3];
      if (!(x1 - x0 < 1.0F) && !(y1 - y0 < 1.0F)) {
         if (ad != null && ad.getWidth() > 0 && ad.getHeight() > 0) {
            float[] uv = adSourceUv();
            Shape old = g.getClip();
            g.clip(new Rectangle2D.Float(x0, y0, x1 - x0, y1 - y0));

            try {
               drawScaled(g, ad, x0, y0, x1 - x0, y1 - y0, uv);
            } finally {
               g.setClip(old);
            }

         }
      }
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static void card(Graphics2D g, float w, float h) {
      float x0 = 0.002F * w;
      float x1 = 0.0995F * w;
      float y0 = 0.0193F * h;
      float y1 = 0.9756F * h;
      float sw = Math.max(1.0F, 0.009F * h);
      g.setColor(argb(-723201));
      g.setStroke(new BasicStroke(sw, 0, 1));
      g.draw(new RoundRectangle2D.Float(x0 + sw * 0.5F, y0 + sw * 0.5F, x1 - x0 - sw, y1 - y0 - sw, 0.048F * h * 2.0F, 0.048F * h * 2.0F));
      g.setStroke(new BasicStroke(1.0F));
   }

   private static void logo(Graphics2D g, PsdLcdPainter.Assets assets, float w, float h) {
      if (assets != null && assets.logo != null) {
         BufferedImage image = assets.logo;
         int[] box = inkBox(image);
         float inkW = Math.max(1.0F, (float)(box[2] - box[0]));
         float inkH = Math.max(1.0F, (float)(box[3] - box[1]));
         float cardInk = 0.097500004F * w - 0.0365F * h;
         float scale = Math.min(0.298F * h / inkH, cardInk / inkW);
         float drawW = (float)image.getWidth() * scale;
         float drawH = (float)image.getHeight() * scale;
         float inkLeftInImage = (float)box[0] * scale;
         float inkTopInImage = (float)box[1] * scale;
         float x = 0.0499F * w - inkW * scale * 0.5F - inkLeftInImage;
         float y = 0.0405F * h - inkTopInImage;
         drawScaled(g, image, x, y, drawW, drawH);
      }
   }

   private static int[] inkBox(BufferedImage image) {
      int iw = image.getWidth();
      int ih = image.getHeight();
      int x0 = iw;
      int y0 = ih;
      int x1 = -1;
      int y1 = -1;
      boolean hasAlpha = image.getColorModel().hasAlpha();
      int corner = image.getRGB(0, 0);

      for(int y = 0; y < ih; ++y) {
         for(int x = 0; x < iw; ++x) {
            int argb = image.getRGB(x, y);
            boolean ink = hasAlpha ? argb >>> 24 > 31 : argb != corner;
            if (ink) {
               if (x < x0) {
                  x0 = x;
               }

               if (y < y0) {
                  y0 = y;
               }

               if (x > x1) {
                  x1 = x;
               }

               if (y > y1) {
                  y1 = y;
               }
            }
         }
      }

      return x1 >= 0 && y1 >= 0 ? new int[]{x0, y0, x1 + 1, y1 + 1} : new int[]{0, 0, iw, ih};
   }

   private static void cardTexts(Graphics2D g, PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, float w, float h) {
      Font cjk = assets == null ? null : assets.cjk();
      Font latin = assets == null ? null : assets.latin();
      float cx = 0.0477F * w;
      float maxW = 0.097500004F * w * 0.9F;
      int white = -1;
      line(g, dashDate(c.date()), latin, "2026-09-23", 0.4223F * h, 0.0436F * h, cx, PsdLcdPainter.Align.CENTER, maxW, -1);
      line(g, c.weekdayZh(), cjk, "\u661f\u671f\u4e09", 0.5558F * h, 0.0834F * h, cx, PsdLcdPainter.Align.CENTER, maxW, -1);
      line(g, c.weekdayEn(), latin, "Wednesday", 0.6919F * h, 0.068F * h, cx, PsdLcdPainter.Align.CENTER, maxW, -1);
      line(g, c.clock(), latin, "11:10", 0.8395F * h, 0.068F * h, cx, PsdLcdPainter.Align.CENTER, maxW, -1);
   }

   private static String dashDate(String date) {
      return date == null ? "" : date.replace('-', '\u2013').replace('/', '\u2013');
   }

   public static PsdLcdPainter.ArrivalColumns arrivalColumns(String label1, String sub1, String label2, String sub2, PsdLcdPainter.Assets assets, float width, float height) {
      PsdLcdPainter.Assets a = assets == null ? new PsdLcdPainter.Assets() : assets;
      float h = layoutHeight(width, height);
      Font zh = labelZhFont(a, h);
      Font en = labelEnFont(a, h);
      List<PidsXiamenColumns.Label> labels = new ArrayList(4);
      List<TextFit.Metrics> metrics = new ArrayList(4);
      addLabel(labels, metrics, label1, zh);
      addLabel(labels, metrics, sub1, en);
      addLabel(labels, metrics, label2, zh);
      addLabel(labels, metrics, sub2, en);
      float labelX = 0.11F * width;
      float labelW = PidsXiamenColumns.labelWidth(labels, metrics);
      float gap = Math.max(4.0F, PidsXiamenColumns.gap(0.1014F * h));
      float nameRight = 0.2981F * width;
      return new PsdLcdPainter.ArrivalColumns(labelX, labelW, gap, Math.min(labelX + labelW + gap, nameRight), nameRight);
   }

   public static PsdLcdPainter.ArrivalColumns arrivalColumns(PsdLcdPainter.Assets assets, float width, float height) {
      return arrivalColumns("\u5f00\u5f80", "Bound for", "\u4e0b\u4e00\u8d9f\u5f00\u5f80", "Next train", assets, width, height);
   }

   private static void addLabel(List<PidsXiamenColumns.Label> labels, List<TextFit.Metrics> metrics, String text, Font nominal) {
      if (text != null && !text.isEmpty() && nominal != null) {
         labels.add(new PidsXiamenColumns.Label(text, nominal.getSize2D()));
         metrics.add((TextFit.Metrics)(t, size) -> inkWidth(nominal.deriveFont(Math.max(1.0F, size)), t));
      }
   }

   public static PsdLcdPainter.Fitted fitLabel(String text, boolean chinese, PsdLcdPainter.Assets assets, float width, float height, PsdLcdPainter.ArrivalColumns cols) {
      PsdLcdPainter.Assets a = assets == null ? new PsdLcdPainter.Assets() : assets;
      float h = layoutHeight(width, height);
      Font base = chinese ? labelZhFont(a, h) : labelEnFont(a, h);
      String ref = chinese ? "\u5f00\u5f80" : "Bound for";
      float inkH = (chinese ? 0.0591F : 0.0411F) * h;
      float maxW = cols == null ? 0.0F : cols.labelW();
      return fitToColumn(text, base, ref, inkH, maxW);
   }

   public static float labelInkRight(String text, boolean chinese, PsdLcdPainter.Assets assets, float width, float height, PsdLcdPainter.ArrivalColumns cols) {
      float labelX = cols == null ? 0.11F * width : cols.labelX();
      PsdLcdPainter.Fitted fitted = fitLabel(text, chinese, assets, width, height, cols);
      return labelX + inkWidth(fitted.font(), fitted.text());
   }

   private static Font labelZhFont(PsdLcdPainter.Assets a, float h) {
      return fit(a.cjk(), "\u5f00\u5f80", 0.0591F * h);
   }

   private static Font labelEnFont(PsdLcdPainter.Assets a, float h) {
      return fit(a.latin(), "Bound for", 0.0411F * h);
   }

   private static void row(Graphics2D g, PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, float w, float h, float cyH, PsdLcdPainter.ArrivalColumns cols, String label, String labelSub, String dest, String destEn, String count, String unitZh, String unitEn) {
      Font cjk = assets == null ? null : assets.cjk();
      Font latin = assets == null ? null : assets.latin();
      Font digits = assets == null ? null : assets.digits();
      float cy = cyH * h;
      int white = -1;
      if (dest != null && !dest.isBlank() || destEn != null && !destEn.isBlank() || count != null && !count.isBlank()) {
         line(g, label, cjk, "\u5f00\u5f80", cy + -0.0539F * h, 0.0591F * h, cols.labelX(), PsdLcdPainter.Align.LEFT, cols.labelW(), -1);
         line(g, labelSub, latin, "Bound for", cy + 0.0398F * h, 0.0411F * h, cols.labelX(), PsdLcdPainter.Align.LEFT, cols.labelW(), -1);
         float nameW = Math.max(1.0F, cols.nameW());
         line(g, dest, cjk, "\u534e\u4fa8\u5927\u5b66", cy + -0.0976F * h, 0.1014F * h, cols.nameCenter(), PsdLcdPainter.Align.CENTER, nameW, -1);
         line(g, destEn, latin, "Huaqiao University", cy + 0.0462F * h, 0.0513F * h, cols.nameCenter(), PsdLcdPainter.Align.CENTER, nameW, -1);
         if (count != null && !count.isEmpty()) {
            boolean state = !count.codePoints().allMatch(Character::isDigit);
            if (state) {
               line(g, count, cjk, "\u5df2\u5230\u7ad9", cy + -0.1066F * h, 0.1772F * h, columnAnchor(PsdLcdPainter.Column.COUNT_STATE, w), PsdLcdPainter.Align.LEFT, columnWidth(PsdLcdPainter.Column.COUNT_STATE, w), -1);
            } else {
               float numColW = columnWidth(PsdLcdPainter.Column.COUNT, w);
               float numInk = 0.1772F * h;
               float natW = inkWidth(digits.deriveFont(numInk), count);
               if (natW > numColW && natW > 0.0F) {
                  numInk *= Math.max(0.42F, numColW / natW);
               }

               line(g, count, digits, "0", cy + -0.1066F * h, numInk, 0.34F * w, PsdLcdPainter.Align.RIGHT, numColW, -1);
            }

            if (state) {
               return;
            }

            float unitX = 0.366F * w;
            line(g, unitZh, cjk, "\u5206\u949f", cy + -0.0077F * h, 0.043F * h, unitX, PsdLcdPainter.Align.LEFT, 0.045F * w, -1);
            line(g, unitEn, latin, "min", cy + 0.0397F * h, 0.0373F * h, unitX, PsdLcdPainter.Align.LEFT, 0.045F * w, -1);
         }

      }
   }

   private static void badge(Graphics2D g, PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, float w, float h) {
      boolean hasLine = c.lineZh() != null && !c.lineZh().isBlank() || c.lineEn() != null && !c.lineEn().isBlank();
      int color = c.lineColor();
      if (hasLine && (color >>> 24 & 255) != 0) {
         float x0 = 0.4392F * w;
         float x1 = 0.5367F * w;
         float y0 = 0.0565F * h;
         float y1 = 0.3273F * h;
         g.setColor(new Color(color, true));
         g.fill(new RoundRectangle2D.Float(x0, y0, x1 - x0, y1 - y0, 0.03F * h * 2.0F, 0.03F * h * 2.0F));
         Font cjk = assets == null ? null : assets.cjk();
         Font latin = assets == null ? null : assets.latin();
         int white = -1;
         float cx = 0.488F * w;
         float maxW = (x1 - x0) * 0.9F;
         line(g, c.lineZh(), cjk, "6\u53f7\u7ebf", 0.0988F * h, 0.1142F * h, cx, PsdLcdPainter.Align.CENTER, maxW, -1);
         line(g, c.lineEn(), latin, "Line 6", 0.2388F * h, 0.0757F * h, cx, PsdLcdPainter.Align.CENTER, maxW, -1);
      }
   }

   private static void station(Graphics2D g, PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, float w, float h) {
      Font cjk = assets == null ? null : assets.cjk();
      Font latin = assets == null ? null : assets.latin();
      float cx = 0.6075F * w;
      int white = -1;
      line(g, c.stationZh(), cjk, "\u8463\u4efb", 0.0693F * h, 0.1643F * h, cx, PsdLcdPainter.Align.CENTER, columnWidth(PsdLcdPainter.Column.STATION_ZH, w), -1);
      line(g, c.stationEn(), latin, "Dongren", 0.2593F * h, 0.0552F * h, cx, PsdLcdPainter.Align.CENTER, columnWidth(PsdLcdPainter.Column.STATION_EN, w), -1);
   }

   public static float rowLeft(float width) {
      return Math.max(0.3158F, 0.4392F) * width;
   }

   public static PsdLcdPainter.CarRow carRow(int cars, PsdLcdPainter.Assets assets, float width, float height) {
      float h = layoutHeight(width, height);
      float rowBottom = 0.4492F * h;
      float rowTop = 0.687F * h;
      float rowH = Math.max(1.0F, rowTop - rowBottom);
      float right = 0.6894F * width;
      float left = rowLeft(width);
      float ratio = 0.47761193F;
      float ch = Math.max(1.0F, Math.min(0.469F * h * 0.47761193F, rowH));
      float cwNat = ch / 0.47761193F;
      float noseH = ch * 1.035F;
      boolean hasNose = assets != null && assets.nose != null;
      float noseAspect = hasNose ? (float)assets.nose.getWidth() / (float)assets.nose.getHeight() : 0.41111112F;
      float noseW = hasNose ? noseH * noseAspect : 0.0F;
      int n = Math.max(1, Math.min(16, cars));
      float r = 0.1F;
      float avail = Math.max(1.0F, right - left - noseW);
      float cw = Math.min(cwNat, avail / ((float)n * 1.1F));
      int shown = n;
      boolean degraded = false;
      if (cw < cwNat * 0.32F) {
         cw = cwNat * 0.32F;
         float slot = cw * 1.1F;
         float ell = cw * 0.55F;
         int fit = (int)Math.floor((double)((avail - ell - cw * 0.1F) / slot));
         shown = Math.max(1, Math.min(n, fit));
         degraded = shown < n;
      }

      float gap = cw * 0.1F;
      float ellipsisW = degraded ? cw * 0.55F : 0.0F;
      float cardsW = (float)shown * cw + (float)(shown - 1) * gap + (degraded ? gap + ellipsisW : 0.0F);
      float cardsX0 = right - cardsW;
      float cardY0 = rowBottom + (rowH - ch) * 0.5F;
      float noseX = cardsX0 - gap - noseW;
      float noseY0 = cardY0 + (ch - noseH) * 0.5F;
      float boxX0 = hasNose ? Math.min(noseX, cardsX0) : cardsX0;
      float boxY0 = hasNose ? Math.min(cardY0, noseY0) : cardY0;
      float boxY1 = hasNose ? Math.max(cardY0 + ch, noseY0 + noseH) : cardY0 + ch;
      return new PsdLcdPainter.CarRow(segments(width), n, shown, degraded, boxX0, right, boxY0, boxY1, cardsX0, cw, cwNat, gap, ch, cardY0, noseX, noseW, noseH, ellipsisW, avail);
   }

   public static float[] carsBox(int cars, PsdLcdPainter.Assets assets, float width, float height) {
      if (cars > 0 && assets != null) {
         PsdLcdPainter.CarRow row = carRow(cars, assets, width, height);
         float offY = (height - layoutHeight(width, height)) * 0.5F;
         return new float[]{row.boxX0(), offY + row.boxY0(), row.boxX1(), offY + row.boxY1()};
      } else {
         return new float[0];
      }
   }

   private static float[] columnInkBand(PsdLcdPainter.Column column) {
      float[] var10000;
      switch (column.ordinal()) {
         case 0:
            var10000 = new float[]{-0.0976F, 0.1014F};
            break;
         case 1:
            var10000 = new float[]{0.0462F, 0.0513F};
            break;
         case 2:
            var10000 = new float[]{0.0693F, 0.1643F};
            break;
         case 3:
            var10000 = new float[]{0.2593F, 0.0552F};
            break;
         case 4:
         case 5:
            var10000 = new float[]{-0.1066F, 0.1772F};
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static float[] textBox(PsdLcdPainter.Column column, int row, String text, PsdLcdPainter.Assets assets, float width, float height) {
      if (text != null && !text.isEmpty()) {
         PsdLcdPainter.ArrivalColumns cols = arrivalColumns(assets, width, height);
         PsdLcdPainter.Fitted fitted = fitColumn(column, text, assets, width, height, cols);
         float h = layoutHeight(width, height);
         float offY = (height - h) * 0.5F;
         float inkH = inkHeight(fitted.font(), fitted.text());
         float[] band = columnInkBand(column);
         boolean absolute = column == PsdLcdPainter.Column.STATION_ZH || column == PsdLcdPainter.Column.STATION_EN;
         float bandTop = absolute ? band[0] * h : (0.3723F + (float)Math.max(0, row) * 0.295F) * h + band[0] * h;
         float top = fitted.shrunk() ? bandTop + (band[1] * h - inkH) * 0.5F : bandTop;
         float x0 = inkLeft(column, fitted, width, cols);
         return new float[]{x0, offY + top, x0 + inkWidth(fitted.font(), fitted.text()), offY + top + inkH};
      } else {
         return new float[0];
      }
   }

   private static void cars(Graphics2D g, PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, float w, float h) {
      List<String> numbers = c.carNumbers();
      if (numbers != null && !numbers.isEmpty() && assets != null) {
         PsdLcdPainter.CarRow row = carRow(numbers.size(), assets, w, h);
         if (assets.nose != null && row.noseW() > 0.0F) {
            drawScaled(g, assets.nose, row.noseX(), row.cardY0() + (row.cardH() - row.noseH()) * 0.5F, row.noseW(), row.noseH());
         }

         for(int i = 0; i < row.shown(); ++i) {
            BufferedImage card = i != 0 && i != row.shown() - 1 ? assets.carMiddle : assets.carEnd;
            float cx = row.cardsX0() + (float)i * (row.cardW() + row.gap());
            if (card != null) {
               drawScaled(g, card, cx, row.cardY0(), row.cardW(), row.cardH());
            }

            String number = (String)numbers.get(i);
            if (number != null && !number.isEmpty()) {
               float inkH = row.cardH() * 0.20019531F;
               float textRight = Math.max(cx + row.cardW() * 0.02F, cx + row.cardW() * 0.32509327F - row.cardW() * 0.025F);
               float inkCy = row.cardY0() + row.cardH() * 0.7680664F;
               line(g, number, assets.cjk(), "1", inkCy - inkH * 0.5F, inkH, textRight, PsdLcdPainter.Align.RIGHT, 0.0F, -1);
            }
         }

         if (row.degraded() && row.ellipsisW() > 0.0F) {
            float lastRight = row.cardsX0() + (float)(row.shown() - 1) * (row.cardW() + row.gap()) + row.cardW();
            float ellX0 = lastRight + row.gap();
            float ellX1 = ellX0 + row.ellipsisW();
            float d = Math.max(1.5F, Math.min(row.cardH() * 0.13F, row.ellipsisW() * 0.22F));
            float step = d * 1.9F;
            float total = d + 2.0F * step;
            float cy = row.cardY0() + row.cardH() * 0.7680664F;
            float dx = (ellX0 + ellX1) * 0.5F - total * 0.5F;
            g.setColor(argb(-1));

            for(int i = 0; i < 3; ++i) {
               g.fill(new Ellipse2D.Float(dx, cy - d * 0.5F, d, d));
               dx += step;
            }
         }

      }
   }

   private static void note(Graphics2D g, PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, float w, float h) {
      String text = c.carNote();
      if (text != null && !text.isBlank() && c.carNumbers() != null && !c.carNumbers().isEmpty()) {
         line(g, text, assets == null ? null : assets.cjk(), "\u6ce8\uff1a\u7f16\u7ec4\u6309\u7ebf\u8def\u5b9e\u9645\u8fd0\u8425\u8f66\u8f86", 0.7F * h, 0.042F * h, 0.395F * w, PsdLcdPainter.Align.LEFT, 0.2944F * w, -1);
      }
   }

   public static float[] tickerBar(float width, float height) {
      float h = layoutHeight(width, height);
      float offY = (height - h) * 0.5F;
      return new float[]{0.0999F * width, offY + 0.828F * h, 0.7171F * width, offY + 0.9756F * h};
   }

   public static float[] tickerInner(float width, float height) {
      float[] bar = tickerBar(width, height);
      float h = layoutHeight(width, height);
      float inset = Math.max(1.0F, 0.0075F * h) * 1.5F;
      return new float[]{bar[0] + inset, bar[1] + inset, bar[2] - inset, bar[3] - inset};
   }

   public static float tickerFontPx(float width, float height) {
      float[] bar = tickerBar(width, height);
      return Math.max(1.0F, (bar[3] - bar[1]) * 0.52F);
   }

   public static float tickerTextWidth(PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, float width, float height) {
      return tickerTextWidthFor(c == null ? null : c.tickerText(), assets, width, height);
   }

   public static float tickerTextWidthFor(String text, PsdLcdPainter.Assets assets, float width, float height) {
      if (text != null && !text.isBlank()) {
         float inkH = tickerFontPx(width, height);
         String fontKey = assets == null ? "logic" : String.valueOf(System.identityHashCode(assets.cjk));
         if (text.equals(widthCacheText) && Math.abs(inkH - widthCacheInk) < 1.0E-4F && fontKey.equals(widthCacheFont)) {
            return widthCacheWidth;
         } else {
            Font font = fit(assets == null ? null : assets.cjk(), text, inkH);
            float ink = inkWidth(font, text);
            widthCacheText = text;
            widthCacheInk = inkH;
            widthCacheFont = fontKey;
            widthCacheWidth = ink;
            return ink;
         }
      } else {
         return 0.0F;
      }
   }

   public static int tickerStripWidth(float width, float height) {
      float[] inner = tickerInner(width, height);
      return Math.max(1, Math.round(inner[2] - inner[0]));
   }

   public static int tickerStripHeight(float width, float height) {
      float[] inner = tickerInner(width, height);
      return Math.max(1, Math.round(inner[3] - inner[1]));
   }

   public static float tickerScrollSpanPx(PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, float width, float height) {
      float[] inner = tickerInner(width, height);
      return inner[2] - inner[0] + tickerTextWidth(c, assets, width, height);
   }

   public static BufferedImage paintTickerStrip(PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, float width, float height, float phase) {
      int sw = tickerStripWidth(width, height);
      int sh = tickerStripHeight(width, height);
      BufferedImage image = new BufferedImage(sw, sh, 2);
      String text = c == null ? null : c.tickerText();
      if (text != null && !text.isBlank()) {
         Graphics2D g = image.createGraphics();

         try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            float inkH = tickerFontPx(width, height);
            float span = tickerScrollSpanPx(c, assets, width, height);
            float p = Math.max(0.0F, Math.min(1.0F, phase));
            float x = (float)sw - p * span;
            line(g, text, assets == null ? null : assets.cjk(), text, (float)sh * 0.5F - inkH * 0.5F, inkH, x, PsdLcdPainter.Align.LEFT, 0.0F, -1);
         } finally {
            g.dispose();
         }

         return image;
      } else {
         return image;
      }
   }

   private static void paintTicker(Graphics2D g, PsdLcdPainter.Content c, PsdLcdPainter.Assets assets, float width, float height, Float tickerPhase) {
      String text = c.tickerText();
      if (text != null && !text.isBlank()) {
         float[] bar = tickerBar(width, height);
         float h = layoutHeight(width, height);
         float offY = (height - h) * 0.5F;
         float tx0 = bar[0];
         float tx1 = bar[2];
         float ty0 = bar[1] - offY;
         float ty1 = bar[3] - offY;
         float radius = (ty1 - ty0) * 0.5F;
         g.setColor(argb(-1643777));
         g.setStroke(new BasicStroke(Math.max(1.0F, 0.0075F * h), 0, 1));
         g.draw(new RoundRectangle2D.Float(tx0 + 0.0075F * h * 0.5F, ty0 + 0.0075F * h * 0.5F, tx1 - tx0 - 0.0075F * h, ty1 - ty0 - 0.0075F * h, radius * 2.0F, radius * 2.0F));
         g.setStroke(new BasicStroke(1.0F));
         float[] inner = tickerInner(width, height);
         float textW = tickerTextWidth(c, assets, width, height);
         if (tickerPhase != null && !(textW <= 0.0F)) {
            BufferedImage strip = paintTickerStrip(c, assets, width, height, tickerPhase);
            g.drawImage(strip, Math.round(inner[0]), Math.round(inner[1] - offY), (ImageObserver)null);
         }
      }
   }

   private static void drawScaled(Graphics2D g, BufferedImage src, float x, float y, float w, float h) {
      if (src != null && !(w < 1.0F) && !(h < 1.0F)) {
         BufferedImage current = src;
         int cw = src.getWidth();

         for(int chh = src.getHeight(); (float)(cw / 2) >= w * 2.0F && (float)(chh / 2) >= h * 2.0F; chh /= 2) {
            BufferedImage half = new BufferedImage(cw / 2, chh / 2, 2);
            Graphics2D hg = half.createGraphics();

            try {
               hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
               hg.drawImage(current, 0, 0, cw / 2, chh / 2, (ImageObserver)null);
            } finally {
               hg.dispose();
            }

            current = half;
            cw /= 2;
         }

         g.drawImage(current, Math.round(x), Math.round(y), Math.round(w), Math.round(h), (ImageObserver)null);
      }
   }

   private static void drawScaled(Graphics2D g, BufferedImage src, float x, float y, float w, float h, float[] uv) {
      if (src != null && !(w < 1.0F) && !(h < 1.0F)) {
         if (uv != null && uv.length >= 4) {
            int iw = src.getWidth();
            int ih = src.getHeight();
            float u0 = clamp01(Math.min(uv[0], uv[2]));
            float v0 = clamp01(Math.min(uv[1], uv[3]));
            float u1 = clamp01(Math.max(uv[0], uv[2]));
            float v1 = clamp01(Math.max(uv[1], uv[3]));
            int sx0 = Math.round(u0 * (float)iw);
            int sy0 = Math.round(v0 * (float)ih);
            int sx1 = Math.max(sx0 + 1, Math.round(u1 * (float)iw));
            int sy1 = Math.max(sy0 + 1, Math.round(v1 * (float)ih));
            int dx0 = Math.round(x);
            int dy0 = Math.round(y);
            g.drawImage(src, dx0, dy0, dx0 + Math.round(w), dy0 + Math.round(h), sx0, sy0, sx1, sy1, (ImageObserver)null);
         } else {
            drawScaled(g, src, x, y, w, h);
         }
      }
   }

   private static void line(Graphics2D g, String text, Font base, String ref, float inkTop, float inkH, float anchor, PsdLcdPainter.Align align, float maxW, int argbColor) {
      if (text != null && !text.isEmpty() && !(inkH <= 0.0F)) {
         PsdLcdPainter.Fitted fitted = fitToColumn(text, base, ref == null ? text : ref, inkH, maxW);
         Font font = fitted.font();
         String drawn = fitted.text();
         float w = inkWidth(font, drawn);
         float inkX0 = inkX0(font, drawn);
         float inkY0 = inkY0(font, drawn);
         float top = fitted.shrunk() ? inkTop + (inkH - inkHeight(font, drawn)) * 0.5F : inkTop;
         float var10000;
         switch (align.ordinal()) {
            case 1:
               var10000 = anchor - inkX0 - w * 0.5F;
               break;
            case 2:
               var10000 = anchor - inkX0 - w;
               break;
            default:
               var10000 = anchor - inkX0;
         }

         float x = var10000;
         g.setFont(font);
         g.setColor(new Color(argbColor, true));
         g.drawString(drawn, x, top - inkY0);
      }
   }

   public static float columnWidth(PsdLcdPainter.Column column, float width) {
      float var10000;
      switch (column.ordinal()) {
         case 0:
         case 1:
            var10000 = 2.0F * Math.min(0.058300003F, 0.07689999F) * width;
            break;
         case 2:
         case 3:
            var10000 = 2.0F * Math.min(0.06380004F, 0.10519999F) * width;
            break;
         case 4:
            var10000 = 0.04190001F * width;
            break;
         case 5:
            var10000 = 0.12710002F * width;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static float columnWidth(PsdLcdPainter.Column column, float width, PsdLcdPainter.ArrivalColumns cols) {
      return cols == null || column != PsdLcdPainter.Column.DEST_ZH && column != PsdLcdPainter.Column.DEST_EN ? columnWidth(column, width) : cols.nameW();
   }

   public static float columnAnchor(PsdLcdPainter.Column column, float width) {
      float var10000;
      switch (column.ordinal()) {
         case 0:
         case 1:
            var10000 = 0.2212F * width;
            break;
         case 2:
         case 3:
            var10000 = 0.6075F * width;
            break;
         case 4:
            var10000 = 0.34F * width;
            break;
         case 5:
            var10000 = 0.3051F * width;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static float columnAnchor(PsdLcdPainter.Column column, float width, PsdLcdPainter.ArrivalColumns cols) {
      return cols == null || column != PsdLcdPainter.Column.DEST_ZH && column != PsdLcdPainter.Column.DEST_EN ? columnAnchor(column, width) : cols.nameCenter();
   }

   public static PsdLcdPainter.Align columnAlign(PsdLcdPainter.Column column) {
      PsdLcdPainter.Align var10000;
      switch (column.ordinal()) {
         case 0:
         case 1:
         case 2:
         case 3:
            var10000 = PsdLcdPainter.Align.CENTER;
            break;
         case 4:
            var10000 = PsdLcdPainter.Align.RIGHT;
            break;
         case 5:
            var10000 = PsdLcdPainter.Align.LEFT;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static float columnLeft(PsdLcdPainter.Column column, float width) {
      float anchor = columnAnchor(column, width);
      float var10000;
      switch (columnAlign(column).ordinal()) {
         case 0:
            var10000 = anchor;
            break;
         case 1:
            var10000 = anchor - columnWidth(column, width) * 0.5F;
            break;
         case 2:
            var10000 = anchor - columnWidth(column, width);
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static float columnLeft(PsdLcdPainter.Column column, float width, PsdLcdPainter.ArrivalColumns cols) {
      float anchor = columnAnchor(column, width, cols);
      float var10000;
      switch (columnAlign(column).ordinal()) {
         case 0:
            var10000 = anchor;
            break;
         case 1:
            var10000 = anchor - columnWidth(column, width, cols) * 0.5F;
            break;
         case 2:
            var10000 = anchor - columnWidth(column, width, cols);
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static float columnRight(PsdLcdPainter.Column column, float width) {
      float anchor = columnAnchor(column, width);
      float var10000;
      switch (columnAlign(column).ordinal()) {
         case 0:
            var10000 = anchor + columnWidth(column, width);
            break;
         case 1:
            var10000 = anchor + columnWidth(column, width) * 0.5F;
            break;
         case 2:
            var10000 = anchor;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static float columnRight(PsdLcdPainter.Column column, float width, PsdLcdPainter.ArrivalColumns cols) {
      float anchor = columnAnchor(column, width, cols);
      float var10000;
      switch (columnAlign(column).ordinal()) {
         case 0:
            var10000 = anchor + columnWidth(column, width, cols);
            break;
         case 1:
            var10000 = anchor + columnWidth(column, width, cols) * 0.5F;
            break;
         case 2:
            var10000 = anchor;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static float inkLeft(PsdLcdPainter.Column column, PsdLcdPainter.Fitted fitted, float width) {
      float anchor = columnAnchor(column, width);
      float w = inkWidth(fitted.font(), fitted.text());
      float var10000;
      switch (columnAlign(column).ordinal()) {
         case 0:
            var10000 = anchor;
            break;
         case 1:
            var10000 = anchor - w * 0.5F;
            break;
         case 2:
            var10000 = anchor - w;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static float inkLeft(PsdLcdPainter.Column column, PsdLcdPainter.Fitted fitted, float width, PsdLcdPainter.ArrivalColumns cols) {
      float anchor = columnAnchor(column, width, cols);
      float w = inkWidth(fitted.font(), fitted.text());
      float var10000;
      switch (columnAlign(column).ordinal()) {
         case 0:
            var10000 = anchor;
            break;
         case 1:
            var10000 = anchor - w * 0.5F;
            break;
         case 2:
            var10000 = anchor - w;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static float inkRight(PsdLcdPainter.Column column, PsdLcdPainter.Fitted fitted, float width) {
      return inkLeft(column, fitted, width) + inkWidth(fitted.font(), fitted.text());
   }

   public static float inkRight(PsdLcdPainter.Column column, PsdLcdPainter.Fitted fitted, float width, PsdLcdPainter.ArrivalColumns cols) {
      return inkLeft(column, fitted, width, cols) + inkWidth(fitted.font(), fitted.text());
   }

   public static PsdLcdPainter.Fitted fitColumn(PsdLcdPainter.Column column, String text, PsdLcdPainter.Assets assets, float width, float height) {
      return fitColumn(column, text, assets, width, height, arrivalColumns(assets, width, height));
   }

   public static PsdLcdPainter.Fitted fitColumn(PsdLcdPainter.Column column, String text, PsdLcdPainter.Assets assets, float width, float height, PsdLcdPainter.ArrivalColumns cols) {
      PsdLcdPainter.Assets a = assets == null ? new PsdLcdPainter.Assets() : assets;
      float h = layoutHeight(width, height);
      Font base;
      String ref;
      float inkH;
      switch (column.ordinal()) {
         case 0:
            base = a.cjk();
            ref = "\u534e\u4fa8\u5927\u5b66";
            inkH = 0.1014F * h;
            break;
         case 1:
            base = a.latin();
            ref = "Huaqiao University";
            inkH = 0.0513F * h;
            break;
         case 2:
            base = a.cjk();
            ref = "\u8463\u4efb";
            inkH = 0.1643F * h;
            break;
         case 3:
            base = a.latin();
            ref = "Dongren";
            inkH = 0.0552F * h;
            break;
         case 4:
            base = a.digits();
            ref = "0";
            inkH = 0.1772F * h;
            break;
         default:
            base = a.cjk();
            ref = "\u5df2\u5230\u7ad9";
            inkH = 0.1772F * h;
      }

      boolean mayTruncate = column != PsdLcdPainter.Column.COUNT;
      return fitToColumn(text, base, ref, inkH, columnWidth(column, width, cols), mayTruncate);
   }

   private static PsdLcdPainter.Fitted fitToColumn(String text, Font base, String ref, float nominalInkH, float maxW) {
      return fitToColumn(text, base, ref, nominalInkH, maxW, true);
   }

   private static PsdLcdPainter.Fitted fitToColumn(String text, Font base, String ref, float nominalInkH, float maxW, boolean mayTruncate) {
      Font nominal = fit(base, ref, nominalInkH);
      float nominalSize = nominal.getSize2D();
      if (text != null && !text.isEmpty() && !(maxW <= 0.0F)) {
         TextFit.Metrics metrics = metrics(nominal);
         float size = TextFit.fitFontSize(text, metrics, nominalSize, maxW, 0.62F);
         boolean shrunk = size < nominalSize - 0.01F;
         String drawn = mayTruncate ? TextFit.ellipsize(text, metrics, size, maxW) : text;
         return new PsdLcdPainter.Fitted(nominal.deriveFont(Math.max(1.0F, size)), drawn, size, shrunk, !drawn.equals(text));
      } else {
         return new PsdLcdPainter.Fitted(nominal, text == null ? "" : text, nominalSize, false, false);
      }
   }

   private static TextFit.Metrics metrics(Font base) {
      return (text, size) -> inkWidth(base.deriveFont(Math.max(1.0F, size)), text);
   }

   private static Font fit(Font base, String ref, float inkH) {
      Font font = base != null ? base : new Font("SansSerif", 0, 100);
      Font probe = font.deriveFont(100.0F);
      float ih = inkHeight(probe, ref);
      return ih < 0.5F ? probe : font.deriveFont(Math.max(1.0F, 100.0F * inkH / ih));
   }

   private static Shape outline(Font font, String text) {
      if (text != null && !text.isEmpty()) {
         GlyphVector gv = font.createGlyphVector(FRC, text);
         return gv.getOutline(0.0F, 0.0F);
      } else {
         return null;
      }
   }

   private static float inkWidth(Font font, String text) {
      Shape s = outline(font, text);
      return s == null ? 0.0F : (float)s.getBounds2D().getWidth();
   }

   private static float inkHeight(Font font, String text) {
      Shape s = outline(font, text);
      return s == null ? 0.0F : (float)s.getBounds2D().getHeight();
   }

   private static float inkX0(Font font, String text) {
      Shape s = outline(font, text);
      return s == null ? 0.0F : (float)s.getBounds2D().getX();
   }

   private static float inkY0(Font font, String text) {
      Shape s = outline(font, text);
      return s == null ? 0.0F : (float)s.getBounds2D().getY();
   }

   private static Color argb(int c) {
      return new Color(c, true);
   }

   public static enum Align {
      LEFT,
      CENTER,
      RIGHT;
   }

   public static record ArrivalColumns(float labelX, float labelW, float gap, float nameX, float nameRight) {
      public float labelRight() {
         return this.labelX + this.labelW;
      }

      public float nameW() {
         return Math.max(0.0F, this.nameRight - this.nameX);
      }

      public float nameCenter() {
         return this.nameX + this.nameW() * 0.5F;
      }
   }

   public static final class Assets {
      public BufferedImage logo;
      public BufferedImage nose;
      public BufferedImage carEnd;
      public BufferedImage carMiddle;
      public Font cjk;
      public Font latin;
      public Font digits;

      public Assets() {
      }

      public Assets(BufferedImage logo, Font cjk, Font latin, Font digits) {
         this.logo = logo;
         this.cjk = cjk;
         this.latin = latin;
         this.digits = digits;
      }

      Font cjk() {
         return this.cjk != null ? this.cjk : new Font("SansSerif", 1, 100);
      }

      Font latin() {
         return this.latin != null ? this.latin : new Font("SansSerif", 0, 100);
      }

      Font digits() {
         return this.digits != null ? this.digits : this.latin();
      }
   }

   public static record CarRow(int segments, int declared, int shown, boolean degraded, float boxX0, float boxX1, float boxY0, float boxY1, float cardsX0, float cardW, float cardWNat, float gap, float cardH, float cardY0, float noseX, float noseW, float noseH, float ellipsisW, float avail) {
      public float[] box() {
         return new float[]{this.boxX0, this.boxY0, this.boxX1, this.boxY1};
      }
   }

   public static enum Column {
      DEST_ZH,
      DEST_EN,
      STATION_ZH,
      STATION_EN,
      COUNT,
      COUNT_STATE;
   }

   public static record Content(String date, String weekdayZh, String weekdayEn, String clock, String boundLabel, String boundSub, String dest1, String dest1En, String count1, String nextLabel, String nextSub, String dest2, String dest2En, String count2, String unitZh, String unitEn, String unitZh2, String unitEn2, String lineZh, String lineEn, int lineColor, String stationZh, String stationEn, List<String> carNumbers, String carNote, String tickerText, String adLabel) {
   }

   public static record Fitted(Font font, String text, float size, boolean shrunk, boolean truncated) {
   }
}
