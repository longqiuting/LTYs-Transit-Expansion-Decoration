package dev.js.mtrheadlight.forge.platform;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * 命令 shim：Fabric 的 {@code ClientCommandRegistrationCallback} /
 * {@code ClientCommandManager} / {@code FabricClientCommandSource}
 * → Forge 的 {@link RegisterClientCommandsEvent} / {@link RegisterCommandsEvent}
 * + {@link CommandSourceStack}。
 *
 * <p><b>客户端 vs 服务端</b>：Fabric 侧的 {@code /mtr_lte} 是客户端命令
 * （{@code Commands.register()} 由 {@code LtedClient} 调用，内部用 {@code Minecraft.getInstance()}
 * 开屏），对应 Forge 的 {@link RegisterClientCommandsEvent}，只在物理客户端触发。
 * 所以 {@link #onRegisterClientCommands} <b>只能</b>在客户端接线（{@code ClientBootstrap}），
 * 不能写到 {@code LtedForge} 构造器里 —— 这个方法签名引用了客户端专属事件类，
 * 专用服务端一解析这个类就会 {@code NoClassDefFoundError}。
 *
 * <p>{@link #server} 是给「以后要加的服务端命令」准备的入口，事件在双端都安全。
 */
public final class Cmds {

    private static final List<Consumer<CommandDispatcher<CommandSourceStack>>> CLIENT_REGISTRARS = new ArrayList<>();
    private static final List<Consumer<CommandDispatcher<CommandSourceStack>>> SERVER_REGISTRARS = new ArrayList<>();

    private Cmds() {
    }

    /** 客户端命令注册（{@code ClientCommandRegistrationCallback.EVENT.register}）。 */
    public static void register(Consumer<CommandDispatcher<CommandSourceStack>> registrar) {
        if (registrar != null) {
            CLIENT_REGISTRARS.add(registrar);
        }
    }

    /** 服务端命令注册（Fabric 侧对标 {@code CommandRegistrationCallback.EVENT}）。 */
    public static void server(Consumer<CommandDispatcher<CommandSourceStack>> registrar) {
        if (registrar != null) {
            SERVER_REGISTRARS.add(registrar);
        }
    }

    public static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return net.minecraft.commands.Commands.literal(name);
    }

    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String name, ArgumentType<T> type) {
        return net.minecraft.commands.Commands.argument(name, type);
    }

    /** 替代 {@code FabricClientCommandSource#sendFeedback(Component)}。 */
    public static void feedback(CommandSourceStack source, Component message) {
        if (source != null && message != null) {
            source.sendSystemMessage(message);
        }
    }

    /** Forge 总线监听器：每次进入世界都会触发，所以 registrars 不能清空。 */
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        for (Consumer<CommandDispatcher<CommandSourceStack>> registrar : CLIENT_REGISTRARS) {
            registrar.accept(event.getDispatcher());
        }
    }

    /** Forge 总线监听器（双端）：服务端命令表在服务器启动时构建一次。 */
    public static void onRegisterServerCommands(RegisterCommandsEvent event) {
        for (Consumer<CommandDispatcher<CommandSourceStack>> registrar : SERVER_REGISTRARS) {
            registrar.accept(event.getDispatcher());
        }
    }
}
