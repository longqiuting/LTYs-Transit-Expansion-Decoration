package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class PidsXiamenAdSetPickScreen extends Screen {
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
   private static final int LIST_TOP = 34;
   private static final int ROW_H = 20;
   private static final int PITCH = 24;
   private static final int SIDE = 20;
   private static final int PAGER_W = 56;
   private final Screen parent;
   private final List<PidsXiamenAdSetPickScreen.Row> rows = new ArrayList();
   private int page;
   private int rowsPerPage = 1;
   private int listW;

   public PidsXiamenAdSetPickScreen(Screen parent) {
      super(tr("screen.mtrlte.pids_ad_set.pick"));
      this.parent = parent;
      this.buildRows();
   }

   private static MutableComponent tr(String key, Object... args) {
      return Component.translatable(key, args);
   }

   private void buildRows() {
      this.rows.clear();

      try {
         for(PidsXiamenAds.Category category : PidsXiamenAds.categories()) {
            Component name = (Component)(category.name() == null ? Component.literal(category.key()) : category.name());

            for(PidsXiamenAds.Ad ad : category.ads()) {
               if (ad.image() != null) {
                  String image = ad.image().toString();
                  if (this.indexOf(image) < 0) {
                     MutableComponent label = name.copy().append(Component.literal(" \u00b7 "));
                     label.append((Component)(ad.text() != null && !ad.text().getString().isBlank() ? ad.text() : Component.literal(image)));
                     this.rows.add(new PidsXiamenAdSetPickScreen.Row(image, label));
                  }
               }
            }
         }
      } catch (Throwable var8) {
         LteLog.LOGGER.warn("[lte] xiamen pids ad set: could not list the advertisements", var8);
      }

   }

   private int indexOf(String image) {
      for(int i = 0; i < this.rows.size(); ++i) {
         if (image.equals(((PidsXiamenAdSetPickScreen.Row)this.rows.get(i)).image())) {
            return i;
         }
      }

      return -1;
   }

   protected void init() {
      this.listW = Math.max(80, this.width - 40);
      int doneY = this.height - 40;
      int pagerY = doneY - 28;
      int listBottom = pagerY - 12;
      this.rowsPerPage = Math.max(1, (listBottom - 34) / 24);
      int pages = Math.max(1, (this.rows.size() + this.rowsPerPage - 1) / this.rowsPerPage);
      this.page = Math.max(0, Math.min(this.page, pages - 1));
      int from = this.page * this.rowsPerPage;
      int to = Math.min(this.rows.size(), from + this.rowsPerPage);
      int y = 34;

      for(int i = from; i < to; ++i) {
         PidsXiamenAdSetPickScreen.Row row = (PidsXiamenAdSetPickScreen.Row)this.rows.get(i);
         boolean selected = PidsAdSetCache.get().contains(row.image());
         Component label = (Component)(selected ? tr("gui.mtrlte.pids_ad_set.pick_selected", row.label()) : row.label());
         this.addRenderableWidget(Button.builder(label, (b) -> this.toggle(row.image())).bounds(20, y, this.listW, 20).build());
         y += 24;
      }

      this.addRenderableWidget(Button.builder(tr("gui.done"), (b) -> this.back()).bounds(this.width / 2 - 72, doneY, 144, 20).build());
      if (pages > 1) {
         Button prev = Button.builder(tr("gui.mtrlte.pids_xiamen.page_prev"), (b) -> this.turnPage(-1)).bounds(20, pagerY, 56, 20).build();
         prev.active = this.page > 0;
         this.addRenderableWidget(prev);
         Button next = Button.builder(tr("gui.mtrlte.pids_xiamen.page_next"), (b) -> this.turnPage(1)).bounds(this.width - 20 - 56, pagerY, 56, 20).build();
         next.active = this.page < pages - 1;
         this.addRenderableWidget(next);
      }

   }

   private void turnPage(int delta) {
      this.page = Math.max(0, this.page + delta);
      this.rebuildWidgets();
   }

   private void toggle(String image) {
      PidsAdSet next = PidsAdSetCache.get();
      if (next.contains(image)) {
         next.remove(image);
      } else {
         next.add(image);
      }

      PidsAdSetCache.save(next);
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
      context.drawCenteredString(this.font, this.title, this.width / 2, 10, 16777215);
      context.drawCenteredString(this.font, tr("gui.mtrlte.pids_ad_set.count", PidsAdSetCache.get().size()), this.width / 2, 22, 11579568);
      context.drawCenteredString(this.font, tr("gui.mtrlte.pids_ad_set.pick_hint"), this.width / 2, this.height - 14, 9474192);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      this.back();
   }

   private static record Row(String image, Component label) {
   }
}
