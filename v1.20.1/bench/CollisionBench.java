import dev.js.mtrheadlight.TrainPose;
import dev.js.mtrheadlight.collision.CarGeometry;
import dev.js.mtrheadlight.server.CarScan;
import mtr.data.Rail;
import mtr.data.RailAngle;
import mtr.data.RailType;
import mtr.data.TransportMode;
import mtr.data.Train;
import mtr.path.PathData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline benchmark for the server-side collision scan (v1.20.1).
 *
 * <p>It runs the <b>shipping</b> code - {@link TrainPose}, {@link CarGeometry.Car} and the new
 * {@link CarScan} - against synthetic trains, because a 300-train world is not something a build
 * box can start. Two per-tick bodies are compared, both measured the same way (nanoTime around one
 * whole tick, 400 ticks, after a 200-tick warm-up):
 *
 * <ul>
 *   <li><b>before</b> = the code that shipped up to now: rebuild {@code TrainPose.of} + a
 *       {@link CarGeometry.Car} per car for <b>every</b> train, every tick, then scan every living
 *       entity against all of them ({@code ServerCollision.cars()} + {@code collide()} broad phase);</li>
 *   <li><b>after</b> = {@link CarScan#scan} (distance cull + per-train car cache) plus the same
 *       entity broad phase over the (much shorter) car list.</li>
 * </ul>
 *
 * <p>The trains are real {@code mtr.data.TrainServer} instances allocated with
 * {@code Unsafe.allocateInstance} and hand-populated (the MTR constructor needs a bootstrapped
 * registry, which an offline JVM cannot do). Everything the benchmark measures - path sampling,
 * car geometry, the scan - is the real production code path.
 *
 * <p>Run (see {@code bench/run-collision-bench.sh}):
 * <pre>
 * java -cp "out;$(cat build/cp.txt);../libs/MTR-fabric-1.20.1-3.6.4.2-named.jar" CollisionBench
 * </pre>
 */
public final class CollisionBench {

    /** The world from the bug report: ~303 trains, ~1231 cars, 1 player. */
    private static final int TRAINS = 303;
    private static final int CARS_PER_TRAIN = 4;
    /** MTR car spacing (blocks between car ends) and train width. */
    private static final int CAR_SPACING = 24;
    private static final int TRAIN_WIDTH = 18;
    /** Distance between the noses of consecutive trains on our synthetic line. */
    private static final double TRAIN_GAP = 20.0D;
    /** Speed in blocks/tick; 1.0 = 72 km/h. */
    private static final float SPEED = 1.0F;
    /** Living entities that the broad phase has to test (players + animals on a busy server). */
    private static final int ENTITIES = 12;

    private static final int WARMUP = 400;
    private static final int TICKS = 1500;

    private static final double SEARCH_RANGE = 48.0D;

    private static final Unsafe U;

    static {
        try {
            final Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            U = (Unsafe) f.get(null);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static double sink;

    public static void main(final String[] args) throws Exception {
        if (args.length >= 1 && "verify".equalsIgnoreCase(args[0])) {
            verify();
            return;
        }
        if (args.length >= 2) {
            // One scenario per JVM: no cross-scenario JIT / heap state.
            final int rails = Integer.parseInt(args[0]);
            final boolean moving = "running".equalsIgnoreCase(args[1]);
            System.out.println("== path = " + rails + " rails (" + (rails * 10) + " blocks), "
                    + TRAINS + " trains x " + CARS_PER_TRAIN + " cars, " + ENTITIES + " entities ==");
            scenario(moving ? "trains running" : "trains parked ", rails, moving);
            System.out.printf("  scan: trains=%d culled=%d reused=%d cars kept=%d of %d%n",
                    CarScan.lastTrains(), CarScan.lastCulled(), CarScan.lastReused(),
                    CarScan.lastKeptCars(), CarScan.lastTotalCars());
            return;
        }
        for (final int rails : new int[]{40, 200}) {
            System.out.println("== path = " + rails + " rails (" + (rails * 10) + " blocks), "
                    + TRAINS + " trains x " + CARS_PER_TRAIN + " cars, " + ENTITIES + " entities ==");
            scenario("trains running", rails, true);
            scenario("trains parked ", rails, false);
            System.out.println();
        }
    }

    private static void scenario(final String label, final int rails, final boolean moving)
            throws Exception {
        final List<Train> trains = new ArrayList<>();
        for (int i = 0; i < TRAINS; i++) {
            // Each train sits on its own 10-block-spaced `rails`-long piece of straight track.
            // railProgress starts mid-path: that is what makes positionAt() walk half the path,
            // which is the cost a real train pays on every sample.
            trains.add(make(i, rails, i * TRAIN_GAP, rails * 10.0D * 0.5D, 0.0D));
        }
        // The player stands in the middle of the line, so ~3 trains of 303 are inside the
        // 176-block reach - the "one player on a big network" case from the report.
        final double[] px = {TRAINS * TRAIN_GAP * 0.5D};
        final double[] pz = {0.0D};
        // Entities clustered around the player, at various distances inside the broad phase.
        final double[][] entities = new double[ENTITIES][];
        for (int i = 0; i < ENTITIES; i++) {
            entities[i] = new double[]{px[0] + 4.0D * i, 64.0D, pz[0] + 3.0D * i};
        }

        CarScan.reset();
        final long oldNanos = runOld(trains, entities, rails * 10.0D, moving);
        CarScan.reset();
        final long newNanos = runNew(trains, px, pz, entities, moving, rails);

        System.out.printf("  %s  before=%7.3f ms/tick   after=%7.3f ms/tick   speedup=%5.1fx"
                        + "   (cars kept after cull: %d of %d)%n",
                label, oldNanos / 1.0E6D / TICKS, newNanos / 1.0E6D / TICKS,
                (double) oldNanos / (double) Math.max(1L, newNanos),
                CarScan.lastKeptCars(), CarScan.lastTotalCars());
    }

    // ---------------------------------------------------------------------------------------------
    // equivalence check: cached/culled scan must produce the same cars as the old rebuild
    // ---------------------------------------------------------------------------------------------

    /**
     * Builds a mixed fleet and compares {@code oldCars()} against {@link CarScan#scan} car by car,
     * field by field - cold cache, cache hit, and after the trains moved. Run with
     * {@code -Dlte.cull.radius=100000} so nothing is culled and every train has to be present.
     */
    private static void verify() throws Exception {
        final CarScan.Extras extras = new CarScan.Extras() {
            @Override
            public double head(final long sidingId) {
                return 0.5D;
            }

            @Override
            public double tail(final long sidingId) {
                return 0.75D;
            }
        };
        final double height = 1.0D;
        final double endPad = 0.25D;
        final List<Train> trains = new ArrayList<>();
        final List<double[]> players = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            final List<PathData> path = new ArrayList<>();
            final List<Double> distances = new ArrayList<>();
            final int rails = 20 + i * 3;
            for (int r = 0; r < rails; r++) {
                final BlockPos start = new BlockPos(r * 10, 64, i * 40);
                final BlockPos end = new BlockPos((r + 1) * 10, 64, i * 40);
                final Rail rail = new Rail(start, RailAngle.E, end, RailAngle.E, RailType.SIDING,
                        TransportMode.TRAIN);
                path.add(new PathData(rail, 0L, 0, 0, start, end, 0));
                distances.add(Double.valueOf((r + 1) * 10.0D));
            }
            final Train train = (Train) U.allocateInstance(mtr.data.TrainServer.class);
            set(train, "id", Long.valueOf(1000 + i));
            set(train, "sidingId", Long.valueOf(i));
            set(train, "trainId", "verify");
            set(train, "baseTrainType", "train_9_base");
            set(train, "transportMode", TransportMode.TRAIN);
            set(train, "spacing", Integer.valueOf(18 + i));
            set(train, "width", Integer.valueOf(16 + i));
            set(train, "trainCars", Integer.valueOf(1 + (i % 5)));
            set(train, "path", path);
            set(train, "distances", distances);
            set(train, "speed", Float.valueOf(0.25F * i));
            set(train, "railProgress", Double.valueOf(50.0D + i * 7.0D));
            set(train, "reversed", Boolean.valueOf((i & 1) == 1));
            set(train, "doorValue", Float.valueOf(i % 3 == 0 ? 0.5F : 0.0F));
            trains.add(train);
            players.add(new double[]{i * 40.0D, 0.0D});
        }
        final double[] px = new double[players.size()];
        final double[] pz = new double[players.size()];
        for (int i = 0; i < players.size(); i++) {
            px[i] = players.get(i)[0];
            pz[i] = players.get(i)[1];
        }
        CarScan.reset();
        int mismatches = 0;
        int compared = 0;
        for (int tick = 0; tick < 4; tick++) {
            if (tick == 3) {
                // move everything: exercises the "rebuild because progress changed" branch
                for (final Train train : trains) {
                    set(train, "railProgress",
                            Double.valueOf(train.getRailProgress() + 3.0D));
                }
            }
            final List<CarGeometry.Car> before = oldCars(trains, height, endPad, 0.5D, 0.75D);
            final List<CarGeometry.Car> after = CarScan.scan(trains, height, endPad, extras,
                    px, pz, px.length, tick);
            if (before.size() != after.size()) {
                System.out.printf("tick %d: car count %d != %d%n", tick, before.size(), after.size());
                mismatches++;
                continue;
            }
            for (int i = 0; i < before.size(); i++) {
                compared++;
                final String diff = diff(before.get(i), after.get(i));
                if (diff != null) {
                    mismatches++;
                    if (mismatches <= 5) {
                        System.out.println("tick " + tick + " car " + i + ": " + diff);
                    }
                }
            }
        }
        System.out.printf("equivalence: %d car records compared over 4 ticks, %d mismatch(es)%n",
                compared, mismatches);
        if (mismatches != 0) {
            throw new IllegalStateException("scan output differs from the old builder");
        }
    }

    private static String diff(final CarGeometry.Car a, final CarGeometry.Car b) {
        if (!a.key.equals(b.key) || a.index != b.index) {
            return "identity " + a.key + '/' + a.index + " vs " + b.key + '/' + b.index;
        }
        if (a.cx != b.cx || a.cz != b.cz || a.ux != b.ux || a.uz != b.uz || a.len != b.len
                || a.halfW != b.halfW || a.roofA != b.roofA || a.roofB != b.roofB
                || a.floorA != b.floorA || a.floorB != b.floorB
                || a.doorsOpen != b.doorsOpen || a.speedKmh != b.speedKmh
                || a.sidingId != b.sidingId) {
            return "scalar fields differ";
        }
        if (a.body.length != b.body.length || a.caps.length != b.caps.length) {
            return "segment count " + a.body.length + '/' + a.caps.length + " vs "
                    + b.body.length + '/' + b.caps.length;
        }
        for (int i = 0; i < a.body.length; i++) {
            if (!sameSeg(a.body[i], b.body[i])) {
                return "body seg " + i;
            }
        }
        for (int i = 0; i < a.caps.length; i++) {
            if (!sameSeg(a.caps[i], b.caps[i])) {
                return "cap seg " + i;
            }
        }
        return null;
    }

    private static boolean sameSeg(final CarGeometry.Seg a, final CarGeometry.Seg b) {
        return a.cx == b.cx && a.cz == b.cz && a.ux == b.ux && a.uz == b.uz
                && a.halfLen == b.halfLen && a.halfW == b.halfW && a.yLo == b.yLo && a.yHi == b.yHi
                && a.cap == b.cap && a.side == b.side
                && a.minX == b.minX && a.maxX == b.maxX && a.minZ == b.minZ && a.maxZ == b.maxZ;
    }

    // ---------------------------------------------------------------------------------------------
    // before: the code that shipped up to now (verbatim per-tick body)
    // ---------------------------------------------------------------------------------------------

    private static long runOld(final List<Train> trains, final double[][] entities, final double pathTotal,
                               final boolean moving) {
        final double height = 1.0D;
        final double endPad = 0.5D;
        long sum = 0L;
        for (int tick = 0; tick < WARMUP + TICKS; tick++) {
            final long t0 = System.nanoTime();
            final List<CarGeometry.Car> cars = oldCars(trains, height, endPad, 0.5D, 0.5D);
            final int entitiesSeen = oldBroadPhase(cars, entities);
            final long elapsed = System.nanoTime() - t0;
            if (tick >= WARMUP) {
                sum += elapsed;
            }
            sink += entitiesSeen;
            if (moving) {
                advance(trains, pathTotal);
            }
        }
        return sum;
    }

    /** Verbatim copy of the removed {@code ServerCollision.cars(trains, cfg)}. */
    private static List<CarGeometry.Car> oldCars(final List<Train> trains, final double height,
                                                 final double endPad, final double headExtra,
                                                 final double tailExtra) {
        final List<CarGeometry.Car> out = new ArrayList<>();
        for (final Train train : trains) {
            try {
                if (train == null || train.path == null || train.path.isEmpty()
                        || train.trainCars <= 0) {
                    continue;
                }
                if (train.transportMode != TransportMode.TRAIN) {
                    continue;
                }
                final TrainPose pose = TrainPose.of(train, train.isReversed());
                final int carCount = pose.cars();
                if (carCount <= 0) {
                    continue;
                }
                final double halfW = CarGeometry.halfWidth(train.width);
                final double speed = CarGeometry.speedKmh(train.getSpeed());
                final boolean doorsOpen = train.getDoorValue() > 0.02D;
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
                        // vertical / degenerate car: fall back to its own heading
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
            } catch (Throwable ignored) {
                // same as production
            }
        }
        return out;
    }

    /** {@code ServerCollision.collide}'s broad phase: onRoof() pass + collide() pass per entity. */
    private static int oldBroadPhase(final List<CarGeometry.Car> cars, final double[][] entities) {
        int hits = 0;
        for (final double[] e : entities) {
            for (final CarGeometry.Car car : cars) {
                if (near(e, car)) {
                    hits++;
                }
            }
            for (final CarGeometry.Car car : cars) {
                if (near(e, car)) {
                    hits++;
                }
            }
        }
        return hits;
    }

    // ---------------------------------------------------------------------------------------------
    // after: the new scanner + the same broad phase
    // ---------------------------------------------------------------------------------------------

    private static long runNew(final List<Train> trains, final double[] px, final double[] pz,
                               final double[][] entities, final boolean moving, final int rails) {
        final double height = 1.0D;
        final double endPad = 0.5D;
        final CarScan.Extras extras = new CarScan.Extras() {
            @Override
            public double head(final long sidingId) {
                return 0.5D;
            }

            @Override
            public double tail(final long sidingId) {
                return 0.5D;
            }
        };
        long total = 0L;
        for (int tick = 0; tick < WARMUP + TICKS; tick++) {
            final long t0 = System.nanoTime();
            final List<CarGeometry.Car> cars = CarScan.scan(trains, height, endPad, extras,
                    px, pz, px.length, tick);
            final int entitiesSeen = oldBroadPhase(cars, entities);
            final long elapsed = System.nanoTime() - t0;
            if (tick >= WARMUP) {
                total += elapsed;
            }
            sink += entitiesSeen;
            if (moving) {
                advance(trains, rails * 10.0D);
            }
        }
        return total;
    }

    /** The broad-phase distance test used by {@code ServerCollision.near(entity, car)}. */
    private static boolean near(final double[] entity, final CarGeometry.Car car) {
        final double dx = entity[0] - car.cx;
        final double dz = entity[2] - car.cz;
        final double reach = SEARCH_RANGE + car.len;
        return dx * dx + dz * dz <= reach * reach;
    }

    private static void advance(final List<Train> trains, final double total) {
        for (final Train train : trains) {
            try {
                // Wrap around at the end of the path: a real train stays *inside* its path, and
                // sampling mid-rail (curve maths) is what positionAt() actually costs.
                double progress = train.getRailProgress() + SPEED;
                if (progress > total - 100.0D) {
                    progress = 100.0D;
                }
                set(train, "railProgress", Double.valueOf(progress));
            } catch (Throwable ignored) {
                // shouldn't happen
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // synthetic trains
    // ---------------------------------------------------------------------------------------------

    private static Train make(final long id, final int rails, final double offset,
                             final double progress, final double extraY) throws Exception {
        final List<PathData> path = new ArrayList<>();
        final List<Double> distances = new ArrayList<>();
        for (int i = 0; i < rails; i++) {
            final BlockPos start = new BlockPos((int) (offset + i * 10), 64, 0);
            final BlockPos end = new BlockPos((int) (offset + (i + 1) * 10), 64, 0);
            final Rail rail = new Rail(start, RailAngle.E, end, RailAngle.E, RailType.SIDING,
                    TransportMode.TRAIN);
            path.add(new PathData(rail, 0L, 0, 0, start, end, 0));
            distances.add(Double.valueOf((i + 1) * 10.0D));
        }
        final Train train = (Train) U.allocateInstance(mtr.data.TrainServer.class);
        set(train, "id", Long.valueOf(id));
        set(train, "sidingId", Long.valueOf(id + 1L));
        set(train, "trainId", "bench");
        set(train, "baseTrainType", "train_9_base");
        set(train, "transportMode", TransportMode.TRAIN);
        set(train, "spacing", Integer.valueOf(CAR_SPACING));
        set(train, "width", Integer.valueOf(TRAIN_WIDTH));
        set(train, "trainCars", Integer.valueOf(CARS_PER_TRAIN));
        set(train, "path", path);
        set(train, "distances", distances);
        set(train, "speed", Float.valueOf(SPEED));
        set(train, "railProgress", Double.valueOf(progress));
        set(train, "reversed", Boolean.FALSE);
        set(train, "doorValue", Float.valueOf(0.0F));
        return train;
    }

    private static void set(final Object target, final String name, final Object value)
            throws Exception {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                final Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
