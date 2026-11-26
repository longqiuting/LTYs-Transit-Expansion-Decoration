package dev.js.mtrheadlight.mixin.mtr;

import dev.js.mtrheadlight.pole.PoleClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 中键拾取（{@code MinecraftClient#doItemPick}）的拦截点：<b>只有 ctrl+中键 + 准星指着柱底 +
 * 手里拿的是柱底物品</b> 时取消原版拾取，改成「把这一格的属性复制进手里的那堆」。
 *
 * <h2>为什么要拦这里</h2>
 * <p>ctrl+中键在 1.20.1 里没有任何入口能「先于原版拾取」判断：{@code doItemPick} 只由
 * {@code handleInputEvents} 里的 {@code while (pickItemKey.wasPressed())} 调用，键的按下计数在那一瞬间
 * 就被消费掉了（客户端 tick 事件拿到的永远是 0），而它执行完就已经把方块物品塞进背包了。
 * 所以只能在方法头拦一下。
 *
 * <h2>安全性</h2>
 * <ul>
 *   <li>{@link PoleClient#tryCopyProperties} 不满足条件一律返回 {@code false} → <b>不取消</b> →
 *       原版拾取原样发生（「不按 ctrl 的中键保持原版行为」）；</li>
 *   <li>{@code required: false} + {@code defaultRequire: 0}：MC 换版本改名时这条注入静默失效，
 *       ctrl+中键就退回普通拾取，不会崩；</li>
 *   <li>只在客户端混入（mixin 配置的 {@code client} 列表）。</li>
 * </ul>
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientPickMixin {

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void lte$poleCopyProperties(final CallbackInfo ci) {
        if (PoleClient.tryCopyProperties((MinecraftClient) (Object) this)) {
            ci.cancel();
        }
    }
}
