package dev.js.mtrheadlight.mixin;

public final class HostProbe {
   private static final String MTR_PIDS_RENDERER = "mtr.render.RenderPIDS";
   private static final String TJ_PIDS_RENDERER = "ziyue.tjmetro.render.RenderPIDSTianjin";
   private static final String MTR_PSD_DOOR_RENDERER = "mtr.render.RenderPSDAPGDoor";
   private static final String MTR_ROUTE_MAP = "mtr.client.RouteMapGenerator";
   private static final String MTR_BLOCK_PLATFORM = "mtr.block.BlockPlatform";
   private static final boolean MTR_PIDS = present("mtr.render.RenderPIDS");
   private static final boolean TJ_PIDS = present("ziyue.tjmetro.render.RenderPIDSTianjin");
   private static final boolean MTR_PSD_DOOR = present("mtr.render.RenderPSDAPGDoor");
   private static final boolean MTR_ROUTE_MAP_PRESENT = present("mtr.client.RouteMapGenerator");
   private static final boolean MTR_PLATFORM_BLOCK = present("mtr.block.BlockPlatform");

   private HostProbe() {
   }

   public static boolean mtrPidsRenderer() {
      return MTR_PIDS;
   }

   public static boolean tjPidsRenderer() {
      return TJ_PIDS;
   }

   public static boolean mtrPsdDoorRenderer() {
      return MTR_PSD_DOOR;
   }

   public static boolean mtrRouteMap() {
      return MTR_ROUTE_MAP_PRESENT;
   }

   public static boolean mtrBlockPlatform() {
      return MTR_PLATFORM_BLOCK;
   }

   private static boolean present(String className) {
      try {
         String path = className.replace('.', '/') + ".class";
         ClassLoader own = HostProbe.class.getClassLoader();
         if (own != null && own.getResource(path) != null) {
            return true;
         } else {
            ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            return ctx != null && ctx.getResource(path) != null;
         }
      } catch (Throwable var4) {
         return false;
      }
   }
}
