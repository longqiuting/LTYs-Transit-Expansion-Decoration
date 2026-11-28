// 福州地铁售票机的数据：模式 / 面板颜色数据源 / 自选色 / 线路色 / LED 文字。
package dev.js.mtrheadlight.region.fuzhou;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * 售票机配置。和柱底那套一样：数据在服务端写入、同步给客户端，<b>渲染只读字段</b>。
 *
 * <ul>
 *   <li>{@code mode}：0 = 方速、1 = 原版、2 = 关机（只有行为不同，外观一样；关机用 off 贴图）；</li>
 *   <li>{@code colorSource}：0 = 车站色、1 = 线路色、2 = 玩家自选；</li>
 *   <li>{@code customColor}：自选色（{@code 0xRRGGBB}）；</li>
 *   <li>{@code lineColor}：玩家选的线路色（{@code 0xRRGGBB}）；</li>
 *   <li>{@code ledText}：LED 文字，默认「正常服务」；</li>
 *   <li>{@code ledScroll}：{@code true} = 像 PIDS 那样滚动，{@code false} = 居中固定。</li>
 * </ul>
 */
public class FuzhouMachineBlockEntity extends BlockEntity implements dev.js.mtrheadlight.machine.MachineConfigurable {

    public static final int MODE_FANGSU = 0;
    public static final int MODE_VANILLA = 1;
    public static final int MODE_OFF = 2;

    public static final int COLOR_STATION = 0;
    public static final int COLOR_LINE = 1;
    public static final int COLOR_CUSTOM = 2;

    public static final String DEFAULT_TEXT = "正常服务";

    private static final String TAG_MODE = "MachineMode";
    private static final String TAG_SOURCE = "ColorSource";
    private static final String TAG_CUSTOM = "CustomColor";
    private static final String TAG_LINE = "LineColor";
    private static final String TAG_TEXT = "LedText";
    private static final String TAG_SCROLL = "LedScroll";

    private int mode = MODE_VANILLA;
    private int colorSource = COLOR_STATION;
    private int customColor = 0xFFFFFF;
    private int lineColor = 0xFFFFFF;
    private String ledText = DEFAULT_TEXT;
    private boolean ledScroll = true;

    public FuzhouMachineBlockEntity(final BlockPos pos, final BlockState state) {
        super(FuzhouMachines.entityType(), pos, state);
    }

    public int mode() {
        return this.mode;
    }

    public int colorSource() {
        return this.colorSource;
    }

    public int customColor() {
        return this.customColor & 0xFFFFFF;
    }

    public int lineColor() {
        return this.lineColor & 0xFFFFFF;
    }

    public String ledText() {
        return this.ledText;
    }

    public boolean ledScroll() {
        return this.ledScroll;
    }

    public void applyConfig(final int newMode, final int newSource, final int newCustom,
                            final int newLine, final String newText, final boolean newScroll) {
        this.mode = Math.max(0, Math.min(2, newMode));
        this.colorSource = Math.max(0, Math.min(2, newSource));
        this.customColor = newCustom & 0xFFFFFF;
        this.lineColor = newLine & 0xFFFFFF;
        this.ledText = newText == null ? DEFAULT_TEXT : newText;
        this.ledScroll = newScroll;
        if (this.world != null && !this.world.isClient) {
            this.markDirty();
            this.sync();
        }
    }

    /** 服务端 → 客户端同步（只发状态，不需要自定义包）。 */
    private void sync() {
        if (this.world == null || this.world.isClient) {
            return;
        }
        try {
            final BlockState state = this.world.getBlockState(this.pos);
            this.world.updateListeners(this.pos, state, state, 3);
        } catch (final Throwable t) {
            LteLog.LOGGER.debug("[lte] fuzhou machine could not refresh listeners", t);
        }
    }

    @Override
    public void readNbt(final NbtCompound nbt) {
        super.readNbt(nbt);
        this.mode = nbt.getInt(TAG_MODE);
        this.colorSource = nbt.contains(TAG_SOURCE) ? nbt.getInt(TAG_SOURCE) : COLOR_STATION;
        this.customColor = nbt.contains(TAG_CUSTOM) ? nbt.getInt(TAG_CUSTOM) : 0xFFFFFF;
        this.lineColor = nbt.contains(TAG_LINE) ? nbt.getInt(TAG_LINE) : 0xFFFFFF;
        this.ledText = nbt.contains(TAG_TEXT) ? nbt.getString(TAG_TEXT) : DEFAULT_TEXT;
        this.ledScroll = !nbt.contains(TAG_SCROLL) || nbt.getBoolean(TAG_SCROLL);
    }

    @Override
    public void writeNbt(final NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt(TAG_MODE, this.mode);
        nbt.putInt(TAG_SOURCE, this.colorSource);
        nbt.putInt(TAG_CUSTOM, this.customColor);
        nbt.putInt(TAG_LINE, this.lineColor);
        nbt.putString(TAG_TEXT, this.ledText);
        nbt.putBoolean(TAG_SCROLL, this.ledScroll);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    // ── 统一配置面板（MachineConfigurable）────────────────────────────────

    /** 这台机器三项全支持：模式 / 颜色数据源 / LED 文字。 */
    @Override
    public dev.js.mtrheadlight.machine.MachineConfig machineConfig() {
        return new dev.js.mtrheadlight.machine.MachineConfig(this.mode, this.colorSource,
                this.customColor, this.lineColor, this.ledText, this.ledScroll,
                dev.js.mtrheadlight.machine.MachineConfig.CAP_MODE
                        | dev.js.mtrheadlight.machine.MachineConfig.CAP_COLOR
                        | dev.js.mtrheadlight.machine.MachineConfig.CAP_LED);
    }

    @Override
    public void applyMachineConfig(final dev.js.mtrheadlight.machine.MachineConfig config) {
        applyConfig(config.mode(), config.colorSource(), config.custom(), config.line(),
                config.text(), config.scroll());
    }
}
