package dev.js.mtrheadlight.xiamen.pids;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.bind.PidsStationResolver;
import dev.js.mtrheadlight.client.RenderDistance;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mtr.MTRClient;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.Route;
import mtr.data.ScheduleEntry;
import mtr.data.Station;
import mtr.data.TrainClient;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.render.MoreRenderLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class PidsXiamenRenderer extends BlockEntityRendererMapper<PidsXiamenBlockEntity> {
   private static final LocalDate EPOCH = LocalDate.of(2024, 1, 1);
   private static final long LOG_INTERVAL_TICKS = 200L;
   private static boolean warned;
   private static boolean warnedDouble;
   private static long nextLogTick;
   private static long countedTick = -1L;
   private static final Map<BlockPos, Integer> DRAWN_THIS_TICK = new HashMap();
   private static final Set<PidsXiamenBlock.Kind> AD_LOGGED = EnumSet.noneOf(PidsXiamenBlock.Kind.class);
   private static final float CONSIST_LAYER_BLOCKS = 0.002F;
   private static final String CAR_SUFFIX = "\u8f66";
   private static boolean carsWarned;

   public boolean isInRenderDistance(PidsXiamenBlockEntity entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public PidsXiamenRenderer(BlockEntityRendererProvider.Context context) {
      super(context.getBlockEntityRenderDispatcher());
   }

   public void render(PidsXiamenBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      try {
         this.renderSafe(entity, matrices, vertexConsumers);
      } catch (Throwable var8) {
         if (!warned) {
            warned = true;
            LteLog.LOGGER.error("[lte] xiamen pids render failed", var8);
         }
      }

   }

   private void renderSafe(PidsXiamenBlockEntity entity, PoseStack matrices, MultiBufferSource vertexConsumers) {
      Level world = entity.getLevel();
      if (world != null) {
         BlockPos pos = entity.getBlockPos();
         BlockState state = world.getBlockState(pos);
         Block facing = state.getBlock();
         if (facing instanceof PidsXiamenBlock) {
            PidsXiamenBlock block = (PidsXiamenBlock)facing;
            if (PidsXiamenBlock.canStoreData(world, pos)) {
               Direction facingLted = PidsXiamenRenderer.IBlockFacing.of(world, pos);
               if (facingLted != null) {
                  Minecraft client = Minecraft.getInstance();
                  if (client != null && client.font != null) {
                     MultiBufferSource.BufferSource textConsumers;
                     boolean flushText;
                     if (vertexConsumers instanceof MultiBufferSource.BufferSource) {
                        MultiBufferSource.BufferSource immediate = (MultiBufferSource.BufferSource)vertexConsumers;
                        textConsumers = immediate;
                        flushText = false;
                     } else {
                        textConsumers = client.renderBuffers().bufferSource();
                        flushText = true;
                     }

                     PidsXiamenBlock.Kind kind = block.kind();
                     boolean partner = world.getBlockState(pos.relative(facingLted)).getBlock() instanceof PidsXiamenBlock;
                     List<ScheduleEntry> schedules = schedulesFor(entity, pos);
                     long gameTick = (long)MTRClient.getGameTick();
                     List<PidsXiamenAds.Ad> ads = PidsXiamenSettings.applyAdOverride(entity.getAds());
                     int cars = kind == PidsXiamenBlock.Kind.METRO ? consistOf(resolveStation(pos), schedules.isEmpty() ? 0L : ((ScheduleEntry)schedules.get(0)).routeId) : 0;
                     PidsXiamenRenderer.Screen screen = new PidsXiamenRenderer.Screen(kind, schedules, entity.getDisplayPage(), slideOf(client.font, kind, gameTick, entity.getAdvertisementIndex(), ads), gameTick, formatTime(world), formatIsoDate(world), formatBrtDate(world), cars);
                     countDraw(pos, kind, facingLted, partner);
                     PidsXiamenConsistTextures.sweep();

                     try {
                        this.renderFace(matrices, vertexConsumers, textConsumers, kind, client.font, facingLted, 0.0F, 0.0F, screen);
                        if (partner) {
                           this.renderFace(matrices, vertexConsumers, textConsumers, kind, client.font, facingLted.getOpposite(), (float)facingLted.getStepX(), (float)facingLted.getStepZ(), screen);
                        }
                     } finally {
                        if (flushText) {
                           textConsumers.endBatch();
                        }

                     }

                  }
               }
            }
         }
      }
   }

   private void renderFace(PoseStack matrices, MultiBufferSource vertexConsumers, MultiBufferSource.BufferSource textConsumers, PidsXiamenBlock.Kind kind, Font textRenderer, Direction screenFacing, float cellDx, float cellDz, PidsXiamenRenderer.Screen screen) {
      matrices.pushPose();

      try {
         applyScreenTransform(matrices, screenFacing, cellDx, cellDz);
         step("base", () -> drawQuad(matrices, vertexConsumers.getBuffer(MoreRenderLayers.getLight(kind.screenTexture(), false)), 0.0F, 0.0F, 512.0F, 235.0F));
         step("consist", () -> renderConsist(matrices, vertexConsumers, screen));
         matrices.translate(0.0F, 0.0F, -0.7F);
         step("ad", () -> {
            PidsXiamenRenderer.Slide slide = screen.slide();
            if (slide.ad() != null) {
               matrices.pushPose();

               try {
                  matrices.translate(0.0F, 0.0F, 0.35F);
                  drawAdvertisement(matrices, vertexConsumers, kind, slide);
               } finally {
                  matrices.popPose();
               }

            }
         });
         step("clock", () -> this.renderClock(matrices, textRenderer, textConsumers, kind, screen));
         step("rows", () -> this.renderArrivals(matrices, textRenderer, textConsumers, kind, screen));
         step("consistText", () -> renderConsistText(matrices, textRenderer, textConsumers, screen));
         step("ticker", () -> {
            PidsXiamenLayout.Rect bar = tickerOf(kind);
            float sizeRatio = tickerTextOf(kind).sizeRatio();
            PidsXiamenRenderer.Slide slide = screen.slide();
            List<PidsXiamenTicker.Message> caption = captionOf(slide.ad());
            long tickerTick = slide.ad() == null ? screen.gameTick() : slide.tick();
            PidsXiamenTicker.draw(matrices, textRenderer, textConsumers, bar, sizeRatio, tickerTick, caption);
         });
      } finally {
         matrices.popPose();
      }

   }

   private static void applyScreenTransform(PoseStack matrices, Direction screenFacing, float cellDx, float cellDz) {
      Direction outward = screenFacing.getClockWise();
      matrices.translate(0.5F + cellDx + (float)outward.getStepX() * 0.4797F - (float)screenFacing.getStepX() * 0.48F, 0.8467F, 0.5F + cellDz + (float)outward.getStepZ() * 0.4797F - (float)screenFacing.getStepZ() * 0.48F);
      matrices.mulPose(Axis.YP.rotationDegrees(90.0F - screenFacing.toYRot()));
      matrices.mulPose(Axis.ZP.rotationDegrees(180.0F));
      matrices.mulPose(Axis.XP.rotationDegrees(22.5F));
      float inverse = 0.0038299502F;
      matrices.scale(0.0038299502F, 0.0038299502F, 0.0038299502F);
   }

   private static void drawQuad(PoseStack matrices, VertexConsumer buffer, float x, float y, float width, float height) {
      PoseStack.Pose entry = matrices.last();
      Matrix4f position = entry.pose();
      Matrix3f normal = entry.normal();
      vertex(buffer, position, normal, x, y, 0.0F, 0.0F);
      vertex(buffer, position, normal, x, y + height, 0.0F, 1.0F);
      vertex(buffer, position, normal, x + width, y + height, 1.0F, 1.0F);
      vertex(buffer, position, normal, x + width, y, 1.0F, 0.0F);
   }

   private static void vertex(VertexConsumer buffer, Matrix4f position, Matrix3f normal, float x, float y, float u, float v) {
      buffer.vertex(position, x, y, 0.0F).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
   }

   private static void renderConsist(PoseStack matrices, MultiBufferSource vertexConsumers, PidsXiamenRenderer.Screen screen) {
      if (screen.kind() == PidsXiamenBlock.Kind.METRO) {
         ResourceLocation id = PidsXiamenConsistTextures.texture(screen.cars());
         if (id != null) {
            float[] strip = PidsXiamenConsist.strip();
            matrices.pushPose();

            try {
               matrices.translate(0.0F, 0.0F, -0.52220005F);
               drawQuad(matrices, vertexConsumers.getBuffer(MoreRenderLayers.getLight(id, true)), strip[0], strip[1], strip[2], strip[3]);
            } finally {
               matrices.popPose();
            }

         }
      }
   }

   private static void renderConsistText(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, PidsXiamenRenderer.Screen screen) {
      if (screen.kind() == PidsXiamenBlock.Kind.METRO && screen.cars() > 0) {
         if (PidsXiamenConsistTextures.texture(screen.cars()) != null) {
            PidsXiamenConsist.Card[] cards = PidsXiamenConsist.layout(screen.cars());

            for(PidsXiamenConsist.Card card : cards) {
               String label = card.index() + 1 + "\u8f66";
               PidsXiamenText.drawRun(matrices, textRenderer, immediate, new PidsXiamenText.Segment[]{new PidsXiamenText.Segment(label, 6.0F, -1)}, card.textRight(), 24.0F, 6.0F, PidsXiamenLayout.HAlign.RIGHT, card.textW(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
            }

         }
      }
   }

   private void renderClock(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, PidsXiamenBlock.Kind kind, PidsXiamenRenderer.Screen screen) {
      PidsXiamenLayout.Clock clock = clockOf(kind);
      PidsXiamenLayout.Rect card = clock.card();
      PidsXiamenLayout.TextStyle date = clock.date();
      PidsXiamenLayout.TextStyle time = clock.time();
      this.drawFlow(matrices, textRenderer, immediate, card, clock.padRatio(), clock.gapRatio(), line(date, card, kind == PidsXiamenBlock.Kind.BRT ? screen.dateBrt() : screen.dateIso()), line(time, card, screen.clock()));
   }

   private void renderArrivals(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, PidsXiamenBlock.Kind kind, PidsXiamenRenderer.Screen screen) {
      if (kind == PidsXiamenBlock.Kind.BRT) {
         this.renderBrtRows(matrices, textRenderer, immediate, screen);
      } else {
         this.renderMetroRows(matrices, textRenderer, immediate, screen);
      }

   }

   private void renderMetroRows(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, PidsXiamenRenderer.Screen screen) {
      PidsXiamenLayout.Row[] rows = PidsXiamenLayout.METRO.rows();
      TextFit.Metrics metrics = metrics(textRenderer);
      List<PidsXiamenColumns.Label> labels = new ArrayList();
      float rowGlyph = 0.0F;

      for(int i = 0; i < rows.length; ++i) {
         PidsXiamenLayout.Row row = rows[i];
         labels.add(new PidsXiamenColumns.Label(Component.translatable(labelKey(i)).getString(), size(row.label(), row.card())));
         labels.add(new PidsXiamenColumns.Label(Component.translatable(labelSubKey(i)).getString(), size(row.labelSub(), row.card())));
         rowGlyph = Math.max(rowGlyph, size(row.dest(), row.card()));
      }

      float labelW = PidsXiamenColumns.labelWidth(metrics, labels);
      float gap = PidsXiamenColumns.gap(rowGlyph);

      for(int i = 0; i < rows.length; ++i) {
         PidsXiamenLayout.Row row = rows[i];
         PidsXiamenLayout.Rect card = row.card();
         PidsXiamenColumns.Columns cols = PidsXiamenColumns.of(card.x0() + row.labelXInset(), labelW, rowGlyph, card.x1() - gap);
         ScheduleEntry entry = entryAt(screen, i);
         Route route = entry == null ? null : (Route)ClientData.DATA_CACHE.routeIdMap.get(entry.routeId);
         if (entry != null && route != null) {
            this.drawFlow(matrices, textRenderer, immediate, card, row.padRatio(), row.gapRatio(), line(row.label(), card, Component.translatable(labelKey(i)).getString(), cols.labelX(), cols.labelW()), line(row.labelSub(), card, Component.translatable(labelSubKey(i)).getString(), cols.labelX(), cols.labelW()));
            String[] destination = destinations(route, entry);
            PidsXiamenRenderer.Countdown countdown = countdown(entry);
            this.drawFlow(matrices, textRenderer, immediate, card, row.padRatio(), row.gapRatio(), fittedLine(row.dest(), card, destination[0], textRenderer, cols.nameX(), cols.nameW()), fittedLine(row.destSub(), card, destination[1], textRenderer, cols.nameX(), cols.nameW()), this.countLine(row.count(), row.unit(), card, countdown, cols.nameX(), cols.nameW()));
         }
      }

   }

   private void renderBrtRows(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, PidsXiamenRenderer.Screen screen) {
      PidsXiamenLayout.BrtRow[] rows = PidsXiamenLayout.BRT.rows();

      for(int i = 0; i < rows.length; ++i) {
         PidsXiamenLayout.BrtRow row = rows[i];
         PidsXiamenLayout.Rect card = row.card();
         ScheduleEntry entry = entryAt(screen, i);
         Route route = entry == null ? null : (Route)ClientData.DATA_CACHE.routeIdMap.get(entry.routeId);
         if (entry != null && route != null) {
            String destination = destinations(route, entry)[0];
            String routeName = firstPart(route.name);
            PidsXiamenRenderer.Countdown countdown = countdown(entry);
            this.drawFlow(matrices, textRenderer, immediate, card, row.padRatio(), row.gapRatio(), fittedLine(row.dest(), card, Component.translatable("gui.mtrlte.pids.bound_for_station", new Object[]{destination}).getString(), textRenderer), this.routeLine(row, card, route, routeName, countdown));
         }
      }

   }

   private PidsXiamenRenderer.Line routeLine(PidsXiamenLayout.BrtRow row, PidsXiamenLayout.Rect card, Route route, String routeName, PidsXiamenRenderer.Countdown countdown) {
      PidsXiamenLayout.TextStyle routeStyle = row.route();
      PidsXiamenLayout.TextStyle about = row.about();
      PidsXiamenLayout.TextStyle count = row.count();
      PidsXiamenLayout.TextStyle unit = row.unit();
      if (countdown.unit().isEmpty()) {
         float routeSize = size(routeStyle, card);
         float stateSize = size(count, card);
         PidsXiamenText.Segment[] state = new PidsXiamenText.Segment[]{new PidsXiamenText.Segment(routeName, routeSize, routeColor(route)), new PidsXiamenText.Segment(" ", routeSize, -1), new PidsXiamenText.Segment(countdown.number(), stateSize, count.color())};
         return new PidsXiamenRenderer.Line(state, Math.max(routeSize, stateSize), card.x(routeStyle.xRatio()), PidsXiamenLayout.HAlign.LEFT, card.w() * 0.88F);
      } else {
         float height = Math.max(Math.max(size(routeStyle, card), size(count, card)), size(unit, card));
         PidsXiamenText.Segment[] segments = new PidsXiamenText.Segment[]{new PidsXiamenText.Segment(routeName, size(routeStyle, card), routeColor(route)), new PidsXiamenText.Segment(" " + Component.translatable("gui.mtrlte.pids.about").getString(), size(about, card), about.color()), new PidsXiamenText.Segment(pad(countdown.number()), size(count, card), count.color()), new PidsXiamenText.Segment(padUnit(countdown.unit()), size(unit, card), unit.color())};
         return new PidsXiamenRenderer.Line(segments, height, card.x(routeStyle.xRatio()), PidsXiamenLayout.HAlign.LEFT, card.w() * 0.88F);
      }
   }

   private static int routeColor(Route route) {
      return route != null && (route.color & 16777215) != 0 ? -16777216 | route.color & 16777215 : -1;
   }

   private PidsXiamenRenderer.Line countLine(PidsXiamenLayout.TextStyle count, PidsXiamenLayout.TextStyle unit, PidsXiamenLayout.Rect card, PidsXiamenRenderer.Countdown countdown, float x, float maxWidth) {
      float countSize = size(count, card);
      float unitSize = size(unit, card);
      PidsXiamenText.Segment[] segments = new PidsXiamenText.Segment[]{new PidsXiamenText.Segment(countdown.number(), countSize, count.color()), new PidsXiamenText.Segment(padUnit(countdown.unit()), unitSize, unit.color())};
      return new PidsXiamenRenderer.Line(segments, Math.max(countSize, unitSize), x, PidsXiamenLayout.HAlign.LEFT, maxWidth);
   }

   private static PidsXiamenRenderer.Slide slideOf(Font textRenderer, PidsXiamenBlock.Kind kind, long gameTick, int adIndex, List<PidsXiamenAds.Ad> ads) {
      if (!PidsXiamenSettings.advertisements()) {
         return PidsXiamenRenderer.Slide.NONE;
      } else {
         int count = ads == null ? 0 : ads.size();
         if (count <= 0) {
            return PidsXiamenRenderer.Slide.NONE;
         } else {
            PidsXiamenLayout.Rect bar = tickerOf(kind);
            float sizeRatio = tickerTextOf(kind).sizeRatio();
            PidsXiamenAds.Ad[] order = new PidsXiamenAds.Ad[count];
            int[] dwell = new int[count];
            long total = 0L;

            for(int slot = 0; slot < count; ++slot) {
               int index = Math.floorMod(adIndex + slot, count);
               order[slot] = (PidsXiamenAds.Ad)ads.get(index);
               dwell[slot] = dwellTicks(textRenderer, order[slot], bar, sizeRatio);
               total += (long)dwell[slot];
            }

            if (total <= 0L) {
               return PidsXiamenRenderer.Slide.NONE;
            } else {
               long t = Math.floorMod(gameTick, total);

               for(int slot = 0; slot < count; ++slot) {
                  if (t < (long)dwell[slot]) {
                     PidsXiamenAds.Ad ad = order[slot];
                     if (ad != null && ad.image() != null) {
                        return new PidsXiamenRenderer.Slide(ad, Math.floorMod(adIndex + slot, count), count, t);
                     }

                     return PidsXiamenRenderer.Slide.NONE;
                  }

                  t -= (long)dwell[slot];
               }

               return PidsXiamenRenderer.Slide.NONE;
            }
         }
      }
   }

   private static int dwellTicks(Font textRenderer, PidsXiamenAds.Ad ad, PidsXiamenLayout.Rect bar, float sizeRatio) {
      if (ad == null) {
         return 200;
      } else {
         List<PidsXiamenTicker.Message> caption = captionOf(ad);
         int ticks = PidsXiamenTicker.scrollSpanTicks(textRenderer, caption == null ? PidsXiamenTicker.messages() : caption, bar, sizeRatio);
         return ticks > 0 ? ticks : 200;
      }
   }

   private static void drawAdvertisement(PoseStack matrices, MultiBufferSource vertexConsumers, PidsXiamenBlock.Kind kind, PidsXiamenRenderer.Slide slide) {
      PidsXiamenAds.Ad ad = slide.ad();
      if (ad != null && ad.image() != null) {
         PidsXiamenLayout.Rect box = adOf(kind);
         float width = box.w();
         float height = box.h();
         drawQuad(matrices, vertexConsumers.getBuffer(MoreRenderLayers.getLight(ad.image(), false)), box.x0() + (box.w() - width) * 0.5F, box.y0() + (box.h() - height) * 0.5F, width, height);
         if (AD_LOGGED.add(kind)) {
            LteLog.LOGGER.info("[lte] pids ad: kind={} box=({},{}..{},{}) index={}/{} tick={} tex={}", new Object[]{kind, box.x0(), box.y0(), box.x1(), box.y1(), slide.index(), slide.count(), slide.tick(), ad.image()});
         }

      }
   }

   static List<PidsXiamenTicker.Message> captionOf(PidsXiamenAds.Ad ad) {
      if (ad != null && ad.text() != null) {
         String text = ad.text().getString();
         return text != null && !text.isBlank() ? List.of(new PidsXiamenTicker.Message(text, -1, 0.0F)) : null;
      } else {
         return null;
      }
   }

   private void drawFlow(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, PidsXiamenLayout.Rect card, float padRatio, float gapRatio, PidsXiamenRenderer.Line... lines) {
      if (lines.length != 0) {
         float gap = card.h() * gapRatio;
         float total = gap * (float)(lines.length - 1);

         for(PidsXiamenRenderer.Line line : lines) {
            total += line.height();
         }

         float y = card.y0() + (card.h() - total) * 0.5F;
         y = Math.max(y, card.y0() + card.h() * padRatio);

         for(PidsXiamenRenderer.Line line : lines) {
            if (hasText(line)) {
               PidsXiamenText.drawRun(matrices, textRenderer, immediate, line.segments(), line.x(), y, line.height(), line.align(), line.maxWidth(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
            }

            y += line.height() + gap;
         }

      }
   }

   private static boolean hasText(PidsXiamenRenderer.Line line) {
      for(PidsXiamenText.Segment segment : line.segments()) {
         if (segment.text() != null && !segment.text().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private static PidsXiamenRenderer.Line line(PidsXiamenLayout.TextStyle style, PidsXiamenLayout.Rect card, String text) {
      return line(style, card, text, card.x(style.xRatio()), card.w() * style.maxWidthRatio());
   }

   private static PidsXiamenRenderer.Line line(PidsXiamenLayout.TextStyle style, PidsXiamenLayout.Rect card, String text, float x, float maxWidth) {
      float size = size(style, card);
      return new PidsXiamenRenderer.Line(new PidsXiamenText.Segment[]{new PidsXiamenText.Segment(text, size, style.color())}, size, x, style.align(), maxWidth);
   }

   private static TextFit.Metrics metrics(Font textRenderer) {
      return (text, size) -> PidsXiamenText.width(textRenderer, text, size);
   }

   private static PidsXiamenRenderer.Line fittedLine(PidsXiamenLayout.TextStyle style, PidsXiamenLayout.Rect card, String text, Font textRenderer) {
      return fittedLine(style, card, text, textRenderer, card.x(style.xRatio()), card.w() * style.maxWidthRatio());
   }

   private static PidsXiamenRenderer.Line fittedLine(PidsXiamenLayout.TextStyle style, PidsXiamenLayout.Rect card, String text, Font textRenderer, float x, float maxWidth) {
      float nominal = size(style, card);
      TextFit.Fitted fitted = TextFit.fit(text, metrics(textRenderer), nominal, maxWidth, 0.62F);
      return new PidsXiamenRenderer.Line(new PidsXiamenText.Segment[]{new PidsXiamenText.Segment(fitted.text(), fitted.size(), style.color())}, fitted.size(), x, style.align(), maxWidth);
   }

   private static float size(PidsXiamenLayout.TextStyle style, PidsXiamenLayout.Rect card) {
      return Math.max(3.0F, card.h() * style.sizeRatio());
   }

   private static String pad(String text) {
      return text != null && !text.isEmpty() ? " " + text : "";
   }

   private static String padUnit(String unit) {
      return unit != null && !unit.isEmpty() ? " " + unit : "";
   }

   private static PidsXiamenLayout.Clock clockOf(PidsXiamenBlock.Kind kind) {
      return kind == PidsXiamenBlock.Kind.BRT ? PidsXiamenLayout.BRT.clock() : PidsXiamenLayout.METRO.clock();
   }

   private static PidsXiamenLayout.Rect adOf(PidsXiamenBlock.Kind kind) {
      return kind == PidsXiamenBlock.Kind.BRT ? PidsXiamenLayout.BRT.ad() : PidsXiamenLayout.METRO.ad();
   }

   private static PidsXiamenLayout.Rect tickerOf(PidsXiamenBlock.Kind kind) {
      return kind == PidsXiamenBlock.Kind.BRT ? PidsXiamenLayout.BRT.ticker() : PidsXiamenLayout.METRO.ticker();
   }

   private static PidsXiamenLayout.TextStyle tickerTextOf(PidsXiamenBlock.Kind kind) {
      return kind == PidsXiamenBlock.Kind.BRT ? PidsXiamenLayout.BRT.tickerText() : PidsXiamenLayout.METRO.tickerText();
   }

   private static String labelKey(int row) {
      String var10000;
      switch (row) {
         case 0:
            var10000 = "gui.mtrlte.pids.bound_for_label";
            break;
         case 1:
            var10000 = "gui.mtrlte.pids.next_train_label";
            break;
         default:
            var10000 = "gui.mtrlte.pids.third_train_label";
      }

      return var10000;
   }

   private static String labelSubKey(int row) {
      String var10000;
      switch (row) {
         case 0:
            var10000 = "gui.mtrlte.pids.bound_for_sub";
            break;
         case 1:
            var10000 = "gui.mtrlte.pids.next_train_sub";
            break;
         default:
            var10000 = "gui.mtrlte.pids.third_train_sub";
      }

      return var10000;
   }

   static PidsXiamenRenderer.Countdown countdown(ScheduleEntry entry) {
      return countdown(entry, false);
   }

   static PidsXiamenRenderer.Countdown countdown(ScheduleEntry entry, boolean psdLcd) {
      long remainingSeconds = (entry.arrivalMillis - System.currentTimeMillis()) / 1000L;
      ArrivalCountdown.State state = ArrivalCountdown.state(remainingSeconds, PidsXiamenSettings.arriving());
      String stateKey = ArrivalCountdown.stateKey(state, psdLcd);
      if (stateKey != null) {
         return new PidsXiamenRenderer.Countdown(Component.translatable(stateKey).getString(), "");
      } else {
         String unitKey = ArrivalCountdown.unitKey(state);
         return new PidsXiamenRenderer.Countdown(ArrivalCountdown.digits(state, remainingSeconds), unitKey == null ? "" : Component.translatable(unitKey).getString());
      }
   }

   private static ScheduleEntry entryAt(PidsXiamenRenderer.Screen screen, int row) {
      int index = row + screen.displayPage() * PidsXiamenLayout.rowsOf(screen.kind() == PidsXiamenBlock.Kind.BRT);
      return index >= 0 && index < screen.schedules().size() ? (ScheduleEntry)screen.schedules().get(index) : null;
   }

   static String[] destinations(Route route, ScheduleEntry entry) {
      try {
         String[] parts = ClientData.DATA_CACHE.getFormattedRouteDestination(route, entry.currentStationIndex, "").split("\\|", -1);
         String first = firstPart(parts.length > 0 ? parts[0] : "");
         String second = parts.length > 1 ? firstPart(parts[1]) : "";
         return new String[]{first, second};
      } catch (Throwable var5) {
         return new String[]{"", ""};
      }
   }

   static String firstPart(String raw) {
      if (raw == null) {
         return "";
      } else {
         int bar = raw.indexOf(124);
         String text = bar < 0 ? raw : raw.substring(0, bar);
         return text.replace("|", "").trim();
      }
   }

   static Station resolveStation(BlockPos pos) {
      try {
         return PidsStationResolver.resolve(ClientData.STATIONS, ClientData.DATA_CACHE, pos).station();
      } catch (Throwable var2) {
         return null;
      }
   }

   static List<ScheduleEntry> schedulesFor(PidsXiamenBlockEntity entity, BlockPos pos) {
      Station station = resolveStation(pos);
      return station == null ? Collections.emptyList() : getSchedules(entity, station, pos, new HashMap());
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

               if (station != null && train.getThisStation() != null && train.getThisStation().id == station.id) {
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
         LteLog.LOGGER.warn("[lte] xiamen pids: no live train for this platform, leaving the consist strip blank");
      }

      return 0;
   }

   static List<ScheduleEntry> getSchedules(PidsXiamenBlockEntity entity, Station station, BlockPos pos, Map<Long, String> platformIdToName) {
      Map<Long, Platform> stationIdToPlatforms = ClientData.DATA_CACHE.requestStationIdToPlatforms(station.id);
      if (stationIdToPlatforms != null && !stationIdToPlatforms.isEmpty()) {
         Set<Long> configured = entity.getPlatformIds();
         Set<Long> targets;
         if (configured != null && !configured.isEmpty()) {
            targets = configured;
         } else {
            long close = RailwayData.getClosePlatformId(new HashSet(stationIdToPlatforms.values()), ClientData.DATA_CACHE, pos);
            targets = close == 0L ? Collections.emptySet() : Collections.singleton(close);
         }

         Set<ScheduleEntry> entries = new HashSet();

         for(Platform platform : stationIdToPlatforms.values()) {
            if (targets.contains(platform.id)) {
               Set<ScheduleEntry> platformEntries = (Set)ClientData.SCHEDULES_FOR_PLATFORM.get(platform.id);
               if (platformEntries != null) {
                  for(ScheduleEntry entry : platformEntries) {
                     Route route = (Route)ClientData.DATA_CACHE.routeIdMap.get(entry.routeId);
                     if (route != null && entry.currentStationIndex < route.platformIds.size() - 1) {
                        entries.add(entry);
                        platformIdToName.put(platform.id, platform.name);
                     }
                  }
               }
            }
         }

         if (entries.isEmpty()) {
            return Collections.emptyList();
         } else {
            List<ScheduleEntry> sorted = new ArrayList(entries);
            Collections.sort(sorted);
            return sorted;
         }
      } else {
         return Collections.emptyList();
      }
   }

   static String formatTime(Level world) {
      if (PidsXiamenSettings.localTime()) {
         LocalTime now = LocalTime.now();
         return String.format("%02d:%02d", now.getHour(), now.getMinute());
      } else {
         long modulo = world.getDayTime() % 24000L;
         long scaled = modulo * 36L / 10L;
         long hour = (scaled / 3600L + 6L) % 24L;
         long minute = scaled / 60L % 60L;
         return String.format("%02d:%02d", hour, minute);
      }
   }

   static String formatIsoDate(Level world) {
      return dateNow(world).toString();
   }

   static LocalDate dateNow(Level world) {
      return PidsXiamenSettings.localTime() ? LocalDate.now() : worldDate(world);
   }

   private static LocalDate worldDate(Level world) {
      long day = Math.max(0L, world.getGameTime() / 24000L);
      return EPOCH.plusDays(day);
   }

   private static String formatBrtDate(Level world) {
      LocalDate date = dateNow(world);
      String weekday = Component.translatable("gui.mtrlte.pids.weekday." + date.getDayOfWeek().getValue()).getString();
      return Component.translatable("gui.mtrlte.pids.date_line", new Object[]{date.getMonthValue(), date.getDayOfMonth(), weekday}).getString();
   }

   private static void countDraw(BlockPos pos, PidsXiamenBlock.Kind kind, Direction facing, boolean partner) {
      long tick = (long)MTRClient.getGameTick();
      if (tick != countedTick) {
         countedTick = tick;
         DRAWN_THIS_TICK.clear();
      }

      int count = DRAWN_THIS_TICK.merge(pos, 1, Integer::sum);
      if (count > 1 && !warnedDouble) {
         warnedDouble = true;
         LteLog.LOGGER.warn("[lte] pids draw: pos={} \u540c\u4e00\u5e27\u88ab\u753b\u4e86 {} \u6b21\uff08\u4e24\u534a\u90fd\u5728\u6e32\u67d3\uff1f\uff09", pos, count);
      }

      if (tick >= nextLogTick) {
         nextLogTick = tick + 200L;
         LteLog.LOGGER.info("[lte] pids draw halves=1 faces={} kind={} pos={} facing={} partner={} rows={}", new Object[]{partner ? 2 : 1, kind, pos, facing, partner, PidsXiamenLayout.rowsOf(kind == PidsXiamenBlock.Kind.BRT)});
      }

   }

   private static void step(String what, Runnable action) {
      try {
         action.run();
      } catch (Throwable var3) {
         if (!warned) {
            warned = true;
            LteLog.LOGGER.error("[lte] xiamen pids: step '{}' failed", what, var3);
         }
      }

   }

   public boolean rendersOutsideBoundingBox(PidsXiamenBlockEntity entity) {
      return true;
   }

   public int getViewDistance() {
      return 64;
   }

   static record Countdown(String number, String unit) {
   }

   private static final class IBlockFacing {
      private static final Property<Direction> FACING = HorizontalDirectionalBlock.FACING;

      static Direction of(Level world, BlockPos pos) {
         try {
            return (Direction)IBlock.getStatePropertySafe(world, pos, FACING);
         } catch (Throwable var3) {
            return null;
         }
      }
   }

   private static record Line(PidsXiamenText.Segment[] segments, float height, float x, PidsXiamenLayout.HAlign align, float maxWidth) {
   }

   private static record Screen(PidsXiamenBlock.Kind kind, List<ScheduleEntry> schedules, int displayPage, PidsXiamenRenderer.Slide slide, long gameTick, String clock, String dateIso, String dateBrt, int cars) {
   }

   private static record Slide(PidsXiamenAds.Ad ad, int index, int count, long tick) {
      private static final PidsXiamenRenderer.Slide NONE = new PidsXiamenRenderer.Slide((PidsXiamenAds.Ad)null, -1, 0, 0L);
   }
}
