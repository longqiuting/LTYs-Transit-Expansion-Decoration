package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 柱底的方块实体（Forge / mojmap 版）。
 *
 * <p>与 Fabric 树 {@code pole/AmtrPoleBottomBlockEntity.java} 一一对应，只换名字：
 * {@code writeNbt/readNbt} → {@link #saveAdditional}/{@link #load}，{@code markDirty} →
 * {@link #setChanged}，{@link #toInitialChunkDataNbt} → {@link #getUpdateTag}，
 * {@link #toUpdatePacket} → {@link #getUpdatePacket}，{@code world.updateListeners} →
 * {@code level.sendBlockUpdated}。
 *
 * <p>同步三件套一件都不能少：{@code setChanged()} 只落盘、不发包；{@code getUpdatePacket()} 默认返回
 * {@code null}（{@code ChunkHolder} 看到 null 就一个包都不发）；{@code getUpdateTag()} 默认是空 tag。
 */
public class AmtrPoleBottomBlockEntity extends BlockEntity {

    public static final int MODE_AUTO = 0;
    public static final int MODE_CUSTOM = 1;

    private static final int RESOLVE_RETRY_TICKS = 100;

    private static final String TAG_MODE = "PoleMode";
    private static final String TAG_COLOR = "PoleColor";
    private static final String TAG_STATION = "PoleStation";

    private int mode = MODE_AUTO;
    private int customColor = PoleColors.WHITE;
    private int stationColor;
    private long nextResolveTick;

    public AmtrPoleBottomBlockEntity(final BlockPos pos, final BlockState state) {
        super(PoleBlocks.bottomEntityType(), pos, state);
    }

    public boolean isCustomMode() {
        return this.mode == MODE_CUSTOM;
    }

    public int customColor() {
        return this.customColor & 0xFFFFFF;
    }

    /** 渲染用的只读颜色（不触发车站色解析）。 */
    public int cacheOnlyColor() {
        if (this.mode == MODE_CUSTOM) {
            return 0xFF000000 | (this.customColor & 0xFFFFFF);
        }
        return this.stationColor == 0 ? PoleColors.WHITE : (0xFF000000 | (this.stationColor & 0xFFFFFF));
    }

    /** 染色 provider 唯一该调的读方法：只读字段，必要时才解析一次车站色。 */
    public int effectiveColor() {
        if (this.mode == MODE_CUSTOM) {
            return 0xFF000000 | (this.customColor & 0xFFFFFF);
        }
        if (this.stationColor == 0) {
            this.resolveStationColor(false);
        }
        return this.stationColor == 0 ? PoleColors.WHITE : (0xFF000000 | (this.stationColor & 0xFFFFFF));
    }

    public void setCustomColor(final int rgb) {
        this.mode = MODE_CUSTOM;
        this.customColor = rgb & 0xFFFFFF;
        this.setChanged();
        this.syncToClients();
    }

    public void resetToStationColor() {
        this.mode = MODE_AUTO;
        this.customColor = PoleColors.WHITE;
        this.stationColor = 0;
        this.nextResolveTick = 0L;
        this.setChanged();
        this.ensureStationColor();
        this.syncToClients();
    }

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
        this.setChanged();
        this.ensureStationColor();
        this.syncToClients();
    }

    public void applySynced(final int newMode, final int rgb) {
        this.mode = newMode == MODE_CUSTOM ? MODE_CUSTOM : MODE_AUTO;
        if (this.mode == MODE_CUSTOM) {
            this.customColor = rgb & 0xFFFFFF;
        } else {
            this.stationColor = rgb & 0xFFFFFF;
            this.nextResolveTick = 0L;
        }
    }

    public void ensureStationColor() {
        if (this.mode == MODE_AUTO && this.stationColor == 0) {
            this.resolveStationColor(true);
        }
    }

    /** 方块实体 ticker：AUTO 模式下车站色还空着就每 {@link #RESOLVE_RETRY_TICKS} tick 试一次。 */
    public static void tick(final Level level, final BlockPos pos, final BlockState state,
                            final AmtrPoleBottomBlockEntity pole) {
        if (level == null || level.isClientSide || pole == null) {
            return;
        }
        if (pole.mode == MODE_AUTO && pole.stationColor == 0) {
            pole.resolveStationColor(false);
        }
    }

    private void resolveStationColor(final boolean force) {
        final Level level = this.level;
        if (level == null) {
            return;
        }
        final long now = level.getGameTime();
        if (!force && now < this.nextResolveTick) {
            return;
        }
        this.nextResolveTick = now + RESOLVE_RETRY_TICKS;
        final int rgb = PoleColors.resolve(level, this.worldPosition);
        if (rgb == 0) {
            return;
        }
        this.stationColor = rgb & 0xFFFFFF;
        if (!level.isClientSide) {
            this.setChanged();
            this.syncToClients();
        }
    }

    private void syncToClients() {
        final Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        try {
            final BlockState state = level.getBlockState(this.worldPosition);
            level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
        } catch (final Throwable t) {
            LteLog.LOGGER.debug("[lte] pole at {} could not refresh listeners", this.worldPosition, t);
        }
        try {
            final int raw = this.mode == MODE_CUSTOM ? (this.customColor & 0xFFFFFF)
                    : (this.stationColor & 0xFFFFFF);
            PoleNet.sendSync(level, this.worldPosition, this.mode, raw);
        } catch (final Throwable t) {
            LteLog.LOGGER.debug("[lte] pole at {} could not push its colour", this.worldPosition, t);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_MODE, this.mode);
        tag.putInt(TAG_COLOR, this.customColor & 0xFFFFFF);
        tag.putInt(TAG_STATION, this.stationColor & 0xFFFFFF);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        this.mode = tag.getInt(TAG_MODE) == MODE_CUSTOM ? MODE_CUSTOM : MODE_AUTO;
        final int color = tag.getInt(TAG_COLOR);
        this.customColor = color == 0 ? PoleColors.WHITE : (color & 0xFFFFFF);
        this.stationColor = tag.getInt(TAG_STATION) & 0xFFFFFF;
        this.nextResolveTick = 0L;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
