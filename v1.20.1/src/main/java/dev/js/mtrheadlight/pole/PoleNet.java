// 柱子的颜色通道 mtrlte:pole_color：开面板（S2C）/ 写颜色（C2S）/ 复位（C2S）/ 状态同步（S2C）。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 柱底的颜色通道 {@code mtrlte:pole_color}，一个通道四个包袱，第一字节区分：
 *
 * <ul>
 *   <li>{@link #KIND_OPEN} <b>S2C</b> —— 刷子右键后服务端把「当前模式 + 当前颜色」推给点它的玩家，
 *       客户端收到就开 {@code ColorPickerScreen}；</li>
 *   <li>{@link #KIND_SET} <b>C2S</b> —— 面板点「完成」，把选的颜色写回来；</li>
 *   <li>{@link #KIND_RESET} <b>C2S</b> —— 恢复「跟随车站色」（目前只有潜行右键那条路，服务端直接改，
 *       留着这个包是为了以后能做界面上的按钮）；</li>
 *   <li>{@link #KIND_SYNC} <b>S2C</b> —— 颜色改了：推给「看得见这根柱子」的所有客户端。</li>
 * </ul>
 *
 * <h2>为什么开面板要服务端点头</h2>
 * <p>颜色是服务端权威（要落盘、要防作弊），客户端不能自己编一份「当前颜色」出来 ——
 * 而且刷子右键本来就只在服务端的 {@code onUse} 里才有权威的方块实体。客户端拿到的这份只是
 * 面板的<b>初值</b>，一改完就发回来。
 *
 * <h2>{@link #KIND_SYNC} 为什么单独存在</h2>
 * <p>原版路径（{@code world.updateListeners} → {@code BlockEntityUpdateS2CPacket}）也修好了
 * （见 {@link AmtrPoleBottomBlockEntity#toUpdatePacket()}），但那条路要等区块 flush。
 * 改颜色是玩家点了「完成」就该立刻看见的事，所以再直接推一份。
 *
 * <h2>写回时校验什么（按顺序）</h2>
 * <ol>
 *   <li>玩家 / 坐标不为空；</li>
 *   <li>区块已加载；</li>
 *   <li>离方块不超过 {@value #MAX_DISTANCE} 格（刷子够得着的距离）；</li>
 *   <li>那一格真的是 {@link AmtrPoleBottomBlock}，方块实体真的是 {@link AmtrPoleBottomBlockEntity}；</li>
 *   <li>颜色本身就是 24 位（{@code & 0xFFFFFF}）。</li>
 * </ol>
 * 没装 LTED 的客户端 {@code canSend} 为 false，什么都不发生 —— 柱子本身照常显示。
 */
public final class PoleNet {

    /** 通道 id。 */
    public static final Identifier ID = new Identifier("mtrlte", "pole_color");

    /** S2C：开颜色面板（pos + 模式 + 当前颜色）。 */
    public static final byte KIND_OPEN = 0;
    /** C2S：写自定义颜色（pos + 颜色）。 */
    public static final byte KIND_SET = 1;
    /** C2S：恢复「跟随车站色」（pos）。 */
    public static final byte KIND_RESET = 2;
    /** S2C：状态变更（pos + 模式 + 颜色：CUSTOM 时是自定义色，AUTO 时是车站色，0 = 未知）。 */
    public static final byte KIND_SYNC = 3;
    /** C2S：ctrl+中键复制属性（pos）—— 服务端把这一格的属性写进玩家手里那堆柱底物品。 */
    public static final byte KIND_COPY = 4;

    /** 玩家离方块多远之内还能改颜色（格，平方比较）。 */
    private static final int MAX_DISTANCE = 64;

    private static boolean registered;

    private PoleNet() {
    }

    /** 服务端接收器，{@link PoleBlocks#register()} 里调一次。 */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        try {
            ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, sender) -> {
                final byte kind;
                final BlockPos pos;
                final int rgb;
                try {
                    kind = buf.readByte();
                    pos = buf.readBlockPos();
                    rgb = kind == KIND_SET ? buf.readInt() : 0;
                } catch (final Throwable bad) {
                    return; // 坏包：当没收到，绝不半截写数据。
                }
                server.execute(() -> {
                    if (kind == KIND_SET) {
                        applySet(player, pos, rgb);
                    } else if (kind == KIND_RESET) {
                        applyReset(player, pos);
                    } else if (kind == KIND_COPY) {
                        applyCopy(player, pos);
                    }
                });
            });
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole colour channel unavailable", t);
        }
    }

    // ---------------------------------------------------------------- S2C

    /** 把这一格的当前状态推给玩家（刷子右键 → 客户端开面板）。 */
    public static void sendOpen(final ServerPlayerEntity player, final BlockPos pos,
                                final AmtrPoleBottomBlockEntity pole) {
        if (player == null || pos == null || pole == null
                || !ServerPlayNetworking.canSend(player, ID)) {
            return;
        }
        final PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(KIND_OPEN);
        buf.writeBlockPos(pos);
        buf.writeInt(pole.isCustomMode() ? AmtrPoleBottomBlockEntity.MODE_CUSTOM
                : AmtrPoleBottomBlockEntity.MODE_AUTO);
        buf.writeInt(pole.effectiveColor());
        ServerPlayNetworking.send(player, ID, buf);
    }

    /**
     * 状态改完：推给看得见这根柱子的客户端。
     *
     * <p>{@code world} 必须是服务端世界；客户端调到直接返回（否则会自己给自己发包）。
     */
    public static void sendSync(final World world, final BlockPos pos, final int mode, final int rgb) {
        if (!(world instanceof ServerWorld serverWorld) || pos == null) {
            return;
        }
        for (final ServerPlayerEntity player : PlayerLookup.tracking(serverWorld, pos)) {
            if (ServerPlayNetworking.canSend(player, ID)) {
                ServerPlayNetworking.send(player, ID, syncPacket(pos, mode, rgb));
            }
        }
    }

    /** 打包 S2C「状态变更」（pos + 模式 + 颜色）。 */
    public static PacketByteBuf syncPacket(final BlockPos pos, final int mode, final int rgb) {
        final PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(KIND_SYNC);
        buf.writeBlockPos(pos);
        buf.writeInt(mode);
        buf.writeInt(rgb & 0xFFFFFF);
        return buf;
    }

    // ---------------------------------------------------------------- 写

    /** 打包 C2S「写颜色」（客户端调）。 */
    public static PacketByteBuf setPacket(final BlockPos pos, final int rgb) {
        final PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(KIND_SET);
        buf.writeBlockPos(pos);
        buf.writeInt(rgb & 0xFFFFFF);
        return buf;
    }

    /** 打包 C2S「复位」（客户端调）。 */
    public static PacketByteBuf resetPacket(final BlockPos pos) {
        final PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(KIND_RESET);
        buf.writeBlockPos(pos);
        return buf;
    }

    /** 打包 C2S「ctrl+中键复制属性」（客户端调）。 */
    public static PacketByteBuf copyPacket(final BlockPos pos) {
        final PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(KIND_COPY);
        buf.writeBlockPos(pos);
        return buf;
    }

    /** 服务端：校验 + 写自定义颜色。 */
    private static void applySet(final ServerPlayerEntity player, final BlockPos pos, final int rgb) {
        final AmtrPoleBottomBlockEntity pole = resolve(player, pos);
        if (pole == null) {
            return;
        }
        pole.setCustomColor(rgb & 0xFFFFFF);
        LteLog.LOGGER.debug("[lte] pole at {} coloured #{}", pos,
                String.format("%06X", Integer.valueOf(rgb & 0xFFFFFF)));
    }

    /** 服务端：校验 + 复位成跟随车站色。 */
    private static void applyReset(final ServerPlayerEntity player, final BlockPos pos) {
        final AmtrPoleBottomBlockEntity pole = resolve(player, pos);
        if (pole == null) {
            return;
        }
        pole.resetToStationColor();
    }

    /**
     * 服务端：ctrl+中键复制属性 —— 把这一格的状态写进玩家<b>主手</b>那堆柱底物品的 NBT。
     *
     * <p>为什么必须走服务端：物品堆的 NBT 是服务端权威的（放置时读的是服务端那一份）。
     * 客户端自己改本地 NBT 只是「看起来改了」，放下时服务端仍按旧的那份处理。
     * 写完刷新一次容器，客户端槽位随之更新（tooltip 也跟着变）。
     */
    private static void applyCopy(final ServerPlayerEntity player, final BlockPos pos) {
        final AmtrPoleBottomBlockEntity pole = resolve(player, pos);
        if (pole == null) {
            return;
        }
        final ItemStack held = player.getMainHandStack();
        if (!(held.getItem() instanceof AmtrPoleBottomItem)) {
            return;
        }
        PoleItemData.write(held, pole.isCustomMode(), pole.customColor());
        player.currentScreenHandler.sendContentUpdates();
        player.playerScreenHandler.sendContentUpdates();
        player.sendMessage(Text.translatable("message.mtrlte.amtr_pole.copied",
                pole.isCustomMode()
                        ? "#" + String.format("%06X", Integer.valueOf(pole.customColor()))
                        : Text.translatable("message.mtrlte.amtr_pole.auto").getString()), true);
        LteLog.LOGGER.debug("[lte] pole at {} copied to {}'s hand", pos, player.getName().getString());
    }

    /** 公共校验：距离 / 区块 / 方块 / 方块实体，任何一条不过都返回 {@code null}。 */
    private static AmtrPoleBottomBlockEntity resolve(final ServerPlayerEntity player, final BlockPos pos) {
        if (player == null || pos == null) {
            return null;
        }
        final World world = player.getWorld();
        if (world == null || !world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            return null;
        }
        if (player.getBlockPos().getSquaredDistance(pos) > (double) MAX_DISTANCE * MAX_DISTANCE) {
            return null;
        }
        if (!(world.getBlockState(pos).getBlock() instanceof AmtrPoleBottomBlock)) {
            return null;
        }
        return world.getBlockEntity(pos) instanceof AmtrPoleBottomBlockEntity pole ? pole : null;
    }
}
