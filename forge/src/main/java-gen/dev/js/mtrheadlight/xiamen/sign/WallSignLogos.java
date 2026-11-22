package dev.js.mtrheadlight.xiamen.sign;

public final class WallSignLogos {
   private static final ThreadLocal<String> BRAND = new ThreadLocal();

   private WallSignLogos() {
   }

   public static void push(String brand) {
      if (brand != null && !brand.isEmpty()) {
         BRAND.set(brand);
      }
   }

   public static void pop() {
      BRAND.remove();
   }

   public static String brand() {
      return (String)BRAND.get();
   }

   public static String texturePath(String logo) {
      return "textures/block/" + logo + ".png";
   }

   public static String cacheKeySuffix() {
      String brand = (String)BRAND.get();
      return brand == null ? "" : "_sign_" + brand;
   }
}
