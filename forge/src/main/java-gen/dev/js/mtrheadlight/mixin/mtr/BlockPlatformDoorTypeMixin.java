package dev.js.mtrheadlight.mixin.mtr;

import dev.js.mtrheadlight.xiamen.XiamenPlatformDoorType;
import mtr.block.BlockPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {BlockPlatform.class},
   remap = false
)
public abstract class BlockPlatformDoorTypeMixin {
   @Inject(
      method = {"getActualState"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void lte$resolveXiamenPsdDoorType(BlockGetter world, BlockPos pos, BlockState state, CallbackInfoReturnable<BlockState> cir) {
      if (XiamenPlatformDoorType.isFullHeightFamily(world.getBlockState(pos.above()).getBlock())) {
         cir.setReturnValue(XiamenPlatformDoorType.actualState(world, pos, state));
      }
   }
}
