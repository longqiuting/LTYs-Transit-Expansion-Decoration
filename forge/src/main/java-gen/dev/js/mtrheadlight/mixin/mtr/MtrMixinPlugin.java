package dev.js.mtrheadlight.mixin.mtr;

import dev.js.mtrheadlight.mixin.HostProbe;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class MtrMixinPlugin implements IMixinConfigPlugin {
   public void onLoad(String mixinPackage) {
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      if (mixinClassName.endsWith(".RenderPIDSStationMixin")) {
         return HostProbe.mtrPidsRenderer();
      } else if (mixinClassName.endsWith(".RenderPidsTianjinStationMixin")) {
         return HostProbe.tjPidsRenderer();
      } else if (mixinClassName.endsWith(".RenderPsdDoorTextureMixin")) {
         return HostProbe.mtrPsdDoorRenderer();
      } else if (mixinClassName.endsWith(".RouteMapArtMixin")) {
         return HostProbe.mtrRouteMap();
      } else if (mixinClassName.endsWith(".StationNameEntranceLogoMixin")) {
         return HostProbe.mtrRouteMap();
      } else if (mixinClassName.endsWith(".BlockPlatformDoorTypeMixin")) {
         return HostProbe.mtrBlockPlatform();
      } else {
         return !mixinClassName.endsWith(".RouteMapCacheKeyMixin") && !mixinClassName.endsWith(".RouteAsyncScopeMixin") ? HostProbe.mtrRouteMap() : HostProbe.mtrRouteMap();
      }
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
