package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class PidsXiamenAdSetScreen extends Screen {
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
   private static final int SIDE = 20;
   private static final int ROW_H = 20;
   private static final int PITCH = 24;
   private static final int LIST_TOP = 92;
   private static final int REMOVE_W = 24;
   private static final int PAGER_W = 56;
   private final Screen parent;
   private int page;
   private int rowsPerPage = 1;
   private int listW;

   public PidsXiamenAdSetScreen(Screen parent) {
      super(tr("screen.mtrlte.pids_ad_set"));
      this.parent = parent;
   }

   private static MutableComponent tr(String key, Object... args) {
      return Component.translatable(key, args);
   }

   protected void init() {
      this.listW = Math.max(120, this.width - 40);
      int footerY = this.height - 34;
      int pagerY = footerY - 28;
      int listBottom = pagerY - 8;
      this.rowsPerPage = Math.max(1, (listBottom - 92) / 24);
      PidsAdSet set = PidsAdSetCache.get();
      List<String> images = set.images();
      int pages = Math.max(1, (images.size() + this.rowsPerPage - 1) / this.rowsPerPage);
      this.page = Math.max(0, Math.min(this.page, pages - 1));
      this.addRenderableWidget(Button.builder(toggleLabel(set), (b) -> {
         PidsAdSet next = PidsAdSetCache.get();
         next.setEnabled(!next.enabled());
         PidsAdSetCache.save(next);
         this.reinit();
      }).bounds(20, 34, this.listW, 20).build());
      this.addRenderableWidget(Button.builder(tr("btn.pids_ad_set.add"), (b) -> this.openPicker()).bounds(20, 58, this.listW, 20).build());
      int from = this.page * this.rowsPerPage;
      int to = Math.min(images.size(), from + this.rowsPerPage);
      int y = 92;

      for(int i = from; i < to; ++i) {
         String image = (String)images.get(i);
         this.addRenderableWidget(Button.builder(tr("btn.pids_ad_set.remove"), (b) -> {
            PidsAdSet next = PidsAdSetCache.get();
            next.remove(image);
            PidsAdSetCache.save(next);
            this.reinit();
         }).bounds(this.width - 20 - 24, y, 24, 20).build());
         y += 24;
      }

      if (pages > 1) {
         Button prev = Button.builder(tr("gui.mtrlte.pids_xiamen.page_prev"), (b) -> this.turnPage(-1)).bounds(20, pagerY, 56, 20).build();
         prev.active = this.page > 0;
         this.addRenderableWidget(prev);
         Button next = Button.builder(tr("gui.mtrlte.pids_xiamen.page_next"), (b) -> this.turnPage(1)).bounds(this.width - 20 - 56, pagerY, 56, 20).build();
         next.active = this.page < pages - 1;
         this.addRenderableWidget(next);
      }

      int half = (this.listW - 8) / 2;
      Button clear = Button.builder(tr("btn.pids_ad_set.clear"), (b) -> {
         PidsAdSet next = PidsAdSetCache.get();
         next.clear();
         PidsAdSetCache.save(next);
         this.reinit();
      }).bounds(20, footerY, half, 20).build();
      clear.active = !images.isEmpty();
      this.addRenderableWidget(clear);
      this.addRenderableWidget(Button.builder(tr("btn.back"), (b) -> this.back()).bounds(20 + half + 8, footerY, half, 20).build());
   }

   private static Component toggleLabel(PidsAdSet set) {
      if (!set.enabled()) {
         return tr("opt.pids_ad_set.off");
      } else {
         return set.size() == 0 ? tr("opt.pids_ad_set.empty") : tr("opt.pids_ad_set.on", set.size());
      }
   }

   private static Component rowLabel(int index, String image) {
      PidsXiamenAds.Ad ad = PidsXiamenAds.adWithImage(image);
      Component caption;
      if (ad == null) {
         caption = tr("gui.mtrlte.pids_ad_set.missing", image);
      } else if (ad.text() != null && !ad.text().getString().isBlank()) {
         caption = ad.text();
      } else {
         caption = Component.literal(image);
      }

      return tr("gui.mtrlte.pids_ad_set.row", index + 1, caption);
   }

   private void turnPage(int delta) {
      this.page = Math.max(0, this.page + delta);
      this.rebuildWidgets();
   }

   private void openPicker() {
      if (this.minecraft != null) {
         try {
            this.minecraft.setScreen(new PidsXiamenAdSetPickScreen(this));
         } catch (Throwable var2) {
            LteLog.LOGGER.warn("[lte] xiamen pids ad set: could not open the picker", var2);
         }
      }

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
      PidsAdSet set = PidsAdSetCache.get();
      String count = tr("gui.mtrlte.pids_ad_set.count", set.size()).getString();
      context.drawCenteredString(this.font, this.title, this.width / 2, 10, 16777215);
      context.drawCenteredString(this.font, tr("gui.mtrlte.pids_ad_set.hint"), this.width / 2, 22, 11579568);
      if (set.size() == 0) {
         context.drawCenteredString(this.font, tr("gui.mtrlte.pids_ad_set.empty"), this.width / 2, 98, 9474192);
      } else {
         context.drawString(this.font, Component.literal(count), 20, 80, 10526880);
         int from = this.page * this.rowsPerPage;
         int to = Math.min(set.size(), from + this.rowsPerPage);
         int maxW = this.listW - 24 - 12;

         for(int i = from; i < to; ++i) {
            String text = this.font.plainSubstrByWidth(rowLabel(i, (String)set.images().get(i)).getString(), maxW);
            context.drawString(this.font, Component.literal(text), 24, 92 + (i - from) * 24 + 6, 16777215);
         }
      }

      context.drawCenteredString(this.font, tr("hint.author"), this.width / 2, this.height - 14, 11579568);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      this.back();
   }
}
