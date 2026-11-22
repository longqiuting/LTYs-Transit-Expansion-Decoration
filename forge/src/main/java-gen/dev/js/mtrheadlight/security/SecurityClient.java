package dev.js.mtrheadlight.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class SecurityClient implements ClientModInitializer {
   public void onInitializeClient() {
      try {
         LiquidScannerBlock block = SecurityObjects.liquidScanner();
         if (block != null) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout());
         }
      } catch (Throwable var3) {
      }

      try {
         ClientPlayNetworking.registerGlobalReceiver(LiquidScannerNet.ID, (client, handler, buf, sender) -> {
            Set<ResourceLocation> ids = new LinkedHashSet();

            BlockPos pos;
            boolean configured;
            try {
               if (buf.readByte() != 0) {
                  return;
               }

               pos = buf.readBlockPos();
               configured = buf.readBoolean();
               int n = Math.min(buf.readVarInt(), 512);

               for(int i = 0; i < n; ++i) {
                  ResourceLocation id = ResourceLocation.tryParse(buf.readUtf());
                  if (id != null) {
                     ids.add(id);
                  }
               }
            } catch (Throwable var10) {
               return;
            }

            client.execute(() -> client.setScreen(new LiquidScannerConfigScreen(pos, configured, ids)));
         });
      } catch (Throwable var2) {
      }

   }

   public static void sendSave(BlockPos pos, Collection<ResourceLocation> ids) {
      try {
         if (!ClientPlayNetworking.canSend(LiquidScannerNet.ID)) {
            return;
         }

         ClientPlayNetworking.send(LiquidScannerNet.ID, LiquidScannerNet.savePacket(pos, ids));
      } catch (Throwable var3) {
      }

   }
}
