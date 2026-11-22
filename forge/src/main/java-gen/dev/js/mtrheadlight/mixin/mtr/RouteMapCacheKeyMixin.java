package dev.js.mtrheadlight.mixin.mtr;

import dev.js.mtrheadlight.xiamen.XiamenRouteScope;
import dev.js.mtrheadlight.xiamen.sign.WallSignLogos;
import mtr.client.ClientCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {ClientCache.class},
   remap = false
)
public abstract class RouteMapCacheKeyMixin {
   @Redirect(
      method = {"getRouteMap", "getDirectionArrow", "getStationNameEntrance"},
      at = @At(
   value = "INVOKE",
   target = "Ljava/lang/String;format",
   remap = false
),
      remap = false
   )
   private String lte$scopedKey(String format, Object[] args) {
      String key = String.format(format, args);
      String suffix = XiamenRouteScope.cacheKeySuffix() + WallSignLogos.cacheKeySuffix();
      return suffix.isEmpty() ? key : key + suffix;
   }
}
