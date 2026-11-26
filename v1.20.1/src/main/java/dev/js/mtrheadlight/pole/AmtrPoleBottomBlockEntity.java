// 柱底的方块实体：只存「跟随车站色 / 自定义颜色」这一件事，并且负责让客户端立刻看到。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * {@code mtrlte:amtr_pole_bottom} 的方块实体。
 *
 * <h2>它只存三样东西</h2>
 * <ul>
 *   <li>{@link #MODE_AUTO} / {@link #MODE_CUSTOM}：柱子上的那圈颜色是「跟车站走」还是「玩家刷的」；</li>
 *   <li>{@code customColor}：玩家用颜色面板选的颜色（{@code 0xRRGGBB}）；</li>
 *   <li>{@code stationColor}：<b>放下时解析出来并缓存的</b>车站色（见 {@link PoleColors}）。</li>
 * </ul>
 *
 * <h2>为什么每帧不查 MTR 数据</h2>
 * <p>方块染色 provider（{@code PoleClient} 里注册的那个）是<b>每帧、每个面</b>都会调一次的。
 * 所以 {@link #effectiveColor()} 只读字段；只有字段里没有车站色、而且距离上次尝试已经过了
 * {@value #RESOLVE_RETRY_TICKS} tick 时，才会去问一次 {@link PoleColors}，问到了就<b>写回字段</b>
 * 从此不再问。服务端问到的会同时落盘 + 同步，客户端问到的只是本地那份缓存的补充。
 *
 * <h2>同步（改了颜色客户端必须立刻变）</h2>
 * <p>{@code markDirty()} 只负责落盘、<b>不发包</b>。所以每次写入都走 {@link #syncToClients()}：
 * {@code world.updateListeners(...)} + {@link #toUpdatePacket()} + {@link #toInitialChunkDataNbt()}
 * 三件套（少了 {@code toUpdatePacket} 就是「客户端永远收不到」，时钟那边踩过），
 * 外加一条 {@link PoleNet} 的直接推送（不等区块 flush）。
 */
public class AmtrPoleBottomBlockEntity extends BlockEntity {

    /** 跟随车站色（默认）。 */
    public static final int MODE_AUTO = 0;
    /** 玩家自定义颜色。 */
    public static final int MODE_CUSTOM = 1;

    /** 解析失败后隔多久再试（tick）。 */
    private static final int RESOLVE_RETRY_TICKS = 100;

    private static final String TAG_MODE = "PoleMode";
    private static final String TAG_COLOR = "PoleColor";
    private static final String TAG_STATION = "PoleStation";

    /** 0 = AUTO，1 = CUSTOM。 */
    private int mode = MODE_AUTO;
    /** CUSTOM 时用的颜色。 */
    private int customColor = PoleColors.WHITE;
    /** 解析出来的车站色（AUTO 时用）；0 = 还没解析出来。 */
    private int stationColor;
    /** 下次允许再问 {@link PoleColors} 的 tick（限流，客户端每帧调也不会打爆数据缓存）。 */
    private long nextResolveTick;

    public AmtrPoleBottomBlockEntity(final BlockPos pos, final BlockState state) {
        super(PoleBlocks.bottomEntityType(), pos, state);
    }

    // ------------------------------------------------------------------ 读

    /** 是不是「玩家自定义颜色」模式。 */
    public boolean isCustomMode() {
        return this.mode == MODE_CUSTOM;
    }

    /** 玩家选的颜色（AUTO 模式下没意义，读出来是白色）。 */
    public int customColor() {
        return this.customColor & 0xFFFFFF;
    }

    /**
     * 染色 provider 用的**只读**版本：绝不触发车站色解析。区块网格化、每帧每面都会调，
     * 一旦在这里摸 {@code ClientData} 就会把渲染线程拖进未知代价（甚至坏网格）。
     */
    public int cacheOnlyColor() {
        if (this.mode == MODE_CUSTOM) {
            return 0xFF000000 | (this.customColor & 0xFFFFFF);
        }
        return this.stationColor == 0 ? PoleColors.WHITE : (0xFF000000 | (this.stationColor & 0xFFFFFF));
    }

    /**
     * 「当前该用的颜色」，必要时解析一次车站色 —— 供放置/交互路径用，<b>不要</b>在渲染路径上调。
     *
     * <p>顺序：CUSTOM → 玩家选的颜色；AUTO → 缓存的车站色；缓存里没有就试一次解析；
     * 还不行就是白色（{@link PoleColors#WHITE}）。
     */
    public int effectiveColor() {
        if (this.mode == MODE_CUSTOM) {
            return 0xFF000000 | (this.customColor & 0xFFFFFF);
        }
        if (this.stationColor == 0) {
            this.resolveStationColor(false);
        }
        return this.stationColor == 0 ? PoleColors.WHITE : (0xFF000000 | (this.stationColor & 0xFFFFFF));
    }

    // ------------------------------------------------------------------ tick

    /**
     * 方块实体 ticker（服务端 / 客户端都会调，只有服务端真的做事）。
     *
     * <p>唯一职责：AUTO 模式下车站色还空着 → 每隔 {@link #RESOLVE_RETRY_TICKS} tick 试一次解析。
     * 解析成功后 {@code stationColor != 0}，这里就只剩一次整数比较。
     */
    public static void tick(final World world, final BlockPos pos, final BlockState state,
                            final AmtrPoleBottomBlockEntity pole) {
        if (world == null || world.isClient || pole == null) {
            return;
        }
        if (pole.mode == MODE_AUTO && pole.stationColor == 0) {
            pole.resolveStationColor(false);
        }
    }

    // ------------------------------------------------------------------ 写（服务端）

    /**
     * 玩家在颜色面板里点了「完成」→ 服务端写进这里。
     *
     * <p>只在服务端调（{@link PoleNet} 收到 C2S 包后）。写完立刻 {@link #syncToClients()}。
     */
    public void setCustomColor(final int rgb) {
        this.mode = MODE_CUSTOM;
        this.customColor = rgb & 0xFFFFFF;
        this.markDirty();
        this.syncToClients();
    }

    /**
     * 潜行 + 刷子右键 = 恢复「跟随车站色」。
     *
     * <p>顺手把缓存的车站色清掉（下次渲染 / 下次查询会重新解析一次），这样「线路刚改过色」时
     * 复位一次就能拿到新颜色。
     */
    public void resetToStationColor() {
        this.mode = MODE_AUTO;
        this.customColor = PoleColors.WHITE;
        this.stationColor = 0;
        this.nextResolveTick = 0L;
        this.markDirty();
        this.ensureStationColor();
        this.syncToClients();
    }

    /**
     * 放下柱子时套用物品堆里的属性（{@link AmtrPoleBottomItem#place}）。
     *
     * <p>没有 NBT = 跟随车站色：这时把缓存清掉并<b>立刻</b>解析一次，于是「放下就取到车站色」。
     */
    public void applyFromItem(final ItemStack stack) {
        if (PoleItemData.isCustom(stack)) {
            this.mode = MODE_CUSTOM;
            this.customColor = PoleItemData.color(stack);
        } else {
            this.mode = MODE_AUTO;
            this.customColor = PoleColors.WHITE;
            this.stationColor = 0;
            this.nextResolveTick = 0L;
        }
        this.markDirty();
        this.ensureStationColor();
        this.syncToClients();
    }

    /** 客户端收到 S2C 同步包：把服务端那份状态写进本地这份方块实体（不落盘、不回推）。 */
    public void applySynced(final int newMode, final int rgb) {
        this.mode = newMode == MODE_CUSTOM ? MODE_CUSTOM : MODE_AUTO;
        if (this.mode == MODE_CUSTOM) {
            this.customColor = rgb & 0xFFFFFF;
        } else {
            // 服务端说「跟随车站色」：那是它算出来的车站色（0 = 它也没算出来，客户端自己去算）。
            this.stationColor = rgb & 0xFFFFFF;
            this.nextResolveTick = 0L;
        }
    }

    /** 服务端：如果还没解析出车站色就现在试一次（放下 / 复位时调，不等渲染）。 */
    public void ensureStationColor() {
        if (this.mode == MODE_AUTO && this.stationColor == 0) {
            this.resolveStationColor(true);
        }
    }

    /**
     * 问一次 {@link PoleColors}。
     *
     * @param force {@code true} = 无视限流（放下 / 复位时）；{@code false} = 染色 provider 那条热路径
     */
    private void resolveStationColor(final boolean force) {
        final World world = this.world;
        if (world == null) {
            return;
        }
        final long now = world.getTime();
        if (!force && now < this.nextResolveTick) {
            return;
        }
        this.nextResolveTick = now + RESOLVE_RETRY_TICKS;
        final int rgb = PoleColors.resolve(world, this.pos);
        if (rgb == 0) {
            return;
        }
        this.stationColor = rgb & 0xFFFFFF;
        if (!world.isClient) {
            // 服务端解析出来的要落盘 + 让客户端也拿到（客户端自己算的不回推，避免来回刷包）。
            this.markDirty();
            this.syncToClients();
        }
    }

    /**
     * 把「这一格的数据变了」告诉看得见它的客户端。
     *
     * <p>只发服务端；客户端调到直接返回。
     */
    private void syncToClients() {
        final World world = this.world;
        if (world == null || world.isClient) {
            return;
        }
        try {
            final BlockState state = world.getBlockState(this.pos);
            world.updateListeners(this.pos, state, state, Block.NOTIFY_LISTENERS);
        } catch (final Throwable t) {
            LteLog.LOGGER.debug("[lte] pole at {} could not refresh listeners", this.pos, t);
        }
        try {
            // 直接发「原始字段」而不是 effectiveColor()：AUTO 模式下车站色还没解析出来时发 0，
            // 客户端收到 0 就会自己去算（而不是把白色当成答案缓存下来）。
            final int raw = this.mode == MODE_CUSTOM ? (this.customColor & 0xFFFFFF)
                    : (this.stationColor & 0xFFFFFF);
            PoleNet.sendSync(world, this.pos, this.mode, raw);
        } catch (final Throwable t) {
            LteLog.LOGGER.debug("[lte] pole at {} could not push its colour", this.pos, t);
        }
    }

    // ------------------------------------------------------------------ NBT

    @Override
    public void writeNbt(final NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt(TAG_MODE, this.mode);
        nbt.putInt(TAG_COLOR, this.customColor & 0xFFFFFF);
        nbt.putInt(TAG_STATION, this.stationColor & 0xFFFFFF);
    }

    @Override
    public void readNbt(final NbtCompound nbt) {
        super.readNbt(nbt);
        this.mode = nbt.getInt(TAG_MODE) == MODE_CUSTOM ? MODE_CUSTOM : MODE_AUTO;
        final int color = nbt.getInt(TAG_COLOR);
        this.customColor = color == 0 ? PoleColors.WHITE : (color & 0xFFFFFF);
        this.stationColor = nbt.getInt(TAG_STATION) & 0xFFFFFF;
        this.nextResolveTick = 0L;
    }

    /**
     * 客户端「这块柱子长什么样」的那一份数据（默认实现是空 compound —— 不覆写的话，
     * 客户端收到的永远是一份空 NBT）。
     */
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    /**
     * 数据变了要发给客户端的那一份。
     *
     * <p>默认实现返回 {@code null}，而 {@code ChunkHolder} 看到 {@code null} 就不发包 ——
     * 那样 {@code world.updateListeners} 调多少次客户端都不会变。
     */
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
