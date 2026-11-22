package dev.js.mtrheadlight.xiamen.fangsu;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;

final class FangsuPsdPlacement {
   private FangsuPsdPlacement() {
   }

   static Direction facingOf(BlockPlaceContext ctx) {
      return ctx.getHorizontalDirection();
   }
}
