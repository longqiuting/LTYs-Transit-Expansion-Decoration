package dev.js.mtrheadlight.mixin.mtr;

import dev.js.mtrheadlight.xiamen.XiamenTextures;
import mtr.render.RenderPSDAPGDoor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {RenderPSDAPGDoor.class},
   remap = false
)
public abstract class RenderPsdDoorTextureMixin {
   @Redirect(
      method = {"render"},
      at = @At(
   value = "INVOKE",
   target = "Ljava/lang/String;format",
   remap = false
),
      remap = false
   )
   private static String lte$rewriteTextureId(String format, Object[] args) {
      return XiamenTextures.resolveActive(String.format(format, args));
   }
}
