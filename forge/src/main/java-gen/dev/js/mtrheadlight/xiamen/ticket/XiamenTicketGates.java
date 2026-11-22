package dev.js.mtrheadlight.xiamen.ticket;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
import java.util.List;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public final class XiamenTicketGates {
   public static final XiamenTicketGates.Side XIAMEN_SIDE = new XiamenTicketGates.Side("ticket_gate_xiamen_side", "ticket_barrier_side");
   public static final XiamenTicketGates.Side BRT_SIDE = new XiamenTicketGates.Side("ticket_gate_brt_side", "brt_barrier_side");
   private static final List<XiamenTicketGates.Side> SIDES = List.of(XIAMEN_SIDE, BRT_SIDE);
   private static final List<XiamenTicketGates.Gate> GATES = List.of(new XiamenTicketGates.Gate("ticket_gate_xiamen_in", true, "ticket_gate_xiamen", "ticket_barrier", XIAMEN_SIDE), new XiamenTicketGates.Gate("ticket_gate_xiamen_out", false, "ticket_gate_xiamen", "ticket_barrier", XIAMEN_SIDE), new XiamenTicketGates.Gate("ticket_gate_brt_in", true, "ticket_gate_brt", "brt_barrier", BRT_SIDE), new XiamenTicketGates.Gate("ticket_gate_brt_out", false, "ticket_gate_brt", "brt_barrier", BRT_SIDE));
   private static final ResourceLocation ENTITY_ID = new ResourceLocation("mtrlte", "ticket_gate");
   private static BlockEntityType<XiamenTicketGateBlockEntity> entityType;
   private static boolean registered;

   private XiamenTicketGates() {
   }

   public static List<XiamenTicketGates.Gate> gates() {
      return GATES;
   }

   public static List<XiamenTicketGates.Side> sides() {
      return SIDES;
   }

   public static BlockEntityType<XiamenTicketGateBlockEntity> entityType() {
      return entityType;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         XiamenTicketGateSounds.register();
         Block[] blocks = new Block[GATES.size() + SIDES.size()];
         int cursor = 0;

         for(XiamenTicketGates.Gate gate : GATES) {
            gate.block = (XiamenTicketGate)Registry.register(BuiltInRegistries.BLOCK, gate.id, new XiamenTicketGate(gate.entrance, gate.rawModel));
            blocks[cursor++] = gate.block;
            gate.item = (Item)Registry.register(BuiltInRegistries.ITEM, gate.id, new BlockItem(gate.block, new Item.Properties()));
         }

         for(XiamenTicketGates.Side side : SIDES) {
            side.block = (XiamenTicketGateSide)Registry.register(BuiltInRegistries.BLOCK, side.id, new XiamenTicketGateSide(Properties.of().strength(2.0F).requiresCorrectToolForDrops().noOcclusion(), side.rawModel));
            blocks[cursor++] = side.block;
            side.item = (Item)Registry.register(BuiltInRegistries.ITEM, side.id, new BlockItem(side.block, new Item.Properties()));
         }

         entityType = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ENTITY_ID, FabricBlockEntityTypeBuilder.create(XiamenTicketGateBlockEntity::new, blocks).build());

         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> {
               for(XiamenTicketGates.Gate gate : GATES) {
                  entries.method_45421(gate.item);
               }

               for(XiamenTicketGates.Side side : SIDES) {
                  entries.method_45421(side.item);
               }

            });
         } catch (Throwable var4) {
            LteLog.LOGGER.warn("[lte] ticket gates could not be added to the core tab", var4);
         }

         for(XiamenTicketGates.Gate gate : GATES) {
            LteLog.LOGGER.info("[lte] ticket gate {} (entrance={}, rawmodel={}_*, side={})", new Object[]{gate.id, gate.entrance, gate.rawModel, gate.side.id});
         }

         for(XiamenTicketGates.Side side : SIDES) {
            LteLog.LOGGER.info("[lte] ticket gate side shell {} (rawmodel={})", side.id, side.rawModel);
         }

      }
   }

   public static final class Gate {
      public final ResourceLocation id;
      public final boolean entrance;
      public final String model;
      public final String rawModel;
      public final XiamenTicketGates.Side side;
      private XiamenTicketGate block;
      private Item item;

      private Gate(String path, boolean entrance, String model, String rawModel, XiamenTicketGates.Side side) {
         this.id = new ResourceLocation("mtrlte", path);
         this.entrance = entrance;
         this.model = model;
         this.rawModel = rawModel;
         this.side = side;
      }

      public XiamenTicketGate block() {
         return this.block;
      }

      public Item item() {
         return this.item;
      }
   }

   public static final class Side {
      public final ResourceLocation id;
      public final String rawModel;
      private XiamenTicketGateSide block;
      private Item item;

      private Side(String path, String rawModel) {
         this.id = new ResourceLocation("mtrlte", path);
         this.rawModel = rawModel;
      }

      public XiamenTicketGateSide block() {
         return this.block;
      }

      public Item item() {
         return this.item;
      }
   }
}
