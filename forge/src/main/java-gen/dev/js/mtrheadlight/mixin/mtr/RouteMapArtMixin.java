package dev.js.mtrheadlight.mixin.mtr;

import com.mojang.blaze3d.platform.NativeImage;
import dev.js.mtrheadlight.xiamen.XiamenRouteStrip;
import mtr.client.RouteMapGenerator;
import mtr.data.IGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {RouteMapGenerator.class},
   remap = false
)
public abstract class RouteMapArtMixin {
   @Inject(
      method = {"generateRouteMap"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private static void lte$routeMap(long platformId, boolean vertical, boolean flip, float aspectRatio, boolean transparentWhite, CallbackInfoReturnable<NativeImage> cir) {
      NativeImage image = XiamenRouteStrip.paint(platformId, vertical, flip, aspectRatio);
      if (image != null) {
         cir.setReturnValue(image);
      }

   }

   @Inject(
      method = {"generateColorStrip"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private static void lte$colorStrip(long platformId, CallbackInfoReturnable<NativeImage> cir) {
      NativeImage image = XiamenRouteStrip.colorStrip(platformId);
      if (image != null) {
         cir.setReturnValue(image);
      }

   }

   @Inject(
      method = {"generateDirectionArrow"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private static void lte$directionArrow(long platformId, boolean hasLeft, boolean hasRight, IGui.HorizontalAlignment horizontalAlignment, boolean showToString, float paddingScale, float aspectRatio, int backgroundColor, int textColor, int transparentColor, CallbackInfoReturnable<NativeImage> cir) {
      NativeImage image = XiamenRouteStrip.directionArrow(platformId, hasLeft, hasRight, showToString, aspectRatio);
      if (image != null) {
         cir.setReturnValue(image);
      }

   }
}
