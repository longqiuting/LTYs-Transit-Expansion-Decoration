package dev.js.mtrheadlight.mixin.mtr;

import dev.js.mtrheadlight.xiamen.XiamenRouteScope;
import dev.js.mtrheadlight.xiamen.sign.WallSignLogos;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import mtr.client.ClientCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {ClientCache.class},
   remap = false
)
public abstract class RouteAsyncScopeMixin {
   @Redirect(
      method = {"getResource"},
      at = @At(
   value = "INVOKE",
   target = "Ljava/util/concurrent/CompletableFuture;supplyAsync",
   remap = false
),
      remap = false
   )
   private <U> CompletableFuture<U> lte$carryScope(Supplier<U> supplier) {
      String signLogo = WallSignLogos.brand();
      boolean routeActive = XiamenRouteScope.active();
      if (!routeActive && signLogo == null) {
         return CompletableFuture.supplyAsync(supplier);
      } else {
         int arrow = XiamenRouteScope.arrowDirection();
         return CompletableFuture.supplyAsync(() -> {
            if (routeActive) {
               XiamenRouteScope.push(true);
               XiamenRouteScope.arrow(arrow);
            }

            WallSignLogos.push(signLogo);

            U var4;
            try {
               var4 = supplier.get();
            } finally {
               WallSignLogos.pop();
               if (routeActive) {
                  XiamenRouteScope.pop();
               }

            }

            return var4;
         });
      }
   }
}
