package dev.js.mtrheadlight.collision;

import dev.js.mtrheadlight.HeadlightConfig;
import dev.js.mtrheadlight.LteDebug;
import dev.js.mtrheadlight.MtrHeadlightClient;
import dev.js.mtrheadlight.server.CarScan;
import mtr.data.Train;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * 列车碰撞的<b>生物</b>部分：村民 / 猫 / 狗 / 动物 … 同样被推、被扣血、被撞飞。
 *
 * <h2>为什么在服务端</h2>
 * 玩家的解算能挂在客户端，是因为「自己的位置自己说了算」（{@code ClientPlayerEntity#move} 之后发
 * 包）。生物不行：{@code LivingEntity#damage} 在 {@code world.isClient} 时第一行就
 * {@code return false}，客户端的 {@code setPosition} 也会被服务端的位置包冲掉。所以生物这条路
 * 跑在<b>整合服务器</b>上（单人存档里的那个服务端），由 {@code ServerTickEvents.END_SERVER_TICK}
 * 驱动。
 *
 * <p><b>dedicated server</b> 上由 {@code server.ServerCollision} 接管（它同时管玩家和生物）；
 * 那边也不会有本类 —— {@code CollisionBootstrap} 是 client 入口点。两边不会同时跑。
 *
 * <h2>几何：与玩家那条路是同一份代码</h2>
 * 车厢两端点由 {@link TrainPose} 采样，分段 OBB 与推出用 {@link CarGeometry}（从
 * {@code CollisionSystem} 抽出来的纯计算）：车身段只做侧向推出、端盖允许轴向推出、车门开着时整侧
 * 放行、推完站不下人就不推 —— 与本地玩家完全一致，不会出现「人能穿过去、猫被弹开」。
 *
 * <p>后果（推开 / 扣血 / 撞飞 / 飞出 / 死亡消息）交给 {@link CollisionImpact}，与玩家同一套公式与
 * 冷却。
 *
 * <h2>排除</h2>
 * <ul>
 *   <li>{@link PlayerEntity}：玩家走客户端那条路（或 dedicated server 的 ServerCollision），这里
 *       再算一次就是双倍伤害；</li>
 *   <li>{@code hasVehicle()}/有载具：被车、船或列车载着的乘客不该被自己坐的车撞；</li>
 *   <li>创造 / 旁观 / 无敌 / 已死：{@link CollisionImpact} 里还有一道同样的判断。</li>
 * </ul>
 *
 * <p>生物被撞飞后<b>不需要</b>死亡消息（原版只有玩家死亡会广播），所以这里不碰任何文案；真被撞死时
 * 死亡消息仍然来自 {@link TrainImpactDamageSource}，只是没人会看到。
 */
public final class MobCollision {

    /** 只给这个距离内的列车生成碰撞段（格）。与 {@link CollisionSystem} 的搜索半径一致。 */
    private static final double SEARCH_RANGE = 48.0D;

    /** 诊断周期（server ticks）。 */
    private static final int DIAG_PERIOD = 100;

    private static long lastDiagTick = Long.MIN_VALUE / 2L;
    private static int diagHits;
    private static int diagTrains;
    private static double diagHp;

    private MobCollision() {
    }

    /** {@code END_SERVER_TICK}：对每个维度里的每个非玩家生物结算一次。 */
    public static void serverTick(final MinecraftServer server) {
        if (server == null) {
            return;
        }
        final HeadlightConfig cfg = MtrHeadlightClient.config();
        if (!cfg.trainCollision || !cfg.trainCollisionMobs) {
            return;
        }
        final long now = server.getTicks();
        final double height = cfg.trainCollisionHeightClamped();
        final double endPad = cfg.trainCollisionEndPadClamped();
        try {
            for (final ServerWorld world : server.getWorlds()) {
                final List<ServerPlayerEntity> players = world.getPlayers();
                final int playerCount = players.size();
                if (playerCount <= 0) {
                    continue;
                }
                final List<Train> trains = TrainTickCapture.trains(world, world.getTime());
                if (trains.isEmpty()) {
                    continue;
                }
                final double[] px = new double[playerCount];
                final double[] pz = new double[playerCount];
                for (int i = 0; i < playerCount; i++) {
                    px[i] = players.get(i).getX();
                    pz[i] = players.get(i).getZ();
                }
                // 与玩家那条路共用同一个扫描器：停稳的车复用车厢，远处的车直接跳过。
                final List<CarGeometry.Car> cars = CarScan.scan(trains, height, endPad,
                        EXTRAS, px, pz, playerCount, now);
                if (LteDebug.collision()) {
                    diagTrains = Math.max(diagTrains, CarScan.lastTrains());
                }
                if (cars.isEmpty()) {
                    continue;
                }
                for (final Entity entity : world.iterateEntities()) {
                    if (!(entity instanceof LivingEntity living) || entity instanceof PlayerEntity) {
                        continue;
                    }
                    try {
                        collide(world, living, cars, cfg, now);
                    } catch (Throwable ignored) {
                        // 单个生物算错不能影响其它生物。
                    }
                }
            }
            CollisionImpact.sweep(now);
            diag(now);
        } catch (Throwable t) {
            // 生物碰撞永远不许影响服务端 tick。
        }
    }

    /** 世界卸载 / 重载：清掉列车表与撞击状态。 */
    public static void clear() {
        TrainTickCapture.clear();
        CollisionImpact.clear();
    }

    // ---------------------------------------------------------------------------------------------
    // 单个生物
    // ---------------------------------------------------------------------------------------------

    private static void collide(final ServerWorld world, final LivingEntity living,
                                final List<CarGeometry.Car> cars, final HeadlightConfig cfg,
                                final long now) {
        // 死亡 / 0 血：连飞出阶段的掉血都不算，更不推。死了就该安静躺着。
        if (!living.isAlive() || living.isDead() || living.getHealth() <= 0.0F) {
            CollisionImpact.forget(living.getUuid());
            return;
        }
        // 飞出阶段：不需要再碰到车，按撞击车速继续扣到死。
        final float flightHp = CollisionImpact.flightDamage(living, cfg, now);
        if (flightHp > 0.0F) {
            CollisionImpact.applyDamage(living, flightHp, CollisionImpact.wasFast(living.getUuid()), cfg);
            diagHp += flightHp;
            return;
        }

        if (living.isSpectator() || living.isInvulnerable()) {
            return;
        }
        // 被载着的乘客不算（MTR / 船 / 矿车）：自己坐的车不该撞自己。
        if (living.hasVehicle() || living.getVehicle() != null) {
            return;
        }

        CarGeometry.Car hitCar = null;
        CarGeometry.Push hit = null;
        for (final CarGeometry.Car car : cars) {
            if (!(car.speedKmh > 0.0D) || !near(living, car)) {
                continue;
            }
            hit = firstHit(world, living, car);
            if (hit != null && hit.moved) {
                hitCar = car;
                break;
            }
            hit = null;
        }
        if (hit == null || hitCar == null) {
            // 不在接触中：撞击后的持续掉血窗口（默认 25 s）照样结算，否则生物只掉几 tick 就没事了。
            final float hurtHp = CollisionImpact.hurtDamage(living, cfg, now);
            if (hurtHp > 0.0F) {
                CollisionImpact.applyDamage(living, hurtHp,
                        CollisionImpact.wasFast(living.getUuid()), cfg);
                diagHp += hurtHp;
            }
            return;
        }

        CarGeometry.applyPush(world, living, hit.dirX, hit.dirZ, hit.pen);
        final HeadlightConfig.Effective eff = cfg.effectiveFor(hitCar.sidingId);
        final float hp = CollisionImpact.tick(living, cfg, eff, hitCar.speedKmh,
                hit.dirX, hit.dirZ, now);
        if (hp > 0.0F) {
            // 死亡消息的分段用撞击时的车速快照（持续掉血可能发生在车开走之后）。
            CollisionImpact.applyDamage(living, hp, CollisionImpact.wasFast(living.getUuid()), cfg);
            diagHits++;
            diagHp += hp;
        }
    }

    /** 粗筛：实体到车厢中心的距离（含车厢长度余量）。 */
    private static boolean near(final LivingEntity living, final CarGeometry.Car car) {
        final double dx = living.getX() - car.cx;
        final double dz = living.getZ() - car.cz;
        final double reach = SEARCH_RANGE + car.len;
        return dx * dx + dz * dz <= reach * reach;
    }

    /** 一节车的第一次命中：先车身段（开门时整段跳过），再两个端盖。与客户端同序。 */
    private static CarGeometry.Push firstHit(final ServerWorld world, final LivingEntity living,
                                             final CarGeometry.Car car) {
        if (!car.doorsOpen) {
            for (final CarGeometry.Seg seg : car.body) {
                final CarGeometry.Push push = CarGeometry.contact(living, seg);
                if (push.moved) {
                    return push;
                }
            }
        }
        for (final CarGeometry.Seg cap : car.caps) {
            final CarGeometry.Push push = CarGeometry.contact(living, cap);
            if (push.moved) {
                return push;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // 车厢
    // ---------------------------------------------------------------------------------------------

    /** 侧线覆盖（车头 / 车尾端部延长）；只在真的要重建车厢时被问到。 */
    private static final CarScan.Extras EXTRAS = new CarScan.Extras() {
        @Override
        public double head(final long sidingId) {
            return MtrHeadlightClient.config().effectiveFor(sidingId).collisionHeadExtra;
        }

        @Override
        public double tail(final long sidingId) {
            return MtrHeadlightClient.config().effectiveFor(sidingId).collisionTailExtra;
        }
    };

    // 车厢的构造在 {@code server.CarScan}（与 ServerCollision 共用同一份）：按玩家距离粗筛、
    // 停稳的车复用车厢。原来这里那份每次每列车重建一遍的 cars() 已经删除。

    /** 每 5 秒一行：这一窗撞了多少生物、扣了多少血 —— 只有 -Dlte.debug.collision=true 才打印。 */
    private static void diag(final long now) {
        if (!LteDebug.collision()) {
            diagHits = 0;
            diagTrains = 0;
            diagHp = 0.0D;
            return;
        }
        if (now - lastDiagTick < DIAG_PERIOD) {
            return;
        }
        lastDiagTick = now;
        final int hits = diagHits;
        final int trains = diagTrains;
        final double hp = diagHp;
        diagHits = 0;
        diagTrains = 0;
        diagHp = 0.0D;
        try {
            MtrHeadlightClient.LOGGER.info(
                    "[lte] debug: collision(mobs) trains={} hits={} damage={}",
                    Integer.valueOf(trains), Integer.valueOf(hits),
                    String.format(java.util.Locale.ROOT, "%.1f", Double.valueOf(hp)));
        } catch (Throwable ignored) {
            // 日志永远不许影响 tick。
        }
    }
}
