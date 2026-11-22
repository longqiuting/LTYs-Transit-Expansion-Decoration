package dev.js.mtrheadlight.decor;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.clock.ClockClient;
import dev.js.mtrheadlight.xiamen.pids.PidsXiamenAdSetScreen;
import dev.js.mtrheadlight.xiamen.pids.PidsXiamenSettingsScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import dev.js.mtrheadlight.forge.platform.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class LtedDashboardScreen extends Screen {
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
   private static final int ROW_H = 20;
   private static final int PITCH = 24;
   private static final int GAP = 8;
   private static final int RESET_W = 20;
   private static final int MIN_COLUMN = 176;
   private static final int MAX_COLUMN = 320;
   private static final int HEADER_H = 16;
   private static final int NOTE_H = 12;
   private static final int CONTENT_TOP = 46;
   private static final int FOOTER_H = 36;
   private final Screen parent;
   private int colX;
   private int colW;
   private int scroll;
   private int contentHeight;
   private int maxScroll;
   private int versionY;
   private final List<LtedDashboardScreen.Header> headers = new ArrayList();
   private final List<LtedDashboardScreen.Note> notes = new ArrayList();

   public LtedDashboardScreen(Screen parent) {
      super(tr("screen.mtrlte.lted_dashboard"));
      this.parent = parent;
   }

   private static MutableComponent tr(String key, Object... args) {
      return Component.translatable(key, args);
   }

   public static void open() {
      try {
         Minecraft client = Minecraft.getInstance();
         client.execute(() -> client.setScreen(new LtedDashboardScreen(client.screen)));
      } catch (Throwable var1) {
         LteLog.LOGGER.warn("[lte] could not open the LTED dashboard", var1);
      }

   }

   protected void init() {
      this.headers.clear();
      this.notes.clear();
      this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, this.maxScroll)));
      int maxCol = Math.max(176, this.width - 24);
      this.colW = Math.min(320, maxCol);
      this.colX = this.width / 2 - this.colW / 2;
      int viewBottom = this.height - 36;
      int y = 46 - this.scroll;
      y = this.header(y, "screen.mtrlte.lted.sec.display");
      y = this.actionRow(y, tr("screen.mtrlte.pids_xiamen"), (b) -> this.openPidsSettings());
      y = this.actionRow(y, tr("screen.mtrlte.pids_ad_set"), (b) -> this.openAdSet());
      y = this.note(y, "screen.mtrlte.lted.psd_lcd.none");
      y = this.header(y, "screen.mtrlte.lted.sec.clock");
      y = this.actionRow(y, tr("screen.mtrlte.clock.global"), (b) -> this.openClockGlobal());
      y = this.header(y, "screen.mtrlte.lted.sec.device");
      y = this.note(y, "screen.mtrlte.lted.device.none");
      y = this.header(y, "screen.mtrlte.lted.sec.other");
      y = this.toggleRow(y, "screen.mtrlte.lted.strip", (cfg) -> cfg.xiamenRouteStrip, (cfg, v) -> cfg.xiamenRouteStrip = v);
      y = this.toggleRow(y, "screen.mtrlte.lted.names_below", (cfg) -> cfg.routeStripNamesBelow, (cfg, v) -> cfg.routeStripNamesBelow = v);
      y = this.doorLightRow(y);
      y = this.toggleRow(y, "screen.mtrlte.lted.door_sound", (cfg) -> cfg.fangsuDoorSound, (cfg, v) -> cfg.fangsuDoorSound = v);
      this.contentHeight = y - (46 - this.scroll);
      this.versionY = y + 2;
      int footerY = this.height - 30;
      this.addRenderableWidget(Button.builder(tr("gui.done"), (b) -> this.onClose()).bounds(this.colX, footerY, this.colW, 20).build());
      int visible = this.height - 36 - 46;
      this.maxScroll = Math.max(0, this.contentHeight + 16 - visible);
      if (this.scroll > this.maxScroll) {
         this.scroll = this.maxScroll;
      }

      if (this.scroll < 0) {
         this.scroll = 0;
      }

   }

   private int header(int yGuess, String key) {
      this.headers.add(new LtedDashboardScreen.Header(yGuess, key));
      return yGuess + 16;
   }

   private int note(int yGuess, String key) {
      this.notes.add(new LtedDashboardScreen.Note(yGuess, key));
      return yGuess + 12;
   }

   private boolean inView(int y) {
      return y + 20 > 46 && y < this.height - 36;
   }

   private int actionRow(int yGuess, Component label, Button.OnPress action) {
      int w = this.colW - 20 - 8;
      if (this.inView(yGuess)) {
         this.addRenderableWidget(Button.builder(label, action).bounds(this.colX, yGuess, w, 20).build());
      }

      return yGuess + 24;
   }

   private int toggleRow(int yGuess, String labelKey, Function<DecorConfig, Boolean> getter, LtedDashboardScreen.BoolSetter setter) {
      if (!this.inView(yGuess)) {
         return yGuess + 24;
      } else {
         int w = this.colW - 20 - 8;
         boolean[] value = new boolean[]{Boolean.TRUE.equals(getter.apply(DecorConfig.get()))};
         Button[] holder = new Button[]{(Button)this.addRenderableWidget(Button.builder(toggleLabel(labelKey, value[0]), (b) -> {
            value[0] = !value[0];
            setter.set(DecorConfig.get(), value[0]);
            saveDecor();
            b.setMessage(toggleLabel(labelKey, value[0]));
         }).bounds(this.colX, yGuess, w, 20).build())};
         this.addRenderableWidget(Button.builder(tr("btn.resetOne"), (b) -> {
            DecorConfig def = DecorConfig.defaults();
            boolean d = Boolean.TRUE.equals(getter.apply(def));
            value[0] = d;
            setter.set(DecorConfig.get(), d);
            saveDecor();
            holder[0].setMessage(toggleLabel(labelKey, d));
         }).bounds(this.colX + w + 8, yGuess, 20, 20).tooltip(Tooltip.create(tr("screen.mtrlte.lted.reset"))).build());
         return yGuess + 24;
      }
   }

   private int doorLightRow(int yGuess) {
      if (!this.inView(yGuess)) {
         return yGuess + 24;
      } else {
         int w = this.colW - 20 - 8;
         Button[] holder = new Button[]{(Button)this.addRenderableWidget(Button.builder(this.doorLightLabel(), (b) -> {
            DecorConfig cfg = DecorConfig.get();
            String var10000;
            switch (DecorConfig.normalizeDoorLightColor(cfg.doorLightColor)) {
               case "orange":
                  var10000 = "green";
                  break;
               case "green":
                  var10000 = "blue";
                  break;
               default:
                  var10000 = "orange";
            }

            String next = var10000;
            cfg.doorLightColor = next;
            saveDecor();
            b.setMessage(this.doorLightLabel());
         }).bounds(this.colX, yGuess, w, 20).build())};
         this.addRenderableWidget(Button.builder(tr("btn.resetOne"), (b) -> {
            DecorConfig.get().doorLightColor = DecorConfig.defaults().doorLightColor;
            saveDecor();
            holder[0].setMessage(this.doorLightLabel());
         }).bounds(this.colX + w + 8, yGuess, 20, 20).tooltip(Tooltip.create(tr("screen.mtrlte.lted.reset"))).build());
         return yGuess + 24;
      }
   }

   private Component doorLightLabel() {
      return tr("screen.mtrlte.lted.door_light", tr("screen.mtrlte.lted.door_light." + DecorConfig.normalizeDoorLightColor(DecorConfig.get().doorLightColor)));
   }

   private static Component toggleLabel(String labelKey, boolean on) {
      return tr("screen.mtrlte.lted.toggle", tr(labelKey), tr(on ? "config.on" : "config.off"));
   }

   private static void saveDecor() {
      try {
         DecorConfig.get().save();
      } catch (Throwable var1) {
         LteLog.LOGGER.warn("[lte] could not save the decor config", var1);
      }

   }

   private void openPidsSettings() {
      Minecraft client = this.minecraft;
      if (client != null) {
         client.setScreen(new PidsXiamenSettingsScreen(this));
      }
   }

   private void openAdSet() {
      Minecraft client = this.minecraft;
      if (client != null) {
         client.setScreen(new PidsXiamenAdSetScreen(this));
      }
   }

   private void openClockGlobal() {
      Minecraft client = this.minecraft;
      if (client != null) {
         ClockClient.openGlobalScreen(client);
      }
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      this.drawVanillaBackground(context);
      super.render(context, mouseX, mouseY, delta);
      this.drawCentered(context, this.title, this.width / 2, 16, 16777215);
      this.drawCentered(context, this.hint(), this.width / 2, 30, 11579568);

      for(LtedDashboardScreen.Header h : this.headers) {
         if (h.y >= 30 && h.y <= this.height - 36) {
            context.drawString(this.font, tr(h.key), this.colX, h.y + 3, 16765056);
         }
      }

      for(LtedDashboardScreen.Note n : this.notes) {
         if (n.y >= 34 && n.y <= this.height - 36) {
            context.drawString(this.font, tr(n.key), this.colX, n.y + 1, 9474192);
         }
      }

      context.drawString(this.font, this.versionText(), this.colX, this.versionY, 8421504);
   }

   private Component hint() {
      return this.maxScroll <= 0 ? tr("screen.mtrlte.lted_dashboard.hint") : tr("screen.mtrlte.lted_dashboard.hint.scroll");
   }

   private Component versionText() {
      String version = "?";

      try {
         version = Env.modVersion("mtrlte");
      } catch (Throwable var3) {
      }

      return tr("screen.mtrlte.lted.version", version, Env.modVersion("minecraft"));
   }

   private void drawVanillaBackground(GuiGraphics context) {
      context.blit(OPTIONS_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
      context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
   }

   private void drawCentered(GuiGraphics context, Component text, int cx, int y, int color) {
      context.drawCenteredString(this.font, text, cx, y, color);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (this.maxScroll > 0) {
         int before = this.scroll;
         this.scroll = Math.max(0, Math.min(this.maxScroll, this.scroll - (int)Math.signum(amount) * 24));
         if (this.scroll != before) {
            this.rebuildWidgets();
         }

         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, amount);
      }
   }

   public void onClose() {
      Minecraft client = this.minecraft;
      if (client != null) {
         client.setScreen(this.parent);
      }

   }

   @FunctionalInterface
   private interface BoolSetter {
      void set(DecorConfig var1, boolean var2);
   }

   private static record Header(int y, String key) {
   }

   private static record Note(int y, String key) {
   }
}
