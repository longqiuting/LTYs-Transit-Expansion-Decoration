package dev.js.mtrheadlight.region;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import dev.js.mtrheadlight.forge.platform.Reg;

// mojmap 版（与 Fabric 侧 v1.20.1/.../region/RegionalMachines.java 一一对应）：
// AbstractBlock.Settings → BlockBehaviour.Properties，BlockSoundGroup → SoundType，
// Registry.register → Reg.block / Reg.item，ItemGroupEvents → Reg.tabEntries。
public final class RegionalMachines {
   public static final String NAMESPACE = "mtrlte";
   public static final ResourceLocation MTR_TICKET_ID = new ResourceLocation("mtrlte", "ticket_machine_mtr");
   private static MtrTicketMachineBlock ticket;
   private static Item ticketItem;
   private static boolean registered;

   private RegionalMachines() {
   }

   public static MtrTicketMachineBlock ticket() {
      return ticket;
   }

   public static void register() {
      if (!registered) {
         registered = true;
         ticket = (MtrTicketMachineBlock)Reg.block(MTR_TICKET_ID, new MtrTicketMachineBlock(settings()));
         ticketItem = (Item)Reg.item(MTR_TICKET_ID, new BlockItem(ticket, new Item.Properties()));

         try {
            Reg.tabEntries(RegionalTabs.MTR_HK_TAB_ID, (Reg.TabEntriesConsumer)(entries) -> entries.method_45421(ticketItem));
         } catch (Throwable var1) {
            LteLog.LOGGER.warn("[lte] mtr ticket machine: could not add the creative tab entry", var1);
         }

         LteLog.LOGGER.info("[lte] mtr ticket machine: {} (default ui={})", new Object[]{MTR_TICKET_ID, MtrTicketMachineBlock.defaultUi()});
      }
   }

   private static Properties settings() {
      return Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.METAL).noOcclusion();
   }
}
