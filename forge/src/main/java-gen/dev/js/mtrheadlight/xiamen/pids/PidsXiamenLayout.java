package dev.js.mtrheadlight.xiamen.pids;

public final class PidsXiamenLayout {
   public static final float SCREEN_WIDTH = 512.0F;
   public static final float SCREEN_HEIGHT = 235.0F;
   public static final float SCREEN_SCALE_DIVISOR = 261.1F;
   public static final float ANCHOR_OUT = 0.4797F;
   public static final float ANCHOR_ALONG = 0.48F;
   public static final float ANCHOR_Y = 0.8467F;
   public static final float SCREEN_TILT_X = 22.5F;
   public static final float TEXT_LAYER_Z = -0.7F;
   public static final float AD_LAYER_Z = 0.35F;
   public static final int FULL_LIGHT = 15728880;
   public static final float FONT_UNIT_PX = 8.0F;
   public static final int COLOR_WHITE = -1;
   public static final boolean AD_KEEP_ASPECT = false;
   public static final int AD_ADVANCE_TICKS = 200;
   public static final float TICKER_INSET = 5.0F;
   public static final int LINES_PER_ARRIVAL = 1;
   private static final PidsXiamenLayout.Clock METRO_CLOCK = new PidsXiamenLayout.Clock(new PidsXiamenLayout.Rect(7.0F, 6.0F, 169.0F, 47.0F), 0.12F, 0.1F, new PidsXiamenLayout.TextStyle(0.22F, -1, 0.33F, PidsXiamenLayout.HAlign.LEFT, 0.62F), new PidsXiamenLayout.TextStyle(0.3F, -1, 0.33F, PidsXiamenLayout.HAlign.LEFT, 0.62F));
   private static final PidsXiamenLayout.Row[] METRO_ROWS = new PidsXiamenLayout.Row[]{metroRow(new PidsXiamenLayout.Rect(7.0F, 51.0F, 169.0F, 107.0F)), metroRow(new PidsXiamenLayout.Rect(7.0F, 111.0F, 169.0F, 166.0F)), metroRow(new PidsXiamenLayout.Rect(7.0F, 172.0F, 169.0F, 227.0F))};
   public static final PidsXiamenLayout.Metro METRO = new PidsXiamenLayout.Metro(METRO_CLOCK, new PidsXiamenLayout.Rect(179.0F, 33.0F, 512.0F, 198.0F), new PidsXiamenLayout.Rect(178.0F, 199.0F, 501.0F, 227.0F), new PidsXiamenLayout.TextStyle(0.52F, -1, 0.5F, PidsXiamenLayout.HAlign.CENTER, 1.0F), METRO_ROWS);
   private static final PidsXiamenLayout.Clock BRT_CLOCK = new PidsXiamenLayout.Clock(new PidsXiamenLayout.Rect(9.0F, 9.0F, 167.0F, 70.0F), 0.12F, 0.1F, new PidsXiamenLayout.TextStyle(0.15F, -1, 0.41F, PidsXiamenLayout.HAlign.LEFT, 0.57F), new PidsXiamenLayout.TextStyle(0.2F, -1, 0.41F, PidsXiamenLayout.HAlign.LEFT, 0.57F));
   private static final PidsXiamenLayout.BrtRow[] BRT_ROWS = new PidsXiamenLayout.BrtRow[]{brtRow(new PidsXiamenLayout.Rect(9.0F, 78.0F, 167.0F, 147.0F)), brtRow(new PidsXiamenLayout.Rect(9.0F, 156.0F, 167.0F, 225.0F))};
   public static final PidsXiamenLayout.Brt BRT = new PidsXiamenLayout.Brt(BRT_CLOCK, new PidsXiamenLayout.Rect(179.0F, 10.0F, 504.0F, 176.0F), new PidsXiamenLayout.Rect(178.0F, 185.0F, 504.0F, 225.0F), new PidsXiamenLayout.TextStyle(0.38F, -1, 0.5F, PidsXiamenLayout.HAlign.CENTER, 1.0F), BRT_ROWS);

   private PidsXiamenLayout() {
   }

   private static PidsXiamenLayout.Row metroRow(PidsXiamenLayout.Rect card) {
      return new PidsXiamenLayout.Row(card, 0.07F, 0.05F, 9.0F, new PidsXiamenLayout.TextStyle(0.18F, -1, 0.0F, PidsXiamenLayout.HAlign.LEFT, 0.0F), new PidsXiamenLayout.TextStyle(0.105F, -1, 0.0F, PidsXiamenLayout.HAlign.LEFT, 0.0F), new PidsXiamenLayout.TextStyle(0.27F, -1, 0.0F, PidsXiamenLayout.HAlign.LEFT, 0.0F), new PidsXiamenLayout.TextStyle(0.125F, -1, 0.0F, PidsXiamenLayout.HAlign.LEFT, 0.0F), new PidsXiamenLayout.TextStyle(0.36F, -1, 0.0F, PidsXiamenLayout.HAlign.LEFT, 0.0F), new PidsXiamenLayout.TextStyle(0.16F, -1, 0.0F, PidsXiamenLayout.HAlign.LEFT, 0.0F));
   }

   private static PidsXiamenLayout.BrtRow brtRow(PidsXiamenLayout.Rect card) {
      float x = 13.0F / card.w();
      return new PidsXiamenLayout.BrtRow(card, 0.1F, 0.12F, new PidsXiamenLayout.TextStyle(0.21F, -1, x, PidsXiamenLayout.HAlign.LEFT, 0.88F), new PidsXiamenLayout.TextStyle(0.21F, -1, x, PidsXiamenLayout.HAlign.LEFT, 0.42F), new PidsXiamenLayout.TextStyle(0.16F, -1, 0.0F, PidsXiamenLayout.HAlign.LEFT, 0.0F), new PidsXiamenLayout.TextStyle(0.3F, -1, 0.0F, PidsXiamenLayout.HAlign.LEFT, 0.0F), new PidsXiamenLayout.TextStyle(0.15F, -1, 0.0F, PidsXiamenLayout.HAlign.LEFT, 0.0F));
   }

   public static int rowsOf(boolean brt) {
      return brt ? BRT.rows().length : METRO.rows().length;
   }

   public static record Brt(PidsXiamenLayout.Clock clock, PidsXiamenLayout.Rect ad, PidsXiamenLayout.Rect ticker, PidsXiamenLayout.TextStyle tickerText, PidsXiamenLayout.BrtRow[] rows) {
   }

   public static record BrtRow(PidsXiamenLayout.Rect card, float padRatio, float gapRatio, PidsXiamenLayout.TextStyle dest, PidsXiamenLayout.TextStyle route, PidsXiamenLayout.TextStyle about, PidsXiamenLayout.TextStyle count, PidsXiamenLayout.TextStyle unit) {
   }

   public static record Clock(PidsXiamenLayout.Rect card, float padRatio, float gapRatio, PidsXiamenLayout.TextStyle date, PidsXiamenLayout.TextStyle time) {
   }

   public static enum HAlign {
      LEFT,
      CENTER,
      RIGHT;
   }

   public static record Metro(PidsXiamenLayout.Clock clock, PidsXiamenLayout.Rect ad, PidsXiamenLayout.Rect ticker, PidsXiamenLayout.TextStyle tickerText, PidsXiamenLayout.Row[] rows) {
   }

   public static record Rect(float x0, float y0, float x1, float y1) {
      public float w() {
         return this.x1 - this.x0;
      }

      public float h() {
         return this.y1 - this.y0;
      }

      public float cx() {
         return (this.x0 + this.x1) * 0.5F;
      }

      public float cy() {
         return (this.y0 + this.y1) * 0.5F;
      }

      public float x(float ratio) {
         return this.x0 + this.w() * ratio;
      }

      public float y(float ratio) {
         return this.y0 + this.h() * ratio;
      }
   }

   public static record Row(PidsXiamenLayout.Rect card, float padRatio, float gapRatio, float labelXInset, PidsXiamenLayout.TextStyle label, PidsXiamenLayout.TextStyle labelSub, PidsXiamenLayout.TextStyle dest, PidsXiamenLayout.TextStyle destSub, PidsXiamenLayout.TextStyle count, PidsXiamenLayout.TextStyle unit) {
   }

   public static record TextStyle(float sizeRatio, int color, float xRatio, PidsXiamenLayout.HAlign align, float maxWidthRatio) {
   }
}
