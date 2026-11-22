package dev.js.mtrheadlight.xiamen.machine;

import dev.js.mtrheadlight.LteLog;
import dev.js.mtrheadlight.xiamen.XiamenObjects;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public final class XiamenMachines {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation TICKET_ID = new ResourceLocation("mtrlte", "ticket_machine_xiamen");
   public static final ResourceLocation BALANCE_ID = new ResourceLocation("mtrlte", "balance_machine_xiamen");
   public static final ResourceLocation BALANCE_ENTITY_ID = new ResourceLocation("mtrlte", "balance_machine");
   private static XiamenTicketMachineBlock ticket;
   private static XiamenBalanceMachineBlock balance;
   private static BlockEntityType<XiamenBalanceMachineBlockEntity> balanceEntityType;
   private static Item ticketItem;
   private static Item balanceItem;
   private static boolean registered;

   private XiamenMachines() {
   }

   public static XiamenTicketMachineBlock ticket() {
      return ticket;
   }

   public static XiamenBalanceMachineBlock balance() {
      return balance;
   }

   public static BlockEntityType<XiamenBalanceMachineBlockEntity> balanceEntityType() {
      return balanceEntityType;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         ticket = (XiamenTicketMachineBlock)Registry.register(BuiltInRegistries.BLOCK, TICKET_ID, new XiamenTicketMachineBlock(settings()));
         ticketItem = (Item)Registry.register(BuiltInRegistries.ITEM, TICKET_ID, new BlockItem(ticket, new Item.Properties()));
         balance = (XiamenBalanceMachineBlock)Registry.register(BuiltInRegistries.BLOCK, BALANCE_ID, new XiamenBalanceMachineBlock(settings()));
         balanceItem = (Item)Registry.register(BuiltInRegistries.ITEM, BALANCE_ID, new BlockItem(balance, new Item.Properties()));
         balanceEntityType = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BALANCE_ENTITY_ID, FabricBlockEntityTypeBuilder.create(XiamenBalanceMachineBlockEntity::new, new Block[]{balance}).build());

         try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, XiamenObjects.CORE_TAB_ID)).register((ItemGroupEvents.ModifyEntries)(entries) -> {
               entries.method_45421(ticketItem);
               entries.method_45421(balanceItem);
            });
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] ticket machines: could not add the creative tab entries", var1);
         }

         LteLog.LOGGER.info("[lte] ticket machines: {} (default ui={}) / {}", new Object[]{TICKET_ID, XiamenTicketMachineBlock.defaultUi(), BALANCE_ID});
      }
   }

   private static BlockBehaviour.Properties settings() {
      return Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.METAL).noOcclusion();
   }
}
