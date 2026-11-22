package dev.js.mtrheadlight.clock;

import dev.js.mtrheadlight.ColorPickerScreen;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ClockConfigScreen extends Screen {
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
   private static final int MAX_PANEL_W = 380;
   private static final int MAX_ROW_H = 22;
   private static final int MIN_ROW_H = 16;
   private static final int ROWS = 18;
   private static final int TICK_ROW_SLOTS = 1;
   private static final int HEADER_H = 26;
   private static final int FOOTER_H = 38;
   private static final int PAD = 8;
   private static final int RESET_W = 18;
   private static final int GAP = 4;
   private final BlockPos pos;
   private final boolean global;
   private final ClockConfig cfg;
   private boolean enabled;
   private final ClockConfig defaults = new ClockConfig();
   private final List<ResourceLocation> allSounds = new ArrayList();
   private final List<ResourceLocation> viewSounds = new ArrayList();
   private final List<ClockConfigScreen.Swatch> swatches = new ArrayList();
   private final List<Button> footerButtons = new ArrayList();
   private EditBox offsetField;
   private EditBox clockField;
   private EditBox soundField;
   private int panelX;
   private int panelY;
   private int panelW;
   private int panelH;
   private int bodyTop;
   private int bodyBottom;
   private int rowsTop;
   private int scrollRows;
   private String offsetText;
   private String soundText;
   private int colW;
   private int contentW;
   private int rowH;
   private int rowOffset;
   private int rowCount;
   private int soundScroll;
   private Button sourceButton;
   private Button glowButton;
   private Button tickButton;
   private Button chimeButton;
   private Button chimeModeButton;
   private Button redstoneButton;
   private Button enabledButton;

   public ClockConfigScreen(BlockPos pos, ClockConfig config) {
      super(Component.translatable("screen.mtrlte.clock"));
      this.pos = pos;
      this.global = pos == null;
      this.cfg = config == null ? new ClockConfig() : config.copy();
      this.cfg.clamp();
      this.enabled = false;
      this.allSounds.addAll(BuiltInRegistries.SOUND_EVENT.keySet());
      this.allSounds.sort(Comparator.comparing(ResourceLocation::toString));
      this.refreshSounds();
   }

   public ClockConfigScreen(BlockPos pos, ClockGlobalConfig config) {
      super(Component.translatable(pos == null ? "screen.mtrlte.clock.globalTitle" : "screen.mtrlte.clock"));
      this.pos = pos;
      this.global = pos == null;
      ClockGlobalConfig source = config == null ? new ClockGlobalConfig() : config;
      this.cfg = source.config == null ? new ClockConfig() : source.config.copy();
      this.cfg.clamp();
      this.enabled = source.enabled;
      this.allSounds.addAll(BuiltInRegistries.SOUND_EVENT.keySet());
      this.allSounds.sort(Comparator.comparing(ResourceLocation::toString));
      this.refreshSounds();
   }

   protected void init() {
      if (this.offsetField != null) {
         this.offsetText = this.offsetField.getValue();
      }

      if (this.soundField != null) {
         this.soundText = this.soundField.getValue();
      }

      if (this.offsetText == null) {
         this.offsetText = formatOffset(this.cfg.offsetMinutes);
      }

      if (this.soundText == null) {
         this.soundText = this.cfg.chimeSound;
      }

      this.rowOffset = this.global ? 1 : 0;
      this.rowCount = 18 + this.rowOffset;
      this.panelW = Math.min(380, this.width - 16);
      int slotCount = this.rowCount + 1;
      int available = Math.max(0, this.height - 4 - 26 - 38);
      this.rowH = clamp(available / Math.max(1, slotCount), 16, 22);
      int minPanelH = 64 + this.rowH * 2;
      this.panelH = Math.min(this.height - 4, Math.max(minPanelH, 26 + slotCount * this.rowH + 38));
      this.panelY = Math.max(2, (this.height - this.panelH) / 2);
      this.panelX = (this.width - this.panelW) / 2;
      this.bodyTop = this.panelY + 26;
      this.bodyBottom = this.panelY + this.panelH - 38;
      this.rowsTop = this.bodyTop + 1 * this.rowH;
      this.scrollRows = clamp(this.scrollRows, 0, this.maxScrollRows());
      this.contentW = this.panelW - 16 - 18 - 4;
      this.colW = (this.contentW - 4) / 2;
      this.swatches.clear();
      int left = this.panelX + 8;
      int right = left + this.colW + 4;
      int wh = this.rowH - 4;
      this.tickButton = this.pinnedCycle(left, this.panelX + 8 + this.contentW, this.bodyTop, wh, () -> Component.translatable("screen.mtrlte.clock.row.tick", new Object[]{Component.translatable(this.cfg.tickStyle.labelKey())}), () -> this.cfg.tickStyle = next(ClockConfig.TickStyle.values(), this.cfg.tickStyle));
      this.resetButtonAt(this.bodyTop, wh, () -> this.cfg.tickStyle = this.defaults.tickStyle);
      if (this.global) {
         this.enabledButton = this.cycle(left, this.row(-1), this.contentW, wh, () -> Component.translatable("screen.mtrlte.clock.global.enabled", new Object[]{onOff(this.enabled)}), () -> this.enabled = !this.enabled);
         this.resetButton(this.row(-1), wh, () -> this.enabled = false);
      }

      this.sourceButton = this.cycle(left, this.row(0), this.colW, wh, () -> Component.translatable("screen.mtrlte.clock.row.source", new Object[]{Component.translatable(this.cfg.timeSource.labelKey())}), () -> this.cfg.timeSource = next(ClockConfig.TimeSource.values(), this.cfg.timeSource));
      this.glowButton = this.cycle(right, this.row(0), this.colW, wh, () -> Component.translatable("screen.mtrlte.clock.row.glow", new Object[]{Component.translatable(this.cfg.glow.labelKey())}), () -> this.cfg.glow = next(ClockConfig.GlowMode.values(), this.cfg.glow));
      this.resetButton(this.row(0), wh, () -> {
         this.cfg.timeSource = this.defaults.timeSource;
         this.cfg.glow = this.defaults.glow;
      });
      this.chimeButton = this.cycle(left, this.row(1), this.colW, wh, () -> Component.translatable("screen.mtrlte.clock.row.chime", new Object[]{onOff(this.cfg.chime)}), () -> this.cfg.chime = !this.cfg.chime);
      this.chimeModeButton = this.cycle(right, this.row(1), this.colW, wh, () -> Component.translatable("screen.mtrlte.clock.row.chimeMode", new Object[]{Component.translatable(this.cfg.chimeMode.labelKey())}), () -> this.cfg.chimeMode = next(ClockConfig.ChimeMode.values(), this.cfg.chimeMode));
      this.resetButton(this.row(1), wh, () -> {
         this.cfg.chime = this.defaults.chime;
         this.cfg.chimeMode = this.defaults.chimeMode;
      });
      this.redstoneButton = this.cycle(left, this.row(2), this.colW, wh, () -> Component.translatable("screen.mtrlte.clock.row.redstone", new Object[]{onOff(this.cfg.redstone)}), () -> this.cfg.redstone = !this.cfg.redstone);
      this.resetButton(this.row(2), wh, () -> this.cfg.redstone = this.defaults.redstone);
      this.cycle(left, this.row(3), this.colW, wh, () -> Component.translatable("screen.mtrlte.clock.row.showFace", new Object[]{onOff(this.cfg.showFace)}), () -> this.cfg.showFace = !this.cfg.showFace);
      this.cycle(right, this.row(3), this.colW, wh, () -> Component.translatable("screen.mtrlte.clock.row.showHour", new Object[]{onOff(this.cfg.showHour)}), () -> this.cfg.showHour = !this.cfg.showHour);
      this.resetButton(this.row(3), wh, () -> {
         this.cfg.showFace = this.defaults.showFace;
         this.cfg.showHour = this.defaults.showHour;
      });
      this.cycle(left, this.row(4), this.colW, wh, () -> Component.translatable("screen.mtrlte.clock.row.showMinute", new Object[]{onOff(this.cfg.showMinute)}), () -> this.cfg.showMinute = !this.cfg.showMinute);
      this.cycle(right, this.row(4), this.colW, wh, () -> Component.translatable("screen.mtrlte.clock.row.showSecond", new Object[]{onOff(this.cfg.showSecond)}), () -> this.cfg.showSecond = !this.cfg.showSecond);
      this.resetButton(this.row(4), wh, () -> {
         this.cfg.showMinute = this.defaults.showMinute;
         this.cfg.showSecond = this.defaults.showSecond;
      });
      this.cycle(left, this.row(5), this.colW, wh, () -> Component.translatable("screen.mtrlte.clock.row.showHub", new Object[]{onOff(this.cfg.showHub)}), () -> this.cfg.showHub = !this.cfg.showHub);
      this.cycle(right, this.row(5), this.colW, wh, () -> Component.translatable("screen.mtrlte.clock.row.showTicks", new Object[]{onOff(this.cfg.showTicks)}), () -> this.cfg.showTicks = !this.cfg.showTicks);
      this.resetButton(this.row(5), wh, () -> {
         this.cfg.showHub = this.defaults.showHub;
         this.cfg.showTicks = this.defaults.showTicks;
      });
      this.lengthSlider(this.row(6), wh, "screen.mtrlte.clock.row.hourLen", this.cfg.hourLength, 0.2F, (v) -> this.cfg.hourLength = v, () -> this.cfg.hourLength = this.defaults.hourLength);
      this.lengthSlider(this.row(7), wh, "screen.mtrlte.clock.row.minuteLen", this.cfg.minuteLength, 0.28F, (v) -> this.cfg.minuteLength = v, () -> this.cfg.minuteLength = this.defaults.minuteLength);
      this.lengthSlider(this.row(8), wh, "screen.mtrlte.clock.row.secondLen", this.cfg.secondLength, 0.33F, (v) -> this.cfg.secondLength = v, () -> this.cfg.secondLength = this.defaults.secondLength);
      this.lengthSlider(this.row(9), wh, "screen.mtrlte.clock.row.faceRadius", this.cfg.faceRadius, 0.36F, (v) -> this.cfg.faceRadius = v, () -> this.cfg.faceRadius = this.defaults.faceRadius);
      this.lengthSlider(this.row(10), wh, "screen.mtrlte.clock.row.tickRadius", this.cfg.tickRadius, 0.33F, (v) -> this.cfg.tickRadius = v, () -> this.cfg.tickRadius = this.defaults.tickRadius);
      int applyW = 44;
      int clockW = Math.max(60, this.contentW / 3);
      int offsetW = this.contentW - clockW - 44 - 8;
      if (this.rowVisible(this.row(11), wh)) {
         this.offsetField = new EditBox(this.font, left, this.row(11), offsetW, wh, Component.translatable("screen.mtrlte.clock.offset"));
         this.offsetField.setMaxLength(10);
         this.offsetField.setFilter((t) -> t.matches("-?[0-9]{0,6}(\\.[0-9]{0,3})?"));
         this.offsetField.setValue(this.offsetText);
         this.offsetField.setResponder((text) -> {
            this.offsetText = text;
            Double v = parseDouble(text);
            if (v != null) {
               this.cfg.offsetMinutes = v;
            }

         });
         this.addRenderableWidget(this.offsetField);
         this.addRenderableWidget(Button.builder(Component.translatable("screen.mtrlte.clock.apply"), (b) -> this.applyClockText()).bounds(left + offsetW + 4, this.row(11), 44, wh).build());
         this.clockField = new EditBox(this.font, left + offsetW + 44 + 8, this.row(11), clockW, wh, Component.translatable("screen.mtrlte.clock.clockField"));
         this.clockField.setMaxLength(5);
         this.clockField.setFilter((t) -> t.matches("[0-9]{0,2}:?[0-9]{0,2}"));
         this.clockField.setHint(Component.translatable("screen.mtrlte.clock.clockHint"));
         this.addRenderableWidget(this.clockField);
      } else {
         this.offsetField = null;
         this.clockField = null;
      }

      this.resetButton(this.row(11), wh, () -> {
         this.cfg.offsetMinutes = this.defaults.offsetMinutes;
         this.offsetText = formatOffset(this.defaults.offsetMinutes);
         if (this.offsetField != null) {
            this.offsetField.setValue(this.offsetText);
         }

      });
      this.colorCell(left, this.row(12), wh, "screen.mtrlte.clock.color.face", () -> this.cfg.faceColor, (v) -> this.cfg.faceColor = v);
      this.colorCell(right, this.row(12), wh, "screen.mtrlte.clock.color.hour", () -> this.cfg.hourColor, (v) -> this.cfg.hourColor = v);
      this.resetButton(this.row(12), wh, () -> {
         this.cfg.faceColor = this.defaults.faceColor;
         this.cfg.hourColor = this.defaults.hourColor;
      });
      this.colorCell(left, this.row(13), wh, "screen.mtrlte.clock.color.minute", () -> this.cfg.minuteColor, (v) -> this.cfg.minuteColor = v);
      this.colorCell(right, this.row(13), wh, "screen.mtrlte.clock.color.second", () -> this.cfg.secondColor, (v) -> this.cfg.secondColor = v);
      this.resetButton(this.row(13), wh, () -> {
         this.cfg.minuteColor = this.defaults.minuteColor;
         this.cfg.secondColor = this.defaults.secondColor;
      });
      this.colorCell(left, this.row(14), wh, "screen.mtrlte.clock.color.hub", () -> this.cfg.hubColor, (v) -> this.cfg.hubColor = v);
      this.colorCell(right, this.row(14), wh, "screen.mtrlte.clock.color.tick", () -> this.cfg.tickColor, (v) -> this.cfg.tickColor = v);
      this.resetButton(this.row(14), wh, () -> {
         this.cfg.hubColor = this.defaults.hubColor;
         this.cfg.tickColor = this.defaults.tickColor;
      });
      int previewW = 44;
      if (this.rowVisible(this.row(15), wh)) {
         this.soundField = new EditBox(this.font, left, this.row(15), this.contentW - 44 - 4, wh, Component.translatable("screen.mtrlte.clock.sound.search"));
         this.soundField.setMaxLength(64);
         this.soundField.setValue(this.soundText);
         this.soundField.setResponder((text) -> {
            this.soundText = text;
            this.soundScroll = 0;
            this.refreshSounds();
         });
         this.addRenderableWidget(this.soundField);
         this.addRenderableWidget(Button.builder(Component.translatable("screen.mtrlte.clock.preview"), (b) -> this.preview()).bounds(left + this.contentW - 44, this.row(15), 44, wh).build());
      } else {
         this.soundField = null;
      }

      this.resetButton(this.row(15), wh, () -> {
         this.cfg.chimeSound = this.defaults.chimeSound;
         this.soundText = this.defaults.chimeSound;
         if (this.soundField != null) {
            this.soundField.setValue(this.soundText);
         }

      });
      this.footerButtons.clear();
      int by = this.panelY + this.panelH - 24;
      int right0 = this.panelX + this.panelW - 8;
      int bw = Math.max(56, Math.min(80, (this.contentW - 100) / 2));
      Button done = Button.builder(Component.translatable("btn.done"), (b) -> this.saveAndClose()).bounds(right0 - bw, by, bw, 20).build();
      Button cancel = Button.builder(Component.translatable("btn.cancel"), (b) -> this.onClose()).bounds(right0 - 2 * bw - 4, by, bw, 20).build();
      Button resetAll = Button.builder(Component.translatable("btn.resetAll"), (b) -> this.resetAll()).bounds(this.panelX + 8, by, Math.max(70, this.contentW - 2 * bw - 12), 20).build();
      this.addRenderableWidget(done);
      this.addRenderableWidget(cancel);
      this.addRenderableWidget(resetAll);
      this.footerButtons.add(resetAll);
      this.footerButtons.add(cancel);
      this.footerButtons.add(done);
      this.refreshSounds();
   }

   private int row(int index) {
      return this.rowsTop + (index + this.rowOffset - this.scrollRows) * this.rowH;
   }

   private int visibleRows() {
      return Math.max(1, (this.bodyBottom - this.rowsTop) / Math.max(1, this.rowH));
   }

   private int maxScrollRows() {
      return Math.max(0, this.rowCount - this.visibleRows());
   }

   private boolean rowVisible(int y, int h) {
      return y >= this.rowsTop && y + h <= this.bodyBottom;
   }

   private int resetX() {
      return this.panelX + this.panelW - 8 - 18;
   }

   private static int clamp(int v, int min, int max) {
      return Math.max(min, Math.min(max, v));
   }

   private void resetButton(int y, int h, Runnable reset) {
      if (this.rowVisible(y, h)) {
         this.resetButtonAt(y, h, reset);
      }
   }

   private void resetButtonAt(int y, int h, Runnable reset) {
      if (y >= this.bodyTop && y + h <= this.bodyBottom) {
         this.addRenderableWidget(Button.builder(Component.translatable("btn.resetOne"), (b) -> {
            reset.run();
            this.requestRebuild();
         }).bounds(this.resetX(), y, 18, h).build());
      }
   }

   private void requestRebuild() {
      if (this.minecraft != null) {
         this.minecraft.tell(() -> {
            if (this.minecraft != null && this.minecraft.screen == this) {
               this.rebuildWidgets();
            }

         });
      }
   }

   private Button cycle(int x, int y, int w, int h, Supplier<Component> label, Runnable advance) {
      if (!this.rowVisible(y, h)) {
         return null;
      } else {
         Button[] self = new Button[1];
         Button button = Button.builder((Component)label.get(), (b) -> {
            advance.run();
            self[0].setMessage((Component)label.get());
         }).bounds(x, y, w, h).build();
         self[0] = button;
         this.addRenderableWidget(button);
         return button;
      }
   }

   private Button pinnedCycle(int x, int x1, int y, int h, Supplier<Component> label, Runnable advance) {
      if (y >= this.bodyTop && y + h <= this.bodyBottom) {
         Button[] self = new Button[1];
         Button button = Button.builder((Component)label.get(), (b) -> {
            advance.run();
            self[0].setMessage((Component)label.get());
         }).bounds(x, y, Math.max(40, x1 - x), h).build();
         self[0] = button;
         this.addRenderableWidget(button);
         return button;
      } else {
         return null;
      }
   }

   private void lengthSlider(int y, int h, String labelKey, float initial, float fallback, ClockConfigScreen.FloatSetter setter, Runnable reset) {
      if (this.rowVisible(y, h)) {
         this.addRenderableWidget(new ClockConfigScreen.LengthSlider(this.panelX + 8, y, this.contentW, h, labelKey, initial, fallback, setter));
         this.resetButton(y, h, reset);
      }
   }

   private void colorCell(int x, int y, int h, String key, IntSupplier current, IntConsumer setter) {
      if (this.rowVisible(y, h)) {
         int swatchW = 14;
         this.swatches.add(new ClockConfigScreen.Swatch(x, y + (h - 10) / 2, current));
         this.addRenderableWidget(Button.builder(Component.translatable(key), (b) -> this.openColor(key, current.getAsInt(), setter)).bounds(x + 14, y, this.colW - 14, h).build());
      }
   }

   private void openColor(String key, int rgb, IntConsumer setter) {
      if (this.minecraft != null) {
         this.minecraft.setScreen(new ColorPickerScreen(this, Component.translatable(key).getString(), rgb, setter::accept));
      }
   }

   private void resetAll() {
      this.cfg.timeSource = this.defaults.timeSource;
      this.cfg.glow = this.defaults.glow;
      this.cfg.hourLength = this.defaults.hourLength;
      this.cfg.minuteLength = this.defaults.minuteLength;
      this.cfg.secondLength = this.defaults.secondLength;
      this.cfg.faceRadius = this.defaults.faceRadius;
      this.cfg.tickRadius = this.defaults.tickRadius;
      this.cfg.showFace = this.defaults.showFace;
      this.cfg.showHour = this.defaults.showHour;
      this.cfg.showMinute = this.defaults.showMinute;
      this.cfg.showSecond = this.defaults.showSecond;
      this.cfg.showHub = this.defaults.showHub;
      this.cfg.faceColor = this.defaults.faceColor;
      this.cfg.hourColor = this.defaults.hourColor;
      this.cfg.minuteColor = this.defaults.minuteColor;
      this.cfg.secondColor = this.defaults.secondColor;
      this.cfg.hubColor = this.defaults.hubColor;
      this.cfg.tickColor = this.defaults.tickColor;
      this.cfg.chime = this.defaults.chime;
      this.cfg.chimeSound = this.defaults.chimeSound;
      this.cfg.chimeMode = this.defaults.chimeMode;
      this.cfg.tickStyle = this.defaults.tickStyle;
      this.cfg.redstone = this.defaults.redstone;
      this.cfg.offsetMinutes = this.defaults.offsetMinutes;
      this.offsetText = formatOffset(this.defaults.offsetMinutes);
      this.soundText = this.defaults.chimeSound;
      if (this.offsetField != null) {
         this.offsetField.setValue(this.offsetText);
      }

      if (this.soundField != null) {
         this.soundField.setValue(this.soundText);
      }

      this.enabled = false;
      this.requestRebuild();
   }

   private void applyClockText() {
      String text = this.clockField == null ? "" : this.clockField.getValue().trim();
      String[] parts = text.split(":");
      if (parts.length == 2) {
         Integer h = parseInt(parts[0]);
         Integer m = parseInt(parts[1]);
         if (h != null && m != null && h >= 0 && h <= 23 && m >= 0 && m <= 59) {
            double target = (double)h.intValue() * 3600.0D + (double)m.intValue() * 60.0D;
            double now = ClockTime.secondsOfDay(this.minecraft == null ? null : this.minecraft.level, this.cfg.timeSource, 0.0D);
            double delta = ClockTime.wrap(target - now);
            if (delta > 43200.0D) {
               delta -= 86400.0D;
            }

            this.cfg.offsetMinutes = delta / 60.0D;
            this.cfg.clamp();
            if (this.offsetField != null) {
               this.offsetField.setValue(formatOffset(this.cfg.offsetMinutes));
            }

         }
      }
   }

   private static String formatOffset(double minutes) {
      return String.format(Locale.ROOT, "%.1f", minutes);
   }

   private static Double parseDouble(String text) {
      try {
         return Double.valueOf(text.trim());
      } catch (NumberFormatException var2) {
         return null;
      }
   }

   private static Integer parseInt(String text) {
      try {
         return Integer.valueOf(text.trim());
      } catch (NumberFormatException var2) {
         return null;
      }
   }

   private void refreshSounds() {
      String q = this.soundField == null ? "" : this.soundField.getValue().trim().toLowerCase(Locale.ROOT);
      this.viewSounds.clear();

      for(ResourceLocation id : this.allSounds) {
         if (q.isEmpty() || id.toString().toLowerCase(Locale.ROOT).contains(q)) {
            this.viewSounds.add(id);
         }
      }

      this.soundScroll = Math.max(0, Math.min(this.soundScroll, this.maxSoundScroll()));
   }

   private int soundRowH() {
      return Math.max(10, this.rowH / 2);
   }

   private int soundListH() {
      return this.rowH * 2 - 12;
   }

   private int maxSoundScroll() {
      return Math.max(0, this.viewSounds.size() * this.soundRowH() - this.soundListH());
   }

   private void acceptTypedSound() {
      if (this.soundField != null) {
         ResourceLocation id = ResourceLocation.tryParse(this.soundField.getValue().trim());
         if (id != null && BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
            this.cfg.chimeSound = id.toString();
         }

      }
   }

   private void saveAndClose() {
      this.acceptTypedSound();
      this.cfg.clamp();
      if (this.global) {
         ClockClient.sendGlobalSave(new ClockGlobalConfig(this.cfg, this.enabled));
      } else if (this.pos != null) {
         ClockClient.sendSave(this.pos, this.cfg);
      }

      this.onClose();
   }

   private void preview() {
      this.acceptTypedSound();
      this.cfg.clamp();
      if (this.global) {
         ClockClient.sendGlobalSave(new ClockGlobalConfig(this.cfg, this.enabled));
         ClockClient.sendGlobalPreview();
      } else {
         ClockClient.sendSave(this.pos, this.cfg);
         ClockClient.sendPreview(this.pos);
      }

   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      context.blit(OPTIONS_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
      context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
      context.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, -535555052);
      context.renderOutline(this.panelX, this.panelY, this.panelW, this.panelH, -9539986);
      context.drawCenteredString(this.font, this.title, this.width / 2, this.panelY + 8, 16777215);
      context.enableScissor(this.panelX + 1, this.bodyTop, this.panelX + this.panelW - 1, Math.max(this.bodyTop, this.bodyBottom));
      super.render(context, mouseX, mouseY, delta);

      for(ClockConfigScreen.Swatch s : this.swatches) {
         if (s.y + 10 >= this.bodyTop && s.y <= this.bodyBottom) {
            context.fill(s.x, s.y, s.x + 10, s.y + 10, -16777216 | s.color.getAsInt() & 16777215);
            context.renderOutline(s.x, s.y, 10, 10, -16777216);
         }
      }

      this.drawSoundList(context, mouseX, mouseY);
      context.disableScissor();
      this.drawScrollBar(context);
      this.drawFooter(context, mouseX, mouseY, delta);
   }

   private void drawFooter(GuiGraphics context, int mouseX, int mouseY, float delta) {
      int top = this.panelY + this.panelH - 38;
      int bottom = this.panelY + this.panelH;
      context.fill(this.panelX + 1, top, this.panelX + this.panelW - 1, bottom - 1, -267119596);
      context.drawCenteredString(this.font, Component.translatable("screen.mtrlte.clock.sound.current", new Object[]{this.cfg.chimeSound}), this.width / 2, top + 4, 11579568);

      for(Button button : this.footerButtons) {
         button.render(context, mouseX, mouseY, delta);
      }

   }

   private void drawScrollBar(GuiGraphics context) {
      int max = this.maxScrollRows();
      if (max > 0) {
         int x = this.panelX + this.panelW - 8 + 3;
         int top = this.rowsTop;
         int h = this.bodyBottom - this.rowsTop;
         if (h > 0) {
            int total = this.rowCount;
            int barH = Math.max(12, h * this.visibleRows() / total);
            int barY = top + (h - barH) * this.scrollRows / max;
            context.fill(x, top, x + 2, top + h, 1090519039);
            context.fill(x, barY, x + 2, barY + barH, -4210753);
         }
      }
   }

   private int soundListTop() {
      return this.row(16);
   }

   private int soundListHeight() {
      return Math.max(0, Math.min(this.soundListH(), this.bodyBottom - this.soundListTop()));
   }

   private void drawSoundList(GuiGraphics context, int mouseX, int mouseY) {
      int x = this.panelX + 8;
      int y = this.soundListTop();
      int w = this.contentW;
      int h = this.soundListHeight();
      int rh = this.soundRowH();
      if (h > 0 && y < this.bodyBottom) {
         context.fill(x, y, x + w, y + h, 1610612736);
         context.renderOutline(x, y, w, h, -12961222);
         context.enableScissor(x, y, x + w, y + h);
         int ry = y - this.soundScroll;

         for(ResourceLocation id : this.viewSounds) {
            if (ry + rh > y && ry < y + h) {
               boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= Math.max(y, ry) && mouseY < Math.min(y + h, ry + rh);
               boolean selected = id.toString().equals(this.cfg.chimeSound);
               if (hovered) {
                  context.fill(x, ry, x + w, ry + rh, 822083583);
               }

               if (selected) {
                  context.fill(x, ry, x + 2, ry + rh, -13057458);
               }

               context.drawString(this.font, Component.literal(id.toString()), x + 6, ry + (rh - 8) / 2, selected ? 16777215 : 12566463);
            }

            ry += rh;
         }

         context.disableScissor();
         if (this.viewSounds.isEmpty()) {
            context.drawCenteredString(this.font, Component.translatable("screen.mtrlte.clock.sound.empty"), x + w / 2, y + h / 2 - 4, 9474192);
         }

      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (super.mouseClicked(mouseX, mouseY, button)) {
         return true;
      } else if (button != 0) {
         return false;
      } else {
         int x = this.panelX + 8;
         int y = this.soundListTop();
         int w = this.contentW;
         int h = this.soundListHeight();
         if (h > 0 && mouseX >= (double)x && mouseX < (double)(x + w) && mouseY >= (double)y && mouseY < (double)(y + h)) {
            int index = (int)((mouseY - (double)y + (double)this.soundScroll) / (double)this.soundRowH());
            if (index >= 0 && index < this.viewSounds.size()) {
               this.cfg.chimeSound = ((ResourceLocation)this.viewSounds.get(index)).toString();
               this.soundText = this.cfg.chimeSound;
               if (this.soundField != null) {
                  this.soundField.setValue(this.cfg.chimeSound);
               }

               return true;
            }
         }

         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      int max = this.maxSoundScroll();
      int x = this.panelX + 8;
      int y = this.soundListTop();
      if (max > 0 && mouseX >= (double)x && mouseX < (double)(x + this.contentW) && mouseY >= (double)y && mouseY < (double)(y + this.soundListHeight())) {
         this.soundScroll = (int)Math.max(0.0D, Math.min((double)max, (double)this.soundScroll - amount * (double)this.soundRowH() * 3.0D));
         return true;
      } else if (this.maxScrollRows() > 0 && mouseX >= (double)this.panelX && mouseX < (double)(this.panelX + this.panelW) && mouseY >= (double)this.bodyTop && mouseY < (double)this.bodyBottom) {
         int before = this.scrollRows;
         int step = amount > 0.0D ? 1 : -1;
         this.scrollRows = clamp(this.scrollRows + step, 0, this.maxScrollRows());
         if (this.scrollRows != before) {
            this.requestRebuild();
         }

         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, amount);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static Component onOff(boolean value) {
      return Component.translatable(value ? "screen.mtrlte.clock.on" : "screen.mtrlte.clock.off");
   }

   private static <E> E next(E[] values, E current) {
      for(int i = 0; i < values.length; ++i) {
         if (values[i] == current) {
            return values[(i + 1) % values.length];
         }
      }

      return values[0];
   }

   private interface FloatSetter {
      void set(float var1);
   }

   private static final class LengthSlider extends AbstractSliderButton {
      private final String labelKey;
      private final float fallback;
      private final ClockConfigScreen.FloatSetter setter;

      LengthSlider(int x, int y, int w, int h, String labelKey, float initial, float fallback, ClockConfigScreen.FloatSetter setter) {
         super(x, y, w, h, Component.translatable(labelKey, new Object[]{String.format(Locale.ROOT, "%.1f", ClockConfig.snapLength(initial, fallback))}), toSlider(ClockConfig.snapLength(initial, fallback)));
         this.labelKey = labelKey;
         this.fallback = fallback;
         this.setter = setter;
      }

      static double toSlider(float length) {
         return Math.log((double)(length / 0.1F)) / Math.log(500.0D);
      }

      float length() {
         return ClockConfig.snapLength((float)((double)0.1F * Math.pow(500.0D, this.value)), this.fallback);
      }

      protected void updateMessage() {
         if (this.labelKey != null) {
            this.setMessage(Component.translatable(this.labelKey, new Object[]{String.format(Locale.ROOT, "%.1f", this.length())}));
         }
      }

      protected void applyValue() {
         if (this.setter != null) {
            this.setter.set(this.length());
         }

      }
   }

   private static final class Swatch {
      final int x;
      final int y;
      final IntSupplier color;

      Swatch(int x, int y, IntSupplier color) {
         this.x = x;
         this.y = y;
         this.color = color;
      }
   }
}
