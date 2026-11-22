package dev.js.mtrheadlight;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.js.mtrheadlight.decor.LtedDashboardScreen;
import dev.js.mtrheadlight.xiamen.dep.DepOnWallDebug;
import dev.js.mtrheadlight.xiamen.dep.DepOnWallOrient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * {@code /mtr_lte} —— LTED（装饰侧）剩下的唯一一批客户端指令。
 *
 * <p>拆分前这里是「优化」侧的指令树（光束编辑器 / 车头灯 / 雨雾 / 内置车型 / 光效 / 散射 /
 * 道床 / 撞飞取证 …）。那些子指令随代码一起删掉了；只有<b>装饰</b>相关的
 * {@code dep}（挂墙发车倒数器的读数方向与取证开关）留在这里。
 *
 * <p>{@code /externallights} 仍然作为第二个根保留（改名前的旧名字），两个根由 {@link #root}
 * 同一份构建，不会走岔。
 *
 * <p>{@code /mtr_lte dep [status|next|normal|mirror|flip|rotate]} —— 挂墙发车倒数器
 * （{@code mtrlte:dep_on_wall}）的<b>读数方向</b>。四档都是绕屏幕正中的 {@code scale(±1,±1,1)}：
 * 屏幕占的面积不变，变的只有数字的朝向。改完下一帧生效、落盘到
 * {@code config/mtrlte-dep-orient.json}，所以实机方向不对时不用等重编发版 —— 现场翻到对的那一档，
 * 再把 {@link DepOnWallOrient#DEFAULT} 改过去即可。
 *
 * <p>{@code /mtr_lte dep debug [on|off|status]} —— 这块牌子的<b>取证日志</b>（默认关，只在内存里）。
 * 打开后每块牌子每秒一行 {@code [lte] dep: pos=… platform=… entries=… firstArrival=… now=… sec=…
 * dwell=… dwellHalf=… remain=… phase=… lockArrival=…}；「倒到 2 突然变 5」时靠
 * {@code firstArrival} 和 {@code lockArrival} 是否一致，就能判断是服务端换了条目 / 重算了到站时刻，
 * 还是 {@code now} 跳了。见 {@link DepOnWallDebug}。
 */
public final class Commands {
    /** Current name. */
    private static final String NAME = "mtr_lte";
    /** Pre-rename name, kept as an alias for one release. */
    private static final String LEGACY_NAME = "externallights";

    private Commands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(root(ClientCommandManager.literal(NAME)));
            dispatcher.register(root(ClientCommandManager.literal(LEGACY_NAME)));
            LteLog.LOGGER.info("[lte] commands registered: /{} dep={{}}", NAME);
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> root(
            final LiteralArgumentBuilder<FabricClientCommandSource> root) {
        return root
                .executes(context -> openDashboard(context.getSource()))
                // 挂墙发车倒数器的读数方向：现场翻，不用重编发版。
                .then(ClientCommandManager.literal("dep")
                        .executes(context -> depNext(context.getSource()))
                        .then(ClientCommandManager.literal("status")
                                .executes(context -> depStatus(context.getSource())))
                        .then(ClientCommandManager.literal("next")
                                .executes(context -> depNext(context.getSource())))
                        .then(ClientCommandManager.literal("normal")
                                .executes(context -> depSet(context.getSource(), "normal")))
                        .then(ClientCommandManager.literal("mirror")
                                .executes(context -> depSet(context.getSource(), "mirror")))
                        .then(ClientCommandManager.literal("flip")
                                .executes(context -> depSet(context.getSource(), "flip")))
                        .then(ClientCommandManager.literal("rotate")
                                .executes(context -> depSet(context.getSource(), "rotate")))
                        // 取证开关：打开后每块牌子每秒打一行 [lte] dep: …（只在内存里，默认关）。
                        .then(ClientCommandManager.literal("debug")
                                .executes(context -> depDebugStatus(context.getSource()))
                                .then(ClientCommandManager.literal("status")
                                        .executes(context -> depDebugStatus(context.getSource())))
                                .then(ClientCommandManager.literal("on")
                                        .executes(context -> setDepDebug(context.getSource(), true)))
                                .then(ClientCommandManager.literal("off")
                                        .executes(context -> setDepDebug(context.getSource(), false)))
                                .then(ClientCommandManager.argument("state",
                                                StringArgumentType.word())
                                        .suggests((context, builder) -> builder
                                                .suggest("on")
                                                .suggest("off")
                                                .buildFuture())
                                        .executes(context -> setDepDebug(context.getSource(),
                                                "on".equalsIgnoreCase(
                                                        StringArgumentType.getString(context,
                                                                "state"))))))
                        .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                                .suggests((context, builder) -> builder
                                        .suggest("normal")
                                        .suggest("mirror")
                                        .suggest("flip")
                                        .suggest("rotate")
                                        .buildFuture())
                                .executes(context -> depSet(context.getSource(),
                                        StringArgumentType.getString(context, "mode")))));
    }

    /** {@code /mtr_lte} —— 打开 LTED 仪表板（与仪表板物品右键同一条路径）。 */
    private static int openDashboard(final FabricClientCommandSource source) {
        final MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> client.setScreen(new LtedDashboardScreen(client.currentScreen)));
        source.sendFeedback(Text.literal("LTED：已打开仪表板"));
        return 1;
    }

    /**
     * {@code /mtr_lte dep} —— 切到下一档读数方向。
     *
     * <p>这块牌子的数字是几何画的，方向只可能是「整体转过去」的四种组合之一；每切一次都在
     * 聊天栏报当前档位，改完下一帧就生效，不需要重编、不需要重进世界。写进
     * {@code config/mtrlte-dep-orient.json}，重启也还在。
     */
    private static int depNext(final FabricClientCommandSource source) {
        final DepOnWallOrient.Mode mode = DepOnWallOrient.cycle();
        source.sendFeedback(Text.literal("LTED：倒数器方向 → " + mode.id() + "（" + mode.label()
                + "）— 屏幕面积不变，只有数字的朝向变；再执行一次 /mtr_lte dep 继续翻"));
        return 1;
    }

    /** {@code /mtr_lte dep status} —— 当前档位 + 下一档 + 用法。 */
    private static int depStatus(final FabricClientCommandSource source) {
        source.sendFeedback(Text.literal(DepOnWallOrient.statusLine()
                + "；取证：" + (DepOnWallDebug.enabled() ? "on" : "off")
                + "（/mtr_lte dep debug on|off）"));
        return 1;
    }

    /** {@code /mtr_lte dep <normal|mirror|flip|rotate>} —— 点名切到某一档。 */
    private static int depSet(final FabricClientCommandSource source, final String raw) {
        final DepOnWallOrient.Mode mode = DepOnWallOrient.Mode.byId(raw);
        if (mode == null) {
            source.sendFeedback(Text.literal("LTED：不认识的方向 \"" + raw
                    + "\"，可用：normal / mirror / flip / rotate"));
            return 0;
        }
        DepOnWallOrient.set(mode);
        source.sendFeedback(Text.literal("LTED：倒数器方向 = " + mode.id() + "（" + mode.label() + "）"));
        return 1;
    }

    /** {@code /mtr_lte dep debug [on|off|status]} —— 倒数器的取证日志状态。 */
    private static int depDebugStatus(final FabricClientCommandSource source) {
        source.sendFeedback(Text.literal(DepOnWallDebug.statusLine()));
        return 1;
    }

    /** {@code /mtr_lte dep debug on|off} —— 切取证开关，立刻生效（下一帧就开始打点）。 */
    private static int setDepDebug(final FabricClientCommandSource source, final boolean on) {
        DepOnWallDebug.set(on);
        source.sendFeedback(Text.literal("LTED：倒数器取证 = " + (on ? "on" : "off")
                + (on
                        ? "（每块牌子每秒一行 [lte] dep: …，看 firstArrival / sec / dwell / phase）"
                        : "（已停止打点）")));
        return 1;
    }
}
