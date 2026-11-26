// 港铁（MTR）新售票机：与厦门售票机同一套交互框架，只换模型 / 贴图 / 包围盒。
package dev.js.mtrheadlight.region;

import dev.js.mtrheadlight.xiamen.machine.XiamenTicketMachineBlock;
import net.minecraft.block.AbstractBlock;

/**
 * 港铁新售票机 {@code mtrlte:ticket_machine_mtr}（两格高，下段有物品）。
 *
 * <h2>抄的是哪一份</h2>
 * <p>直接 {@code extends XiamenTicketMachineBlock} —— <b>刷子三档循环（方速界面 → 原版 MTR 界面 →
 * 节能关机）、{@code ui} 方块状态、空手右键开界面、关机态只回一句提示、上下两格一起切</b>
 * 全部原样继承，一行都没重写。用户要的「和厦门地铁售票机一个逻辑」就是这个意思：
 * 同一个交互框架，只换外形。
 *
 * <h2>只换两样东西</h2>
 * <ul>
 *   <li><b>包围盒</b>：素材模型的 z 只到 15.5（厦门那台上段缩到 14.31），
 *       所以 {@code LOWER_BOX} / {@code UPPER_BOX} 的 maxZ 从模型顶点重算；</li>
 *   <li><b>物品提示</b>：{@code tooltip.mtrlte.ticket_machine_mtr}。</li>
 * </ul>
 *
 * <h2>资源（{@code tools/gen-mtr-ticket-machine-assets.js} 生成）</h2>
 * <table border="1">
 *   <caption>两种状态各一套上下段模型，几何相同、只换贴图</caption>
 *   <tr><th>ui</th><th>贴图</th><th>模型（bottom / top）</th></tr>
 *   <tr><td>{@code fangsu} / {@code vanilla}</td><td>{@code block/ticket_machine_mtr.png}</td>
 *       <td>{@code block/ticket_machine_mtr_bottom} / {@code _top}</td></tr>
 *   <tr><td>{@code eco}（节能关机）</td><td>{@code block/ticket_machine_mtr_off.png}</td>
 *       <td>{@code block/ticket_machine_mtr_bottom_off} / {@code _top_off}</td></tr>
 * </table>
 * <p>blockstate 的 24 个变体（4 朝向 × 2 段 × 3 个 ui）就是照厦门那份
 * {@code blockstates/ticket_machine_xiamen.json} 排的 —— 贴图切换不需要额外发包，
 * {@code ui} 一变客户端自然换模型。
 */
public class MtrTicketMachineBlock extends XiamenTicketMachineBlock {

    /**
     * 港铁售票机的包围盒：上下两段的模型顶点都是 x 0..16 / z 0..15.5
     * （从 {@code new_ticket_machine_{bottom,top}.json} 的元素顶点连旋转一起算出来的 AABB）。
     * 数组是 {@code {minX, maxX, minZ, maxZ}}，与 {@code XiamenMachineBlock} 同一套约定。
     */
    private static final double[] LOWER_BOX = {0.0D, 16.0D, 0.0D, 15.5D};
    private static final double[] UPPER_BOX = {0.0D, 16.0D, 0.0D, 15.5D};

    public MtrTicketMachineBlock(final AbstractBlock.Settings settings) {
        super(settings, LOWER_BOX, UPPER_BOX, "tooltip.mtrlte.ticket_machine_mtr");
    }

    /** 方速界面标题：写「港铁售票机」，不是厦门那台的名字。 */
    @Override
    protected String screenTitleKey() {
        return "screen.mtrlte.ticket_machine_mtr";
    }
}
