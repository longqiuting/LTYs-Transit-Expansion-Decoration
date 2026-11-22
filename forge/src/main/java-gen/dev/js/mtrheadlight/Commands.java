package dev.js.mtrheadlight;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.js.mtrheadlight.decor.LtedDashboardScreen;
import dev.js.mtrheadlight.xiamen.dep.DepOnWallDebug;
import dev.js.mtrheadlight.xiamen.dep.DepOnWallOrient;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import dev.js.mtrheadlight.forge.platform.Cmds;

public final class Commands {
   private static final String NAME = "mtr_lte";
   private static final String LEGACY_NAME = "externallights";

   private Commands() {
   }

   public static void register() {
      Cmds.register((dispatcher) -> {
         dispatcher.register(root(Cmds.literal("mtr_lte")));
         dispatcher.register(root(Cmds.literal("externallights")));
         LteLog.LOGGER.info("[lte] commands registered: /{} dep={{}}", "mtr_lte");
      });
   }

   private static LiteralArgumentBuilder<CommandSourceStack> root(LiteralArgumentBuilder<CommandSourceStack> root) {
      return (LiteralArgumentBuilder)((LiteralArgumentBuilder)root.executes((context) -> openDashboard((CommandSourceStack)context.getSource()))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Cmds.literal("dep").executes((context) -> depNext((CommandSourceStack)context.getSource()))).then(Cmds.literal("status").executes((context) -> depStatus((CommandSourceStack)context.getSource())))).then(Cmds.literal("next").executes((context) -> depNext((CommandSourceStack)context.getSource())))).then(Cmds.literal("normal").executes((context) -> depSet((CommandSourceStack)context.getSource(), "normal")))).then(Cmds.literal("mirror").executes((context) -> depSet((CommandSourceStack)context.getSource(), "mirror")))).then(Cmds.literal("flip").executes((context) -> depSet((CommandSourceStack)context.getSource(), "flip")))).then(Cmds.literal("rotate").executes((context) -> depSet((CommandSourceStack)context.getSource(), "rotate")))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Cmds.literal("debug").executes((context) -> depDebugStatus((CommandSourceStack)context.getSource()))).then(Cmds.literal("status").executes((context) -> depDebugStatus((CommandSourceStack)context.getSource())))).then(Cmds.literal("on").executes((context) -> setDepDebug((CommandSourceStack)context.getSource(), true)))).then(Cmds.literal("off").executes((context) -> setDepDebug((CommandSourceStack)context.getSource(), false)))).then(Cmds.argument("state", StringArgumentType.word()).suggests((context, builder) -> builder.suggest("on").suggest("off").buildFuture()).executes((context) -> setDepDebug((CommandSourceStack)context.getSource(), "on".equalsIgnoreCase(StringArgumentType.getString(context, "state"))))))).then(Cmds.argument("mode", StringArgumentType.word()).suggests((context, builder) -> builder.suggest("normal").suggest("mirror").suggest("flip").suggest("rotate").buildFuture()).executes((context) -> depSet((CommandSourceStack)context.getSource(), StringArgumentType.getString(context, "mode")))));
   }

   private static int openDashboard(CommandSourceStack source) {
      Minecraft client = Minecraft.getInstance();
      client.execute(() -> client.setScreen(new LtedDashboardScreen(client.screen)));
      Cmds.feedback(source, Component.literal("LTED\uff1a\u5df2\u6253\u5f00\u4eea\u8868\u677f"));
      return 1;
   }

   private static int depNext(CommandSourceStack source) {
      DepOnWallOrient.Mode mode = DepOnWallOrient.cycle();
      Cmds.feedback(source, Component.literal("LTED\uff1a\u5012\u6570\u5668\u65b9\u5411 \u2192 " + mode.id() + "\uff08" + mode.label() + "\uff09\u2014 \u5c4f\u5e55\u9762\u79ef\u4e0d\u53d8\uff0c\u53ea\u6709\u6570\u5b57\u7684\u671d\u5411\u53d8\uff1b\u518d\u6267\u884c\u4e00\u6b21 /mtr_lte dep \u7ee7\u7eed\u7ffb"));
      return 1;
   }

   private static int depStatus(CommandSourceStack source) {
      Cmds.feedback(source, Component.literal(DepOnWallOrient.statusLine() + "\uff1b\u53d6\u8bc1\uff1a" + (DepOnWallDebug.enabled() ? "on" : "off") + "\uff08/mtr_lte dep debug on|off\uff09"));
      return 1;
   }

   private static int depSet(CommandSourceStack source, String raw) {
      DepOnWallOrient.Mode mode = DepOnWallOrient.Mode.byId(raw);
      if (mode == null) {
         Cmds.feedback(source, Component.literal("LTED\uff1a\u4e0d\u8ba4\u8bc6\u7684\u65b9\u5411 \"" + raw + "\"\uff0c\u53ef\u7528\uff1anormal / mirror / flip / rotate"));
         return 0;
      } else {
         DepOnWallOrient.set(mode);
         Cmds.feedback(source, Component.literal("LTED\uff1a\u5012\u6570\u5668\u65b9\u5411 = " + mode.id() + "\uff08" + mode.label() + "\uff09"));
         return 1;
      }
   }

   private static int depDebugStatus(CommandSourceStack source) {
      Cmds.feedback(source, Component.literal(DepOnWallDebug.statusLine()));
      return 1;
   }

   private static int setDepDebug(CommandSourceStack source, boolean on) {
      DepOnWallDebug.set(on);
      Cmds.feedback(source, Component.literal("LTED\uff1a\u5012\u6570\u5668\u53d6\u8bc1 = " + (on ? "on" : "off") + (on ? "\uff08\u6bcf\u5757\u724c\u5b50\u6bcf\u79d2\u4e00\u884c [lte] dep: \u2026\uff0c\u770b firstArrival / sec / dwell / phase\uff09" : "\uff08\u5df2\u505c\u6b62\u6253\u70b9\uff09")));
      return 1;
   }
}
