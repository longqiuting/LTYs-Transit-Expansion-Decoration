package dev.js.mtrheadlight.xiamen.pids;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.js.mtrheadlight.LteLog;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class PidsXiamenTicker {
   public static final ResourceLocation CONFIG_ID = new ResourceLocation("mtrlte", "pids_xiamen_ticker.json");
   public static final float DEFAULT_SPEED = 40.0F;
   public static final float DEFAULT_GAP = 48.0F;
   public static final float REF_FONT_PX = 14.56F;
   private static volatile List<PidsXiamenTicker.Message> cache;
   private static volatile float speed = 40.0F;
   private static volatile float gap = 48.0F;
   private static volatile List<PidsXiamenTicker.Message> override;

   private PidsXiamenTicker() {
   }

   public static void invalidate() {
      cache = null;
   }

   public static List<PidsXiamenTicker.Message> messages() {
      List<PidsXiamenTicker.Message> local = override;
      if (local != null) {
         return local;
      } else {
         List<PidsXiamenTicker.Message> cached = cache;
         if (cached == null) {
            cached = load();
            cache = cached;
         }

         return cached;
      }
   }

   public static float speed() {
      return speed;
   }

   public static float gap() {
      return gap;
   }

   public static float pxPerSecond(float speedPxPerSecond, float fontPx) {
      return Math.max(1.0F, speedPxPerSecond) * Math.max(1.0F, fontPx) / 14.56F;
   }

   public static float pxPerTick(float fontPx) {
      return pxPerSecond(speed, fontPx) / 20.0F;
   }

   public static void override(List<PidsXiamenTicker.Message> messages) {
      override = messages != null && !messages.isEmpty() ? Collections.unmodifiableList(new ArrayList(messages)) : null;
   }

   public static void clearOverride() {
      override = null;
   }

   public static void draw(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, PidsXiamenLayout.Rect bar, float sizeRatio, long gameTick) {
      draw(matrices, textRenderer, immediate, bar, sizeRatio, gameTick, (List<PidsXiamenTicker.Message>)null);
   }

   public static void draw(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, PidsXiamenLayout.Rect bar, float sizeRatio, long gameTick, List<PidsXiamenTicker.Message> messages) {
      List<PidsXiamenTicker.Message> list = messages != null && !messages.isEmpty() ? messages : messages();
      if (!list.isEmpty() && bar != null) {
         float sizePx = sizePx(bar, sizeRatio);
         float innerLeft = bar.x0() + 5.0F;
         float innerRight = bar.x1() - 5.0F;
         float innerWidth = innerRight - innerLeft;
         if (!(innerWidth <= 4.0F)) {
            float boxTop = bar.cy() - sizePx * 0.5F;
            int count = list.size();
            PidsXiamenTicker.Timing timing = timing(textRenderer, list, sizePx, innerWidth);
            if (!(timing.cycle() <= 0.0D)) {
               double t = (double)gameTick / 20.0D % timing.cycle();

               for(int i = 0; i < count; ++i) {
                  if (t < (double)timing.scroll()[i]) {
                     float progress = (float)(t / (double)timing.scroll()[i]);
                     float x = innerRight - progress * (innerWidth + timing.width()[i]);
                     PidsXiamenText.drawRun(matrices, textRenderer, immediate, new PidsXiamenText.Segment[]{new PidsXiamenText.Segment(((PidsXiamenTicker.Message)list.get(i)).text(), sizePx, ((PidsXiamenTicker.Message)list.get(i)).color())}, x, boxTop, sizePx, PidsXiamenLayout.HAlign.LEFT, 0.0F, innerLeft, innerRight);
                     return;
                  }

                  t -= (double)timing.scroll()[i];
                  if (t < (double)timing.idle()[i]) {
                     return;
                  }

                  t -= (double)timing.idle()[i];
               }

            }
         }
      }
   }

   private static PidsXiamenTicker.Timing timing(Font textRenderer, List<PidsXiamenTicker.Message> list, float sizePx, float innerWidth) {
      int count = list.size();
      float[] width = new float[count];
      float[] scroll = new float[count];
      float[] idle = new float[count];
      double cycle = 0.0D;
      double span = 0.0D;

      for(int i = 0; i < count; ++i) {
         PidsXiamenTicker.Message message = (PidsXiamenTicker.Message)list.get(i);
         float natural = Math.max(1.0F, PidsXiamenText.width(textRenderer, message.text(), sizePx));
         width[i] = natural;
         float rate = pxPerSecond(message.speed() > 0.0F ? message.speed() : speed, sizePx);
         scroll[i] = (innerWidth + width[i]) / rate;
         idle[i] = Math.max(0.0F, gap) / rate;
         cycle += (double)(scroll[i] + idle[i]);
         span += (double)scroll[i] + (i < count - 1 ? (double)idle[i] : 0.0D);
      }

      return new PidsXiamenTicker.Timing(width, scroll, idle, cycle, span);
   }

   public static float sizePx(PidsXiamenLayout.Rect bar, float sizeRatio) {
      return Math.max(4.0F, bar.h() * sizeRatio);
   }

   public static int scrollSpanTicks(Font textRenderer, List<PidsXiamenTicker.Message> messages, PidsXiamenLayout.Rect bar, float sizeRatio) {
      if (textRenderer != null && messages != null && !messages.isEmpty() && bar != null) {
         float innerWidth = bar.x1() - 5.0F - (bar.x0() + 5.0F);
         if (innerWidth <= 4.0F) {
            return 0;
         } else {
            double span = timing(textRenderer, messages, sizePx(bar, sizeRatio), innerWidth).span();
            return span <= 0.0D ? 0 : Math.max(1, (int)Math.round(span * 20.0D));
         }
      } else {
         return 0;
      }
   }

   private static List<PidsXiamenTicker.Message> load() {
      Minecraft client = Minecraft.getInstance();
      if (client == null) {
         return Collections.emptyList();
      } else {
         try {
            Optional<Resource> resource = client.getResourceManager().getResource(CONFIG_ID);
            if (resource.isEmpty()) {
               return Collections.emptyList();
            } else {
               InputStream in = ((Resource)resource.get()).open();

               List var4;
               try {
                  JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                  var4 = parse(root);
               } catch (Throwable var6) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                     }
                  }

                  throw var6;
               }

               if (in != null) {
                  in.close();
               }

               return var4;
            }
         } catch (Throwable var7) {
            LteLog.LOGGER.warn("[lte] xiamen pids ticker: could not read {}", CONFIG_ID, var7);
            return Collections.emptyList();
         }
      }
   }

   static List<PidsXiamenTicker.Message> parse(JsonElement root) {
      List<PidsXiamenTicker.Message> out = new ArrayList();
      if (root != null && root.isJsonObject()) {
         JsonObject top = root.getAsJsonObject();

         try {
            JsonElement speedElement = top.get("speed");
            if (speedElement != null && speedElement.getAsJsonPrimitive().isNumber()) {
               speed = speedElement.getAsFloat();
            }

            JsonElement gapElement = top.get("gap");
            if (gapElement != null && gapElement.getAsJsonPrimitive().isNumber()) {
               gap = gapElement.getAsFloat();
            }
         } catch (Throwable var15) {
         }

         JsonElement messagesElement = top.get("messages");
         if (messagesElement != null && messagesElement.isJsonArray()) {
            JsonArray array = messagesElement.getAsJsonArray();

            for(int i = 0; i < array.size(); ++i) {
               try {
                  JsonElement entryElement = array.get(i);
                  if (entryElement != null && entryElement.isJsonObject()) {
                     JsonObject entry = entryElement.getAsJsonObject();
                     JsonElement textElement = entry.get("text");
                     if (textElement != null && !textElement.isJsonNull()) {
                        String text = resolve(textElement.getAsString());
                        if (!text.isEmpty()) {
                           int color = -1;
                           JsonElement colorElement = entry.get("color");
                           if (colorElement != null && !colorElement.isJsonNull()) {
                              color = parseColor(colorElement.getAsString());
                           }

                           float own = 0.0F;
                           JsonElement speedElement = entry.get("speed");
                           if (speedElement != null && speedElement.getAsJsonPrimitive().isNumber()) {
                              own = speedElement.getAsFloat();
                           }

                           out.add(new PidsXiamenTicker.Message(text, color, own));
                        }
                     }
                  }
               } catch (Throwable var14) {
                  LteLog.LOGGER.warn("[lte] xiamen pids ticker: bad message #{} in {}", i, CONFIG_ID);
               }
            }

            return Collections.unmodifiableList(out);
         } else {
            return Collections.emptyList();
         }
      } else {
         return Collections.emptyList();
      }
   }

   static String resolve(String raw) {
      if (raw != null && !raw.isEmpty()) {
         try {
            String translated = Component.translatable(raw).getString();
            return translated != null && !translated.isEmpty() ? translated : raw;
         } catch (Throwable var2) {
            return raw;
         }
      } else {
         return "";
      }
   }

   static int parseColor(String raw) {
      try {
         String hex = raw.startsWith("#") ? raw.substring(1) : raw;
         int rgb = Integer.parseInt(hex, 16);
         return -16777216 | rgb & 16777215;
      } catch (Throwable var3) {
         return -1;
      }
   }

   public static record Message(String text, int color, float speed) {
   }

   private static record Timing(float[] width, float[] scroll, float[] idle, double cycle, double span) {
   }
}
