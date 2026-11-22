package dev.js.mtrheadlight.rawmodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.js.mtrheadlight.LteLog;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class RawModelJson {
   public static final String DIRECTORY = "rawmodels/";
   private static final float UV_TO_UNIT = 0.0625F;
   private static final Map<ResourceLocation, RawMesh> CACHE = new ConcurrentHashMap();
   private static final Set<ResourceLocation> FAILED = ConcurrentHashMap.newKeySet();

   private RawModelJson() {
   }

   public static ResourceLocation id(String name) {
      return new ResourceLocation("mtrlte", "rawmodels/" + name);
   }

   public static RawMesh get(ResourceLocation id) {
      RawMesh cached = (RawMesh)CACHE.get(id);
      if (cached != null) {
         return cached;
      } else if (FAILED.contains(id)) {
         return null;
      } else {
         String path = "/assets/" + id.getNamespace() + "/" + id.getPath() + ".json";

         try {
            InputStream in = RawModelJson.class.getResourceAsStream(path);

            RawMesh mesh;
            label59: {
               RawMesh var5;
               try {
                  if (in == null) {
                     FAILED.add(id);
                     LteLog.LOGGER.warn("[lte] rawmodel: {} is missing from the classpath", path);
                     mesh = null;
                     break label59;
                  }

                  mesh = parse(id, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                  CACHE.put(id, mesh);
                  LteLog.LOGGER.info("[lte] rawmodel: loaded {} ({} quads, texture {})", new Object[]{id, mesh.quadCount(), mesh.texture()});
                  var5 = mesh;
               } catch (Throwable var7) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }
                  }

                  throw var7;
               }

               if (in != null) {
                  in.close();
               }

               return var5;
            }

            if (in != null) {
               in.close();
            }

            return mesh;
         } catch (Throwable var8) {
            FAILED.add(id);
            LteLog.LOGGER.warn("[lte] rawmodel: could not load {} ({})", path, var8.toString());
            return null;
         }
      }
   }

   public static float[] bounds(ResourceLocation id) {
      RawMesh mesh = get(id);
      if (mesh != null && mesh.quadCount() != 0) {
         float minX = Float.MAX_VALUE;
         float minY = Float.MAX_VALUE;
         float minZ = Float.MAX_VALUE;
         float maxX = -Float.MAX_VALUE;
         float maxY = -Float.MAX_VALUE;
         float maxZ = -Float.MAX_VALUE;

         for(RawMesh.Quad quad : mesh.quads()) {
            for(float[] p : quad.pos()) {
               minX = Math.min(minX, p[0]);
               minY = Math.min(minY, p[1]);
               minZ = Math.min(minZ, p[2]);
               maxX = Math.max(maxX, p[0]);
               maxY = Math.max(maxY, p[1]);
               maxZ = Math.max(maxZ, p[2]);
            }
         }

         return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
      } else {
         return null;
      }
   }

   public static RawMesh parse(ResourceLocation id, String json) {
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      JsonObject textures = root.has("textures") ? root.getAsJsonObject("textures") : new JsonObject();
      ResourceLocation texture = resolveTexture(id.getNamespace(), firstTextureName(textures));
      List<RawMesh.Quad> quads = new ArrayList();

      for(JsonElement raw : root.has("elements") ? root.getAsJsonArray("elements") : new JsonArray()) {
         parseElement(raw.getAsJsonObject(), quads);
      }

      return new RawMesh(texture, (RawMesh.Quad[])quads.toArray(new RawMesh.Quad[0]));
   }

   private static String firstTextureName(JsonObject textures) {
      String fallback = null;

      for(Map.Entry<String, JsonElement> entry : textures.entrySet()) {
         String name = ((JsonElement)entry.getValue()).getAsString();
         if (!"particle".equals(entry.getKey())) {
            return name;
         }

         fallback = name;
      }

      return fallback;
   }

   private static ResourceLocation resolveTexture(String namespace, String name) {
      if (name == null) {
         return new ResourceLocation(namespace, "textures/block/missing.png");
      } else {
         String ns = namespace;
         String path = name;
         int colon = name.indexOf(58);
         if (colon >= 0) {
            ns = name.substring(0, colon);
            path = name.substring(colon + 1);
         }

         if (!path.startsWith("textures/")) {
            path = "textures/block/" + path;
         }

         if (!path.endsWith(".png")) {
            path = path + ".png";
         }

         return new ResourceLocation(ns, path);
      }
   }

   private static void parseElement(JsonObject raw, List<RawMesh.Quad> out) {
      if (raw.has("from") && raw.has("to") && raw.has("faces")) {
         float[] from = readTriple(raw.getAsJsonArray("from"));
         float[] to = readTriple(raw.getAsJsonArray("to"));
         float x1 = Math.min(from[0], to[0]);
         float y1 = Math.min(from[1], to[1]);
         float z1 = Math.min(from[2], to[2]);
         float x2 = Math.max(from[0], to[0]);
         float y2 = Math.max(from[1], to[1]);
         float z2 = Math.max(from[2], to[2]);
         float[] origin = new float[3];
         float[] angles = new float[3];
         readRotation(raw.getAsJsonObject("rotation"), origin, angles);
         boolean rotated = angles[0] != 0.0F || angles[1] != 0.0F || angles[2] != 0.0F;
         boolean fullBright = raw.has("shade") && !raw.get("shade").getAsBoolean();
         JsonObject faces = raw.getAsJsonObject("faces");

         for(Direction face : Direction.values()) {
            String key = face.getName();
            if (faces.has(key)) {
               JsonObject faceJson = faces.getAsJsonObject(key);
               float[] uv = readQuad(faceJson.getAsJsonArray("uv"));
               int faceRotation = readFaceRotation(faceJson);
               RawMesh.Quad quad = buildQuad(face, x1, y1, z1, x2, y2, z2, uv, faceRotation, origin, angles, rotated, fullBright);
               if (quad != null) {
                  out.add(quad);
               }
            }
         }

      }
   }

   private static int readFaceRotation(JsonObject faceJson) {
      if (!faceJson.has("rotation")) {
         return 0;
      } else {
         float degrees = faceJson.get("rotation").getAsFloat();
         return (Math.round(degrees / 90.0F) % 4 + 4) % 4;
      }
   }

   private static void readRotation(JsonObject rotation, float[] origin, float[] angles) {
      if (rotation != null) {
         if (rotation.has("origin")) {
            float[] o = readTriple(rotation.getAsJsonArray("origin"));
            origin[0] = o[0];
            origin[1] = o[1];
            origin[2] = o[2];
         }

         if (rotation.has("axis")) {
            String axis = rotation.get("axis").getAsString();
            float angle = rotation.has("angle") ? rotation.get("angle").getAsFloat() : 0.0F;
            switch (axis) {
               case "x":
                  angles[0] = angle;
                  break;
               case "y":
                  angles[1] = angle;
                  break;
               case "z":
                  angles[2] = angle;
            }
         } else {
            angles[0] = rotation.has("x") ? rotation.get("x").getAsFloat() : 0.0F;
            angles[1] = rotation.has("y") ? rotation.get("y").getAsFloat() : 0.0F;
            angles[2] = rotation.has("z") ? rotation.get("z").getAsFloat() : 0.0F;
         }

      }
   }

   private static RawMesh.Quad buildQuad(Direction face, float x1, float y1, float z1, float x2, float y2, float z2, float[] uv, int faceRotation, float[] origin, float[] angles, boolean rotated, boolean fullBright) {
      float[] r = faceRight(face);
      float[] d = faceDown(face);
      float[] center = faceCenter(face, x1, y1, z1, x2, y2, z2);
      float halfW = faceWidth(face, x1, y1, z1, x2, y2, z2) * 0.5F;
      float halfH = faceHeight(face, x1, y1, z1, x2, y2, z2) * 0.5F;
      float[][] pos = new float[4][3];
      float[][] uvOut = new float[4][2];
      float[][] corners = new float[][]{{-1.0F, -1.0F}, {-1.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, -1.0F}};
      float[] uus = new float[]{uv[0], uv[0], uv[2], uv[2]};
      float[] vvs = new float[]{uv[1], uv[3], uv[3], uv[1]};
      int quarter = faceRotation & 3;
      float[] ru = new float[4];
      float[] rv = new float[4];

      for(int i = 0; i < 4; ++i) {
         ru[i] = uus[i + quarter & 3];
         rv[i] = vvs[i + quarter & 3];
      }

      for(int i = 0; i < 4; ++i) {
         float sx = corners[i][0];
         float sy = corners[i][1];
         pos[i][0] = center[0] + sx * halfW * r[0] + sy * halfH * d[0];
         pos[i][1] = center[1] + sx * halfW * r[1] + sy * halfH * d[1];
         pos[i][2] = center[2] + sx * halfW * r[2] + sy * halfH * d[2];
         if (rotated) {
            rotate(pos[i], origin, angles);
         }

         uvOut[i][0] = ru[i] * 0.0625F;
         uvOut[i][1] = rv[i] * 0.0625F;
      }

      float[] normal = new float[]{(float)face.getStepX(), (float)face.getStepY(), (float)face.getStepZ()};
      if (rotated) {
         rotate(normal, new float[3], angles);
      }

      float[] e1 = new float[]{pos[1][0] - pos[0][0], pos[1][1] - pos[0][1], pos[1][2] - pos[0][2]};
      float[] e2 = new float[]{pos[2][0] - pos[0][0], pos[2][1] - pos[0][1], pos[2][2] - pos[0][2]};
      double area = Math.sqrt(Math.pow((double)(e1[1] * e2[2] - e1[2] * e2[1]), 2.0D) + Math.pow((double)(e1[2] * e2[0] - e1[0] * e2[2]), 2.0D) + Math.pow((double)(e1[0] * e2[1] - e1[1] * e2[0]), 2.0D));
      return area < 1.0E-6D ? null : new RawMesh.Quad(pos, uvOut, normal, shadeOf(face), fullBright);
   }

   public static float shadeOf(Direction face) {
      float var10000;
      switch (face) {
         case DOWN:
            var10000 = 0.5F;
            break;
         case UP:
            var10000 = 1.0F;
            break;
         case NORTH:
         case SOUTH:
            var10000 = 0.8F;
            break;
         case WEST:
         case EAST:
            var10000 = 0.6F;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   static void rotate(float[] p, float[] origin, float[] angles) {
      float x = p[0] - origin[0];
      float y = p[1] - origin[1];
      float z = p[2] - origin[2];
      if (angles[0] != 0.0F) {
         double a = Math.toRadians((double)angles[0]);
         float c = (float)Math.cos(a);
         float s = (float)Math.sin(a);
         float ny = y * c - z * s;
         float nz = y * s + z * c;
         y = ny;
         z = nz;
      }

      if (angles[1] != 0.0F) {
         double a = Math.toRadians((double)angles[1]);
         float c = (float)Math.cos(a);
         float s = (float)Math.sin(a);
         float nx = x * c + z * s;
         float nz = -x * s + z * c;
         x = nx;
         z = nz;
      }

      if (angles[2] != 0.0F) {
         double a = Math.toRadians((double)angles[2]);
         float c = (float)Math.cos(a);
         float s = (float)Math.sin(a);
         float nx = x * c - y * s;
         float ny = x * s + y * c;
         x = nx;
         y = ny;
      }

      p[0] = x + origin[0];
      p[1] = y + origin[1];
      p[2] = z + origin[2];
   }

   private static float[] faceRight(Direction face) {
      float[] var10000;
      switch (face) {
         case DOWN:
            var10000 = new float[]{1.0F, 0.0F, 0.0F};
            break;
         case UP:
            var10000 = new float[]{1.0F, 0.0F, 0.0F};
            break;
         case NORTH:
            var10000 = new float[]{-1.0F, 0.0F, 0.0F};
            break;
         case SOUTH:
            var10000 = new float[]{1.0F, 0.0F, 0.0F};
            break;
         case WEST:
            var10000 = new float[]{0.0F, 0.0F, 1.0F};
            break;
         case EAST:
            var10000 = new float[]{0.0F, 0.0F, -1.0F};
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   private static float[] faceDown(Direction face) {
      float[] var10000;
      switch (face) {
         case DOWN:
            var10000 = new float[]{0.0F, 0.0F, -1.0F};
            break;
         case UP:
            var10000 = new float[]{0.0F, 0.0F, 1.0F};
            break;
         case NORTH:
         case SOUTH:
         case WEST:
         case EAST:
            var10000 = new float[]{0.0F, -1.0F, 0.0F};
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   private static float[] faceCenter(Direction face, float x1, float y1, float z1, float x2, float y2, float z2) {
      float mx = (x1 + x2) * 0.5F;
      float my = (y1 + y2) * 0.5F;
      float mz = (z1 + z2) * 0.5F;
      float[] var10000;
      switch (face) {
         case DOWN:
            var10000 = new float[]{mx, y1, mz};
            break;
         case UP:
            var10000 = new float[]{mx, y2, mz};
            break;
         case NORTH:
            var10000 = new float[]{mx, my, z1};
            break;
         case SOUTH:
            var10000 = new float[]{mx, my, z2};
            break;
         case WEST:
            var10000 = new float[]{x1, my, mz};
            break;
         case EAST:
            var10000 = new float[]{x2, my, mz};
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   private static float faceWidth(Direction face, float x1, float y1, float z1, float x2, float y2, float z2) {
      float var10000;
      switch (face) {
         case DOWN:
         case UP:
         case NORTH:
         case SOUTH:
            var10000 = x2 - x1;
            break;
         case WEST:
         case EAST:
            var10000 = z2 - z1;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   private static float faceHeight(Direction face, float x1, float y1, float z1, float x2, float y2, float z2) {
      float var10000;
      switch (face) {
         case DOWN:
         case UP:
            var10000 = z2 - z1;
            break;
         case NORTH:
         case SOUTH:
         case WEST:
         case EAST:
            var10000 = y2 - y1;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   private static float[] readTriple(JsonArray array) {
      return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
   }

   private static float[] readQuad(JsonArray array) {
      return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(), array.get(3).getAsFloat()};
   }
}
