package dev.js.mtrheadlight.forge.platform;

import dev.js.mtrheadlight.LteLog;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;

/**
 * 方块实体 / 实体渲染器注册 shim。
 *
 * <p>Fabric 的 {@code BlockEntityRendererRegistry.register} / {@code EntityRendererRegistry.register}
 * 可以在 {@code ClientModInitializer} 里随时调用；Forge 必须等到
 * {@link EntityRenderersEvent.RegisterRenderers}。所以这里先把调用排队，
 * 由 {@link #onRegisterRenderers(EntityRenderersEvent.RegisterRenderers)} 统一回放。
 *
 * <p><b>顺序陷阱（实测，Forge 47.4.0 / 1.20.1）：</b>{@code RegisterRenderers} 早于
 * {@code FMLClientSetupEvent}。前者在 {@code Minecraft} 构造器里由
 * {@code ForgeHooksClient.initClientHooks} 发出，后者晚约 0.5 秒。所以「在 FMLClientSetupEvent 里
 * 排队、等 RegisterRenderers 回放」是反的：事件到的时候队列还是空的，19 个 BER 全部静默丢失
 * （方块只剩一块看不见的模型，PIDS / PSD-LCD / 时钟 / 贴墙 logo / eyecandy 全都空白）。
 *
 * <p>修法：把「客户端初始化」也交给这里调度 —— {@link #onRegisterRenderers} 一进来就先
 * {@link #ensureClientInit()}（跑一次 {@code LtedClient#onInitializeClient}，队列因此在这一刻填满），
 * 再回放。{@code ClientBootstrap} 里的 FMLClientSetupEvent 也调同一个方法，谁先到谁填队列，
 * 不依赖 Forge 版本的事件顺序。
 */
public final class Renderers {

    private static final List<Consumer<EntityRenderersEvent.RegisterRenderers>> PENDING = new ArrayList<>();

    /** 客户端初始化的真正内容（{@code new LtedClient().onInitializeClient()}），由 ClientBootstrap 注入。 */
    private static Runnable clientInit;
    private static int initState; // 0 = 未跑，1 = 已跑，2 = 失败（不再重试）
    private static int berCount;
    private static int entityCount;

    /**
     * 事件已经发过之后才来注册的兜底：直接写进 Forge 给的那张表（MC 的静态 map），
     * 而不是塞进一个永远不会再被回放的队列。
     */
    private static EntityRenderersEvent.RegisterRenderers fired;

    private Renderers() {
    }

    public static void setClientInit(Runnable init) {
        clientInit = init;
    }

    /**
     * 跑一次客户端初始化（幂等）。必须在 {@code RegisterRenderers} 回放之前发生，
     * 否则队列是空的、BER 全丢。
     */
    public static void ensureClientInit() {
        if (initState != 0) {
            return;
        }
        Runnable init = clientInit;
        if (init == null) {
            return; // ClientBootstrap 还没接线（理论上不会发生）
        }
        initState = 1;
        try {
            init.run();
        } catch (Throwable t) {
            initState = 2;
            LteLog.LOGGER.error("[lte] client initialisation failed", t);
        }
    }

    public static <T extends BlockEntity> void ber(BlockEntityType<T> type, BlockEntityRendererProvider<T> provider) {
        if (type == null || provider == null) {
            // 不能静默：BET 为 null 说明注册顺序又乱了，这条方块会永远不显示。
            LteLog.LOGGER.error("[lte] block entity renderer skipped: type={} provider={}", type, provider);
            return;
        }
        if (fired != null) {
            registerNow(fired, () -> fired.registerBlockEntityRenderer(type, provider), type.toString());
            return;
        }
        PENDING.add(event -> event.registerBlockEntityRenderer(type, provider));
        berCount++;
    }

    public static <T extends Entity> void entity(EntityType<T> type, EntityRendererProvider<T> provider) {
        if (type == null || provider == null) {
            LteLog.LOGGER.error("[lte] entity renderer skipped: type={} provider={}", type, provider);
            return;
        }
        if (fired != null) {
            registerNow(fired, () -> fired.registerEntityRenderer(type, provider), type.toString());
            return;
        }
        PENDING.add(event -> event.registerEntityRenderer(type, provider));
        entityCount++;
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 先填队列：Forge 1.20.1 里这个事件早于 FMLClientSetupEvent（见类注释）。
        ensureClientInit();

        List<Consumer<EntityRenderersEvent.RegisterRenderers>> queued = new ArrayList<>(PENDING);
        PENDING.clear();
        int ok = 0;
        for (Consumer<EntityRenderersEvent.RegisterRenderers> registration : queued) {
            try {
                registration.accept(event);
                ok++;
            } catch (Throwable t) {
                LteLog.LOGGER.error("[lte] block entity renderer registration failed", t);
            }
        }
        fired = event;
        LteLog.LOGGER.info("[lte] renderers registered: {} block entity, {} entity", ok, entityCount);
    }

    private static void registerNow(EntityRenderersEvent.RegisterRenderers event, Runnable action, String what) {
        try {
            action.run();
        } catch (Throwable t) {
            LteLog.LOGGER.error("[lte] late renderer registration failed: {}", what, t);
        }
    }
}
