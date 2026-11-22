package dev.js.mtrheadlight.mixin.mtr;

import dev.js.mtrheadlight.bind.PidsStationResolver;
import java.util.Set;
import mtr.data.DataCache;
import mtr.data.RailwayData;
import mtr.data.Station;
import mtr.render.RenderPIDS;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {RenderPIDS.class},
   remap = false
)
public abstract class RenderPIDSStationMixin {
   @Redirect(
      method = {"getSchedules"},
      at = @At(
   value = "INVOKE",
   target = "Lmtr/data/RailwayData;getStation"
),
      remap = false
   )
   private Station lte$stationOfThisScreen(Set<Station> stations, DataCache dataCache, BlockPos pos) {
      try {
         PidsStationResolver.Result result = PidsStationResolver.resolve(stations, dataCache, pos);
         PidsStationResolver.log("mtr", pos, result);
         return result.station();
      } catch (Throwable var6) {
         try {
            return RailwayData.getStation(stations, dataCache, pos);
         } catch (Throwable var5) {
            return null;
         }
      }
   }
}
