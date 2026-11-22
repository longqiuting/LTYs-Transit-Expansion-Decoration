package dev.js.mtrheadlight.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;

public final class LiquidScannerConfigScreen extends Screen {
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
   private static final int ROW_H = 16;
   private static final int BAR_W = 4;
   private final BlockPos pos;
   private final List<ResourceLocation> all = new ArrayList();
   private final List<ResourceLocation> view = new ArrayList();
   private final Set<ResourceLocation> selected = new LinkedHashSet();
   private EditBox search;
   private int scroll;
   private int panelX;
   private int panelY;
   private int panelW;
   private int panelH;
   private int listX;
   private int listTop;
   private int listW;
   private int listH;

   public LiquidScannerConfigScreen(BlockPos pos, boolean configured, Set<ResourceLocation> ids) {
      super(Component.translatable("screen.mtrlte.liquid_scanner"));
      this.pos = pos;
      this.all.addAll(sortedPotions());
      this.selected.addAll(configured ? ids : defaults());
      this.refresh();
   }

   private static List<ResourceLocation> sortedPotions() {
      List<ResourceLocation> out = new ArrayList(BuiltInRegistries.POTION.keySet());
      out.addAll(LiquidScannerBlock.EXTRA_LIQUIDS);
      out.sort(Comparator.comparing((ResourceLocation id) -> nameOf(id).getString(), String.CASE_INSENSITIVE_ORDER).thenComparing(ResourceLocation::toString));
      return out;
   }

   public static Set<ResourceLocation> defaults() {
      Set<ResourceLocation> out = new LinkedHashSet();

      for(ResourceLocation id : BuiltInRegistries.POTION.keySet()) {
         Potion potion = (Potion)BuiltInRegistries.POTION.get(id);
         if (potion != null) {
            for(MobEffectInstance effect : potion.getEffects()) {
               if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                  out.add(id);
                  break;
               }
            }
         }
      }

      out.add(LiquidScannerBlock.LIQUID_LAVA);
      return out;
   }

   private static Component nameOf(ResourceLocation id) {
      if (LiquidScannerBlock.EXTRA_LIQUIDS.contains(id)) {
         return Component.translatable("screen.mtrlte.liquid." + id.getPath().substring("liquid/".length()));
      } else {
         Potion potion = (Potion)BuiltInRegistries.POTION.get(id);
         if (potion != null) {
            String key = potion.getName("item.minecraft.potion.effect.");
            if (key != null && I18n.exists(key)) {
               return Component.translatable(key);
            }
         }

         return Component.literal(id.getPath());
      }
   }

   private static int liquidColor(ResourceLocation id) {
      if (id.equals(LiquidScannerBlock.LIQUID_LAVA)) {
         return 16738816;
      } else if (id.equals(LiquidScannerBlock.LIQUID_WATER)) {
         return 4159204;
      } else if (id.equals(LiquidScannerBlock.LIQUID_MILK)) {
         return 15921906;
      } else if (id.equals(LiquidScannerBlock.LIQUID_HONEY)) {
         return 15245824;
      } else if (id.equals(LiquidScannerBlock.LIQUID_COD)) {
         return 12159568;
      } else if (id.equals(LiquidScannerBlock.LIQUID_SALMON)) {
         return 12872282;
      } else if (id.equals(LiquidScannerBlock.LIQUID_PUFFERFISH)) {
         return 14205514;
      } else if (id.equals(LiquidScannerBlock.LIQUID_TROPICAL_FISH)) {
         return 14701980;
      } else if (id.equals(LiquidScannerBlock.LIQUID_AXOLOTL)) {
         return 15240116;
      } else {
         return id.equals(LiquidScannerBlock.LIQUID_TADPOLE) ? 7306314 : 0;
      }
   }

   private void refresh() {
      String q = this.search == null ? "" : this.search.getValue().trim().toLowerCase(Locale.ROOT);
      this.view.clear();

      for(ResourceLocation id : this.all) {
         if (q.isEmpty() || id.toString().toLowerCase(Locale.ROOT).contains(q) || nameOf(id).getString().toLowerCase(Locale.ROOT).contains(q)) {
            this.view.add(id);
         }
      }

      this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));
   }

   private int maxScroll() {
      return Math.max(0, this.view.size() * 16 - this.listH);
   }

   protected void init() {
      this.panelW = Math.min(320, this.width - 40);
      this.panelH = Math.min(224, this.height - 40);
      this.panelX = (this.width - this.panelW) / 2;
      this.panelY = (this.height - this.panelH) / 2;
      this.search = new EditBox(this.font, this.panelX + 10, this.panelY + 26, this.panelW - 20, 18, Component.translatable("screen.mtrlte.liquid_scanner.search"));
      this.search.setMaxLength(48);
      this.search.setResponder((text) -> this.refresh());
      this.addRenderableWidget(this.search);
      this.listX = this.panelX + 10;
      this.listTop = this.panelY + 50;
      this.listW = this.panelW - 20;
      this.listH = this.panelH - 50 - 48;
      int by = this.panelY + this.panelH - 26;
      int gap = 6;
      int bw = (this.listW - 18) / 4;
      this.addButton(this.listX, by, bw, 20, "screen.mtrlte.liquid_scanner.defaultButton", (b) -> {
         this.selected.clear();
         this.selected.addAll(defaults());
      });
      this.addButton(this.listX + bw + 6, by, bw, 20, "screen.mtrlte.liquid_scanner.clearButton", (b) -> this.selected.clear());
      this.addButton(this.listX + (bw + 6) * 2, by, bw, 20, "btn.done", (b) -> this.saveAndClose());
      this.addButton(this.listX + (bw + 6) * 3, by, bw, 20, "btn.cancel", (b) -> this.onClose());
      this.refresh();
   }

   private void addButton(int x, int y, int w, int h, String key, Button.OnPress action) {
      this.addRenderableWidget(Button.builder(Component.translatable(key), action).bounds(x, y, w, h).build());
   }

   private void saveAndClose() {
      SecurityClient.sendSave(this.pos, this.selected);
      this.onClose();
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      context.blit(OPTIONS_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
      context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
      context.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, -535555052);
      context.renderOutline(this.panelX, this.panelY, this.panelW, this.panelH, -9539986);
      context.drawCenteredString(this.font, this.title, this.width / 2, this.panelY + 8, 16777215);
      super.render(context, mouseX, mouseY, delta);
      context.enableScissor(this.listX, this.listTop, this.listX + this.listW, this.listTop + this.listH);
      int y = this.listTop - this.scroll;

      for(ResourceLocation id : this.view) {
         if (y + 16 > this.listTop && y < this.listTop + this.listH) {
            this.drawRow(context, id, y, mouseX, mouseY);
         }

         y += 16;
      }

      context.disableScissor();
      if (this.view.isEmpty()) {
         context.drawCenteredString(this.font, Component.translatable("screen.mtrlte.liquid_scanner.empty"), this.width / 2, this.listTop + 8, 9474192);
      }

      this.drawScrollBar(context);
      Component hint = Component.translatable("screen.mtrlte.liquid_scanner.hint", new Object[]{this.selected.size()});
      context.drawCenteredString(this.font, hint, this.width / 2, this.panelY + this.panelH - 42, 11579568);
   }

   private void drawRow(GuiGraphics context, ResourceLocation id, int y, int mouseX, int mouseY) {
      boolean hovered = mouseX >= this.listX && mouseX < this.listX + this.listW && mouseY >= y && mouseY < y + 16;
      if (hovered) {
         context.fill(this.listX, y, this.listX + this.listW, y + 16, 822083583);
      }

      boolean on = this.selected.contains(id);
      int bx = this.listX + 3;
      int by = y + 3;
      context.fill(bx, by, bx + 10, by + 10, -15066598);
      context.renderOutline(bx, by, 10, 10, -6645094);
      if (on) {
         context.fill(bx + 2, by + 2, bx + 8, by + 8, -13057458);
         context.renderOutline(bx + 2, by + 2, 6, 6, -2555939);
      }

      context.drawString(this.font, nameOf(id), bx + 16, y + 4, on ? 16777215 : 12566463);
      Potion potion = (Potion)BuiltInRegistries.POTION.get(id);
      int dotX = this.listX + this.listW - 14;
      int extraColor = liquidColor(id);
      if (extraColor != 0) {
         context.fill(dotX, y + 5, dotX + 8, y + 13, -16777216 | extraColor);
         context.renderOutline(dotX, y + 5, 8, 8, 1627389951);
      } else if (potion != null) {
         context.fill(dotX, y + 5, dotX + 8, y + 13, -16777216 | PotionUtils.getColor(potion) & 16777215);
         context.renderOutline(dotX, y + 5, 8, 8, 1627389951);
      }

   }

   private void drawScrollBar(GuiGraphics context) {
      int max = this.maxScroll();
      if (max > 0) {
         int x = this.listX + this.listW - 4;
         context.fill(x, this.listTop, x + 4, this.listTop + this.listH, 1090519039);
         int barH = Math.max(16, this.listH * this.listH / (this.view.size() * 16));
         int barY = this.listTop + (this.listH - barH) * this.scroll / max;
         context.fill(x, barY, x + 4, barY + barH, -4210753);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (super.mouseClicked(mouseX, mouseY, button)) {
         return true;
      } else {
         if (button == 0 && mouseX >= (double)this.listX && mouseX < (double)(this.listX + this.listW) && mouseY >= (double)this.listTop && mouseY < (double)(this.listTop + this.listH)) {
            int index = (int)((mouseY - (double)this.listTop + (double)this.scroll) / 16.0D);
            if (index >= 0 && index < this.view.size()) {
               ResourceLocation id = (ResourceLocation)this.view.get(index);
               if (!this.selected.remove(id)) {
                  this.selected.add(id);
               }

               return true;
            }
         }

         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      int max = this.maxScroll();
      if (max > 0) {
         this.scroll = (int)Math.max(0.0D, Math.min((double)max, (double)this.scroll - amount * 16.0D * 2.0D));
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, amount);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
