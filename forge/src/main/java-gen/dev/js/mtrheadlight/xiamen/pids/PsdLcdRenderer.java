package dev.js.mtrheadlight.xiamen.pids;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.client.RenderDistance;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mtr.client.ClientData;
import mtr.data.RailwayData;
import mtr.data.Route;
import mtr.data.ScheduleEntry;
import mtr.data.Station;
import mtr.data.TrainClient;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.render.MoreRenderLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class PsdLcdRenderer extends BlockEntityRendererMapper<PsdLcdBlockEntity> {
   private static final int AD_ROTATE_TICKS = 200;
   private static final float TICKER_LIFT_PX = 0.032F;
   private static final Map<String, PsdLcdRenderer.AdRotation> ROTATIONS = new HashMap();
   private static final ResourceLocation SOLID_TEXTURE = new ResourceLocation("mtrlte", "textures/block/white.png");
   private static final RenderType SOLID_LAYER = RenderType.entitySolid(SOLID_TEXTURE);
   private boolean warned;
   private static boolean carsWarned;
   private long lastSweep;

   public PsdLcdRenderer(BlockEntityRendererProvider.Context context) {
      super(context.getBlockEntityRenderDispatcher());
   }

   public boolean isInRenderDistance(PsdLcdBlockEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public boolean rendersOutsideBoundingBox(PsdLcdBlockEntity entity) {
      return true;
   }

   public int getViewDistance() {
      return 64;
   }

   public void render(PsdLcdBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      try {
         this.renderSafe(entity, matrices, vertexConsumers);
      } catch (Throwable var8) {
         if (!this.warned) {
            this.warned = true;
            LteLog.LOGGER.error("[lte] psd lcd render failed", var8);
         }
      }

   }

   private void renderSafe(PsdLcdBlockEntity entity, PoseStack matrices, MultiBufferSource vertexConsumers) {
      Level world = entity.getLevel();
      if (world != null) {
         this.sweep(world);
         BlockPos pos = entity.getBlockPos();
         BlockState state = world.getBlockState(pos);
         if (state.getBlock() instanceof PsdLcdBlock) {
            BlockPos anchor = PsdLcdBlock.anchorOf(world, pos);
            if (anchor.equals(pos)) {
               Direction facing = (Direction)state.getValue(PsdLcdBlock.FACING);
               PsdLcdLayout.Face face = faceOf(facing);
               List<BlockPos> chain = PsdLcdBlock.run(world, anchor);
               boolean[] cuts = cutsOf(world, chain, state);
               float extent = (float)Math.max(1, chain.size()) * 16.0F;
               int bitmapW = PsdLcdLayout.bitmapWidthFor(extent);
               int bitmapH = PsdLcdLayout.bitmapHeight(anyCut(cuts));
               PsdLcdRenderer.Content content = contentOf(entity, world, anchor, extent, bitmapW, bitmapH);
               String shapeKey = shapeKey(cuts);
               String key = String.valueOf(world.dimension().location()) + "|" + anchor.asLong() + "|" + shapeKey;
               PsdLcdTextures.Entry texture = PsdLcdTextures.acquire(key, bitmapW, bitmapH);
               if (texture != null) {
                  if (!content.hash().equals(texture.hash)) {
                     BufferedImage image = PsdLcdPainter.paint(content.content(), PsdLcdAssets.get(), content.ad(), bitmapW, bitmapH);
                     texture.hash = content.hash();
                     PsdLcdTextures.upload(texture, image);
                     PsdLcdTextures.invalidateTicker(key);
                  }

                  float[] normal = PsdLcdLayout.screenNormal(face);
                  Matrix4f matrix = matrices.last().pose();
                  Matrix3f normalMatrix = matrices.last().normal();
                  MultiBufferSource.BufferSource immediate;
                  boolean flush;
                  if (vertexConsumers instanceof MultiBufferSource.BufferSource) {
                     MultiBufferSource.BufferSource direct = (MultiBufferSource.BufferSource)vertexConsumers;
                     immediate = direct;
                     flush = false;
                  } else {
                     immediate = Minecraft.getInstance().renderBuffers().bufferSource();
                     flush = true;
                  }

                  matrices.pushPose();
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  RenderSystem.disableCull();

                  try {
                     VertexConsumer vc = immediate.getBuffer(MoreRenderLayers.getLight(texture.id, false));

                     for(int i = 0; i < cuts.length; ++i) {
                        float start = (float)i * 16.0F;
                        float end = start + 16.0F;
                        float[][] quad = PsdLcdLayout.windowQuad(face, start, end, cuts[i]);
                        float u0 = start / extent;
                        float u1 = end / extent;
                        vertex(vc, matrix, normalMatrix, quad[0][0], quad[0][1], quad[0][2], u0, 0.0F, normal);
                        vertex(vc, matrix, normalMatrix, quad[1][0], quad[1][1], quad[1][2], u0, 1.0F, normal);
                        vertex(vc, matrix, normalMatrix, quad[2][0], quad[2][1], quad[2][2], u1, 1.0F, normal);
                        vertex(vc, matrix, normalMatrix, quad[3][0], quad[3][1], quad[3][2], u1, 0.0F, normal);
                     }

                     drawTicker(content, cuts, extent, bitmapW, bitmapH, key, face, matrix, normalMatrix, normal, immediate);
                     if (PidsXiamenSettings.lineStrip()) {
                        drawStrip(entity, world, matrix, normalMatrix, normal, face, cuts, chain, immediate);
                     }
                  } finally {
                     RenderSystem.enableCull();
                     matrices.popPose();
                     if (flush) {
                        immediate.endBatch();
                     }

                  }

               }
            }
         }
      }
   }

   private static boolean[] cutsOf(Level world, List<BlockPos> chain, BlockState anchorState) {
      boolean[] cuts = new boolean[chain.size()];

      for(int i = 0; i < chain.size(); ++i) {
         BlockState each = i == 0 ? anchorState : world.getBlockState((BlockPos)chain.get(i));
         cuts[i] = PsdLcdBlock.isCut(each);
      }

      return cuts;
   }

   private static boolean anyCut(boolean[] cuts) {
      for(boolean cut : cuts) {
         if (cut) {
            return true;
         }
      }

      return false;
   }

   private static String shapeKey(boolean[] cuts) {
      StringBuilder out = new StringBuilder(cuts.length);

      for(boolean cut : cuts) {
         out.append((char)(cut ? '1' : '0'));
      }

      return out.toString();
   }

   private static PsdLcdLayout.Face faceOf(Direction facing) {
      if (facing == null) {
         return PsdLcdLayout.Face.NORTH;
      } else {
         PsdLcdLayout.Face var10000;
         switch (facing) {
            case SOUTH:
               var10000 = PsdLcdLayout.Face.SOUTH;
               break;
            case EAST:
               var10000 = PsdLcdLayout.Face.EAST;
               break;
            case WEST:
               var10000 = PsdLcdLayout.Face.WEST;
               break;
            default:
               var10000 = PsdLcdLayout.Face.NORTH;
         }

         return var10000;
      }
   }

   private void sweep(Level world) {
      long now = System.currentTimeMillis();
      if (now - this.lastSweep > 5000L) {
         this.lastSweep = now;
         PsdLcdTextures.sweep();
         sweepRotations(world);
      }

   }

   private static void sweepRotations(Level world) {
      if (world != null) {
         String prefix = String.valueOf(world.dimension().location()) + "|";
         synchronized(ROTATIONS) {
            if (ROTATIONS.size() > 512) {
               ROTATIONS.keySet().removeIf((key) -> key == null || !key.startsWith(prefix));
            }
         }
      }
   }

   private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, float[] normal) {
      vc.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(n, normal[0], normal[1], normal[2]).endVertex();
   }

   private static void drawStrip(PsdLcdBlockEntity entity, Level world, Matrix4f matrix, Matrix3f normalMatrix, float[] normal, PsdLcdLayout.Face face, boolean[] cuts, List<BlockPos> chain, MultiBufferSource.BufferSource immediate) {
      long platformId;
      try {
         platformId = entity.getPlatformId(ClientData.PLATFORMS, ClientData.DATA_CACHE);
      } catch (Throwable var20) {
         return;
      }

      int[] colors = PsdLcdStrip.colors(platformId);
      VertexConsumer vc = immediate.getBuffer(SOLID_LAYER);

      for(int i = 0; i < cuts.length; ++i) {
         int cellLight = LevelRenderer.getLightColor(world, (BlockPos)chain.get(i));
         float start = (float)i * 16.0F;
         float end = start + 16.0F;

         for(int c = 0; c < colors.length; ++c) {
            float v0 = 1.0F - (float)(c + 1) / (float)colors.length;
            float v1 = 1.0F - (float)c / (float)colors.length;
            stripQuad(vc, matrix, normalMatrix, normal, cellLight, PsdLcdLayout.stripQuad(face, start, end, cuts[i], v0, v1), colors[c]);
         }
      }

   }

   private static void stripQuad(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] normal, int light, float[][] quad, int argb) {
      int r = argb >> 16 & 255;
      int g = argb >> 8 & 255;
      int b = argb & 255;
      int a = argb >>> 24 & 255;

      for(float[] p : quad) {
         vc.vertex(m, p[0], p[1], p[2]).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, normal[0], normal[1], normal[2]).endVertex();
      }

   }

   private static void drawTicker(PsdLcdRenderer.Content content, boolean[] cuts, float extent, int bitmapW, int bitmapH, String key, PsdLcdLayout.Face face, Matrix4f matrix, Matrix3f normalMatrix, float[] normal, MultiBufferSource.BufferSource immediate) {
      if (!(content.tickerPhase() < 0.0F) && bitmapW >= 2 && bitmapH >= 2) {
         PsdLcdPainter.Content c = content.content();
         PsdLcdPainter.Assets assets = PsdLcdAssets.get();
         int sw = PsdLcdPainter.tickerStripWidth((float)bitmapW, (float)bitmapH);
         int sh = PsdLcdPainter.tickerStripHeight((float)bitmapW, (float)bitmapH);
         if (sw >= 4 && sh >= 4) {
            PsdLcdTextures.Entry strip = PsdLcdTextures.acquire(key + "|ticker", sw, sh);
            if (strip != null) {
               int phasePx = Math.round(content.tickerPhase() * PsdLcdPainter.tickerScrollSpanPx(c, assets, (float)bitmapW, (float)bitmapH));
               if (phasePx != strip.lastPhasePx) {
                  strip.lastPhasePx = phasePx;
                  PsdLcdTextures.upload(strip, PsdLcdPainter.paintTickerStrip(c, assets, (float)bitmapW, (float)bitmapH, content.tickerPhase()));
               }

               float[] inner = PsdLcdPainter.tickerInner((float)bitmapW, (float)bitmapH);
               float fx0 = inner[0] / (float)bitmapW;
               float fx1 = inner[2] / (float)bitmapW;
               float fy0 = inner[1] / (float)bitmapH;
               float fy1 = inner[3] / (float)bitmapH;
               if (!(fx1 - fx0 < 1.0E-4F) && !(fy1 - fy0 < 1.0E-4F)) {
                  VertexConsumer vc = immediate.getBuffer(MoreRenderLayers.getLight(strip.id, true));
                  int segments = Math.max(1, cuts.length);

                  for(int i = 0; i < segments; ++i) {
                     float cell0 = (float)i / (float)segments;
                     float cell1 = (float)(i + 1) / (float)segments;
                     float a = Math.max(fx0, cell0);
                     float b = Math.min(fx1, cell1);
                     if (!(b - a < 1.0E-5F)) {
                        float[][] quad = PsdLcdLayout.bandQuad(face, extent, a, b, fy0, fy1, i < cuts.length && cuts[i], 0.032F);
                        float u0 = (a - fx0) / (fx1 - fx0);
                        float u1 = (b - fx0) / (fx1 - fx0);
                        vertex(vc, matrix, normalMatrix, quad[0][0], quad[0][1], quad[0][2], u0, 0.0F, normal);
                        vertex(vc, matrix, normalMatrix, quad[1][0], quad[1][1], quad[1][2], u0, 1.0F, normal);
                        vertex(vc, matrix, normalMatrix, quad[2][0], quad[2][1], quad[2][2], u1, 1.0F, normal);
                        vertex(vc, matrix, normalMatrix, quad[3][0], quad[3][1], quad[3][2], u1, 0.0F, normal);
                     }
                  }

               }
            }
         }
      }
   }

   private static PsdLcdRenderer.Content contentOf(PsdLcdBlockEntity entity, Level world, BlockPos pos, float spanPx, int bitmapW, int bitmapH) {
      List<ScheduleEntry> schedules = PidsXiamenRenderer.schedulesFor(entity, pos);
      int page = Math.max(0, entity.getDisplayPage());
      LocalDate date = PidsXiamenRenderer.dateNow(world);
      String dateText = date.toString();
      String weekdayZh = "\u661f\u671f" + "\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u65e5".charAt(date.getDayOfWeek().getValue() - 1);
      String weekdayEn = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
      String clock = PidsXiamenRenderer.formatTime(world);
      ScheduleEntry first = entryAt(schedules, page * 2);
      ScheduleEntry second = entryAt(schedules, page * 2 + 1);
      Route route = first == null ? null : (Route)ClientData.DATA_CACHE.routeIdMap.get(first.routeId);
      Route route2 = second == null ? null : (Route)ClientData.DATA_CACHE.routeIdMap.get(second.routeId);
      String[] dest1 = first != null && route != null ? PidsXiamenRenderer.destinations(route, first) : new String[]{"", ""};
      String[] dest2 = second != null && route2 != null ? PidsXiamenRenderer.destinations(route2, second) : new String[]{"", ""};
      PidsXiamenRenderer.Countdown count1 = first == null ? new PidsXiamenRenderer.Countdown("", "") : PidsXiamenRenderer.countdown(first, true);
      PidsXiamenRenderer.Countdown count2 = second == null ? new PidsXiamenRenderer.Countdown("", "") : PidsXiamenRenderer.countdown(second, true);
      String unitZh = unitOf(count1, true);
      String unitEn = unitOf(count1, false);
      String unitZh2 = unitOf(count2, true);
      String unitEn2 = unitOf(count2, false);
      String lineZh;
      String lineEn;
      int lineColor;
      if (route == null) {
         lineZh = "";
         lineEn = "";
         lineColor = 0;
      } else {
         String raw = PidsXiamenRenderer.firstPart(route.name);
         lineZh = raw;
         String digits = raw.replaceAll("[^0-9]", "");
         if (!digits.isEmpty()) {
            lineEn = Component.translatable("gui.mtrlte.psd_lcd.line_en", new Object[]{digits}).getString();
         } else {
            int bar = route.name.indexOf(124);
            lineEn = bar < 0 ? "" : route.name.substring(bar + 1).trim();
         }

         lineColor = (route.color & 16777215) == 0 ? -13945144 : -16777216 | route.color & 16777215;
      }

      String stationZh = "";
      String stationEn = "";
      Station station = null;

      try {
         station = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, pos);
         if (station != null) {
            String[] parts = station.name.split("\\|", -1);
            stationZh = parts.length > 0 ? parts[0].trim() : "";
            stationEn = parts.length > 1 ? parts[1].trim() : "";
         }
      } catch (Throwable var46) {
      }

      int cars = consistOf(station, first == null ? 0L : first.routeId);
      List<String> carNumbers = new ArrayList(cars);

      for(int i = 1; i <= cars; ++i) {
         carNumbers.add(String.valueOf(i));
      }

      PsdLcdRenderer.AdSlot slot = adOf(entity, world, bitmapW, bitmapH);
      BufferedImage adImage = slot.ad == null ? null : PsdLcdAssets.ad(slot.ad.image());
      String ticker = slot.ad != null && slot.ad.text() != null ? slot.ad.text().getString() : "";
      PsdLcdPainter.Content content = new PsdLcdPainter.Content(dateText, weekdayZh, weekdayEn, clock, Component.translatable("gui.mtrlte.pids.bound_for_label").getString(), Component.translatable("gui.mtrlte.pids.bound_for_sub").getString(), dest1[0], dest1[1], count1.number(), Component.translatable("gui.mtrlte.psd_lcd.next_train_label").getString(), Component.translatable("gui.mtrlte.pids.next_train_sub").getString(), dest2[0], dest2[1], count2.number(), unitZh, unitEn, unitZh2, unitEn2, lineZh, lineEn, lineColor, stationZh, stationEn, carNumbers, Component.translatable("gui.mtrlte.psd_lcd.consist_note").getString(), ticker, Component.translatable("gui.mtrlte.psd_lcd.place_ad").getString());
      PsdLcdPainter.Assets assets = PsdLcdAssets.get();
      float fontPx = PsdLcdPainter.tickerFontPx((float)bitmapW, (float)bitmapH);
      float textW = PsdLcdPainter.tickerTextWidth(content, assets, (float)bitmapW, (float)bitmapH);
      float innerW = PsdLcdPainter.tickerInner((float)bitmapW, (float)bitmapH)[2] - PsdLcdPainter.tickerInner((float)bitmapW, (float)bitmapH)[0];
      float spanPxOfText = innerW + textW;
      int spanTicks = textW > 0.0F ? Math.max(1, Math.round(spanPxOfText / PidsXiamenTicker.pxPerTick(fontPx))) : 0;
      float phase;
      if (spanTicks <= 0) {
         phase = -1.0F;
      } else if (slot.passEnd > slot.passStart) {
         long now = world.getGameTime();
         phase = Math.max(0.0F, Math.min(0.9999F, (float)((double)(now - slot.passStart) / (double)(slot.passEnd - slot.passStart))));
      } else {
         long now = world.getGameTime();
         phase = (float)((double)Math.floorMod(now, (long)spanTicks) / (double)spanTicks);
      }

      String adKey = slot.ad != null && slot.ad.image() != null ? slot.ad.image().toString() : "-";
      return new PsdLcdRenderer.Content(content, adImage, String.valueOf(content) + "|" + Math.round(spanPx) + "|" + adKey, phase, spanTicks);
   }

   private static PsdLcdRenderer.AdSlot adOf(PsdLcdBlockEntity entity, Level world, int bitmapW, int bitmapH) {
      try {
         if (!PidsXiamenSettings.advertisements()) {
            return new PsdLcdRenderer.AdSlot((PidsXiamenAds.Ad)null, 0L, 0L);
         } else {
            List<PidsXiamenAds.Ad> ads = PidsXiamenSettings.applyAdOverride(entity.getAds());
            if (ads != null && !ads.isEmpty()) {
               List<PidsXiamenAds.Ad> set = PidsAdSetCache.activeAds();
               if (set != null && !set.isEmpty()) {
                  String key = rotationKey(entity, world);
                  if (key == null) {
                     return new PsdLcdRenderer.AdSlot((PidsXiamenAds.Ad)set.get(Math.floorMod(entity.getAdvertisementIndex(), set.size())), 0L, 0L);
                  } else {
                     long now = world.getGameTime();
                     synchronized(ROTATIONS) {
                        PsdLcdRenderer.AdRotation state = (PsdLcdRenderer.AdRotation)ROTATIONS.get(key);
                        if (state != null && state.setRef == set && state.count == set.size()) {
                           if (now >= state.passEnd) {
                              state.slot = (state.slot + 1) % state.count;
                              state.passStart = now;
                              state.passEnd = now + (long)passTicks((PidsXiamenAds.Ad)set.get(state.slot), bitmapW, bitmapH);
                           }
                        } else {
                           state = new PsdLcdRenderer.AdRotation();
                           state.setRef = set;
                           state.count = set.size();
                           state.slot = Math.floorMod(entity.getAdvertisementIndex(), state.count);
                           state.passStart = now;
                           state.passEnd = now + (long)passTicks((PidsXiamenAds.Ad)set.get(state.slot), bitmapW, bitmapH);
                           ROTATIONS.put(key, state);
                        }

                        return new PsdLcdRenderer.AdSlot((PidsXiamenAds.Ad)set.get(state.slot), state.passStart, state.passEnd);
                     }
                  }
               } else {
                  String key = rotationKey(entity, world);
                  if (key != null) {
                     synchronized(ROTATIONS) {
                        ROTATIONS.remove(key);
                     }
                  }

                  return new PsdLcdRenderer.AdSlot((PidsXiamenAds.Ad)ads.get(Math.floorMod(entity.getAdvertisementIndex(), ads.size())), 0L, 0L);
               }
            } else {
               return new PsdLcdRenderer.AdSlot((PidsXiamenAds.Ad)null, 0L, 0L);
            }
         }
      } catch (Throwable var14) {
         return new PsdLcdRenderer.AdSlot((PidsXiamenAds.Ad)null, 0L, 0L);
      }
   }

   private static String rotationKey(PsdLcdBlockEntity entity, Level world) {
      if (entity == null) {
         return null;
      } else {
         try {
            return (world == null ? "?" : world.dimension().location().toString()) + "|" + PsdLcdBlock.anchorOf(world, entity.getBlockPos()).asLong();
         } catch (Throwable var3) {
            return null;
         }
      }
   }

   private static int passTicks(PidsXiamenAds.Ad ad, int bitmapW, int bitmapH) {
      try {
         if (ad != null && ad.text() != null && !ad.text().getString().isBlank()) {
            PsdLcdPainter.Assets assets = PsdLcdAssets.get();
            float textW = PsdLcdPainter.tickerTextWidthFor(ad.text().getString(), assets, (float)bitmapW, (float)bitmapH);
            float innerW = PsdLcdPainter.tickerInner((float)bitmapW, (float)bitmapH)[2] - PsdLcdPainter.tickerInner((float)bitmapW, (float)bitmapH)[0];
            if (textW <= 0.0F) {
               return 200;
            } else {
               float pxPerTick = PidsXiamenTicker.pxPerTick(PsdLcdPainter.tickerFontPx((float)bitmapW, (float)bitmapH));
               int ticks = Math.round((innerW + textW) / Math.max(0.01F, pxPerTick));
               return ticks > 0 ? ticks : 200;
            }
         } else {
            return 200;
         }
      } catch (Throwable var8) {
         return 200;
      }
   }

   private static int consistOf(Station station, long routeId) {
      try {
         int atStationAny = 0;
         int onRouteAny = 0;

         for(TrainClient train : ClientData.TRAINS) {
            if (train != null && train.trainCars > 0) {
               Route route = train.getThisRoute();
               boolean onRoute = routeId > 0L && route != null && route.id == routeId;
               if (onRoute && train.trainCars > onRouteAny) {
                  onRouteAny = train.trainCars;
               }

               if (sameStation(train.getThisStation(), station)) {
                  if (onRoute) {
                     return train.trainCars;
                  }

                  atStationAny = Math.max(atStationAny, train.trainCars);
               }
            }
         }

         if (atStationAny > 0) {
            return atStationAny;
         }

         if (onRouteAny > 0) {
            return onRouteAny;
         }
      } catch (Throwable var9) {
      }

      if (!carsWarned) {
         carsWarned = true;
         LteLog.LOGGER.warn("[lte] psd lcd: no live train at this platform, leaving the consist row blank");
      }

      return 0;
   }

   private static boolean sameStation(Station a, Station b) {
      return a != null && b != null && a.id == b.id;
   }

   private static String unitOf(PidsXiamenRenderer.Countdown countdown, boolean chinese) {
      if (countdown != null && countdown.number() != null && !countdown.number().isEmpty()) {
         String unit = unitOf(countdown.unit(), chinese);
         if (!unit.isEmpty()) {
            return unit;
         } else {
            return !countdown.number().codePoints().allMatch(Character::isDigit) ? "" : Component.translatable(chinese ? "gui.mtrlte.pids.minute" : "gui.mtrlte.psd_lcd.min_en").getString();
         }
      } else {
         return "";
      }
   }

   private static String unitOf(String zhUnit, boolean chinese) {
      if (zhUnit != null && !zhUnit.isEmpty()) {
         String minute = Component.translatable("gui.mtrlte.pids.minute").getString();
         String second = Component.translatable("gui.mtrlte.pids.second").getString();
         if (zhUnit.equals(minute)) {
            return chinese ? zhUnit : Component.translatable("gui.mtrlte.psd_lcd.min_en").getString();
         } else if (zhUnit.equals(second)) {
            return chinese ? zhUnit : Component.translatable("gui.mtrlte.psd_lcd.sec_en").getString();
         } else {
            return chinese ? zhUnit : "";
         }
      } else {
         return "";
      }
   }

   private static ScheduleEntry entryAt(List<ScheduleEntry> schedules, int index) {
      return index >= 0 && index < schedules.size() ? (ScheduleEntry)schedules.get(index) : null;
   }

   private static final class AdRotation {
      Object setRef;
      int count;
      int slot;
      long passStart;
      long passEnd;
   }

   private static record AdSlot(PidsXiamenAds.Ad ad, long passStart, long passEnd) {
   }

   private static record Content(PsdLcdPainter.Content content, BufferedImage ad, String hash, float tickerPhase, int tickerSpanTicks) {
   }
}
