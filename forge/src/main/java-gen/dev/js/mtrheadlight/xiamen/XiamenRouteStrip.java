package dev.js.mtrheadlight.xiamen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.decor.DecorConfig;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mtr.client.ClientCache;
import mtr.client.ClientData;
import mtr.data.Route;
import mtr.data.Station;
import mtr.data.Route.CircularState;

public final class XiamenRouteStrip {
   private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
   private static final Set<String> INTERCHANGE_LOGGED = ConcurrentHashMap.newKeySet();
   private static final Set<String> DIRECTION_LOGGED = ConcurrentHashMap.newKeySet();

   private XiamenRouteStrip() {
   }

   public static NativeImage paint(long platformId, boolean vertical, boolean flip, float aspectRatio) {
      try {
         if (!XiamenRouteScope.active()) {
            return null;
         } else {
            DecorConfig cfg = DecorConfig.get();
            if (cfg != null && cfg.xiamenRouteStrip && !vertical && aspectRatio > 0.05F) {
               List<ClientCache.PlatformRouteDetails> all = ClientData.DATA_CACHE.requestPlatformIdToRoutes(platformId);
               ClientCache.PlatformRouteDetails details = primaryDetails(all);
               if (details == null) {
                  return null;
               } else {
                  List<ClientCache.PlatformRouteDetails.StationDetails> stations = details.stationDetails;
                  int count = stations == null ? 0 : stations.size();
                  if (count < 2) {
                     return null;
                  } else {
                     Route route = XiamenRouteStyle.findRoute(platformId, details.routeName);
                     int lineRgb = XiamenRouteStyle.lineColor(cfg, route, details.routeName);
                     XiamenStripPainter.Model model = new XiamenStripPainter.Model();
                     model.names = new String[count];
                     model.namesEn = new String[count];

                     for(int i = 0; i < count; ++i) {
                        ClientCache.PlatformRouteDetails.StationDetails station = (ClientCache.PlatformRouteDetails.StationDetails)stations.get(i);
                        String raw = station == null ? "" : station.stationName;
                        model.names[i] = XiamenStripPainter.cjkPart(raw);
                        model.namesEn[i] = XiamenStripPainter.enPart(raw);
                     }

                     model.currentIndex = details.currentStationIndex;
                     model.lineRgb = lineRgb;
                     model.lineNumber = XiamenRouteStyle.lineNumber(route, details.routeName);
                     int direction = XiamenRouteScope.arrowDirection();
                     model.direction = direction != 0 ? direction : (flip ? 2 : 0);
                     model.namesBelow = cfg.routeStripNamesBelow;
                     model.aspectRatio = aspectRatio;
                     model.interchanges = interchanges(cfg, all, stations, details.routeName, route);
                     BufferedImage image = XiamenStripPainter.paint(model);
                     if (image == null) {
                        return null;
                     } else {
                        logOnce(platformId, details.routeName, route, lineRgb, count, image, model.direction);
                        return toNativeImage(image);
                     }
                  }
               }
            } else {
               return null;
            }
         }
      } catch (Throwable var16) {
         LteLog.LOGGER.warn("[lte] route strip: fell back to MTR's route map", var16);
         return null;
      }
   }

   private static ClientCache.PlatformRouteDetails primaryDetails(List<ClientCache.PlatformRouteDetails> all) {
      if (all != null && !all.isEmpty()) {
         for(ClientCache.PlatformRouteDetails details : all) {
            if (details != null && details.circularState == CircularState.NONE && details.stationDetails != null && details.stationDetails.size() >= 2) {
               return details;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static XiamenStripPainter.Interchange[] interchanges(DecorConfig cfg, List<ClientCache.PlatformRouteDetails> allDetails, List<ClientCache.PlatformRouteDetails.StationDetails> stations, String routeName, Route route) {
      Set<Integer> ownColors = new HashSet();
      Set<String> ownNames = new HashSet();
      if (allDetails != null) {
         for(ClientCache.PlatformRouteDetails details : allDetails) {
            if (details != null) {
               ownColors.add(details.routeColor & 16777215);
               ownNames.add(XiamenRouteStyle.shortName(details.routeName));
            }
         }
      }

      ownColors.add(XiamenRouteStyle.lineColor(cfg, route, routeName) & 16777215);
      ownNames.add(XiamenRouteStyle.shortName(routeName));
      List<XiamenStripPainter.Interchange> found = new ArrayList();

      for(int i = 0; i < stations.size(); ++i) {
         ClientCache.PlatformRouteDetails.StationDetails station = (ClientCache.PlatformRouteDetails.StationDetails)stations.get(i);
         List<ClientCache.ColorNameTuple> tuples = stationInterchangeTuples(route, i, station == null ? null : station.stationName, ownColors, ownNames);
         if (tuples.isEmpty() && station != null) {
            tuples = foreignTuples(station.interchangeRoutes, ownColors, ownNames);
         }

         for(ClientCache.ColorNameTuple tuple : tuples) {
            Route swapRoute = XiamenRouteStyle.findRouteByName(tuple.name);
            Integer override = XiamenRouteStyle.overrideColor(cfg, swapRoute, tuple.name);
            int rgb;
            if (override != null) {
               rgb = override;
            } else if ((tuple.color & 16777215) != 0) {
               rgb = tuple.color & 16777215;
            } else {
               rgb = XiamenRouteStyle.lineColor(cfg, swapRoute, tuple.name);
            }

            String cjk = XiamenStripPainter.cjkPart(tuple.name);
            String number = XiamenRouteStyle.lineNumber(swapRoute, tuple.name);
            found.add(new XiamenStripPainter.Interchange(i, rgb, number, cjk));
            logInterchange(station == null ? "" : XiamenStripPainter.cjkPart(station.stationName), cjk.isEmpty() ? tuple.name : cjk, number, rgb);
         }
      }

      return found.isEmpty() ? null : (XiamenStripPainter.Interchange[])found.toArray(new XiamenStripPainter.Interchange[0]);
   }

   private static List<ClientCache.ColorNameTuple> stationInterchangeTuples(Route route, int index, String expectedStationName, Set<Integer> ownColors, Set<String> ownNames) {
      if (route != null && route.platformIds != null && index >= 0 && index < route.platformIds.size()) {
         long platformId = ((Route.RoutePlatform)route.platformIds.get(index)).platformId;
         Station station = (Station)ClientData.DATA_CACHE.platformIdToStation.get(platformId);
         if (station == null) {
            return Collections.emptyList();
         } else if (expectedStationName != null && !expectedStationName.isEmpty() && !XiamenStripPainter.cjkPart(expectedStationName).equals(XiamenStripPainter.cjkPart(station.name))) {
            return Collections.emptyList();
         } else {
            Map<Integer, ClientCache.ColorNameTuple> all = (Map)ClientData.DATA_CACHE.stationIdToRoutes.get(station.id);
            return all == null ? Collections.emptyList() : foreignTuples(all.values(), ownColors, ownNames);
         }
      } else {
         return Collections.emptyList();
      }
   }

   private static List<ClientCache.ColorNameTuple> foreignTuples(Collection<ClientCache.ColorNameTuple> tuples, Set<Integer> ownColors, Set<String> ownNames) {
      if (tuples != null && !tuples.isEmpty()) {
         List<ClientCache.ColorNameTuple> found = new ArrayList();

         for(ClientCache.ColorNameTuple tuple : tuples) {
            if (tuple != null && tuple.name != null && !tuple.name.isEmpty() && !ownColors.contains(tuple.color & 16777215) && !ownNames.contains(XiamenRouteStyle.shortName(tuple.name))) {
               boolean dup = false;

               for(ClientCache.ColorNameTuple kept : found) {
                  if ((kept.color & 16777215) == (tuple.color & 16777215) && XiamenRouteStyle.shortName(kept.name).equals(XiamenRouteStyle.shortName(tuple.name))) {
                     dup = true;
                     break;
                  }
               }

               if (!dup) {
                  found.add(tuple);
               }
            }
         }

         found.sort((a, b) -> Integer.compare(a.color & 16777215, b.color & 16777215));
         return found;
      } else {
         return Collections.emptyList();
      }
   }

   private static void logInterchange(String stationName, String lineName, String number, int rgb) {
      String key = stationName + "|" + lineName + "|" + Integer.toHexString(rgb & 16777215);
      if (INTERCHANGE_LOGGED.size() <= 256 && INTERCHANGE_LOGGED.add(key)) {
         LteLog.LOGGER.info("[lte] strip: interchange station={} -> line={} color=#{}", new Object[]{stationName, lineName, String.format("%06X", rgb & 16777215)});
      }
   }

   public static NativeImage colorStrip(long platformId) {
      try {
         if (!XiamenRouteScope.active()) {
            return null;
         } else {
            DecorConfig cfg = DecorConfig.get();
            if (cfg != null && cfg.lineColors != null && !cfg.lineColors.isEmpty()) {
               List<ClientCache.PlatformRouteDetails> all = ClientData.DATA_CACHE.requestPlatformIdToRoutes(platformId);
               if (all != null && !all.isEmpty()) {
                  boolean overridden = false;

                  for(ClientCache.PlatformRouteDetails details : all) {
                     if (details != null) {
                        Route route = XiamenRouteStyle.findRoute(platformId, details.routeName);
                        if (XiamenRouteStyle.overrideColor(cfg, route, details.routeName) != null) {
                           overridden = true;
                           break;
                        }
                     }
                  }

                  if (!overridden) {
                     return null;
                  } else {
                     NativeImage image = new NativeImage(Format.RGBA, 1, all.size(), false);
                     int row = 0;

                     for(ClientCache.PlatformRouteDetails details : all) {
                        if (details != null) {
                           Route route = XiamenRouteStyle.findRoute(platformId, details.routeName);
                           image.setPixelRGBA(0, row++, abgr(-16777216 | XiamenRouteStyle.lineColor(cfg, route, details.routeName)));
                        }
                     }

                     return image;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         }
      } catch (Throwable var10) {
         return null;
      }
   }

   public static NativeImage directionArrow(long platformId, boolean hasLeft, boolean hasRight, boolean showToString, float aspectRatio) {
      try {
         if (!XiamenRouteScope.active()) {
            return null;
         } else {
            DecorConfig cfg = DecorConfig.get();
            if (cfg != null && cfg.xiamenRouteStrip && aspectRatio > 0.05F) {
               List<ClientCache.PlatformRouteDetails> all = ClientData.DATA_CACHE.requestPlatformIdToRoutes(platformId);
               if (all != null && !all.isEmpty()) {
                  StringBuilder cjk = new StringBuilder();
                  StringBuilder en = new StringBuilder();

                  for(ClientCache.PlatformRouteDetails details : all) {
                     if (details != null && details.stationDetails != null && !details.stationDetails.isEmpty()) {
                        int index = Math.max(0, Math.min(details.stationDetails.size() - 1, details.currentStationIndex));
                        ClientCache.PlatformRouteDetails.StationDetails station = (ClientCache.PlatformRouteDetails.StationDetails)details.stationDetails.get(index);
                        String rawName = station == null ? "" : station.stationName;
                        if (rawName != null && !rawName.trim().isEmpty()) {
                           String cjkName = XiamenStripPainter.cjkPart(rawName);
                           String enName = XiamenStripPainter.enPart(rawName);
                           if (!cjkName.isEmpty() && cjk.indexOf(cjkName) < 0) {
                              if (cjk.length() > 0) {
                                 cjk.append('\uff0f');
                              }

                              cjk.append(cjkName);
                           }

                           if (!enName.isEmpty() && en.indexOf(enName) < 0) {
                              if (en.length() > 0) {
                                 en.append(" / ");
                              }

                              en.append(enName);
                           }
                        }
                     }
                  }

                  if (cjk.length() == 0 && en.length() == 0) {
                     return null;
                  } else {
                     BufferedImage image = XiamenDirectionPanel.paint(cjk.toString(), en.toString(), aspectRatio, hasLeft, hasRight);
                     if (image == null) {
                        return null;
                     } else {
                        logDirection(platformId, cjk.toString(), en.toString(), hasLeft, hasRight, image);
                        return toNativeImage(image);
                     }
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         }
      } catch (Throwable var17) {
         LteLog.LOGGER.warn("[lte] direction panel: fell back to MTR's direction arrow", var17);
         return null;
      }
   }

   private static void logDirection(long platformId, String cjk, String en, boolean hasLeft, boolean hasRight, BufferedImage image) {
      String key = "dir|" + platformId + "|" + hasLeft + hasRight;
      if (DIRECTION_LOGGED.size() <= 256 && DIRECTION_LOGGED.add(key)) {
         LteLog.LOGGER.info("[lte] direction panel: platform={} text=\"{}\" en=\"{}\" arrows={}/{} tex={}x{} cjk={}px font={}", new Object[]{platformId, cjk, en, hasLeft ? "L" : "-", hasRight ? "R" : "-", image.getWidth(), image.getHeight(), XiamenDirectionPanel.lastCjkSize, XiamenStripPainter.lastFontName});
      }
   }

   private static int abgr(int argb) {
      return argb & -16711936 | (argb & 16711680) >>> 16 | (argb & 255) << 16;
   }

   private static NativeImage toNativeImage(BufferedImage image) {
      int w = image.getWidth();
      int h = image.getHeight();
      int[] argb = image.getRGB(0, 0, w, h, (int[])null, 0, w);
      NativeImage nativeImage = new NativeImage(Format.RGBA, w, h, false);

      for(int y = 0; y < h; ++y) {
         int row = y * w;

         for(int x = 0; x < w; ++x) {
            int color = argb[row + x];
            int a = color >>> 24 & 255;
            int quantized = a < 64 ? 0 : -16777216 | color & 16777215;
            nativeImage.setPixelRGBA(x, y, abgr(quantized));
         }
      }

      return nativeImage;
   }

   private static void logOnce(long platformId, String routeName, Route route, int lineRgb, int stations, BufferedImage image, int direction) {
      String key = platformId + "|" + routeName + "|" + direction;
      if (LOGGED.size() <= 256 && LOGGED.add(key)) {
         LteLog.LOGGER.info("[lte] route strip: platform={} route={} id={} color=#{} stations={} dir={} tex={}x{} band={} line={} dot={} current={} termRing={} termArrow={} swap={} ring={} plate={} badges={} dropped={} scale={} font={} nameSize={}", new Object[]{platformId, XiamenRouteStyle.shortName(routeName), route == null ? "?" : route.id, String.format("%06X", lineRgb & 16777215), stations, direction, image.getWidth(), image.getHeight(), XiamenStripPainter.lastBandH, XiamenStripPainter.lastLineW, XiamenStripPainter.lastCircle, XiamenStripPainter.lastCurrent, XiamenStripPainter.lastTerminalRing, XiamenStripPainter.lastTerminalArrow, XiamenStripPainter.lastInterchangeCount, XiamenStripPainter.lastSwapHead, XiamenStripPainter.lastSwapPlate, XiamenStripPainter.lastBadgeCount, XiamenStripPainter.lastBadgeDropped, String.format(Locale.ROOT, "%.2f", XiamenStripPainter.lastBadgeScale), XiamenStripPainter.lastFontName, XiamenStripPainter.lastNameSize});
      }
   }
}
