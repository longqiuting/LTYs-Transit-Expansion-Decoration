package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import java.util.HashSet;
import java.util.Set;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.RailwayData;
import mtr.mappings.ScreenMapper;
import mtr.packet.PacketTrainDataGuiClient;
import mtr.screen.PIDSConfigScreen;
import mtr.screen.WidgetBetterCheckbox;
import mtr.screen.WidgetBetterTextField;
import mtr.screen.WidgetBetterTextField.TextFieldFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PidsXiamenConfigScreen extends ScreenMapper {
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
   private static final int ROW_H = 20;
   private static final int MAX_COLUMN = 360;
   private static final int FOOTER_H = 34;
   private final BlockPos dataPos;
   private final BlockPos partnerPos;
   private final Set<Long> platformIds = new HashSet();
   private final Set<Long> initialPlatforms = new HashSet();
   private int displayPage;
   private int initialPage;
   private long[] adCategories = new long[0];
   private int adCount;
   private WidgetBetterCheckbox selectAll;
   private WidgetBetterCheckbox lineStripBox;
   private Button filterButton;
   private Button adsButton;
   private WidgetBetterTextField pageInput;
   private boolean hasStation;
   private int colX;
   private int colW;
   private int stripY;
   private int platformY;
   private int filterY;
   private int noStationY;
   private int pageY;
   private int adsY;

   public PidsXiamenConfigScreen(BlockPos dataPos, BlockPos partnerPos) {
      super(tr("gui.mtrlte.pids_xiamen.config_title"));
      this.dataPos = dataPos;
      this.partnerPos = partnerPos;
      this.readEntity();
   }

   private static MutableComponent tr(String key, Object... args) {
      return Component.translatable(key, args);
   }

   private void readEntity() {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client.level != null) {
            BlockEntity live = client.level.getBlockEntity(this.dataPos);
            if (live instanceof PidsXiamenBlockEntity) {
               PidsXiamenBlockEntity pids = (PidsXiamenBlockEntity)live;
               Set<Long> live = pids.getPlatformIds();
               if (live != null) {
                  this.platformIds.addAll(live);
                  this.initialPlatforms.addAll(live);
               }

               this.displayPage = Math.max(0, pids.getDisplayPage());
               this.initialPage = this.displayPage;
               this.readAds(pids);
               return;
            }
         }

      } catch (Throwable var4) {
         LteLog.LOGGER.warn("[lte] xiamen pids: could not read the block entity for the screen", var4);
      }
   }

   private WidgetBetterCheckbox selectAll() {
      if (this.selectAll == null) {
         this.selectAll = new WidgetBetterCheckbox(0, 0, 0, 20, tr("gui.mtr.automatically_detect_nearby_platform"), (checked) -> {
            if (checked) {
               this.platformIds.clear();
            }

         });
      }

      return this.selectAll;
   }

   private boolean isLcdBlock() {
      try {
         Minecraft client = Minecraft.getInstance();
         return client.level != null && client.level.getBlockState(this.dataPos).getBlock() instanceof PsdLcdBlock;
      } catch (Throwable var2) {
         return false;
      }
   }

   private boolean hasStation() {
      try {
         return RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, this.dataPos) != null;
      } catch (Throwable var2) {
         return false;
      }
   }

   protected void init() {
      if (this.pageInput != null) {
         this.displayPage = parsePage(this.pageInput.getValue(), this.displayPage);
      }

      this.refreshAds();
      this.colW = Math.min(360, this.width - 48);
      this.colX = this.width / 2 - this.colW / 2;
      int y = 66;
      this.stripY = -1;
      if (this.isLcdBlock()) {
         this.stripY = y;
         this.lineStripBox = new WidgetBetterCheckbox(0, 0, 0, 20, tr("gui.mtrlte.psd_lcd.line_strip"), (checked) -> PidsXiamenSettings.setLineStrip(checked));
         this.lineStripBox.setChecked(PidsXiamenSettings.lineStrip());
         IDrawing.setPositionAndWidth(this.lineStripBox, this.colX, y, this.colW);
         this.addDrawableChild(this.lineStripBox);
         y += 30;
      }

      this.platformY = y;
      WidgetBetterCheckbox box = this.selectAll();
      IDrawing.setPositionAndWidth(box, this.colX, y, this.colW);
      box.setChecked(this.platformIds.isEmpty());
      this.addDrawableChild(box);
      y += 30;
      this.hasStation = this.hasStation();
      this.filterY = y;
      this.filterButton = PIDSConfigScreen.getPlatformFilterButton(this.dataPos, this.selectAll, this.platformIds, this);
      if (this.filterButton != null) {
         IDrawing.setPositionAndWidth(this.filterButton, this.colX, y, this.colW);
         this.filterButton.setMessage(tr("gui.mtrlte.pids_xiamen.pick_platforms"));
         this.filterButton.active = this.hasStation;
         this.addDrawableChild(this.filterButton);
      }

      y += 30;
      this.noStationY = y - 4;
      if (!this.hasStation) {
         y += 16;
      }

      this.pageY = y;
      this.pageInput = new WidgetBetterTextField(TextFieldFilter.POSITIVE_INTEGER, "1", 3);
      this.pageInput.setValue(String.valueOf(this.displayPage + 1));
      IDrawing.setPositionAndWidth(this.pageInput, this.colX + 96, y, 56);
      this.addDrawableChild(this.pageInput);
      y += 30;
      this.adsY = y;
      this.adsButton = Button.builder(this.adsLabel(), (b) -> this.openAds()).bounds(this.colX, y, this.colW, 20).build();
      this.addDrawableChild(this.adsButton);
      y += 30;
      int fy = this.height - 34;
      this.addDrawableChild(Button.builder(tr("gui.done"), (b) -> this.onClose()).bounds(this.colX, fy, this.colW, 20).build());
   }

   private MutableComponent adsLabel() {
      return tr("gui.mtrlte.pids_xiamen.pick_ads_count", this.adCategories.length, this.adCount);
   }

   private void readAds(PidsXiamenBlockEntity pids) {
      try {
         this.adCategories = pids.getAdCategories();
         this.adCount = PidsXiamenAds.adsFor(this.adCategories).size();
      } catch (Throwable var3) {
         this.adCategories = new long[0];
         this.adCount = 0;
      }

   }

   private void refreshAds() {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client != null && client.level != null) {
            BlockEntity var3 = client.level.getBlockEntity(this.dataPos);
            if (var3 instanceof PidsXiamenBlockEntity) {
               PidsXiamenBlockEntity pids = (PidsXiamenBlockEntity)var3;
               this.readAds(pids);
               if (this.adsButton != null) {
                  this.adsButton.setMessage(this.adsLabel());
               }
            }
         }
      } catch (Throwable var4) {
         LteLog.LOGGER.debug("[lte] xiamen pids: could not refresh the advertisement state", var4);
      }

   }

   private void openAds() {
      if (this.minecraft != null) {
         try {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
               BlockEntity var3 = client.level.getBlockEntity(this.dataPos);
               if (var3 instanceof PidsXiamenBlockEntity) {
                  PidsXiamenBlockEntity pids = (PidsXiamenBlockEntity)var3;
                  this.readAds(pids);
               }
            }
         } catch (Throwable var4) {
            LteLog.LOGGER.debug("[lte] xiamen pids: could not refresh the advertisement state", var4);
         }

         this.minecraft.setScreen(new PidsXiamenAdsScreen(this.dataPos, this));
      }
   }

   private static String readMessage(PidsXiamenBlockEntity pids, int index) {
      try {
         String text = pids.getMessage(index);
         return text == null ? "" : text;
      } catch (Throwable var3) {
         return "";
      }
   }

   private static boolean readHideArrival(PidsXiamenBlockEntity pids, int index) {
      try {
         return pids.getHideArrival(index);
      } catch (Throwable var3) {
         return false;
      }
   }

   private static int parsePage(String text, int fallback) {
      try {
         return Math.max(0, Integer.parseInt(text.trim()) - 1);
      } catch (Throwable var3) {
         return fallback;
      }
   }

   private void sendToServer() {
      try {
         if (this.selectAll != null && this.selectAll.selected()) {
            this.platformIds.clear();
         }

         int page;
         String[] messages;
         boolean[] hideArrival;
         label41: {
            page = this.pageInput == null ? this.displayPage : parsePage(this.pageInput.getValue(), this.displayPage);
            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
               BlockEntity maxArrivals = client.level.getBlockEntity(this.dataPos);
               if (maxArrivals instanceof PidsXiamenBlockEntity) {
                  PidsXiamenBlockEntity pids = (PidsXiamenBlockEntity)maxArrivals;
                  int maxArrivals = Math.max(0, pids.getMaxArrivals());
                  int lines = Math.max(1, pids.getLinesPerArrival());
                  messages = new String[maxArrivals * lines];

                  for(int i = 0; i < messages.length; ++i) {
                     messages[i] = readMessage(pids, i);
                  }

                  hideArrival = new boolean[maxArrivals];
                  int i = 0;

                  while(true) {
                     if (i >= maxArrivals) {
                        break label41;
                     }

                     hideArrival[i] = readHideArrival(pids, i);
                     ++i;
                  }
               }
            }

            messages = new String[0];
            hideArrival = new boolean[0];
         }

         PacketTrainDataGuiClient.sendPIDSConfigC2S(this.dataPos, this.partnerPos, messages, hideArrival, this.platformIds, page);
      } catch (Throwable var9) {
         LteLog.LOGGER.warn("[lte] xiamen pids: could not send the config", var9);
      }

   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      context.blit(OPTIONS_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
      context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
      context.drawString(this.font, tr("gui.mtrlte.pids_xiamen.platforms", this.platformIds.size()), this.colX, this.platformY - 12, 11579568);
      if (!this.hasStation) {
         context.drawString(this.font, tr("gui.mtrlte.pids_xiamen.no_station"), this.colX, this.noStationY, 16752029);
      }

      context.drawString(this.font, tr("gui.mtrlte.pids_xiamen.display_page"), this.colX, this.pageY + 6, 16777215);
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredString(this.font, this.title, this.width / 2, 24, 16777215);
      context.drawCenteredString(this.font, tr("gui.mtrlte.pids_xiamen.hint"), this.width / 2, 40, 9474192);
      context.drawCenteredString(this.font, tr("hint.author"), this.width / 2, this.height - 14, 11579568);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      int page = this.pageInput == null ? this.displayPage : parsePage(this.pageInput.getValue(), this.displayPage);
      boolean changed = page != this.initialPage || !this.platformIds.equals(this.initialPlatforms);
      if (changed) {
         this.sendToServer();
      }

      if (this.minecraft != null) {
         this.minecraft.setScreen((Screen)null);
      }

   }
}
