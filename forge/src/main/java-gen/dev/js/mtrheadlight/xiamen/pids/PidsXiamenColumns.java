package dev.js.mtrheadlight.xiamen.pids;

import java.util.ArrayList;
import java.util.List;

public final class PidsXiamenColumns {
   public static final float LABEL_GAP_RATIO = 0.6F;
   public static final float MIN_GAP_PX = 1.0F;

   private PidsXiamenColumns() {
   }

   public static float labelWidth(TextFit.Metrics metrics, List<PidsXiamenColumns.Label> labels) {
      return metrics == null ? 0.0F : labelWidth(labels, defaultMetrics(metrics, labels));
   }

   public static float labelWidth(List<PidsXiamenColumns.Label> labels, List<TextFit.Metrics> metricsPerLabel) {
      if (labels != null && !labels.isEmpty() && metricsPerLabel != null) {
         float widest = 0.0F;

         for(int i = 0; i < labels.size() && i < metricsPerLabel.size(); ++i) {
            PidsXiamenColumns.Label label = (PidsXiamenColumns.Label)labels.get(i);
            TextFit.Metrics metrics = (TextFit.Metrics)metricsPerLabel.get(i);
            if (metrics != null && label != null && label.text() != null && !label.text().isEmpty()) {
               float w = metrics.width(label.text(), label.size());
               if (w > widest) {
                  widest = w;
               }
            }
         }

         return widest;
      } else {
         return 0.0F;
      }
   }

   private static List<TextFit.Metrics> defaultMetrics(TextFit.Metrics metrics, List<PidsXiamenColumns.Label> labels) {
      List<TextFit.Metrics> out = new ArrayList(labels == null ? 0 : labels.size());
      if (labels != null) {
         for(int i = 0; i < labels.size(); ++i) {
            out.add(metrics);
         }
      }

      return out;
   }

   public static float gap(float rowGlyphSize) {
      return Math.max(1.0F, rowGlyphSize * 0.6F);
   }

   public static PidsXiamenColumns.Columns of(float labelX, float labelW, float rowGlyphSize, float nameRight) {
      float gap = gap(rowGlyphSize);
      return new PidsXiamenColumns.Columns(labelX, labelW, gap, labelX + labelW + gap, nameRight);
   }

   public static PidsXiamenColumns.Columns of(TextFit.Metrics metrics, List<PidsXiamenColumns.Label> labels, float labelX, float rowGlyphSize, float nameRight) {
      return of(labelX, labelWidth(metrics, labels), rowGlyphSize, nameRight);
   }

   public static record Columns(float labelX, float labelW, float gap, float nameX, float nameRight) {
      public float nameW() {
         return Math.max(0.0F, this.nameRight - this.nameX);
      }

      public float labelRight() {
         return this.labelX + this.labelW;
      }
   }

   public static record Label(String text, float size) {
   }
}
