package dev.js.mtrheadlight;

/**
 * 诊断 / 探针的<b>总开关</b>——默认全关。
 *
 * <p>本模组的服务端路径每 tick 都在跑（碰撞、道床），过去会把统计打成 INFO 心跳行
 * （过去是 {@code [lte] collision} / {@code [lte] trackbed} 两行），在 300 列车、
 * 1 个玩家的服务器上是每 5 秒两行、一天几万行，把真正的错误埋掉。现在规则是：
 *
 * <ul>
 *   <li><b>默认零输出</b>：周期性统计一律不打印；</li>
 *   <li>{@code -Dlte.debug.collision=true} —— 打开那两行周期统计（仍然限频 30 s 一次）；</li>
 *   <li>{@code -Dlte.debug.probe=true} —— 打开每 tick 的耗时探针（ms/tick、扫描/剔除的列车数），
 *       同样限频 30 s 一次。{@code -Dlte.debug.collision=true} 会一并打开它。</li>
 * </ul>
 *
 * <p>属性只在第一次读到的时候求值（启动参数，运行期改不了）。限频是全局一条：两张表加起来
 * 最多每 {@value #PERIOD_MS} ms 一行，不会因为多了一个窗口就翻倍。
 */
public final class LteDebug {

    /** 统计 / 探针的最小间隔（ms）。 */
    public static final long PERIOD_MS = 30_000L;

    private static final boolean COLLISION = flag("lte.debug.collision");
    private static final boolean PROBE = flag("lte.debug.probe") || COLLISION;

    /** 上一次真的打印的时刻（{@link System#currentTimeMillis()}）；0 = 还没打过。 */
    private static long lastMs;

    private LteDebug() {
    }

    private static boolean flag(final String key) {
        try {
            final String v = System.getProperty(key);
            return v != null && !"false".equalsIgnoreCase(v.trim()) && !"0".equals(v.trim());
        } catch (Throwable t) {
            // 安全管理器 / 受限环境：当成关。
            return false;
        }
    }

    /** 周期统计（trains/cars/hits、道床 ok/lost/hop）要不要打印。 */
    public static boolean collision() {
        return COLLISION;
    }

    /** 每 tick 的耗时探针要不要统计。 */
    public static boolean probe() {
        return PROBE;
    }

    /**
     * 限频闸门：{@code true} = 这一窗可以打印。只有真的打印的调用方才该调它
     * （先判断有没有内容，再问闸门），否则一个空窗口会把下一个真窗口挤掉。
     */
    public static boolean due() {
        final long now = System.currentTimeMillis();
        if (lastMs != 0L && now - lastMs < PERIOD_MS) {
            return false;
        }
        lastMs = now;
        return true;
    }
}
