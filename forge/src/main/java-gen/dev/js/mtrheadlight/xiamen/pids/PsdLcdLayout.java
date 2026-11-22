package dev.js.mtrheadlight.xiamen.pids;

public final class PsdLcdLayout {
   public static final float PX_PER_BLOCK = 520.0F;
   public static final float PANEL_Z0_PX = 10.0F;
   public static final float PANEL_Z1_PX = 16.0F;
   public static final float BOARD_THICKNESS_PX = 6.0F;
   public static final float SCREEN_RECESS_PX = 0.2F;
   public static final float SCREEN_FACE_Z_PX = 10.2F;
   public static final float FACE_GAP_PX = 0.016F;
   public static final float WINDOW_Y0_PX = 1.5F;
   public static final float WINDOW_Y1_PX = 8.5F;
   public static final float CUT_WINDOW_Y0_PX = 9.0F;
   public static final float CUT_WINDOW_Y1_PX = 16.0F;
   public static final float WINDOW_X0_PX = 0.0F;
   public static final float WINDOW_X1_PX = 16.0F;
   public static final float WINDOW_ASPECT = 2.2857144F;
   public static final float HEIGHT_BLOCKS = 0.4375F;
   public static final float HEIGHT_PX = 227.5F;
   public static final float WINDOW_Y0_BLOCKS = 0.09375F;
   public static final float WINDOW_Y1_BLOCKS = 0.53125F;
   public static final float CUT_WINDOW_Y0_BLOCKS = 0.5625F;
   public static final float CUT_WINDOW_Y1_BLOCKS = 1.0F;
   public static final float WINDOW_DEPTH_BLOCKS = 0.6365F;
   public static final float CENTER_Y_BLOCKS = 0.3125F;
   public static final float STRIP_Y0_PX = 1.0F;
   public static final float STRIP_Y1_PX = 1.5F;
   public static final float CUT_STRIP_Y0_PX = 8.5F;
   public static final float CUT_STRIP_Y1_PX = 9.0F;
   public static final float STRIP_HEIGHT_PX = 0.5F;
   public static final float STRIP_DEPTH_BLOCKS = 0.624F;
   public static final float BITMAP_SCALE = 2.0F;

   private PsdLcdLayout() {
   }

   public static float[] windowBandPx(boolean cut) {
      return cut ? new float[]{9.0F, 16.0F} : new float[]{1.5F, 8.5F};
   }

   public static float[] windowBandBlocks(boolean cut) {
      float[] px = windowBandPx(cut);
      return new float[]{px[0] / 16.0F, px[1] / 16.0F};
   }

   public static float[] stripBandPx(boolean cut) {
      return cut ? new float[]{8.5F, 9.0F} : new float[]{1.0F, 1.5F};
   }

   public static float[] stripBandBlocks(boolean cut) {
      float[] px = stripBandPx(cut);
      return new float[]{px[0] / 16.0F, px[1] / 16.0F};
   }

   public static float[][] stripQuad(PsdLcdLayout.Face facing, float startPx, float endPx, boolean cut) {
      float[] band = stripBandPx(cut);
      return planeQuad(facing, startPx, endPx, band[0], band[1], 0.624F);
   }

   public static float[][] stripQuad(PsdLcdLayout.Face facing, float startPx, float endPx, boolean cut, float v0, float v1) {
      float[] band = stripBandPx(cut);
      float h = band[1] - band[0];
      return planeQuad(facing, startPx, endPx, band[0] + h * Math.min(v0, v1), band[0] + h * Math.max(v0, v1), 0.624F);
   }

   public static float widthPx(int segments) {
      return (float)Math.max(1, segments) * 520.0F;
   }

   public static int bitmapWidth(int segments) {
      return Math.max(1, Math.round(widthPx(segments) * 2.0F));
   }

   public static int bitmapWidthFor(float spanPx) {
      return Math.max(1, Math.round(Math.max(1.0F, spanPx) / 16.0F * 520.0F * 2.0F));
   }

   public static int bitmapHeight(boolean cut) {
      float[] band = windowBandPx(cut);
      float blocks = (band[1] - band[0]) / 16.0F;
      return Math.max(1, Math.round(blocks * 520.0F * 2.0F));
   }

   public static int bitmapHeight() {
      return bitmapHeight(false);
   }

   public static float[][] windowQuad(PsdLcdLayout.Face facing, int segments) {
      return windowQuad(facing, 0.0F, (float)segments, false);
   }

   public static float[][] windowQuad(PsdLcdLayout.Face facing, float startPx, float endPx) {
      return windowQuad(facing, startPx, endPx, false);
   }

   public static float[][] windowQuad(PsdLcdLayout.Face facing, int segments, boolean cut) {
      return windowQuad(facing, 0.0F, (float)segments, cut);
   }

   public static float[][] windowQuad(PsdLcdLayout.Face facing, float startPx, float endPx, boolean cut) {
      float[] band = windowBandPx(cut);
      return planeQuad(facing, startPx, endPx, band[0], band[1], 0.6365F);
   }

   public static float[] screenNormal(PsdLcdLayout.Face facing) {
      PsdLcdLayout.Face f = facing == null ? PsdLcdLayout.Face.NORTH : facing;
      return new float[]{(float)f.dx, 0.0F, (float)f.dz};
   }

   public static float[][] bandQuad(PsdLcdLayout.Face facing, float totalPx, float fx0, float fx1, float fy0, float fy1, boolean cut, float liftPx) {
      float[] band = windowBandPx(cut);
      float startPx = Math.min(fx0, fx1) * totalPx;
      float endPx = Math.max(fx0, fx1) * totalPx;
      float span = band[1] - band[0];
      float top = band[1] - Math.min(fy0, fy1) * span;
      float bottom = band[1] - Math.max(fy0, fy1) * span;
      return planeQuad(facing, startPx, endPx, bottom, top, (10.184F - liftPx) / 16.0F);
   }

   public static float[][] adQuad(PsdLcdLayout.Face facing, float totalPx, float fx0, boolean cut) {
      float left = Math.max(0.0F, Math.min(1.0F, fx0));
      return bandQuad(facing, totalPx, left, 1.0F, 0.0F, 1.0F, cut, 0.0F);
   }

   private static float[][] planeQuad(PsdLcdLayout.Face facing, float startPx, float endPx, float y0Px, float y1Px, float zBlocks) {
      PsdLcdLayout.Face f = facing == null ? PsdLcdLayout.Face.NORTH : facing;
      PsdLcdLayout.Face right = f.right();
      float from = Math.min(startPx, endPx) / 16.0F;
      float to = Math.max(startPx, endPx) / 16.0F;
      float y0 = Math.min(y0Px, y1Px) / 16.0F;
      float y1 = Math.max(y0Px, y1Px) / 16.0F;
      float plane = f.negativeAxis() ? zBlocks : 1.0F - zBlocks;
      float startX = 0.5F - (float)right.dx * 0.5F;
      float startZ = 0.5F - (float)right.dz * 0.5F;
      if (right.dx != 0) {
         float x0 = startX + (float)right.dx * from;
         float x1 = startX + (float)right.dx * to;
         return new float[][]{{x0, y1, plane}, {x0, y0, plane}, {x1, y0, plane}, {x1, y1, plane}};
      } else {
         float z0 = startZ + (float)right.dz * from;
         float z1 = startZ + (float)right.dz * to;
         return new float[][]{{plane, y1, z0}, {plane, y0, z0}, {plane, y0, z1}, {plane, y1, z1}};
      }
   }

   public static enum Face {
      NORTH(0, -1),
      EAST(1, 0),
      SOUTH(0, 1),
      WEST(-1, 0);

      public final int dx;
      public final int dz;

      private Face(final int dx, final int dz) {
         this.dx = dx;
         this.dz = dz;
      }

      public PsdLcdLayout.Face opposite() {
         PsdLcdLayout.Face var10000;
         switch (this.ordinal()) {
            case 0:
               var10000 = SOUTH;
               break;
            case 1:
               var10000 = WEST;
               break;
            case 2:
               var10000 = NORTH;
               break;
            case 3:
               var10000 = EAST;
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      public PsdLcdLayout.Face right() {
         PsdLcdLayout.Face var10000;
         switch (this.ordinal()) {
            case 0:
               var10000 = WEST;
               break;
            case 1:
               var10000 = NORTH;
               break;
            case 2:
               var10000 = EAST;
               break;
            case 3:
               var10000 = SOUTH;
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      public int blockstateY() {
         short var10000;
         switch (this.ordinal()) {
            case 0:
               var10000 = 0;
               break;
            case 1:
               var10000 = 90;
               break;
            case 2:
               var10000 = 180;
               break;
            case 3:
               var10000 = 270;
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      public boolean negativeAxis() {
         return this.dx < 0 || this.dz < 0;
      }
   }
}
