package dev.js.mtrheadlight.xiamen.eyecandy;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import mtr.block.BlockPSDAPGDoorBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class PsdDoorProbe {
   private static final String[] OPEN_PROPERTIES = new String[]{"open", "door_open", "is_open", "opened"};

   private PsdDoorProbe() {
   }

   private static Integer openValueOf(Level world, BlockPos pos, BlockState state) {
      Integer fromProperties = openFromProperties(state);
      if (fromProperties != null) {
         return fromProperties;
      } else if (state.getBlock() instanceof BlockPSDAPGDoorBase) {
         BlockEntity entity = world.getBlockEntity(pos);
         if (entity instanceof BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase) {
            BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase door = (BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase)entity;
            return door.getOpenValue();
         } else {
            return 0;
         }
      } else {
         return null;
      }
   }

   private static Integer openFromProperties(BlockState state) {
      for(Property<?> property : state.getProperties()) {
         String name = property.getName().toLowerCase(Locale.ROOT);
         boolean matches = false;

         for(String candidate : OPEN_PROPERTIES) {
            if (candidate.equals(name)) {
               matches = true;
               break;
            }
         }

         if (matches) {
            Comparable<?> value = state.getValue(property);
            if (value instanceof Boolean) {
               Boolean bool = (Boolean)value;
               return bool ? 1 : 0;
            }

            if (value instanceof Integer) {
               Integer number = (Integer)value;
               return Math.max(0, number);
            }

            if (value instanceof Enum) {
               Enum<?> constant = (Enum)value;
               String constantName = constant.name().toLowerCase(Locale.ROOT);
               boolean closed = constantName.equals("closed") || constantName.equals("false") || constantName.equals("none");
               return closed ? 0 : 1;
            }
         }
      }

      return null;
   }

   public static final class Scan {
      public static final PsdDoorProbe.Scan NONE = new PsdDoorProbe.Scan(false, false, false);
      public final boolean anyDoor;
      public final boolean open;
      public final boolean closing;

      private Scan(boolean anyDoor, boolean open, boolean closing) {
         this.anyDoor = anyDoor;
         this.open = open;
         this.closing = closing;
      }
   }

   public static final class Tracker {
      private final Map<BlockPos, Integer> previous = new HashMap(8);
      private final Map<BlockPos, Integer> current = new HashMap(8);

      public PsdDoorProbe.Scan scan(Level world, BlockPos center) {
         if (world != null && center != null) {
            this.current.clear();
            boolean open = false;
            boolean closing = false;

            for(int dy = -1; dy <= 1; ++dy) {
               for(int dx = -1; dx <= 1; ++dx) {
                  for(int dz = -1; dz <= 1; ++dz) {
                     if (dx != 0 || dy != 0 || dz != 0) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (world.hasChunkAt(pos)) {
                           BlockState state = world.getBlockState(pos);
                           if (!state.isAir()) {
                              Integer value = PsdDoorProbe.openValueOf(world, pos, state);
                              if (value != null) {
                                 int now = value;
                                 this.current.put(pos, now);
                                 if (now > 0) {
                                    open = true;
                                 }

                                 Integer before = (Integer)this.previous.get(pos);
                                 if (before != null && now < before) {
                                    closing = true;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            this.previous.clear();
            this.previous.putAll(this.current);
            return this.current.isEmpty() ? PsdDoorProbe.Scan.NONE : new PsdDoorProbe.Scan(true, open, closing);
         } else {
            return PsdDoorProbe.Scan.NONE;
         }
      }
   }
}
