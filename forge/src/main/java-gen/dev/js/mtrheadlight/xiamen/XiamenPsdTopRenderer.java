package dev.js.mtrheadlight.xiamen;

import mtr.render.RenderPSDTop;
import mtr.render.RenderRouteBase;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public class XiamenPsdTopRenderer extends RenderPSDTop {
   public XiamenPsdTopRenderer(BlockEntityRenderDispatcher dispatcher) {
      super(dispatcher);
   }

   protected RenderRouteBase.RenderType getRenderType(BlockGetter world, BlockPos pos, BlockState state) {
      XiamenRouteScope.push(true);
      XiamenRouteScope.arrow(XiamenRouteScope.arrowOf(state));
      return super.getRenderType(world, pos, state);
   }

   protected void renderAdditional(StoredMatrixTransformations storedMatrixTransformations, long platformId, BlockState state, int light, int overlay, Direction facing, int rh, int rv) {
      try {
         super.renderAdditional(storedMatrixTransformations, platformId, state, light, overlay, facing, rh, rv);
      } finally {
         XiamenRouteScope.pop();
      }

   }
}
