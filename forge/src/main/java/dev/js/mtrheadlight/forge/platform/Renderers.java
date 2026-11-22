package dev.js.mtrheadlight.forge.platform;

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
 */
public final class Renderers {

    private static final List<Consumer<EntityRenderersEvent.RegisterRenderers>> PENDING = new ArrayList<>();

    private Renderers() {
    }

    public static <T extends BlockEntity> void ber(BlockEntityType<T> type, BlockEntityRendererProvider<T> provider) {
        if (type == null || provider == null) {
            return;
        }
        PENDING.add(event -> event.registerBlockEntityRenderer(type, provider));
    }

    public static <T extends Entity> void entity(EntityType<T> type, EntityRendererProvider<T> provider) {
        if (type == null || provider == null) {
            return;
        }
        PENDING.add(event -> event.registerEntityRenderer(type, provider));
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        List<Consumer<EntityRenderersEvent.RegisterRenderers>> queued = new ArrayList<>(PENDING);
        PENDING.clear();
        for (Consumer<EntityRenderersEvent.RegisterRenderers> registration : queued) {
            registration.accept(event);
        }
    }
}
