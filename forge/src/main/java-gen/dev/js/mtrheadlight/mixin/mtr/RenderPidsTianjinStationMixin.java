package dev.js.mtrheadlight.mixin.mtr;

import dev.js.mtrheadlight.bind.PidsStationResolver;
import java.util.Set;
import mtr.data.DataCache;
import mtr.data.RailwayData;
import mtr.data.Station;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   targets = {"ziyue.tjmetro.render.RenderPIDSTianjin"},
   remap = false
)
public abstract class RenderPidsTianjinStationMixin {
   @Redirect(
      method = {"getSchedules"},
      at = @At(
   value = "INVOKE",
   target = "Lmtr/data/RailwayData;getStation"
),
      remap = false
   )
   private static Station lte$stationOfThisScreen(Set<Station> stations, DataCache dataCache, BlockPos pos) {
      try {
         PidsStationResolver.Result result = PidsStationResolver.resolve(stations, dataCache, pos);
         PidsStationResolver.log("tj", pos, result);
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
