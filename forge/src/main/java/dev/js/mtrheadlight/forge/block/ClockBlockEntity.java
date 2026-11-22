// 迁移自 dev.js.mtrheadlight.clock.ClockBlockEntity（yarn）→ official（mojmap）。
// 本轮是「可编译 + 可注册 + NBT 可存取」的子集：配置对象 ClockConfig / 全局配置 / 网络包
// 都还没迁，所以显示状态机在这里简化成「红石停摆 + 指针时间」两段。
package dev.js.mtrheadlight.forge.block;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.js.mtrheadlight.forge.LtedForge;

/**
 * 时钟方块 {@code mtrlte:clock} 的方块实体（Forge / mojmap 版）。
 *
 * <p>yarn → mojmap 的关键改名都在这里：
 * <ul>
 *   <li>{@code writeNbt(NbtCompound)} → {@link #saveAdditional(CompoundTag)}；</li>
 *   <li>{@code readNbt(NbtCompound)} → {@link #load(CompoundTag)}；</li>
 *   <li>{@code toInitialChunkDataNbt()} → {@link #getUpdateTag()}；</li>
 *   <li>{@code toUpdatePacket()} → {@link #getUpdatePacket()}，
 *       {@code BlockEntityUpdateS2CPacket.create(this)} → {@code ClientboundBlockEntityDataPacket.create(this)}；</li>
 *   <li>{@code markDirty()} → {@link #setChanged()}；</li>
 *   <li>{@code world} → {@link #level}，{@code World.isClient} → {@link Level#isClientSide}；</li>
 *   <li>{@code Registries.SOUND_EVENT} → {@link BuiltInRegistries#SOUND_EVENT}；</li>
 *   <li>{@code SoundCategory.BLOCKS} → {@link SoundSource#BLOCKS}。</li>
 * </ul>
 *
 * <p>真正接上 {@code ClockConfig} 之后，{@code saveAdditional} 只是把整份配置写进
 * {@code Clock} 那个子 tag —— 和 Fabric 版同一段代码。
 */
public class ClockBlockEntity extends BlockEntity {

    static final Logger LOGGER = LoggerFactory.getLogger("lte-forge");

    /** 恢复供电后追上当前时间用多少 tick（与 Fabric 版一致）。 */
    public static final int CATCHUP_TICKS = 26;

    private static final int CHECK_INTERVAL = 20;
    private static final int STRIKE_GAP = 8;
    private static final String TAG_CLOCK = "Clock";
    private static final String TAG_SOUND = "Sound";
    private static final String TAG_CHIME = "Chime";
    private static final String TAG_REDSTONE = "Redstone";
    private static final String DEFAULT_SOUND = "minecraft:block.bell.use";

    private String soundId = DEFAULT_SOUND;
    private boolean chime = true;
    private boolean redstone = false;

    private double displayNow;
    private double gapRemaining;
    private double gapPerTick;
    private boolean initialized;
    private boolean stopped;
    private int tickCounter;
    private int pendingStrikes;
    private long nextStrikeTick;

    public ClockBlockEntity(final BlockPos pos, final BlockState state) {
        super(LtedForge.CLOCK_BE.get(), pos, state);
    }

    // ------------------------------------------------------------------ 配置

    public String soundId() {
        return this.soundId;
    }

    public void setSoundId(final String value) {
        this.soundId = value == null || value.isBlank() ? DEFAULT_SOUND : value;
        sync();
    }

    public boolean chime() {
        return this.chime;
    }

    public void setChime(final boolean value) {
        this.chime = value;
        sync();
    }

    public boolean redstoneControlled() {
        return this.redstone;
    }

    public void setRedstoneControlled(final boolean value) {
        this.redstone = value;
        sync();
    }

    /** 改配置后必须同时落盘 + 通知客户端（Fabric 版的三条路，这里只剩两条）。 */
    private void sync() {
        this.setChanged();
        final Level world = this.level;
        if (world == null || world.isClientSide) {
            return;
        }
        final BlockState state = world.getBlockState(this.worldPosition);
        world.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    // ------------------------------------------------------------------ 时间

    public double displaySeconds() {
        return this.displayNow;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    // ------------------------------------------------------------------ tick

    public static void tick(final Level level, final BlockPos pos, final BlockState state,
                            final ClockBlockEntity entity) {
        try {
            entity.tickOnce(level, pos, state);
        } catch (final Throwable t) {
            LOGGER.warn("[lte] clock at {} failed to tick", pos, t);
        }
    }

    private void tickOnce(final Level level, final BlockPos pos, final BlockState state) {
        final long time = level.getDayTime() % 24000L;
        final double actual = (time / 24000.0D) * 86400.0D;
        final boolean stop = this.redstone && !state.getValue(ClockBlock.POWERED);

        if (!this.initialized) {
            this.initialized = true;
            this.displayNow = actual;
        }
        if (stop) {
            this.stopped = true;
            this.gapRemaining = 0.0D;
            this.gapPerTick = 0.0D;
        } else {
            if (this.stopped) {
                // 刚恢复供电：26 tick 内匀速把差吃完，别瞬跳。
                this.stopped = false;
                this.gapRemaining = actual - this.displayNow;
                this.gapPerTick = this.gapRemaining / CATCHUP_TICKS;
            }
            if (this.gapRemaining > 0.0D) {
                this.gapRemaining = Math.max(0.0D, this.gapRemaining - this.gapPerTick);
                this.displayNow = actual - this.gapRemaining;
            } else {
                this.displayNow = actual;
            }
        }

        if (level.isClientSide) {
            return;
        }
        this.tickChime(level, pos, stop);
    }

    /** 整点报时（服务端权威）。 */
    private void tickChime(final Level level, final BlockPos pos, final boolean stop) {
        if (stop) {
            this.pendingStrikes = 0;
            this.nextStrikeTick = 0L;
        }
        if (this.pendingStrikes > 0 && level.getGameTime() >= this.nextStrikeTick) {
            playSound(level, pos);
            this.pendingStrikes--;
            this.nextStrikeTick = level.getGameTime() + STRIKE_GAP;
        }
        if (++this.tickCounter % CHECK_INTERVAL != 0 || !this.chime || stop) {
            return;
        }
        final int minute = (int) Math.floor(this.displayNow / 60.0D) % 60;
        if (minute != 0) {
            return;
        }
        final int hour12 = (int) Math.floor(this.displayNow / 3600.0D) % 12;
        this.pendingStrikes = Math.max(1, hour12);
        this.nextStrikeTick = level.getGameTime() + STRIKE_GAP;
    }

    /** 配置里的音效 id → {@link SoundEvent}；找不到退回原版钟声。 */
    public SoundEvent soundEvent() {
        final ResourceLocation id = ResourceLocation.tryParse(this.soundId);
        if (id != null) {
            return BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(SoundEvents.BELL_BLOCK);
        }
        return SoundEvents.BELL_BLOCK;
    }

    private void playSound(final Level level, final BlockPos pos) {
        level.playSound(null, pos, soundEvent(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** 点「试听」时服务端播一下。 */
    public void playPreview() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        playSound(this.level, this.worldPosition);
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        final CompoundTag cfg = new CompoundTag();
        cfg.putString(TAG_SOUND, this.soundId);
        cfg.putBoolean(TAG_CHIME, this.chime);
        cfg.putBoolean(TAG_REDSTONE, this.redstone);
        tag.put(TAG_CLOCK, cfg);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TAG_CLOCK)) {
            final CompoundTag cfg = tag.getCompound(TAG_CLOCK);
            this.soundId = cfg.getString(TAG_SOUND);
            this.chime = cfg.getBoolean(TAG_CHIME);
            this.redstone = cfg.getBoolean(TAG_REDSTONE);
        }
    }

    /** 客户端「这块钟长什么样」的那一份数据（1.20.1 默认实现是空 compound）。 */
    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    /** 默认实现返回 null，等于「永远不发同步包」。 */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
