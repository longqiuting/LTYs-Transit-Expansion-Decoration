package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import mtr.client.IDrawing;
import mtr.mappings.ScreenMapper;
import mtr.screen.WidgetBetterCheckbox;
import dev.js.mtrheadlight.forge.platform.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PidsXiamenAdsScreen extends ScreenMapper {
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
   private static final int LIST_TOP = 20;
   private static final int ROW_H = 20;
   private static final int PITCH = 24;
   private static final int SIDE = 20;
   private static final int BUTTON_W = 144;
   private static final int PAGER_W = 56;
   private final BlockPos dataPos;
   private final ScreenMapper parent;
   private final List<PidsXiamenAds.Category> categories = new ArrayList();
   private final Set<Long> selected = new LinkedHashSet();
   private final List<WidgetBetterCheckbox> boxes = new ArrayList();
   private Button prevPage;
   private Button nextPage;
   private int page;
   private int rowsPerPage = 1;
   private int listW;

   public PidsXiamenAdsScreen(BlockPos dataPos, ScreenMapper parent) {
      super(tr("gui.mtrlte.pids_xiamen.select_ads"));
      this.dataPos = dataPos;
      this.parent = parent;
      this.readCategories();
      this.readSelection();
   }

   private static MutableComponent tr(String key, Object... args) {
      return Component.translatable(key, args);
   }

   private void readCategories() {
      this.categories.clear();

      try {
         this.categories.addAll(PidsXiamenAds.categories());
      } catch (Throwable var2) {
         LteLog.LOGGER.warn("[lte] xiamen pids: could not read the advertisement categories", var2);
      }

   }

   private void readSelection() {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client == null || client.level == null) {
            return;
         }

         BlockEntity var3 = client.level.getBlockEntity(this.dataPos);
         if (var3 instanceof PidsXiamenBlockEntity pids) {
            for(long id : pids.getAdCategories()) {
               this.selected.add(id);
            }
         }
      } catch (Throwable var8) {
         LteLog.LOGGER.warn("[lte] xiamen pids: could not read the selected categories", var8);
      }

   }

   protected void init() {
      this.boxes.clear();
      this.readCategories();
      this.listW = Math.max(80, this.width - 40);
      int doneY = this.height - 40;
      int importY = doneY - 28;
      int listBottom = importY - 16;
      this.rowsPerPage = Math.max(1, (listBottom - 20) / 24);
      int pages = Math.max(1, (this.categories.size() + this.rowsPerPage - 1) / this.rowsPerPage);
      this.page = Math.min(this.page, pages - 1);
      int from = this.page * this.rowsPerPage;
      int to = Math.min(this.categories.size(), from + this.rowsPerPage);
      int y = 20;

      for(int i = from; i < to; ++i) {
         PidsXiamenAds.Category category = (PidsXiamenAds.Category)this.categories.get(i);
         WidgetBetterCheckbox box = new WidgetBetterCheckbox(0, 0, this.listW, 20, (Component)(category.name() == null ? Component.literal(category.key()) : category.name()), (checked) -> {
            if (checked) {
               this.selected.add(category.id());
            } else {
               this.selected.remove(category.id());
            }

         });
         IDrawing.setPositionAndWidth(box, 20, y, this.listW);
         box.setChecked(this.selected.contains(category.id()));

         try {
            box.setTooltip(Tooltip.create(tr("gui.mtrlte.pids_xiamen.ad_category_tip", category.description() == null ? Component.empty() : category.description(), category.ads().size())));
         } catch (Throwable var12) {
         }

         this.boxes.add(box);
         this.addDrawableChild(box);
         y += 24;
      }

      this.addDrawableChild(Button.builder(tr("gui.mtrlte.pids_xiamen.import_pack"), (b) -> this.openPackScreen()).bounds(this.width / 2 - 72, importY, 144, 20).build());
      this.addDrawableChild(Button.builder(tr("gui.done"), (b) -> this.saveAndClose()).bounds(this.width / 2 - 72, doneY, 144, 20).build());
      if (pages > 1) {
         this.prevPage = Button.builder(tr("gui.mtrlte.pids_xiamen.page_prev"), (b) -> this.turnPage(-1)).bounds(20, importY, 56, 20).build();
         this.prevPage.active = this.page > 0;
         this.addDrawableChild(this.prevPage);
         this.nextPage = Button.builder(tr("gui.mtrlte.pids_xiamen.page_next"), (b) -> this.turnPage(1)).bounds(this.width - 20 - 56, importY, 56, 20).build();
         this.nextPage.active = this.page < pages - 1;
         this.addDrawableChild(this.nextPage);
      }

   }

   private void turnPage(int delta) {
      this.page = Math.max(0, this.page + delta);
      this.rebuildWidgets();
   }

   private void openPackScreen() {
      Minecraft client = Minecraft.getInstance();
      if (client != null) {
         PidsXiamenAdsScreen self = this;

         try {
            client.setScreen(new PackSelectionScreen(client.getResourcePackRepository(), (manager) -> {
               try {
                  client.options.updateResourcePacks(manager);
               } catch (Throwable var4) {
                  LteLog.LOGGER.warn("[lte] xiamen pids: could not apply the resource packs", var4);
               }

               PidsXiamenAds.invalidate();
               PidsXiamenTicker.invalidate();
               client.setScreen(self);
            }, client.getResourcePackDirectory(), Component.translatable("resourcePack.title")));
         } catch (Throwable var4) {
            LteLog.LOGGER.warn("[lte] xiamen pids: could not open the resource pack screen", var4);
            if (client.player != null) {
               client.player.displayClientMessage(tr("gui.mtrlte.pids_xiamen.import_pack_failed"), false);
            }
         }

      }
   }

   private long[] selectedIds() {
      long[] ids = new long[this.selected.size()];
      int i = 0;

      for(Long id : this.selected) {
         ids[i++] = id;
      }

      return ids;
   }

   private void apply() {
      long[] ids = this.selectedIds();

      try {
         Minecraft client = Minecraft.getInstance();
         if (client != null && client.level != null) {
            BlockEntity var4 = client.level.getBlockEntity(this.dataPos);
            if (var4 instanceof PidsXiamenBlockEntity) {
               PidsXiamenBlockEntity pids = (PidsXiamenBlockEntity)var4;
               pids.setAdCategories(ids);
            }
         }

         if (Net.canSend(PidsXiamenNet.ID)) {
            Net.sendToServer(PidsXiamenNet.ID, PidsXiamenNet.categories(this.dataPos, ids));
         }
      } catch (Throwable var5) {
         LteLog.LOGGER.warn("[lte] xiamen pids: could not send the advertisement categories", var5);
      }

   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      context.blit(OPTIONS_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
      context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
      context.drawCenteredString(this.font, this.title, this.width / 2, 10, 16777215);
      if (this.categories.isEmpty()) {
         context.drawCenteredString(this.font, tr("gui.mtrlte.pids_xiamen.no_ad_categories"), this.width / 2, this.height / 2 - 20, 16752029);
         context.drawCenteredString(this.font, tr("gui.mtrlte.pids_xiamen.no_ad_categories_hint"), this.width / 2, this.height / 2 - 6, 9474192);
      }

      super.render(context, mouseX, mouseY, delta);
      int adCount = this.countAds();
      context.drawCenteredString(this.font, tr("gui.mtrlte.pids_xiamen.ad_selected", this.selected.size(), adCount), this.width / 2, this.height - 80, this.selected.isEmpty() ? 9474192 : 11579568);
      context.drawCenteredString(this.font, tr("gui.mtrlte.pids_xiamen.ads_source"), this.width / 2, this.height - 14, 7368816);
   }

   private int countAds() {
      try {
         return PidsXiamenAds.adsFor(this.selectedIds()).size();
      } catch (Throwable var2) {
         return 0;
      }
   }

   private void saveAndClose() {
      this.apply();
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }

   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }

   }
}
