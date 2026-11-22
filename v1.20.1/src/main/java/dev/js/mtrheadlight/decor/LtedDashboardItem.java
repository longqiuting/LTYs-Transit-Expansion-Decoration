package dev.js.mtrheadlight.decor;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * LTED 仪表板（{@code mtrlte:lted_dashboard}）：拿在手上右键，打开 LTED 自己的配置界面
 * （{@link LtedDashboardScreen}）。
 *
 * <h2>只有客户端</h2>
 * 和 LTE 的 {@code lte_dashboard} 同一个做法：
 * <ul>
 *   <li>服务端那一次 {@code use} 直接 {@code PASS} —— 不发包、不产生任何服务端状态；</li>
 *   <li>客户端用<b>反射</b>调 {@link LtedDashboardScreen#open()}：界面类只存在于客户端，
 *       dedicated server 上没有 {@code MinecraftClient}，直接写死调用会让这个物品类在服务端首次
 *       触碰常量池时就有找不到类的风险。反射 + 捕获 {@code Throwable}，服务端上「点一下什么都没发生」
 *       而不是崩；</li>
 *   <li>物品本身两端都注册（{@link LtedCoreItems}，{@code main} 入口）。</li>
 * </ul>
 */
public final class LtedDashboardItem extends Item {

    /** 客户端界面类的类名（反射用，避免编译期硬依赖）。 */
    private static final String SCREEN_CLASS = "dev.js.mtrheadlight.decor.LtedDashboardScreen";

    public LtedDashboardItem(final Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(final World world, final PlayerEntity user, final Hand hand) {
        final ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            return TypedActionResult.pass(stack);
        }
        open();
        return TypedActionResult.success(stack);
    }

    /**
     * 打开 LTED 仪表板（客户端）。
     *
     * @return {@code false} = 这一侧没有客户端类（dedicated server）或调用失败
     */
    public static boolean open() {
        try {
            Class.forName(SCREEN_CLASS).getMethod("open").invoke(null);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void appendTooltip(final ItemStack stack, final World world,
                              final List<Text> tooltip, final TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.mtrlte.lted_dashboard.hint").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mtrlte.lted_dashboard.client_only").formatted(Formatting.DARK_GRAY));
    }
}
