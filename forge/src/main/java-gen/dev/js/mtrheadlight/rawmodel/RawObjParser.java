package dev.js.mtrheadlight.rawmodel;

import dev.js.mtrheadlight.LteLog;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

public final class RawObjParser {
   public static final String DIRECTORY = "eyecandies/";
   private static final float UNITS_PER_BLOCK = 16.0F;
   private static final float HALF_BLOCK = 8.0F;
   private static final Map<String, RawMesh> CACHE = new ConcurrentHashMap();
   private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
   private static final Map<ResourceLocation, Map<String, RawMesh>> MATERIAL_CACHE = new ConcurrentHashMap();
   public static final int FOOTPRINT_SIZE = 16;
   private static final double FOOTPRINT_SAMPLE = 0.4D;

   private RawObjParser() {
   }

   public static ResourceLocation id(String name) {
      return new ResourceLocation("mtrlte", "eyecandies/" + name + ".obj");
   }

   public static RawMesh get(ResourceLocation id, ResourceLocation texture, boolean flipV, float[] scale, float[] rotation, float[] translation, boolean[] mirror) {
      String key = cacheKey(id, texture, flipV, scale, rotation, translation, mirror);
      RawMesh cached = (RawMesh)CACHE.get(key);
      if (cached != null) {
         return cached;
      } else if (FAILED.contains(key)) {
         return null;
      } else {
         String path = "/assets/" + id.getNamespace() + "/" + id.getPath();

         try {
            InputStream in = RawObjParser.class.getResourceAsStream(path);

            RawMesh mesh;
            label59: {
               RawMesh var12;
               try {
                  if (in == null) {
                     FAILED.add(key);
                     LteLog.LOGGER.warn("[lte] obj model: {} is missing from the classpath", path);
                     mesh = null;
                     break label59;
                  }

                  mesh = parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), texture, flipV, scale, rotation, translation, mirror);
                  CACHE.put(key, mesh);
                  LteLog.LOGGER.info("[lte] obj model: loaded {} ({} quads, texture {})", new Object[]{id, mesh.quadCount(), mesh.texture()});
                  var12 = mesh;
               } catch (Throwable var14) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (in != null) {
                  in.close();
               }

               return var12;
            }

            if (in != null) {
               in.close();
            }

            return mesh;
         } catch (Throwable var15) {
            FAILED.add(key);
            LteLog.LOGGER.warn("[lte] obj model: could not load {} ({})", path, var15.toString());
            return null;
         }
      }
   }

   private static String cacheKey(ResourceLocation id, ResourceLocation texture, boolean flipV, float[] scale, float[] rotation, float[] translation, boolean[] mirror) {
      return String.valueOf(id) + "|" + String.valueOf(texture) + "|" + flipV + "|" + Arrays.toString(scale) + "|" + Arrays.toString(rotation) + "|" + Arrays.toString(translation) + "|" + Arrays.toString(mirror);
   }

   public static RawMesh parse(String text, ResourceLocation texture, boolean flipV, float[] scale, float[] rotation, float[] translation, boolean[] mirror) {
      RawObjParser.Geometry geometry = read(text, true, flipV);
      float[][] verts = place(geometry.positions, scale, rotation, translation, mirror);
      List<RawMesh.Quad> quads = new ArrayList(geometry.faces.size());

      for(RawObjParser.Face face : geometry.faces) {
         emit(face, verts, geometry.texCoords, quads);
      }

      return new RawMesh(texture, (RawMesh.Quad[])quads.toArray(new RawMesh.Quad[0]));
   }

   public static Map<String, RawMesh> materials(ResourceLocation objId, boolean flipV, float[] scale, float[] rotation, float[] translation, boolean[] mirror) {
      Map<String, RawMesh> cached = (Map)MATERIAL_CACHE.get(objId);
      if (cached != null) {
         return cached;
      } else {
         Map<String, RawMesh> result = new LinkedHashMap();
         String path = "/assets/" + objId.getNamespace() + "/" + objId.getPath();

         try {
            InputStream in = RawObjParser.class.getResourceAsStream(path);

            Object var22;
            label90: {
               try {
                  if (in == null) {
                     LteLog.LOGGER.warn("[lte] obj materials: {} is missing from the classpath", path);
                     var22 = result;
                     break label90;
                  }

                  String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                  String dir = directoryOf(objId.getPath());
                  Map<String, String> maps = mtlTextures(text, objId.getNamespace(), dir);
                  RawObjParser.Geometry geometry = read(text, true, flipV);
                  float[][] verts = place(geometry.positions, scale, rotation, translation, mirror);
                  Map<String, List<RawMesh.Quad>> byMaterial = new LinkedHashMap();

                  for(RawObjParser.Face face : geometry.faces) {
                     emit(face, verts, geometry.texCoords, (List)byMaterial.computeIfAbsent(face.material() == null ? "" : face.material(), (key) -> new ArrayList()));
                  }

                  for(Map.Entry<String, List<RawMesh.Quad>> entry : byMaterial.entrySet()) {
                     String png = (String)maps.get(entry.getKey());
                     if (png == null) {
                        LteLog.LOGGER.warn("[lte] obj materials: {}/{} has no map_Kd in the mtl, skipped", objId, entry.getKey());
                     } else {
                        result.put((String)entry.getKey(), new RawMesh(new ResourceLocation(objId.getNamespace(), "textures/" + dir + png), (RawMesh.Quad[])((List)entry.getValue()).toArray(new RawMesh.Quad[0])));
                     }
                  }

                  MATERIAL_CACHE.put(objId, result);
                  LteLog.LOGGER.info("[lte] obj materials: loaded {} ({} materials, {} quads)", new Object[]{objId, result.size(), result.values().stream().mapToInt(RawMesh::quadCount).sum()});
               } catch (Throwable var20) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var19) {
                        var20.addSuppressed(var19);
                     }
                  }

                  throw var20;
               }

               if (in != null) {
                  in.close();
               }

               return result;
            }

            if (in != null) {
               in.close();
            }

            return (Map<String, RawMesh>)var22;
         } catch (Throwable var21) {
            LteLog.LOGGER.warn("[lte] obj materials: could not load {} ({})", path, var21.toString());
            return result;
         }
      }
   }

   public static List<RawObjParser.GroupRun> groupRuns(RawMesh mesh) {
      List<RawObjParser.GroupRun> runs = new ArrayList();
      if (mesh != null && mesh.quads().length != 0) {
         String name = null;
         int paragraph = Integer.MIN_VALUE;
         List<RawMesh.Quad> quads = new ArrayList();

         for(RawMesh.Quad quad : mesh.quads()) {
            String group = quad.group() == null ? "" : quad.group();
            if (name == null) {
               name = group;
               paragraph = quad.paragraph();
            } else if (!name.equals(group) || paragraph != quad.paragraph()) {
               runs.add(new RawObjParser.GroupRun(name, (RawMesh.Quad[])quads.toArray(new RawMesh.Quad[0])));
               quads.clear();
               name = group;
               paragraph = quad.paragraph();
            }

            quads.add(quad);
         }

         runs.add(new RawObjParser.GroupRun(name, (RawMesh.Quad[])quads.toArray(new RawMesh.Quad[0])));
         return runs;
      } else {
         return runs;
      }
   }

   public static RawMesh select(RawMesh mesh, Predicate<String> keep) {
      if (mesh == null) {
         return null;
      } else {
         List<RawMesh.Quad> kept = new ArrayList();

         for(RawMesh.Quad quad : mesh.quads()) {
            if (keep.test(quad.group() == null ? "" : quad.group())) {
               kept.add(quad);
            }
         }

         return new RawMesh(mesh.texture(), (RawMesh.Quad[])kept.toArray(new RawMesh.Quad[0]));
      }
   }

   static String directoryOf(String path) {
      int slash = path.lastIndexOf(47);
      return slash < 0 ? "" : path.substring(0, slash + 1);
   }

   static Map<String, String> mtlTextures(String objText, String namespace, String dir) {
      Map<String, String> maps = new LinkedHashMap();
      String mtlName = null;

      for(String rawLine : objText.split("\n")) {
         String line = rawLine.strip();
         if (line.startsWith("mtllib ")) {
            mtlName = line.substring("mtllib ".length()).strip();
            break;
         }
      }

      if (mtlName != null && !mtlName.isEmpty()) {
         String path = "/assets/" + namespace + "/" + dir + mtlName;

         try {
            InputStream in = RawObjParser.class.getResourceAsStream(path);

            Object var19;
            label94: {
               try {
                  if (in == null) {
                     LteLog.LOGGER.warn("[lte] obj materials: mtl {} is missing from the classpath", path);
                     var19 = maps;
                     break label94;
                  }

                  String material = null;

                  for(String rawLine : (new String(in.readAllBytes(), StandardCharsets.UTF_8)).split("\n")) {
                     String line = rawLine.strip();
                     if (line.startsWith("newmtl ")) {
                        material = line.substring("newmtl ".length()).strip();
                     } else if (line.startsWith("map_Kd ") && material != null) {
                        maps.put(material, line.substring("map_Kd ".length()).strip());
                     }
                  }
               } catch (Throwable var14) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (in != null) {
                  in.close();
               }

               return maps;
            }

            if (in != null) {
               in.close();
            }

            return (Map<String, String>)var19;
         } catch (Throwable var15) {
            LteLog.LOGGER.warn("[lte] obj materials: could not read {} ({})", path, var15.toString());
            return maps;
         }
      } else {
         return maps;
      }
   }

   public static double[] bounds(String text, float[] scale, float[] rotation, float[] translation, boolean[] mirror) {
      RawObjParser.Geometry geometry = read(text, false, false);
      float[][] verts = place(geometry.positions, scale, rotation, translation, mirror);
      if (verts.length == 0) {
         return new double[6];
      } else {
         double[] box = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};

         for(float[] v : verts) {
            for(int axis = 0; axis < 3; ++axis) {
               box[axis] = Math.min(box[axis], (double)v[axis]);
               box[axis + 3] = Math.max(box[axis + 3], (double)v[axis]);
            }
         }

         return box;
      }
   }

   public static float[][] place(List<float[]> positions, float[] scale, float[] rotation, float[] translation, boolean[] mirror) {
      float[] s = scale == null ? new float[]{1.0F, 1.0F, 1.0F} : scale;
      float[] a = rotation == null ? new float[]{0.0F, 0.0F, 0.0F} : rotation;
      float[] t = translation == null ? new float[]{0.0F, 0.0F, 0.0F} : translation;
      boolean[] m = mirror == null ? new boolean[]{false, false, false} : mirror;
      boolean rotated = a[0] != 0.0F || a[1] != 0.0F || a[2] != 0.0F;
      float[][] out = new float[positions.size()][];

      for(int i = 0; i < out.length; ++i) {
         float[] v = (float[])positions.get(i);
         float[] p = new float[]{v[0] * s[0] * (m[0] ? -1.0F : 1.0F), v[1] * s[1] * (m[1] ? -1.0F : 1.0F), v[2] * s[2] * (m[2] ? -1.0F : 1.0F)};
         if (rotated) {
            rotate(p, a);
         }

         p[0] += t[0];
         p[1] += t[1];
         p[2] += t[2];
         out[i] = p;
      }

      for(float[] p : out) {
         p[0] = p[0] * 16.0F + 8.0F;
         p[1] *= 16.0F;
         p[2] = p[2] * 16.0F + 8.0F;
      }

      return out;
   }

   public static int[] footprint(String text, float[] scale, float[] rotation, float[] translation, boolean[] mirror) {
      RawObjParser.Geometry geometry = read(text, true, false);
      float[][] verts = place(geometry.positions, scale, rotation, translation, mirror);
      int[] grid = new int[256];

      for(RawObjParser.Face face : geometry.faces) {
         int n = face.vertex.length;

         for(int i = 1; i + 1 < n; ++i) {
            sample(verts[face.vertex[0]], verts[face.vertex[i]], verts[face.vertex[i + 1]], grid);
         }
      }

      return grid;
   }

   private static void sample(float[] p0, float[] p1, float[] p2, int[] grid) {
      double edge = Math.max(Math.max(span(p0, p1), span(p1, p2)), span(p0, p2));
      int steps = (int)Math.max(3.0D, Math.min(48.0D, Math.ceil(edge / 0.4D)));

      for(int a = 0; a <= steps; ++a) {
         for(int b = 0; a + b <= steps; ++b) {
            double wa = (double)a / (double)steps;
            double wb = (double)b / (double)steps;
            double wc = 1.0D - wa - wb;
            double x = (double)p0[0] * wa + (double)p1[0] * wb + (double)p2[0] * wc;
            double z = (double)p0[2] * wa + (double)p1[2] * wb + (double)p2[2] * wc;
            if (!(x < 0.0D) && !(z < 0.0D) && !(x >= 16.0D) && !(z >= 16.0D)) {
               grid[(int)z * 16 + (int)x] = 1;
            }
         }
      }

   }

   private static double span(float[] a, float[] b) {
      double dx = (double)(a[0] - b[0]);
      double dz = (double)(a[2] - b[2]);
      return Math.sqrt(dx * dx + dz * dz);
   }

   static void rotate(float[] p, float[] angles) {
      float x = p[0];
      float y = p[1];
      float z = p[2];
      if (angles[0] != 0.0F) {
         double r = Math.toRadians((double)angles[0]);
         float c = (float)Math.cos(r);
         float s = (float)Math.sin(r);
         float ny = y * c - z * s;
         float nz = y * s + z * c;
         y = ny;
         z = nz;
      }

      if (angles[1] != 0.0F) {
         double r = Math.toRadians((double)angles[1]);
         float c = (float)Math.cos(r);
         float s = (float)Math.sin(r);
         float nx = x * c + z * s;
         float nz = -x * s + z * c;
         x = nx;
         z = nz;
      }

      if (angles[2] != 0.0F) {
         double r = Math.toRadians((double)angles[2]);
         float c = (float)Math.cos(r);
         float s = (float)Math.sin(r);
         float nx = x * c - y * s;
         float ny = x * s + y * c;
         x = nx;
         y = ny;
      }

      p[0] = x;
      p[1] = y;
      p[2] = z;
   }

   private static void emit(RawObjParser.Face face, float[][] positions, List<float[]> texCoords, List<RawMesh.Quad> out) {
      int n = face.vertex.length;
      if (n >= 3) {
         if (n == 4) {
            emitCorners(face, positions, texCoords, out, 0, 1, 2, 3, 3);
         } else {
            for(int i = 1; i + 1 < n; ++i) {
               emitCorners(face, positions, texCoords, out, 0, i, i + 1, i + 1, i + 1);
            }

         }
      }
   }

   private static void emitCorners(RawObjParser.Face face, float[][] positions, List<float[]> texCoords, List<RawMesh.Quad> out, int a, int b, int c, int d, int uvD) {
      float[][] pos = new float[][]{positions[face.vertex[a]], positions[face.vertex[b]], positions[face.vertex[c]], positions[face.vertex[d]]};
      float[][] uv = new float[][]{(float[])texCoords.get(face.texCoord[a]), (float[])texCoords.get(face.texCoord[b]), (float[])texCoords.get(face.texCoord[c]), (float[])texCoords.get(face.texCoord[uvD])};
      float[] normal = normalOf(pos[0], pos[1], pos[2]);
      if (normal != null) {
         out.add(new RawMesh.Quad(pos, uv, normal, shadeOf(normal), false, face.group(), face.paragraph()));
      }
   }

   private static float[] normalOf(float[] p0, float[] p1, float[] p2) {
      double ux = (double)(p1[0] - p0[0]);
      double uy = (double)(p1[1] - p0[1]);
      double uz = (double)(p1[2] - p0[2]);
      double vx = (double)(p2[0] - p0[0]);
      double vy = (double)(p2[1] - p0[1]);
      double vz = (double)(p2[2] - p0[2]);
      double nx = uy * vz - uz * vy;
      double ny = uz * vx - ux * vz;
      double nz = ux * vy - uy * vx;
      double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
      return len < 1.0E-9D ? null : new float[]{(float)(nx / len), (float)(ny / len), (float)(nz / len)};
   }

   private static float shadeOf(float[] normal) {
      float ax = Math.abs(normal[0]);
      float ay = Math.abs(normal[1]);
      float az = Math.abs(normal[2]);
      if (ay >= ax && ay >= az) {
         return normal[1] >= 0.0F ? 1.0F : 0.5F;
      } else {
         return ax >= az ? 0.6F : 0.8F;
      }
   }

   private static RawObjParser.Geometry read(String text, boolean withFaces, boolean flipV) {
      RawObjParser.Geometry g = new RawObjParser.Geometry();
      String group = "";
      int paragraph = 0;
      String material = "";

      for(String rawLine : text.split("\n")) {
         String line = rawLine.strip();
         if (!line.isEmpty() && line.charAt(0) != '#') {
            int space = indexOfWhitespace(line);
            String keyword = space < 0 ? line : line.substring(0, space);
            if (space >= 0) {
               String rest = line.substring(space + 1).strip();
               switch (keyword) {
                  case "v":
                     g.positions.add(parseTriple(rest));
                     break;
                  case "vt":
                     float[] uv = parsePair(rest);
                     g.texCoords.add(new float[]{wrap(uv[0]), wrap(flipV ? 1.0F - uv[1] : uv[1])});
                     break;
                  case "o":
                  case "g":
                     group = rest;
                     ++paragraph;
                     break;
                  case "usemtl":
                     material = rest;
                     break;
                  case "f":
                     if (withFaces) {
                        RawObjParser.Face face = parseFace(rest, g, group, paragraph, material);
                        if (face != null) {
                           g.faces.add(face);
                        }
                     }
               }
            }
         }
      }

      return g;
   }

   private static int indexOfWhitespace(String s) {
      for(int i = 0; i < s.length(); ++i) {
         if (Character.isWhitespace(s.charAt(i))) {
            return i;
         }
      }

      return -1;
   }

   private static float[] parseTriple(String rest) {
      String[] parts = rest.split("\\s+");
      return new float[]{Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), parts.length > 2 ? Float.parseFloat(parts[2]) : 0.0F};
   }

   private static float[] parsePair(String rest) {
      String[] parts = rest.split("\\s+");
      return new float[]{Float.parseFloat(parts[0]), parts.length > 1 ? Float.parseFloat(parts[1]) : 0.0F};
   }

   private static float wrap(float value) {
      float wrapped = value - (float)Math.floor((double)value);
      return wrapped >= 1.0F ? 0.0F : wrapped;
   }

   private static RawObjParser.Face parseFace(String rest, RawObjParser.Geometry g, String group, int paragraph, String material) {
      String[] corners = rest.split("\\s+");
      List<Integer> vi = new ArrayList(corners.length);
      List<Integer> ti = new ArrayList(corners.length);

      for(String corner : corners) {
         if (!corner.isEmpty()) {
            String[] parts = corner.split("/", -1);
            int vertex = resolve(parts[0], g.positions.size());
            if (vertex < 0 || vertex >= g.positions.size()) {
               return null;
            }

            vi.add(vertex);
            int texCoord = g.texCoords.isEmpty() ? -1 : 0;
            if (parts.length > 1 && !parts[1].isEmpty()) {
               int resolved = resolve(parts[1], g.texCoords.size());
               if (resolved >= 0 && resolved < g.texCoords.size()) {
                  texCoord = resolved;
               }
            }

            ti.add(texCoord);
         }
      }

      return vi.size() < 3 ? null : new RawObjParser.Face(toArray(vi), toArray(ti), group, paragraph, material);
   }

   private static int resolve(String token, int count) {
      try {
         int index = Integer.parseInt(token);
         return index < 0 ? count + index : index - 1;
      } catch (NumberFormatException var3) {
         return -1;
      }
   }

   private static int[] toArray(List<Integer> values) {
      int[] out = new int[values.size()];

      for(int i = 0; i < out.length; ++i) {
         out[i] = values.get(i);
      }

      return out;
   }

   private static record Face(int[] vertex, int[] texCoord, String group, int paragraph, String material) {
   }

   private static final class Geometry {
      final List<float[]> positions = new ArrayList();
      final List<float[]> texCoords = new ArrayList();
      final List<RawObjParser.Face> faces = new ArrayList();
   }

   public static record GroupRun(String name, RawMesh.Quad[] quads) {
   }
}
