// 厦门地铁柱子（柱底）：可染色那一圈 + 刷子开颜色面板 + 破坏时把属性留在掉落物上。
package dev.js.mtrheadlight.pole;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * {@code mtrlte:amtr_pole_bottom} —— 厦门地铁柱子的<b>柱底</b>。
 *
 * <h2>模型里的两条「带」</h2>
 * <p>素材 {@code amtr_pole_bottom.json} 是 17 个 element：
 * <ul>
 *   <li>0..8 —— 与柱中逐字相同的本体：灰混凝土核心（{@code #1}）+ 四周白色鳍片（{@code #0}），
 *       鳍片从 y=2 起（柱中是 y=0 起，所以柱底看上去「脚」矮一截）；</li>
 *   <li>9..16 —— Blockbench 里的 <b>{@code color} 分组</b>：一圈 y 3..3.9、厚 0.9 的凸带。
 *       这八块在模型里带 {@code "tintindex": 0}，颜色由方块染色 provider 每帧提供
 *       （{@link #effectiveTint}），<b>不是</b> BER 重画、也没有把颜色烘进贴图。</li>
 * </ul>
 *
 * <h2>交互</h2>
 * <ul>
 *   <li>手持 MTR 的刷子（{@code mtr.Items.BRUSH}）右键 → 服务端把当前颜色发给玩家，客户端开颜色面板
 *       （{@code ColorPickerScreen}，初始值 = 当前颜色）；选完把颜色发回来，服务端写进方块实体。</li>
 *   <li>潜行 + 刷子右键 → 恢复「跟随车站色」（服务端直接改，不走界面）。</li>
 *   <li>ctrl + 中键 → 把这一格的属性复制进手里的柱底物品（见 {@code PoleClient#tryCopyProperties}
 *       与 {@code MinecraftClientPickMixin}）；<b>不按 ctrl 的中键保持原版拾取</b>。</li>
 * </ul>
 * 颜色的一切写入都在服务端（{@link PoleNet} 的 C2S 包 → 服务端改方块实体 → 同步回客户端）。
 * 客户端只负责开面板和显示 —— 所以这个类里<b>没有任何</b> {@code net.minecraft.client} 引用。
 */
public final class AmtrPoleBottomBlock extends BlockWithEntity {

    public AmtrPoleBottomBlock(final Settings settings) {
        super(settings);
    }

    /**
     * 关键一行：{@code BlockWithEntity} 默认让 {@code getRenderType} 返回 {@link BlockRenderType#INVISIBLE}
     * （原版认为「带方块实体的方块由 BER 画」，于是<b>整块跳过方块模型</b>）。这里改回 MODEL：
     * 方块模型照常烘焙渲染，方块实体只用来存颜色，不需要任何 BER。
     */
    @Override
    public BlockRenderType getRenderType(final BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(final BlockPos pos, final BlockState state) {
        return new AmtrPoleBottomBlockEntity(pos, state);
    }

    /**
     * 方块实体的 ticker：只服务于「车站色还没解析出来」这一种状态（{@link AmtrPoleBottomBlockEntity#tick}）。
     *
     * <p>为什么需要：柱子可能是 {@code /setblock}、结构方块、旧存档放下的 —— 那些路径都不会走
     * {@link AmtrPoleBottomItem#place}，也就没有人触发「放下时解析车站色」。tick 里每隔
     * {@code RESOLVE_RETRY_TICKS} 试一次（数据到齐了就成功并从此不再试），代价只有一次判断。
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> net.minecraft.block.entity.BlockEntityTicker<T> getTicker(
            final World world, final BlockState state,
            final net.minecraft.block.entity.BlockEntityType<T> type) {
        if (type != PoleBlocks.bottomEntityType()) {
            return null;
        }
        return (net.minecraft.block.entity.BlockEntityTicker<T>)
                (net.minecraft.block.entity.BlockEntityTicker<AmtrPoleBottomBlockEntity>)
                        AmtrPoleBottomBlockEntity::tick;
    }

    /**
     * 刷子右键。
     *
     * <p>客户端只回 {@link ActionResult#SUCCESS}（挥手动画，并且不再往下传 —— 否则手上拿着柱底
     * 方块时右键会变成「往里放一个方块」）；服务端才是真有动作的那一侧：
     * 潜行 = 复位成跟随车站色，否则把当前颜色推给玩家让他开面板。
     */
    @Override
    public ActionResult onUse(final BlockState state, final World world, final BlockPos pos,
                              final PlayerEntity player, final Hand hand, final BlockHitResult hit) {
        if (!player.getStackInHand(hand).isOf(mtr.Items.BRUSH.get())) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(world.getBlockEntity(pos) instanceof AmtrPoleBottomBlockEntity pole)) {
            return ActionResult.SUCCESS;
        }
        if (player.isSneaking()) {
            pole.resetToStationColor();
        } else if (player instanceof ServerPlayerEntity serverPlayer) {
            PoleNet.sendOpen(serverPlayer, pos, pole);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * 染色 provider 那一侧的唯一入口 —— <b>每帧每面</b>都会走到这里，所以只读方块实体里缓存好的
     * 那个颜色（不查 MTR 数据、不建对象）。
     *
     * @return {@code 0xRRGGBB}；拿不到方块实体（区块刚换 / 被拆）就当白色
     */
    public static int effectiveTint(@Nullable final BlockView world, @Nullable final BlockPos pos) {
        if (world == null || pos == null) {
            return PoleColors.WHITE;
        }
        if (world.getBlockEntity(pos) instanceof AmtrPoleBottomBlockEntity pole) {
            return pole.cacheOnlyColor();
        }
        return PoleColors.WHITE;
    }

    /**
     * 破坏时把属性留在掉落物上（否则刷好的颜色挖下来就没了，「批量放置」得重新刷一遍）。
     *
     * <p>只有自定义颜色才写 NBT —— 跟随车站色的柱子挖下来仍然是「跟随车站色」。
     */
    @Override
    public void afterBreak(final World world, final PlayerEntity player, final BlockPos pos,
                           final BlockState state, @Nullable final BlockEntity blockEntity,
                           final ItemStack tool) {
        if (!world.isClient && !player.isCreative()
                && blockEntity instanceof AmtrPoleBottomBlockEntity pole) {
            final ItemStack drop = new ItemStack(PoleBlocks.bottomItem());
            if (pole.isCustomMode()) {
                PoleItemData.write(drop, true, pole.customColor());
            }
            final ItemEntity entity = new ItemEntity(world,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop);
            entity.setToDefaultPickupDelay();
            world.spawnEntity(entity);
            return;
        }
        super.afterBreak(world, player, pos, state, blockEntity, tool);
    }
}
