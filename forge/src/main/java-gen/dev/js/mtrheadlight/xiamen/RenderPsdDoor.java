package dev.js.mtrheadlight.xiamen;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.client.RenderDistance;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.IBlock;
import mtr.block.IBlock.EnumSide;
import mtr.render.RenderPSDAPGDoor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

public class RenderPsdDoor extends RenderPSDAPGDoor<BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase> implements IXiamenTextureNamespace {
   private static final int TYPE = 0;
   private static final String FMT_DOOR = "mtr:textures/block/psd_door_%s_%s_%s.png";
   private static final String FMT_END_MAIN = "mtr:textures/block/psd_door_end_%s_%s_1_%s.png";
   private static final String FMT_END_SUB = "mtr:textures/block/psd_door_end_%s_%s_2_%s.png";
   private static final String FMT_LIGHT = "mtr:textures/block/light_%s.png";
   private static final String ID_SIGN = "mtr:textures/block/sign/door_not_in_use.png";
   private static final String TYPE_TAG = "1";
   private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();
   private static final AtomicBoolean TABLE_LOGGED = new AtomicBoolean();

   public boolean rendersOutsideBoundingBox(BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase entity) {
      return true;
   }

   public int getViewDistance() {
      return RenderDistance.blocks();
   }

   public boolean isInRenderDistance(BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase entity, Vec3 cameraPos) {
      return RenderDistance.inRange(entity, cameraPos);
   }

   public RenderPsdDoor(BlockEntityRendererProvider.Context context) {
      super(context.getBlockEntityRenderDispatcher(), 0);
   }

   public String lte$textureNamespace() {
      return "mtrlte";
   }

   public void render(BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      String variant = XiamenObjects.textureVariantOf(entity.getBlockState().getBlock());
      if (TABLE_LOGGED.compareAndSet(false, true)) {
         logTable(variant);
      }

      Level world = entity.getLevel();
      if (world != null) {
         logRequested(world.getBlockState(entity.getBlockPos()), variant);
      }

      XiamenTextures.withVariant(variant, XiamenDoorLights.colorFor(entity.getBlockState().getBlock(), entity.getBlockPos()), () -> this.drawInMtr(entity, tickDelta, matrices, vertexConsumers, light, overlay));
   }

   private void drawInMtr(BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
      super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);
   }

   private static List<String> allIds() {
      List<String> ids = new ArrayList(16);

      for(String half : new String[]{"top", "bottom"}) {
         for(String side : new String[]{"left", "right"}) {
            ids.add(String.format("mtr:textures/block/psd_door_%s_%s_%s.png", half, side, "1"));
            ids.add(String.format("mtr:textures/block/psd_door_end_%s_%s_1_%s.png", half, side, "1"));
            ids.add(String.format("mtr:textures/block/psd_door_end_%s_%s_2_%s.png", half, side, "1"));
         }
      }

      ids.add(String.format("mtr:textures/block/light_%s.png", "on"));
      ids.add(String.format("mtr:textures/block/light_%s.png", "off"));
      ids.add("mtr:textures/block/sign/door_not_in_use.png");
      return ids;
   }

   private static List<String> requestedFor(BlockState state) {
      String half = IBlock.getStatePropertySafe(state, BlockPSDAPGDoorBase.HALF) == DoubleBlockHalf.UPPER ? "top" : "bottom";
      String side = IBlock.getStatePropertySafe(state, BlockPSDAPGDoorBase.SIDE) == EnumSide.RIGHT ? "right" : "left";
      List<String> ids = new ArrayList(4);
      if (IBlock.getStatePropertySafe(state, BlockPSDAPGDoorBase.END)) {
         ids.add(String.format("mtr:textures/block/psd_door_end_%s_%s_2_%s.png", half, side, "1"));
         ids.add(String.format("mtr:textures/block/psd_door_end_%s_%s_1_%s.png", half, side, "1"));
      } else {
         ids.add(String.format("mtr:textures/block/psd_door_%s_%s_%s.png", half, side, "1"));
      }

      ids.add(String.format("mtr:textures/block/light_%s.png", "on"));
      ids.add(String.format("mtr:textures/block/light_%s.png", "off"));
      ids.add("mtr:textures/block/sign/door_not_in_use.png");
      return ids;
   }

   private static void logTable(String variant) {
      try {
         for(String raw : allIds()) {
            logOne(raw, variant);
         }
      } catch (Throwable var3) {
         LteLog.LOGGER.warn("[lte] psd door texture table failed", var3);
      }

   }

   private static void logRequested(BlockState state, String variant) {
      try {
         for(String raw : requestedFor(state)) {
            if (SEEN.add(raw + "#" + (variant == null ? "" : variant))) {
               logOne(raw, variant);
            }
         }
      } catch (Throwable var4) {
         LteLog.LOGGER.warn("[lte] psd door texture probe failed", var4);
      }

   }

   private static void logOne(String raw, String variant) {
      String effective = XiamenTextures.resolve("mtrlte", raw, variant, (String)null);
      ResourceLocation id = new ResourceLocation(effective);
      LteLog.LOGGER.info("[lte] psd door tex: {} <- {}", id, sourceOf(id));
   }

   private static String sourceOf(ResourceLocation id) {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client == null) {
            return "?";
         } else {
            List<Resource> all = client.getResourceManager().getResourceStack(id);
            if (all.isEmpty()) {
               return "MISSING";
            } else {
               Resource top = (Resource)all.get(0);
               String packs = top.sourcePackId();
               return all.size() > 1 ? packs + " (+" + (all.size() - 1) + " lower)" : packs;
            }
         }
      } catch (Throwable var5) {
         return "lookup failed: " + var5.getClass().getSimpleName();
      }
   }
}
