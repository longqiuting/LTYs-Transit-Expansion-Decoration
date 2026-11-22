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

public final class PidsXiamenAdPickScreen extends Screen {
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
   private static final int LIST_TOP = 20;
   private static final int ROW_H = 20;
   private static final int PITCH = 24;
   private static final int SIDE = 20;
   private static final int PAGER_W = 56;
   private final Screen parent;
   private final List<PidsXiamenAdPickScreen.Row> rows = new ArrayList();
   private Button prevPage;
   private Button nextPage;
   private int page;
   private int rowsPerPage = 1;
   private int listW;
   private boolean snapToSelection = true;

   public PidsXiamenAdPickScreen(Screen parent) {
      super(tr("gui.mtrlte.pids_xiamen.ad_override_title"));
      this.parent = parent;
      this.buildRows();
   }

   private static MutableComponent tr(String key, Object... args) {
      return Component.translatable(key, args);
   }

   private void buildRows() {
      this.rows.clear();
      this.rows.add(new PidsXiamenAdPickScreen.Row(tr("gui.mtrlte.pids_xiamen.ad_override_none"), (String)null));

      try {
         for(PidsXiamenAds.Category category : PidsXiamenAds.categories()) {
            Component name = (Component)(category.name() == null ? Component.literal(category.key()) : category.name());

            for(PidsXiamenAds.Ad ad : category.ads()) {
               if (ad.image() != null) {
                  String image = ad.image().toString();
                  if (this.indexOf(image) < 0) {
                     MutableComponent label = name.copy().append(Component.literal(" \u00b7 "));
                     label.append((Component)(ad.text() != null && !ad.text().getString().isBlank() ? ad.text() : Component.literal(image)));
                     this.rows.add(new PidsXiamenAdPickScreen.Row(label, image));
                  }
               }
            }
         }
      } catch (Throwable var8) {
         LteLog.LOGGER.warn("[lte] xiamen pids: could not list the advertisements for the global override", var8);
      }

   }

   private int indexOf(String image) {
      for(int i = 0; i < this.rows.size(); ++i) {
         String candidate = ((PidsXiamenAdPickScreen.Row)this.rows.get(i)).image();
         if (candidate != null && candidate.equals(image)) {
            return i;
         }
      }

      return -1;
   }

   protected void init() {
      this.listW = Math.max(80, this.width - 40);
      int doneY = this.height - 40;
      int pagerY = doneY - 28;
      int listBottom = pagerY - 16;
      this.rowsPerPage = Math.max(1, (listBottom - 20) / 24);
      int pages = Math.max(1, (this.rows.size() + this.rowsPerPage - 1) / this.rowsPerPage);
      if (this.snapToSelection) {
         this.snapToSelection = false;
         String current = PidsXiamenSettings.adOverrideImage();
         if (pages > 1 && current != null) {
            int index = this.indexOf(current);
            if (index >= 0) {
               this.page = index / this.rowsPerPage;
            }
         }
      }

      this.page = Math.min(this.page, pages - 1);
      String current = PidsXiamenSettings.adOverrideImage();
      int from = this.page * this.rowsPerPage;
      int to = Math.min(this.rows.size(), from + this.rowsPerPage);
      int y = 20;

      for(int i = from; i < to; ++i) {
         PidsXiamenAdPickScreen.Row row = (PidsXiamenAdPickScreen.Row)this.rows.get(i);
         boolean selected = row.image() == null ? current == null : row.image().equals(current);
         Component label = (Component)(selected ? tr("gui.mtrlte.pids_xiamen.ad_override_row_selected", row.label()) : row.label());
         int index = i;
         this.addRenderableWidget(Button.builder(label, (b) -> this.choose((PidsXiamenAdPickScreen.Row)this.rows.get(index))).bounds(20, y, this.listW, 20).build());
         y += 24;
      }

      this.addRenderableWidget(Button.builder(tr("gui.done"), (b) -> this.back()).bounds(this.width / 2 - 72, doneY, 144, 20).build());
      if (pages > 1) {
         this.prevPage = Button.builder(tr("gui.mtrlte.pids_xiamen.page_prev"), (b) -> this.turnPage(-1)).bounds(20, pagerY, 56, 20).build();
         this.prevPage.active = this.page > 0;
         this.addRenderableWidget(this.prevPage);
         this.nextPage = Button.builder(tr("gui.mtrlte.pids_xiamen.page_next"), (b) -> this.turnPage(1)).bounds(this.width - 20 - 56, pagerY, 56, 20).build();
         this.nextPage.active = this.page < pages - 1;
         this.addRenderableWidget(this.nextPage);
      }

   }

   private void turnPage(int delta) {
      this.page = Math.max(0, this.page + delta);
      this.rebuildWidgets();
   }

   private void choose(PidsXiamenAdPickScreen.Row row) {
      PidsXiamenSettings.setAdOverrideImage(row.image());
      this.back();
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
      context.drawCenteredString(this.font, tr("gui.mtrlte.pids_xiamen.ad_override_hint"), this.width / 2, this.height - 14, 9474192);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      this.back();
   }

   private static record Row(Component label, String image) {
   }
}
