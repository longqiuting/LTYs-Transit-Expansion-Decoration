package dev.js.mtrheadlight.xiamen;

import mtr.block.BlockAPGGlass;
import mtr.block.BlockPSDTop;
import mtr.block.IBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class XiamenRouteScope {
   private static final ThreadLocal<int[]> STATE = ThreadLocal.withInitial(() -> new int[3]);

   private XiamenRouteScope() {
   }

   public static void push(boolean ours) {
      int[] state = (int[])STATE.get();
      int var10002 = state[0]++;
      if (ours) {
         var10002 = state[1]++;
      }

   }

   public static void pop() {
      int[] state = (int[])STATE.get();
      if (state[0] > 0) {
         int var10002 = state[0]--;
      }

      if (state[1] > state[0]) {
         state[1] = state[0];
      }

      if (state[0] == 0) {
         state[1] = 0;
         state[2] = 0;
      }

   }

   public static void arrow(int direction) {
      ((int[])STATE.get())[2] = direction & 3;
   }

   public static int arrowDirection() {
      return ((int[])STATE.get())[2];
   }

   public static int arrowOf(BlockState state) {
      return state == null ? 0 : Math.max(IBlock.getStatePropertySafe(state, BlockPSDTop.ARROW_DIRECTION), IBlock.getStatePropertySafe(state, BlockAPGGlass.ARROW_DIRECTION));
   }

   public static boolean active() {
      return ((int[])STATE.get())[1] > 0;
   }

   public static int depth() {
      return ((int[])STATE.get())[0];
   }

   public static String cacheKeySuffix() {
      return active() ? "_lte" + ((int[])STATE.get())[2] : "";
   }
}
