package dev.js.mtrheadlight.xiamen.pids;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class PidsXiamenSettingsScreen extends Screen {
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
   private static final int ROW_H = 20;
   private static final int PITCH = 25;
   private static final int GAP = 8;
   private static final int MAX_COLUMN = 320;
   private static final int ROWS = 4;
   private final Screen parent;
   private int colX;
   private int colW;
   private int listTop;

   public PidsXiamenSettingsScreen(Screen parent) {
      super(tr("screen.mtrlte.pids_xiamen"));
      this.parent = parent;
   }

   private static MutableComponent tr(String key, Object... args) {
      return Component.translatable(key, args);
   }

   protected void init() {
      this.colW = Math.min(320, this.width - 48);
      this.colX = this.width / 2 - this.colW / 2;
      int block = 95;
      int centered = (this.height - 95) / 2 - 10;
      int maxTop = this.height - 34 - 8 - 95;
      this.listTop = Math.max(58, Math.min(centered, maxTop));
      int y = this.listTop;
      this.addButton(y, () -> tr(PidsXiamenSettings.localTime() ? "opt.xpids.localTime.on" : "opt.xpids.localTime.off"), (b) -> {
         PidsXiamenSettings.setLocalTime(!PidsXiamenSettings.localTime());
         this.reinit();
      });
      y += 25;
      this.addButton(y, () -> tr(PidsXiamenSettings.arriving() ? "opt.xpids.arriving.on" : "opt.xpids.arriving.off"), (b) -> {
         PidsXiamenSettings.setArriving(!PidsXiamenSettings.arriving());
         this.reinit();
      });
      y += 25;
      this.addButton(y, () -> tr(PidsXiamenSettings.advertisements() ? "opt.xpids.ads.on" : "opt.xpids.ads.off"), (b) -> {
         PidsXiamenSettings.setAdvertisements(!PidsXiamenSettings.advertisements());
         this.reinit();
      });
      y += 25;
      this.addButton(y, PidsXiamenSettingsScreen::adOverrideLabel, (b) -> this.openAdPicker());
      int fy = this.height - 34;
      this.addRenderableWidget(Button.builder(tr("btn.back"), (b) -> this.back()).bounds(this.colX, fy, this.colW, 20).build());
   }

   private static Component adOverrideLabel() {
      String image = PidsXiamenSettings.adOverrideImage();
      if (image == null) {
         return tr("opt.xpids.adOverride.none");
      } else {
         PidsXiamenAds.Ad ad = PidsXiamenSettings.adOverride();
         if (ad == null) {
            return tr("opt.xpids.adOverride.missing", image);
         } else {
            Component caption = (Component)(ad.text() != null && !ad.text().getString().isBlank() ? ad.text() : Component.literal(image));
            return tr("opt.xpids.adOverride.on", caption);
         }
      }
   }

   private void openAdPicker() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(new PidsXiamenAdPickScreen(this));
      }

   }

   private void addButton(int y, Supplier<Component> label, Button.OnPress action) {
      this.addRenderableWidget(Button.builder((Component)label.get(), action).bounds(this.colX, y, this.colW, 20).build());
   }

   private void reinit() {
      this.rebuildWidgets();
   }

   private void back() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }

   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      context.blit(OPTIONS_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
      context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredString(this.font, this.title, this.width / 2, 22, 16777215);
      context.drawCenteredString(this.font, tr("gui.mtrlte.pids_xiamen.global_hint"), this.width / 2, 38, 11579568);
      context.drawCenteredString(this.font, tr("hint.author"), this.width / 2, this.height - 14, 11579568);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      this.back();
   }
}
