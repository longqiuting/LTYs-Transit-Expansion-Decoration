package dev.js.mtrheadlight.region;

import dev.js.mtrheadlight.xiamen.machine.XiamenTicketMachineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

// mojmap 版（与 Fabric 侧 v1.20.1/.../region/MtrTicketMachineBlock.java 一一对应）：
// 只换了两样东西 —— 包围盒（素材模型 z 只到 15.5）与物品提示 / 界面标题的翻译键。
// 刷子三档循环、ui 属性、两套界面的开法全部继承 XiamenTicketMachineBlock。
public class MtrTicketMachineBlock extends XiamenTicketMachineBlock {
   private static final double[] LOWER_BOX = new double[]{0.0D, 16.0D, 0.0D, 15.5D};
   private static final double[] UPPER_BOX = new double[]{0.0D, 16.0D, 0.0D, 15.5D};

   public MtrTicketMachineBlock(BlockBehaviour.Properties settings) {
      super(settings, LOWER_BOX, UPPER_BOX, "tooltip.mtrlte.ticket_machine_mtr");
   }

   protected String screenTitleKey() {
      return "screen.mtrlte.ticket_machine_mtr";
   }
}
