package dev.js.mtrheadlight;

import dev.js.mtrheadlight.decor.DecorConfig;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class ColorPickerScreen extends Screen {
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
   private static final int COLS = 8;
   private static final int ROWS = 5;
   private static final int CELL = 16;
   private static final int CELL_GAP = 4;
   private static final int PAD = 8;
   private static final int BTN_H = 20;
   private static final int GAP = 8;
   private static final int PREVIEW_H = 12;
   private static final int HEX_H = 18;
   private static final int FRAME = -16777216;
   private static final int FRAME_IDLE = -12961222;
   private static final int FRAME_HOVER = -3618616;
   private static final int FRAME_SELECTED = -1;
   private static final int[] PALETTE = new int[]{16774102, 16775399, 16773060, 16769704, 16765066, 16759654, 16752202, 16738906, 16747136, 16732754, 15022389, 12986408, 9313051, 15753874, 16728193, 14162784, 16757504, 16766287, 16773494, 14477173, 10289064, 5036388, 3055183, 1802043, 15267071, 10475775, 5227511, 2733814, 2201331, 1402304, 8146431, 11766015, 16777215, 13684944, 10395294, 7171437, 4342338, 2171169, 9268835, 6111287};
   private static final int CONTENT_TOP = 48;
   private final Screen parentScreen;
   private final IntConsumer onDone;
   private int selected;
   private int cell = 16;
   private int gridX;
   private int gridY;
   private int gridW;
   private int gridH;
   private int previewY;
   private int hexY;
   private int buttonY;
   private EditBox hexField;
   private boolean hexInvalid;

   static MutableComponent tr(String key, Object... args) {
      return Component.translatable(key, args);
   }

   public ColorPickerScreen(Screen parentScreen, String label, int initialRgb, IntConsumer onDone) {
      super(tr("color.title", label));
      this.parentScreen = parentScreen;
      this.onDone = onDone;
      this.selected = initialRgb & 16777215;
   }

   protected void init() {
      int byWidth = (this.width - 40 - 28) / 8;
      int byHeight = (this.height - 160 - 16) / 5;
      this.cell = Math.max(10, Math.min(16, Math.min(byWidth, byHeight)));
      this.gridW = 8 * this.cell + 28;
      this.gridH = 5 * this.cell + 16;
      this.gridX = (this.width - this.gridW) / 2;
      int blockH = 38 + this.gridH + 8 + 18 + 14 + 20;
      int top = Math.max(48, (this.height - blockH) / 2);
      this.previewY = top;
      this.gridY = this.previewY + 12 + 10 + 8;
      this.hexY = this.gridY + this.gridH + 8 + 8;
      this.buttonY = this.hexY + 18 + 14;
      int labelW = this.font.width(tr("color.hex").getString()) + 8;
      this.hexField = new EditBox(this.font, this.gridX + labelW, this.hexY, this.gridW - labelW, 18, tr("color.hex"));
      this.hexField.setMaxLength(7);
      this.hexField.setFilter((text) -> text.matches("#?[0-9a-fA-F]{0,6}"));
      this.hexField.setValue(DecorConfig.formatColor(this.selected));
      this.hexField.setResponder((text) -> {
         Integer rgb = parseHex(text);
         if (rgb != null) {
            this.selected = rgb;
            this.hexInvalid = false;
         }

      });
      this.addRenderableWidget(this.hexField);
      int bw = Math.max(64, Math.min(96, (this.gridW - 8) / 2));
      int bx = (this.width - (bw * 2 + 8)) / 2;
      this.addButton(bx, this.buttonY, bw, 20, tr("btn.done"), (button) -> this.complete());
      this.addButton(bx + bw + 8, this.buttonY, bw, 20, tr("btn.cancel"), (button) -> this.onClose());
   }

   private boolean applyHex() {
      Integer rgb = this.hexField == null ? null : parseHex(this.hexField.getValue());
      if (rgb == null) {
         this.hexInvalid = true;
         return false;
      } else {
         this.selected = rgb;
         this.hexInvalid = false;
         return true;
      }
   }

   static Integer parseHex(String text) {
      if (text == null) {
         return null;
      } else {
         String s = text.trim();
         if (s.startsWith("#")) {
            s = s.substring(1);
         }

         if (s.length() != 6) {
            return null;
         } else {
            try {
               return Integer.parseInt(s, 16) & 16777215;
            } catch (NumberFormatException var3) {
               return null;
            }
         }
      }
   }

   private void complete() {
      if (this.applyHex()) {
         if (this.onDone != null) {
            this.onDone.accept(this.selected & 16777215);
         }

         this.onClose();
      }
   }

   private int colorAt(double mouseX, double mouseY) {
      for(int i = 0; i < PALETTE.length; ++i) {
         int x = this.gridX + i % 8 * (this.cell + 4);
         int y = this.gridY + i / 8 * (this.cell + 4);
         if (mouseX >= (double)x && mouseX < (double)(x + this.cell) && mouseY >= (double)y && mouseY < (double)(y + this.cell)) {
            return PALETTE[i] & 16777215;
         }
      }

      return -1;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int hit = this.colorAt(mouseX, mouseY);
         if (hit >= 0) {
            this.selected = hit;
            this.hexInvalid = false;
            if (this.hexField != null) {
               this.hexField.setValue(DecorConfig.formatColor(hit));
            }

            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.hexField != null && this.hexField.isFocused() && (keyCode == 257 || keyCode == 335)) {
         if (this.applyHex() && this.hexField != null) {
            this.hexField.setValue(DecorConfig.formatColor(this.selected));
         }

         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private void drawCentered(GuiGraphics context, Component text, int cx, int y, int color) {
      context.drawCenteredString(this.font, text, cx, y, color);
   }

   private static int luminance(int rgb) {
      return ((rgb >> 16 & 255) * 299 + (rgb >> 8 & 255) * 587 + (rgb & 255) * 114) / 1000;
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      context.blit(OPTIONS_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
      context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
      super.render(context, mouseX, mouseY, delta);
      this.drawCentered(context, this.title, this.width / 2, 20, 16777215);
      this.drawCentered(context, tr("color.hint"), this.width / 2, 34, 11579568);
      int rgb = this.selected & 16777215;
      context.fill(this.gridX - 1, this.previewY - 1, this.gridX + this.gridW + 1, this.previewY + 12 + 1, -16777216);
      context.fill(this.gridX, this.previewY, this.gridX + this.gridW, this.previewY + 12, -16777216 | rgb);
      this.drawCentered(context, Component.literal(DecorConfig.formatColor(rgb)), this.gridX + this.gridW / 2, this.previewY + 2, luminance(rgb) > 140 ? -15724528 : -1);
      int px = this.gridX - 8;
      int py = this.gridY - 8;
      int pr = this.gridX + this.gridW + 8;
      int pb = this.gridY + this.gridH + 8;
      context.fill(px, py, pr, pb, -1072689136);
      context.fill(px, py, pr, py + 1, -10855846);
      context.fill(px, pb - 1, pr, pb, -10855846);
      context.fill(px, py, px + 1, pb, -10855846);
      context.fill(pr - 1, py, pr, pb, -10855846);

      for(int i = 0; i < PALETTE.length; ++i) {
         int swatch = PALETTE[i] & 16777215;
         int x = this.gridX + i % 8 * (this.cell + 4);
         int y = this.gridY + i / 8 * (this.cell + 4);
         boolean isSelected = swatch == rgb;
         boolean hovered = mouseX >= x && mouseX < x + this.cell && mouseY >= y && mouseY < y + this.cell;
         int ring = isSelected ? -1 : (hovered ? -3618616 : -12961222);
         context.fill(x - 2, y - 2, x + this.cell + 2, y + this.cell + 2, -16777216);
         context.fill(x - 1, y - 1, x + this.cell + 1, y + this.cell + 1, ring);
         context.fill(x, y, x + this.cell, y + this.cell, -16777216 | swatch);
      }

      context.drawString(this.font, tr("color.hex"), this.gridX, this.hexY + 5, 16777215);
      if (this.hexInvalid) {
         this.drawCentered(context, tr("color.hexInvalid"), this.gridX + this.gridW / 2, this.hexY + 18 + 2, 16733525);
      }

   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parentScreen);
      }

   }

   private Button addButton(int x, int y, int w, int h, Component label, Button.OnPress action) {
      return (Button)this.addRenderableWidget(Button.builder(label, action).bounds(x, y, w, h).build());
   }
}
