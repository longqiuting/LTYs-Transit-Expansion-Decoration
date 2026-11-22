package dev.js.mtrheadlight.forge.platform;

/**
 * 客户端初始化入口的 shim 接口：替代 Fabric 的 {@code ClientModInitializer}。
 *
 * <p>java-gen 里的客户端类保持同名方法 {@code onInitializeClient()}，由
 * {@code ClientBootstrap} 在 {@code FMLClientSetupEvent} 里调用。
 */
public interface ClientInit {

    void onInitializeClient();
}
