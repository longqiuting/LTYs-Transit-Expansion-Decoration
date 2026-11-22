package dev.js.mtrheadlight.mixin.mtr;

import dev.js.mtrheadlight.xiamen.sign.WallSignLogos;
import mtr.client.RouteMapGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(
   value = {RouteMapGenerator.class},
   remap = false
)
public abstract class StationNameEntranceLogoMixin {
   @ModifyArg(
      method = {"drawResource"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/util/Identifier;<init>(Ljava/lang/String;Ljava/lang/String;)V",
   remap = true
),
      index = 0,
      remap = false
   )
   private static String lte$brandLogoNamespace(String namespace) {
      String brand = WallSignLogos.brand();
      return brand != null && "mtr".equals(namespace) ? "mtrlte" : namespace;
   }

   @ModifyArg(
      method = {"drawResource"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/util/Identifier;<init>(Ljava/lang/String;Ljava/lang/String;)V",
   remap = true
),
      index = 1,
      remap = false
   )
   private static String lte$brandLogoPath(String path) {
      String brand = WallSignLogos.brand();
      return brand != null && path != null && path.endsWith("sign/logo.png") ? WallSignLogos.texturePath(brand) : path;
   }
}
