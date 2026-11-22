package dev.js.mtrheadlight.xiamen.pids;

import dev.js.mtrheadlight.LteLog;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Layers;
import dev.js.mtrheadlight.forge.platform.Net;
import dev.js.mtrheadlight.forge.platform.Renderers;
import dev.js.mtrheadlight.forge.platform.Resources;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public final class PidsXiamenClient implements ClientInit {
   private static final ResourceLocation RELOAD_ID = new ResourceLocation("mtrlte", "pids_xiamen_ads");

   public void onInitializeClient() {
      try {
         Renderers.ber(PidsXiamenObjects.entityType(), PidsXiamenRenderer::new);
         LteLog.LOGGER.info("[lte] xiamen pids renderer registered: {}", PidsXiamenRenderer.class.getName());
      } catch (Throwable var7) {
         LteLog.LOGGER.error("[lte] could not register the xiamen pids renderer", var7);
      }

      try {
         Layers.put(PidsXiamenObjects.metro(), RenderType.cutout());
         Layers.put(PidsXiamenObjects.brt(), RenderType.cutout());
      } catch (Throwable var6) {
         LteLog.LOGGER.warn("[lte] could not set the cutout layer for the xiamen pids blocks", var6);
      }

      try {
         Resources.registerClient(new PidsXiamenClient.AdsListener());
      } catch (Throwable var5) {
         LteLog.LOGGER.warn("[lte] xiamen pids ads: reload listener unavailable", var5);
      }

      try {
         Net.registerClient(PidsXiamenNet.ID, (client, buf) -> {
            try {
               if (!PidsXiamenNet.isOpen(PidsXiamenNet.readKind(buf))) {
                  return;
               }

               PidsXiamenNet.OpenPacket packet = PidsXiamenNet.readOpen(buf);
               client.execute(() -> client.setScreen(new PidsXiamenConfigScreen(packet.dataPos(), packet.partnerPos())));
            } catch (Throwable var5) {
               LteLog.LOGGER.warn("[lte] xiamen pids: bad open packet", var5);
            }

         });
      } catch (Throwable var4) {
         LteLog.LOGGER.warn("[lte] xiamen pids: could not register the client receiver", var4);
      }

      try {
         Net.registerClient(PidsAdSetNet.ID, (client, buf) -> {
            try {
               if (buf.readByte() != 0) {
                  return;
               }

               PidsAdSet set = PidsAdSet.readBuf(buf);
               client.execute(() -> PidsAdSetCache.set(set));
            } catch (Throwable var5) {
               LteLog.LOGGER.warn("[lte] xiamen pids ad set: bad snapshot packet", var5);
            }

         });
      } catch (Throwable var3) {
         LteLog.LOGGER.warn("[lte] xiamen pids ad set: could not register the client receiver", var3);
      }

      try {
         Net.onDisconnect(() -> PidsAdSetCache.clear());
      } catch (Throwable var2) {
      }

   }

   private static final class AdsListener implements Resources.ReloadListener {
      public ResourceLocation getFabricId() {
         return PidsXiamenClient.RELOAD_ID;
      }

      public CompletableFuture<Void> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
         return CompletableFuture.completedFuture((Void)null);
      }

      public CompletableFuture<Void> apply(Void data, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
         try {
            PidsXiamenAds.invalidate();
         } catch (Throwable var10) {
            LteLog.LOGGER.warn("[lte] xiamen pids ads: could not invalidate the cache", var10);
         }

         try {
            PidsXiamenTicker.invalidate();
         } catch (Throwable var9) {
            LteLog.LOGGER.warn("[lte] xiamen pids ticker: could not invalidate the cache", var9);
         }

         try {
            PsdLcdAssets.invalidateAds();
         } catch (Throwable var8) {
            LteLog.LOGGER.warn("[lte] psd lcd: could not invalidate the ad image cache", var8);
         }

         try {
            PidsAdSetCache.invalidate();
         } catch (Throwable var7) {
            LteLog.LOGGER.warn("[lte] xiamen pids ad set: could not invalidate the resolved cache", var7);
         }

         try {
            PidsXiamenConsistTextures.invalidate();
         } catch (Throwable var6) {
            LteLog.LOGGER.warn("[lte] xiamen pids consist: could not invalidate the overlay", var6);
         }

         return CompletableFuture.completedFuture((Void)null);
      }
   }
}
