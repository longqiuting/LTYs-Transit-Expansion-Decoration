package dev.js.mtrheadlight.server;

import dev.js.mtrheadlight.TrainPose;
import dev.js.mtrheadlight.collision.CarGeometry;
import mtr.data.Train;
import mtr.data.TransportMode;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 服务端每 tick 的「这一 tick 哪些车厢需要参与碰撞」—— 带<b>缓存</b>与<b>距离剔除</b>的扫描器。
 *
 * <h2>为什么要它</h2>
 * {@code ServerCollision} / {@code MobCollision} 以前每 tick、每个维度都把<b>所有</b>列车重新采样成
 * 车厢：{@code TrainPose.of} 一列车要 {@code 3 × 车厢数 + 1} 次轨面采样，而每次 {@code positionAt}
 * 都从 path 的<b>头部线性扫描</b>一遍。300 列车 × 4 节车厢的服务器上，光这一步就是每 tick
 * 四五千次 path 扫描 + 一千多个 {@link CarGeometry.Car}（每节车厢 10 来个 {@code Seg} 对象）——
 * 400k 对象/秒的垃圾，换来的是「列车离所有玩家 400 格远时也照样算一遍」。
 *
 * <h2>现在怎么做</h2>
 * <ol>
 *   <li><b>没有玩家 → 什么都不做</b>（服务端没有人时这条路整段跳过）；</li>
 *   <li><b>粗筛</b>：车里<b>每一节</b>都离最近玩家超过 {@code CULL_RADIUS + SEARCH_RANGE + 车长}
 *       时整列车跳过 —— 用上一 tick 的车厢缓存算距离，零分配。列车的位移上界是
 *       {@code |railProgress 变化量|}，所以判定条件里带上它：距离富余 &gt; 位移 → 这一 tick 绝不可能
 *       碰到任何玩家附近的实体，跳过是<b>安全</b>的（判定范围内的一律照旧每 tick 重算）；</li>
 *   <li><b>缓存</b>：`path / 车厢数 / 车距 / 车宽 / 朝向 / 高度 / 端部延长` 与
 *       `railProgress / 车速 / 车门` 全都与上次相同（停在场站里的车、停稳的列车）时直接复用上一 tick
 *       的车厢对象 —— 不再重新采样、不再分配。</li>
 * </ol>
 *
 * <p>被跳过的只有「离玩家太远的列车」。车厢在判定距离（{@link #SEARCH_RANGE}）内时，
 * 几何、伤害、推开、车顶、撞飞全部走原来那份代码，数值一字不差。
 *
 * <h2>线程</h2>
 * 只在服务端 tick 线程调用（{@code END_SERVER_TICK}）。返回值是<b>内部复用</b>的 List，
 * 调用方只能读，不能留到下一个 tick。
 */
public final class CarScan {

    /** 与 {@code ServerCollision} 一致的判定半径（格，到车厢中心）。 */
    public static final double SEARCH_RANGE = 48.0D;

    /** 车门放行的门值阈值（与 {@code ServerCollision} / {@code MobCollision} / 客户端一致）。 */
    private static final double DOOR_VALUE_EPS = 0.02D;

    /**
     * 粗筛半径（格，到最近玩家）。列车整体（含 {@link #SEARCH_RANGE} 与车长余量）都在这个半径之外
     * 时不参与碰撞。128 = 8 个区块，已经覆盖服务端 tick 范围内任何可能被撞到的实体；
     * {@code -Dlte.cull.radius=<格>} 可以改（0 或负数 = 不剔除）。
     */
    private static final double CULL_RADIUS = cullRadius();

    /** 缓存条目多久没被用到就丢掉（tick）。车厂回收 / 线路删除的列车不会自己消失。 */
    private static final long TTL = 200L;

    private static final double[] NO_X = new double[0];
    private static final double[] NO_Z = new double[0];

    /** 端部延长（按侧线解析）—— 由调用方从配置里取，这个类不碰 config。 */
    public interface Extras {
        double head(long sidingId);

        double tail(long sidingId);
    }

    private static final class Entry {
        final Train train;
        /** 车厢（复用；{@code Car} 的 deltaX/deltaZ 服务端不用）。 */
        final List<CarGeometry.Car> cars;
        final double progress;
        final Object path;
        final int carsCount;
        final int spacing;
        final int width;
        final boolean reversed;
        final double speed;
        final boolean doorsOpen;
        final double height;
        final double endPad;
        final double headExtra;
        final double tailExtra;
        long seen;

        Entry(final Train train, final List<CarGeometry.Car> cars, final double progress,
              final double speed, final boolean doorsOpen, final double height, final double endPad,
              final double headExtra, final double tailExtra, final long seen) {
            this.train = train;
            this.cars = cars;
            this.progress = progress;
            this.path = train.path;
            this.carsCount = train.trainCars;
            this.spacing = train.spacing;
            this.width = train.width;
            this.reversed = train.isReversed();
            this.speed = speed;
            this.doorsOpen = doorsOpen;
            this.height = height;
            this.endPad = endPad;
            this.headExtra = headExtra;
            this.tailExtra = tailExtra;
            this.seen = seen;
        }

        /** 形状没变（不含 railProgress / 车速 / 车门 / 侧线端部延长）：可以做距离粗筛 + 位移上界。 */
        boolean sameBase(final Train t, final boolean rev, final double h, final double pad) {
            return t == train && t.path == path && t.trainCars == carsCount && t.spacing == spacing
                    && t.width == width && rev == reversed && h == height && pad == endPad;
        }

        /** 连端部延长 / 车速 / 车门 / railProgress 都没变：车厢对象可以直接复用。 */
        boolean sameTick(final double prog, final double sp, final boolean doors,
                         final double head, final double tail) {
            return prog == progress && sp == speed && doors == doorsOpen
                    && head == headExtra && tail == tailExtra;
        }

        /**
         * 这一列车有没有可能在 {@code travel} 格位移内进入任何玩家的判定范围？
         * 逐节车厢算中心到最近玩家的距离，减去「判定半径 + 车长 + 粗筛半径」，全为负才算太远。
         */
        boolean mayReach(final double[] px, final double[] pz, final int n, final double travel) {
            for (int i = 0; i < cars.size(); i++) {
                final CarGeometry.Car car = cars.get(i);
                final double reach = CULL_RADIUS + SEARCH_RANGE + car.len;
                for (int j = 0; j < n; j++) {
                    final double dx = px[j] - car.cx;
                    final double dz = pz[j] - car.cz;
                    if (dx * dx + dz * dz <= (reach + travel) * (reach + travel)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /** 列车身份 → 上一 tick 的车厢（同一列车对象，不能用 equals）。 */
    private static final Map<Train, Entry> CACHE = new IdentityHashMap<>();

    /** 复用的输出表。 */
    private static final List<CarGeometry.Car> OUT = new ArrayList<>();

    // 探针用的上一窗读数（只由 ServerCollision / MobCollision 读，单线程写）。
    private static int lastTrains;
    private static int lastCulled;
    private static int lastReused;
    private static int lastKeptCars;
    private static int lastTotalCars;

    private CarScan() {
    }

    private static double cullRadius() {
        try {
            final String v = System.getProperty("lte.cull.radius");
            if (v != null) {
                final double d = Double.parseDouble(v.trim());
                return d > 0.0D ? d : 0.0D;
            }
        } catch (Throwable ignored) {
            // 属性写坏了就用默认值。
        }
        return 128.0D;
    }

    /**
     * 扫一遍这批列车，返回这一 tick 需要参与判定的车厢（内部复用的 List，只读）。
     *
     * @param trains  本维度的列车（{@code TrainTickCapture.trains}）
     * @param height  车体高度（{@code cfg.trainCollisionHeightClamped()}）
     * @param endPad  端部延长（{@code cfg.trainCollisionEndPadClamped()}）
     * @param extras  按侧线解析的端部延长
     * @param px      玩家 X 坐标
     * @param pz      玩家 Z 坐标
     * @param n       玩家数；&lt;= 0 时直接返回空表（并顺手清掉过期缓存）
     * @param now     当前 server tick（缓存过期用）
     */
    public static List<CarGeometry.Car> scan(final List<Train> trains, final double height,
                                             final double endPad, final Extras extras,
                                             final double[] px, final double[] pz, final int n,
                                             final long now) {
        OUT.clear();
        lastTrains = 0;
        lastCulled = 0;
        lastReused = 0;
        lastKeptCars = 0;
        lastTotalCars = 0;
        if (trains == null || trains.isEmpty()) {
            return OUT;
        }
        if (n <= 0) {
            // 没有玩家：不扫、不采样、不分配。缓存留给 prune() 按 TTL 收。
            prune(now);
            return OUT;
        }
        final double[] xs = px == null ? NO_X : px;
        final double[] zs = pz == null ? NO_Z : pz;
        for (int t = 0; t < trains.size(); t++) {
            final Train train = trains.get(t);
            try {
                if (train == null || train.path == null || train.path.isEmpty()
                        || train.trainCars <= 0) {
                    continue;
                }
                // 船 / 缆车 / 飞机的位置语义不同，撞上去只会莫名其妙（与原实现一致）。
                if (train.transportMode != TransportMode.TRAIN) {
                    continue;
                }
                lastTrains++;
                final double progress = train.getRailProgress();
                final boolean reversed = train.isReversed();
                final double speed = CarGeometry.speedKmh(train.getSpeed());
                final boolean doors = train.getDoorValue() > DOOR_VALUE_EPS;

                final Entry cached = CACHE.get(train);
                if (cached != null && cached.sameBase(train, reversed, height, endPad)) {
                    // 位移上界 = railProgress 的变化量（沿轨道量的，必然 ≥ 欧氏位移）。
                    final double travel = Math.abs(progress - cached.progress) + 1.0D;
                    lastTotalCars += cached.cars.size();
                    if (!cached.mayReach(xs, zs, n, travel)) {
                        cached.seen = now;
                        lastCulled++;
                        continue;
                    }
                    // 只有「可能被撞到」的列车才去解析侧线覆盖（effectiveFor 会分配对象）。
                    final double head = extras == null ? endPad : extras.head(train.sidingId);
                    final double tail = extras == null ? endPad : extras.tail(train.sidingId);
                    if (cached.sameTick(progress, speed, doors, head, tail)) {
                        cached.seen = now;
                        lastReused++;
                        OUT.addAll(cached.cars);
                        continue;
                    }
                    final List<CarGeometry.Car> rebuilt = build(train, height, endPad, head, tail,
                            progress, reversed, speed, doors);
                    if (rebuilt.isEmpty()) {
                        CACHE.remove(train);
                        continue;
                    }
                    CACHE.put(train, new Entry(train, rebuilt, progress, speed, doors, height,
                            endPad, head, tail, now));
                    OUT.addAll(rebuilt);
                    continue;
                }
                final double head = extras == null ? endPad : extras.head(train.sidingId);
                final double tail = extras == null ? endPad : extras.tail(train.sidingId);
                final List<CarGeometry.Car> built = build(train, height, endPad, head, tail,
                        progress, reversed, speed, doors);
                if (built.isEmpty()) {
                    CACHE.remove(train);
                    continue;
                }
                CACHE.put(train, new Entry(train, built, progress, speed, doors, height, endPad,
                        head, tail, now));
                lastTotalCars += built.size();
                OUT.addAll(built);
            } catch (Throwable ignored) {
                // 单列车采样失败：跳过它，其它列车照常。
            }
        }
        lastKeptCars = OUT.size();
        prune(now);
        return OUT;
    }

    /** 一列车 → 逐节车厢的分段 OBB（原 {@code ServerCollision.cars()} 的逐字逻辑）。 */
    private static List<CarGeometry.Car> build(final Train train, final double height,
                                               final double endPad, final double headExtra,
                                               final double tailExtra, final double progress,
                                               final boolean reversed, final double speed,
                                               final boolean doorsOpen) {
        final TrainPose pose = TrainPose.of(train, reversed);
        final int carCount = pose.cars();
        final List<CarGeometry.Car> out = new ArrayList<>(Math.max(1, carCount));
        if (carCount <= 0) {
            return out;
        }
        final double halfW = CarGeometry.halfWidth(train.width);
        final String key = Long.toString(train.id);
        final double noseExtra = Math.max(endPad, headExtra);
        final double rearExtra = Math.max(endPad, tailExtra);
        for (int i = 0; i < carCount; i++) {
            final Vec3d a = pose.carStart(i);
            final Vec3d b = pose.carEnd(i);
            double ax = b.x - a.x;
            double az = b.z - a.z;
            double len = Math.sqrt(ax * ax + az * az);
            if (len < 1.0E-3D) {
                // 竖直 / 退化的车厢：用自身朝向投影到水平面兜底。
                final Vec3d fwd = pose.carForward(i);
                ax = fwd.x;
                az = fwd.z;
                len = Math.sqrt(ax * ax + az * az);
                if (len < 1.0E-4D) {
                    continue;
                }
            }
            out.add(new CarGeometry.Car(key, i, a, b, ax / len, az / len, len, halfW,
                    height, doorsOpen, speed, Double.POSITIVE_INFINITY,
                    i == 0 ? noseExtra : endPad,
                    i == carCount - 1 ? rearExtra : endPad,
                    train.sidingId));
        }
        return out;
    }

    /** 丢掉很久没出现的列车（车厂回收 / 线路删除）。每 100 tick 走一遍，表很小。 */
    private static void prune(final long now) {
        if (now % 100L != 0L || CACHE.isEmpty()) {
            return;
        }
        final Iterator<Map.Entry<Train, Entry>> it = CACHE.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().seen > TTL) {
                it.remove();
            }
        }
    }

    /** 上一 tick 实际参与扫描的列车数（探针用）。 */
    public static int lastTrains() {
        return lastTrains;
    }

    /** 上一 tick 被距离粗筛跳过的列车数（探针用）。 */
    public static int lastCulled() {
        return lastCulled;
    }

    /** 上一 tick 直接复用缓存车厢的列车数（探针用）。 */
    public static int lastReused() {
        return lastReused;
    }

    /** 上一 tick 送进判定循环的车厢数（探针 / bench 用）。 */
    public static int lastKeptCars() {
        return lastKeptCars;
    }

    /** 上一 tick 扫描过的列车一共带着多少节车厢（探针 / bench 用）。 */
    public static int lastTotalCars() {
        return lastTotalCars;
    }

    /** 世界卸载：连同缓存一起丢。 */
    public static void reset() {
        CACHE.clear();
        OUT.clear();
        lastTrains = 0;
        lastCulled = 0;
        lastReused = 0;
    }
}
