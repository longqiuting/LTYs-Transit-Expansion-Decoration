package dev.js.mtrheadlight.forge.platform;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络 shim：Fabric 的 {@code ServerPlayNetworking} / {@code ClientPlayNetworking} /
 * {@code PacketByteBufs} / {@code PlayerLookup} / {@code ServerPlayConnectionEvents} /
 * {@code ClientPlayConnectionEvents} → 每个逻辑频道一个 Forge {@link SimpleChannel}。
 *
 * <p><b>字节格式</b>：消息体不解析、不重组，发送端把 {@link FriendlyByteBuf} 里剩下的字节
 * 原样塞进 payload，接收端再包回一个 {@link FriendlyByteBuf} 交给 java-gen 的 lambda。
 * 所以 12 个包的类型（kind 字节 / BlockPos / 变长字段）与 Fabric 侧完全一致；
 * 只有 Forge 频道外层的分帧（discriminator + 长度）不同。
 *
 * <p>每个频道注册两条消息：{@code 0} = PLAY_TO_SERVER，{@code 1} = PLAY_TO_CLIENT。
 * 只有真正用到的方向才会被注册（{@link #registerServer} / {@link #registerClient}）。
 */
public final class Net {

    /** 协议版本：客户端与服务端必须一致（同一 jar 的双端）。 */
    private static final String PROTOCOL = "1";

    private static final int ID_TO_SERVER = 0;
    private static final int ID_TO_CLIENT = 1;

    private static final Map<ResourceLocation, SimpleChannel> CHANNELS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> SERVER_REGISTERED = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> CLIENT_REGISTERED = ConcurrentHashMap.newKeySet();

    private static final List<JoinListener> JOIN_LISTENERS = new ArrayList<>();
    private static final List<Runnable> DISCONNECT_LISTENERS = new ArrayList<>();
    private static boolean commonHooksInstalled;

    private Net() {
    }

    /** {@code PacketByteBufs.create()} */
    public static FriendlyByteBuf buf() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    /** 服务端接收器：原 Fabric 的 {@code (server, player, handler, buf, sender)} 去掉不用的 handler/sender。 */
    @FunctionalInterface
    public interface ServerReceiver {
        void receive(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf);
    }

    /** 客户端接收器：原 Fabric 的 {@code (client, handler, buf, sender)} 去掉不用的 handler/sender。 */
    @FunctionalInterface
    public interface ClientReceiver {
        void receive(Minecraft client, FriendlyByteBuf buf);
    }

    @FunctionalInterface
    public interface JoinListener {
        void onJoin(ServerPlayer player, MinecraftServer server);
    }

    public static void registerServer(ResourceLocation channelId, ServerReceiver receiver) {
        if (!SERVER_REGISTERED.add(channelId)) {
            return;
        }
        channel(channelId)
                .messageBuilder(ToServer.class, ID_TO_SERVER, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToServer::encode)
                .decoder(ToServer::decode)
                .consumerMainThread((payload, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender == null) {
                        return;
                    }
                    MinecraftServer server = sender.server;
                    if (server == null) {
                        return;
                    }
                    receiver.receive(server, sender, payload.open());
                })
                .add();
    }

    public static void registerClient(ResourceLocation channelId, ClientReceiver receiver) {
        if (!CLIENT_REGISTERED.add(channelId)) {
            return;
        }
        channel(channelId)
                .messageBuilder(ToClient.class, ID_TO_CLIENT, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ToClient::encode)
                .decoder(ToClient::decode)
                .consumerMainThread((payload, context) -> {
                    Minecraft client = Minecraft.getInstance();
                    receiver.receive(client, payload.open());
                })
                .add();
    }

    /** {@code ServerPlayNetworking.canSend(player, id)} */
    public static boolean canSend(ServerPlayer player, ResourceLocation channelId) {
        if (player == null || player.connection == null) {
            return false;
        }
        Connection connection = player.connection.connection;
        return connection != null && channel(channelId).isRemotePresent(connection);
    }

    /** {@code ClientPlayNetworking.canSend(id)} */
    public static boolean canSend(ResourceLocation channelId) {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener listener = client == null ? null : client.getConnection();
        if (listener == null || listener.getConnection() == null) {
            return false;
        }
        return channel(channelId).isRemotePresent(listener.getConnection());
    }

    /** {@code ServerPlayNetworking.send(player, id, buf)} */
    public static void send(ServerPlayer player, ResourceLocation channelId, FriendlyByteBuf data) {
        if (player == null || data == null) {
            return;
        }
        channel(channelId).send(PacketDistributor.PLAYER.with(() -> player), new ToClient(bytes(data)));
    }

    /** {@code ClientPlayNetworking.send(id, buf)} */
    public static void sendToServer(ResourceLocation channelId, FriendlyByteBuf data) {
        if (data == null) {
            return;
        }
        channel(channelId).sendToServer(new ToServer(bytes(data)));
    }

    /** {@code PlayerLookup.tracking(level, pos)} */
    public static List<ServerPlayer> tracking(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return List.of();
        }
        return level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false);
    }

    /** {@code PlayerLookup.all(server)} */
    public static List<ServerPlayer> allPlayers(MinecraftServer server) {
        return server == null ? List.of() : server.getPlayerList().getPlayers();
    }

    /**
     * {@code ServerPlayConnectionEvents.JOIN.register(...)}：玩家进服（握手完成）后回调。
     * 监听器只装一次，普通事件总线在 mod 构造阶段就已经可用。
     */
    public static void onJoin(JoinListener listener) {
        JOIN_LISTENERS.add(listener);
    }

    /** {@code ClientPlayConnectionEvents.DISCONNECT.register(...)} */
    public static void onDisconnect(Runnable listener) {
        DISCONNECT_LISTENERS.add(listener);
    }

    /** 由 {@code LtedForge} 在构造阶段挂到 Forge 事件总线。 */
    public static void installCommonHooks() {
        if (commonHooksInstalled) {
            return;
        }
        commonHooksInstalled = true;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Net::onPlayerLoggedIn);
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.server;
        if (server == null) {
            return;
        }
        for (JoinListener listener : JOIN_LISTENERS) {
            listener.onJoin(player, server);
        }
    }

    /** 由 {@code ClientBootstrap} 挂到客户端事件总线。 */
    public static void installClientHooks() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Net::onLoggingOut);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        for (Runnable listener : DISCONNECT_LISTENERS) {
            listener.run();
        }
    }

    private static SimpleChannel channel(ResourceLocation channelId) {
        return CHANNELS.computeIfAbsent(channelId, id -> NetworkRegistry.newSimpleChannel(
                id, () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals));
    }

    private static byte[] bytes(FriendlyByteBuf data) {
        byte[] out = new byte[data.readableBytes()];
        data.readBytes(out);
        return out;
    }

    /** 原始字节 payload：不做任何解析，保持与 Fabric 侧一致的包体。 */
    public abstract static class Wire {

        private final byte[] data;

        private Wire(byte[] data) {
            this.data = data;
        }

        final FriendlyByteBuf open() {
            return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        }

        final void write(FriendlyByteBuf out) {
            out.writeBytes(data);
        }

        static byte[] readAll(FriendlyByteBuf in) {
            byte[] data = new byte[in.readableBytes()];
            in.readBytes(data);
            return data;
        }
    }

    /**
     * C2S 消息体。
     *
     * <p>必须和 {@link ToClient} 分成两个类：Forge 的 {@code IndexedMessageCodec}
     * 用 {@code Class} 做 handler 的键，同一个类挂两个方向会互相覆盖。
     */
    public static final class ToServer extends Wire {

        ToServer(byte[] data) {
            super(data);
        }

        static void encode(ToServer payload, FriendlyByteBuf out) {
            payload.write(out);
        }

        static ToServer decode(FriendlyByteBuf in) {
            return new ToServer(readAll(in));
        }
    }

    /** S2C 消息体（与 {@link ToServer} 同字节布局）。 */
    public static final class ToClient extends Wire {

        ToClient(byte[] data) {
            super(data);
        }

        static void encode(ToClient payload, FriendlyByteBuf out) {
            payload.write(out);
        }

        static ToClient decode(FriendlyByteBuf in) {
            return new ToClient(readAll(in));
        }
    }
}
