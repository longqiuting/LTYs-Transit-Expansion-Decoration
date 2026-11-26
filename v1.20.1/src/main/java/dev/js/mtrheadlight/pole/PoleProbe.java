// 柱子渲染一次性探针（Fabric）：看向柱底时打一行诊断，用来判定「方块模型到底有没有面」。
package dev.js.mtrheadlight.pole;

import dev.js.mtrheadlight.LteLog;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * 柱子渲染探针：准星指向柱底（或柱中）时，每 2 秒最多打一行日志，内容包括：
 *
 * <ul>
 *   <li>方块 id、方块实体是否存在/类型；</li>
 *   <li>该状态的 bakery 模型是不是 builtin（缺模型时会变成 builtin）；</li>
 *   <li>烘焙出来的面数（0 = 方块模型根本没几何；&gt;0 但画不出来 = 渲染层/剔除问题）。</li>
 * </ul>
 *
 * 只在客户端、只在准星命中柱子时说话，平时一行不刷；用于「看不见但也查不出报错」这种场景。
 */
public final class PoleProbe {

    private static long lastLog;

    private PoleProbe() {
    }

    public static void register() {
        try {
            ClientTickEvents.END_CLIENT_TICK.register(PoleProbe::onTick);
            LteLog.LOGGER.info("[lte] pole probe armed (aim at a pole to dump a one-shot report)");
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole probe unavailable", t);
        }
    }

    private static void onTick(final MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - lastLog < 2000L) {
            return;
        }
        if (!(client.crosshairTarget instanceof final BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        final BlockPos pos = hit.getBlockPos();
        final BlockState state = client.world.getBlockState(pos);
        final var block = state.getBlock();
        final Block middle = PoleBlocks.middle();
        if (!(block instanceof AmtrPoleBottomBlock) && (middle == null || block != middle)) {
            return;
        }
        lastLog = now;

        final Object entity = client.world.getBlockEntity(pos);
        int quads = -1;
        boolean builtin = false;
        try {
            final BakedModel model = client.getBlockRenderManager().getModel(state);
            builtin = model.isBuiltin();
            int total = 0;
            for (final Direction direction : Direction.values()) {
                total += model.getQuads(state, direction, Random.create(42L)).size();
            }
            total += model.getQuads(state, null, Random.create(42L)).size();
            quads = total;
        } catch (final Throwable t) {
            LteLog.LOGGER.warn("[lte] pole probe: model lookup failed", t);
        }
        LteLog.LOGGER.info("[lte] pole probe: pos={} state={} block={} be={} builtin={} quads={}",
                pos, state, block, entity == null ? "none" : entity.getClass().getSimpleName(), builtin, quads);
    }
}
