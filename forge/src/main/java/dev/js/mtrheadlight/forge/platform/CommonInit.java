package dev.js.mtrheadlight.forge.platform;

/**
 * 通用初始化入口的 shim 接口：替代 Fabric 的 {@code ModInitializer}。
 *
 * <p>java-gen 里的入口类保持同名方法 {@code onInitialize()}，由
 * {@code LtedForge} 在 mod 构造阶段调用。
 */
public interface CommonInit {

    void onInitialize();
}
