package dev.js.mtrheadlight.xiamen;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.clock.ClockClient;
import dev.js.mtrheadlight.xiamen.eyecandy.EyecandyClient;
import dev.js.mtrheadlight.xiamen.sign.StationSignObjects;
import dev.js.mtrheadlight.xiamen.sign.WallSignObjects;
import dev.js.mtrheadlight.xiamen.ticket.XiamenTicketGateClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Block;

public final class XiamenClient implements ClientModInitializer {
   public void onInitializeClient() {
      try {
         BlockEntityRendererProvider<XiamenPsdBlocks.TileEntityXiamenPSDDoor> doorRendererFactory = RenderPsdDoor::new;
         BlockEntityRendererRegistry.register(XiamenObjects.psdDoorEntityType(), doorRendererFactory);
         LteLog.LOGGER.info("[lte] xiamen door renderer registered: {}", RenderPsdDoor.class.getName());
      } catch (Throwable var9) {
         LteLog.LOGGER.error("[lte] could not register the xiamen door renderer", var9);
      }

      try {
         BlockEntityRendererRegistry.register(XiamenObjects.psdTopEntityType(), (context) -> (BlockEntityRenderer)(new XiamenPsdTopRenderer(context.getBlockEntityRenderDispatcher())));
      } catch (Throwable var8) {
         LteLog.LOGGER.error("[lte] could not register the xiamen psd top renderer", var8);
      }

      try {
         BlockEntityRendererProvider<XiamenPsdBlocks.TileEntityXiamenAPGDoor> apgDoorRendererFactory = RenderPsdApgDoor::new;
         BlockEntityRendererRegistry.register(XiamenObjects.apgDoorEntityType(), apgDoorRendererFactory);
         LteLog.LOGGER.info("[lte] xiamen apg door renderer registered: {}", RenderPsdApgDoor.class.getName());
      } catch (Throwable var7) {
         LteLog.LOGGER.error("[lte] could not register the xiamen apg door renderer", var7);
      }

      try {
         BlockEntityRendererRegistry.register(XiamenObjects.apgGlassEntityType(), (context) -> (BlockEntityRenderer)(new XiamenApgGlassRenderer(context.getBlockEntityRenderDispatcher())));
         LteLog.LOGGER.info("[lte] xiamen apg glass route map renderer registered: {}", XiamenApgGlassRenderer.class.getName());
      } catch (Throwable var6) {
         LteLog.LOGGER.error("[lte] could not register the xiamen apg glass renderer", var6);
      }

      for(XiamenLine line : XiamenLine.values()) {
         XiamenObjects.LineSet set = XiamenObjects.set(line);
         put(set.door, String.valueOf(line) + " psd door");
         put(set.glass, String.valueOf(line) + " psd glass");
         put(set.end, String.valueOf(line) + " psd end");
         put(set.top, String.valueOf(line) + " psd top");
         put(set.apgDoor, String.valueOf(line) + " apg door");
         put(set.apgGlass, String.valueOf(line) + " apg glass");
         put(set.apgEnd, String.valueOf(line) + " apg end");
      }

      put(XiamenObjects.logo(), "logo");

      for(StationSignObjects.Entry entry : StationSignObjects.entries()) {
         put(entry.block(), "station sign " + entry.id());
      }

      for(WallSignObjects.Entry entry : WallSignObjects.entries()) {
         put(entry.block(), "wall sign " + entry.id());
      }

      XiamenTicketGateClient.register();
      EyecandyClient.register();
      ClockClient.register();
   }

   private static void put(Block block, String what) {
      try {
         BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout());
      } catch (Throwable var3) {
         LteLog.LOGGER.warn("[lte] could not set the cutout layer for the xiamen {} block", what, var3);
      }

   }
}
