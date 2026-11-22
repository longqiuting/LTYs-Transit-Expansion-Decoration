package dev.js.mtrheadlight.xiamen;

public enum XiamenLine {
   L1("", "1", "orange"),
   L2("_l2", "2", "orange"),
   L3("_l3", "3", "orange"),
   L4("_l4", "4", "green"),
   L6("_l6", "6", "green");

   public final String suffix;
   public final String number;
   private final String doorLight;

   private XiamenLine(final String suffix, final String number, final String doorLight) {
      this.suffix = suffix;
      this.number = number;
      this.doorLight = doorLight;
   }

   public String id(String base) {
      return base + this.suffix;
   }

   public String textureVariant() {
      return this.suffix.isEmpty() ? null : this.suffix;
   }

   public String doorLight() {
      return this.doorLight;
   }

   public String toString() {
      return "xiamen L" + this.number;
   }
}
