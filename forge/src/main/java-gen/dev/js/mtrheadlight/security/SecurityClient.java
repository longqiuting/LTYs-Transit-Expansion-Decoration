package dev.js.mtrheadlight.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import dev.js.mtrheadlight.forge.platform.ClientInit;
import dev.js.mtrheadlight.forge.platform.Layers;
import dev.js.mtrheadlight.forge.platform.Net;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class SecurityClient implements ClientInit {
   public void onInitializeClient() {
      try {
         LiquidScannerBlock block = SecurityObjects.liquidScanner();
         if (block != null) {
            Layers.put(block, RenderType.cutout());
         }
      } catch (Throwable var3) {
      }

      try {
         Net.registerClient(LiquidScannerNet.ID, (client, buf) -> {
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
         if (!Net.canSend(LiquidScannerNet.ID)) {
            return;
         }

         Net.sendToServer(LiquidScannerNet.ID, LiquidScannerNet.savePacket(pos, ids));
      } catch (Throwable var3) {
      }

   }
}
