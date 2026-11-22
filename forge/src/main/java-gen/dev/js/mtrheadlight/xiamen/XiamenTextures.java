package dev.js.mtrheadlight.xiamen;

import dev.js.mtrheadlight.LteLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class XiamenTextures {
   public static final String MOD_NAMESPACE = "mtrlte";
   public static final String PACK_NAMESPACE = "mtr";
   public static volatile boolean useModTextures = true;
   private static final Map<String, String> RESOLVED = new ConcurrentHashMap();
   private static final Set<String> REPORTED_MISSING = ConcurrentHashMap.newKeySet();
   private static volatile ResourceManager lastManager;
   private static final ThreadLocal<String> ACTIVE_NAMESPACE = new ThreadLocal();
   private static final ThreadLocal<String> ACTIVE_LIGHT_COLOR = new ThreadLocal();
   private static final ThreadLocal<String> ACTIVE_VARIANT = new ThreadLocal();

   private XiamenTextures() {
   }

   public static void withNamespace(String namespace, Runnable body) {
      withNamespace(namespace, (String)null, body);
   }

   public static void withNamespace(String namespace, String doorLightColor, Runnable body) {
      withVariant(namespace, (String)null, doorLightColor, body);
   }

   public static void withVariant(String namespace, String variant, String doorLightColor, Runnable body) {
      if (body != null) {
         if (useModTextures && namespace != null && !namespace.isEmpty() && !"mtr".equals(namespace)) {
            String previous = (String)ACTIVE_NAMESPACE.get();
            String previousColor = (String)ACTIVE_LIGHT_COLOR.get();
            String previousVariant = (String)ACTIVE_VARIANT.get();
            ACTIVE_NAMESPACE.set(namespace);
            if (doorLightColor != null) {
               ACTIVE_LIGHT_COLOR.set(doorLightColor);
            }

            if (variant != null && !variant.isEmpty()) {
               ACTIVE_VARIANT.set(variant);
            }

            try {
               body.run();
            } finally {
               if (previous == null) {
                  ACTIVE_NAMESPACE.remove();
               } else {
                  ACTIVE_NAMESPACE.set(previous);
               }

               if (previousColor == null) {
                  ACTIVE_LIGHT_COLOR.remove();
               } else {
                  ACTIVE_LIGHT_COLOR.set(previousColor);
               }

               if (previousVariant == null) {
                  ACTIVE_VARIANT.remove();
               } else {
                  ACTIVE_VARIANT.set(previousVariant);
               }

            }

         } else {
            body.run();
         }
      }
   }

   public static void withVariant(String variant, String doorLightColor, Runnable body) {
      withVariant("mtrlte", variant, doorLightColor, body);
   }

   public static String resolveActive(String formatted) {
      String namespace = (String)ACTIVE_NAMESPACE.get();
      if (namespace == null) {
         return formatted;
      } else {
         try {
            return resolve(namespace, formatted, (String)ACTIVE_VARIANT.get(), (String)ACTIVE_LIGHT_COLOR.get());
         } catch (Throwable var3) {
            return formatted;
         }
      }
   }

   public static String resolve(Object renderer, String raw) {
      if (useModTextures && raw != null && renderer instanceof IXiamenTextureNamespace provider) {
         return resolve(provider.lte$textureNamespace(), raw);
      } else {
         return raw;
      }
   }

   public static String resolve(String namespace, String raw) {
      return resolve(namespace, raw, (String)null);
   }

   public static String resolve(String namespace, String raw, String doorLightColor) {
      return resolve(namespace, raw, (String)null, doorLightColor);
   }

   public static String resolve(String namespace, String raw, String variant, String doorLightColor) {
      if (useModTextures && raw != null) {
         if (namespace != null && !namespace.isEmpty() && !"mtr".equals(namespace) && raw.startsWith("mtr:")) {
            resetIfResourceManagerChanged();
            String tint = doorLightColor == null ? "" : doorLightColor;
            String suffix = variant == null ? "" : variant;
            String key = namespace + "|" + suffix + "|" + tint + "|" + raw;
            String cached = (String)RESOLVED.get(key);
            if (cached != null) {
               return cached;
            } else {
               String resolved = remap(namespace, raw, suffix, tint);
               RESOLVED.put(key, resolved);
               return resolved;
            }
         } else {
            return raw;
         }
      } else {
         return raw;
      }
   }

   private static String remap(String namespace, String raw, String variant, String lightColor) {
      String path = raw.substring("mtr".length() + 1);
      boolean hasVariant = variant != null && !variant.isEmpty();
      boolean colored = lightColor != null && !lightColor.isEmpty() && !"orange".equals(lightColor) && isDoorLight(path);
      List<String> tries = new ArrayList(4);
      if (colored) {
         if (hasVariant) {
            tries.add(insertBeforeExtension(path, variant + "_" + lightColor));
         }

         tries.add(insertBeforeExtension(path, "_" + lightColor));
      }

      if (hasVariant) {
         tries.add(insertBeforeExtension(path, variant));
      }

      tries.add(path);

      for(String candidatePath : tries) {
         String candidate = namespace + ":" + candidatePath;
         if (exists(candidate)) {
            return candidate;
         }
      }

      if (REPORTED_MISSING.add(raw + "#" + (hasVariant ? variant : "") + "@" + (colored ? lightColor : ""))) {
         LteLog.LOGGER.info("[lte] texture {} is not in {}{} -> keeping the resource-pack id", new Object[]{raw, namespace, hasVariant ? " as " + (String)tries.get(0) : ""});
      }

      return raw;
   }

   private static String insertBeforeExtension(String path, String suffix) {
      return path.endsWith(".png") ? path.substring(0, path.length() - 4) + suffix + ".png" : path + suffix + ".png";
   }

   private static boolean isDoorLight(String path) {
      return path.endsWith("light_on.png") || path.endsWith("light_off.png");
   }

   private static boolean exists(String id) {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client == null) {
            return false;
         } else {
            ResourceLocation identifier = ResourceLocation.tryParse(id);
            if (identifier == null) {
               return false;
            } else {
               return !client.getResourceManager().getResourceStack(identifier).isEmpty();
            }
         }
      } catch (Throwable var3) {
         return false;
      }
   }

   private static void resetIfResourceManagerChanged() {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client == null) {
            return;
         }

         ResourceManager manager = client.getResourceManager();
         if (manager != lastManager) {
            lastManager = manager;
            RESOLVED.clear();
            REPORTED_MISSING.clear();
         }
      } catch (Throwable var2) {
      }

   }

   public static int cachedCount() {
      return RESOLVED.size();
   }
}
