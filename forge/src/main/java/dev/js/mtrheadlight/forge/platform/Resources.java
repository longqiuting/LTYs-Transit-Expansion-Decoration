package dev.js.mtrheadlight.forge.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;

/**
 * 客户端资源重载 shim：Fabric 的
 * {@code ResourceManagerHelper.get(CLIENT_RESOURCES).registerReloadListener(SimpleResourceReloadListener)}
 * → Forge 的 {@link RegisterClientReloadListenersEvent}。
 *
 * <p>java-gen 里的监听器保持原来的 {@code getFabricId / load / apply} 三段式，
 * 由这里包一层 {@link PreparableReloadListener} 适配器。
 */
public final class Resources {

    private static final List<ReloadListener> CLIENT_LISTENERS = new ArrayList<>();

    private Resources() {
    }

    /** Fabric {@code SimpleResourceReloadListener} 的最小同构接口。 */
    public interface ReloadListener {

        ResourceLocation getFabricId();

        CompletableFuture<Void> load(ResourceManager manager, ProfilerFiller profiler, Executor executor);

        CompletableFuture<Void> apply(Void data, ResourceManager manager, ProfilerFiller profiler, Executor executor);
    }

    public static void registerClient(ReloadListener listener) {
        if (listener != null) {
            CLIENT_LISTENERS.add(listener);
        }
    }

    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        for (ReloadListener listener : CLIENT_LISTENERS) {
            event.registerReloadListener(new Adapter(listener));
        }
    }

    private static final class Adapter implements PreparableReloadListener {

        private final ReloadListener delegate;

        private Adapter(ReloadListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler,
                Executor backgroundExecutor, Executor gameExecutor) {
            return delegate.load(manager, prepareProfiler, backgroundExecutor)
                    .thenCompose(barrier::wait)
                    .thenCompose(data -> delegate.apply(data, manager, applyProfiler, gameExecutor));
        }

        @Override
        public String getName() {
            return delegate.getFabricId().toString();
        }
    }
}
