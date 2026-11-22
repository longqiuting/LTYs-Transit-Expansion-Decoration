package dev.js.mtrheadlight.decor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.js.mtrheadlight.LteLog;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * LTED（装饰侧）自己的配置：<b>线路条带与门灯配色</b>。
 *
 * <h2>为什么从这里开始拆</h2>
 * {@code HeadlightConfig} 在 {@code common/}，四个 MC 版本共用；而线路色覆盖 / 门灯配色只服务
 * 「厦门吊板 · APG 玻璃 · 屏蔽门」这些<b>装饰</b>内容。拆分后 LTE（优化版）不再持有这些字段，
 * LTED 自己带一份，两者各自读写各自的文件。
 *
 * <p>文件：{@code config/mtrlte_decor.json}（{@link #CONFIG_FILE}）。
 *
 * <h2>向后兼容（一次性迁移）</h2>
 * 旧版本的这些键写在 {@code config/mtrlte.json} 里。本类首次启动、且
 * {@code mtrlte_decor.json} 还不存在时，会去 {@code mtrlte.json}（以及更早的两个旧文件名）
 * 按<b>键名</b>把这几项读出来，写进新文件，并打<b>一行</b> INFO。之后旧文件由
 * {@code HeadlightConfig} 的 Gson 序列化自然丢弃这些键（类里已经没这几个字段了），
 * 下一次保存只写新位置。
 *
 * <p>旧键一个都读不到 = 全新安装或已经被迁移过：直接用字段初始值（= 出厂默认）建文件，
 * 不报错、不刷日志。
 *
 * <h2>线程</h2>
 * {@link #get()} 永远不返回 {@code null}（未加载时是一份默认值），渲染线程可以随时读；
 * {@link #load()} 只在客户端 / 服务端初始化与配置界面保存时调用。
 */
public final class DecorConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 本类的文件（与 LTE 的 {@code mtrlte.json} 分开）。 */
    public static final String CONFIG_FILE = "mtrlte_decor.json";

    /** 旧版本把这些键写在这个文件里。 */
    private static final String LEGACY_FILE = "mtrlte.json";

    /** 改名前的两个旧文件名（{@code HeadlightConfig} 也会从它们迁移，这里一并兜住）。 */
    private static final String[] LEGACY_NAMES = {
        "mtr-external-lights.json", "mtr-headlight.json"
    };

    /** 本文件自己的版本号（字段增删的迁移用；目前只有 1）。 */
    public static final int CONFIG_VERSION = 1;

    /** 门灯默认颜色：{@code orange}（MTR 原版 {@code light_on.png} = #FF9900）/ {@code green} / {@code blue}。 */
    public static final String DOOR_LIGHT_ORANGE = "orange";
    public static final String DOOR_LIGHT_GREEN = "green";
    public static final String DOOR_LIGHT_BLUE = "blue";

    /** {@code 0xRRGGBB} 的兜底灰白（线路色 = 0 时用）。 */
    private static final int FALLBACK_RGB = 0x808080;

    public int configVersion = CONFIG_VERSION;

    /**
     * 线路条带是否用厦门实拍的样式重画（白底长条 + 线路色主线 + 站点圆点 + 当前站大圈 + 终点箭头）。
     *
     * <p>关掉就完全回到 MTR 自己的 {@code RouteMapGenerator#generateRouteMap} 位图（黑底细线那套），
     * 用作一键回退。条带的<b>几何与着色</b>详见 {@code XiamenRouteStrip}；
     * 颜色一律取“当前站台那条线路的颜色”（{@code Route#color}），本配置<b>不</b>预设任何颜色。
     */
    public boolean xiamenRouteStrip = true;

    /**
     * 站名排在线的上方（厦门 LCD 全图那一套，45° 斜排，默认）还是下方（正排）。
     *
     * <p>实拍吊板用斜排：板子只有 0.44 格高，正排站名放不下几个字；正排留给宽而矮的
     * APG 玻璃试版式时用。
     */
    public boolean routeStripNamesBelow = false;

    /**
     * <b>可选</b>的线路色手工覆盖：键 = 线路 id、线路全名（{@code 1号线}）或线路号（{@code 1}），
     * 值 = {@code #RRGGBB}。
     *
     * <p>默认<b>空</b>——条带一律用 MTR 里那条线路自己的 {@code Route#color}（厦门 1 号线绿、
     * 4 号线红都会自动跟随）。这张表只用来在“MTR 里线路没设色（color = 0）”时补一个颜色，
     * 或者临时试配色。匹配顺序：id → 全名 → 线路号；都没有就取线路色，线路色是 0 时用灰白
     * fallback（{@code #808080}）。
     */
    public Map<String, String> lineColors = new LinkedHashMap<>();

    /**
     * 门灯颜色。默认 {@link #DOOR_LIGHT_ORANGE} —— 就是 MTR jar 里 {@code textures/block/light_on.png}
     * （#FF9900）/ {@code light_off.png}（#993300）那一对橙色。
     *
     * <p>资源包会把这两张图换成别的颜色（厦门包里是蓝色），本模组自带的副本优先于资源包，
     * 所以这里选什么颜色，门上就是什么颜色。
     */
    public String doorLightColor = DOOR_LIGHT_ORANGE;

    /**
     * <b>可选</b>的按线路门灯颜色：键 = 线路号（{@code 4} / {@code 6} / {@code 1号线}），
     * 值 = {@code orange|green|blue}，优先于 {@link #doorLightColor}。
     *
     * <p>默认空。以后 4/6 号线要切绿，加一条 {@code "4": "green"} 即可，不用改代码。
     * 线路号的判定：门所在站台（{@code RailwayData.getClosePlatformId}）→ 该站台的第一条线路
     * → 线路名里抽数字。
     */
    public Map<String, String> doorLightByLine = new LinkedHashMap<>();

    /** 当前生效的一份。永远不会是 {@code null}（渲染线程随时可读）。 */
    private static volatile DecorConfig live = new DecorConfig();

    private DecorConfig() {
    }

    /** 当前生效的配置（未加载时是一份出厂默认，{@code null} 永远拿不到）。 */
    public static DecorConfig get() {
        return live;
    }

    /** 出厂默认（每行「复原」用；只读，不要写它）。 */
    public static DecorConfig defaults() {
        return new DecorConfig();
    }

    // -----------------------------------------------------------------------------------------
    // 读写
    // -----------------------------------------------------------------------------------------

    /** 配置目录里的 {@code mtrlte_decor.json}。 */
    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }

    /**
     * 加载本侧配置，必要时做一次旧键迁移。失败（文件坏了 / 没权限）只打一行 warn 并退回默认。
     */
    public static DecorConfig load() {
        return load(FabricLoader.getInstance().getConfigDir());
    }

    /**
     * 同上，但配置目录由调用方给出 —— 离线迁移测试用这个重载（不需要初始化 Fabric）。
     */
    public static DecorConfig load(final Path configDir) {
        final Path path = configDir.resolve(CONFIG_FILE);
        if (Files.isRegularFile(path)) {
            try {
                final String json = Files.readString(path, StandardCharsets.UTF_8);
                final DecorConfig cfg = GSON.fromJson(json, DecorConfig.class);
                if (cfg != null) {
                    cfg.configVersion = CONFIG_VERSION;
                    live = cfg.sanitized();
                    return live;
                }
            } catch (IOException | RuntimeException e) {
                LteLog.LOGGER.warn("[lte] could not read {} - using defaults", path, e);
            }
        }
        final DecorConfig fresh = migrateLegacy(configDir).sanitized();
        live = fresh;
        fresh.save(configDir);
        return fresh;
    }

    /**
     * 把旧 {@code mtrlte.json} 里属于本侧的那几个键读出来。
     *
     * <p>一个键都没有 = 全新安装 / 已经迁过：返回出厂默认，<b>不报错、不刷日志</b>。
     */
    private static DecorConfig migrateLegacy(final Path configDir) {
        final JsonObject legacy = readLegacyObject(configDir);
        final DecorConfig cfg = new DecorConfig();
        if (legacy == null) {
            return cfg;
        }
        int migrated = 0;
        final JsonElement strip = legacy.get("xiamenRouteStrip");
        if (strip != null && strip.isJsonPrimitive() && strip.getAsJsonPrimitive().isBoolean()) {
            cfg.xiamenRouteStrip = strip.getAsBoolean();
            migrated++;
        }
        final JsonElement namesBelow = legacy.get("routeStripNamesBelow");
        if (namesBelow != null && namesBelow.isJsonPrimitive() && namesBelow.getAsJsonPrimitive().isBoolean()) {
            cfg.routeStripNamesBelow = namesBelow.getAsBoolean();
            migrated++;
        }
        final JsonElement colors = legacy.get("lineColors");
        if (colors != null && colors.isJsonObject()) {
            for (final Map.Entry<String, JsonElement> e : colors.getAsJsonObject().entrySet()) {
                final JsonElement v = e.getValue();
                if (v != null && v.isJsonPrimitive()) {
                    cfg.lineColors.put(e.getKey(), v.getAsString());
                }
            }
            migrated++;
        }
        final JsonElement doorColor = legacy.get("doorLightColor");
        if (doorColor != null && doorColor.isJsonPrimitive()) {
            cfg.doorLightColor = doorColor.getAsString();
            migrated++;
        }
        final JsonElement doorByLine = legacy.get("doorLightByLine");
        if (doorByLine != null && doorByLine.isJsonObject()) {
            for (final Map.Entry<String, JsonElement> e : doorByLine.getAsJsonObject().entrySet()) {
                final JsonElement v = e.getValue();
                if (v != null && v.isJsonPrimitive()) {
                    cfg.doorLightByLine.put(e.getKey(), v.getAsString());
                }
            }
            migrated++;
        }
        if (migrated > 0) {
            // 一行就够：这条只在「旧文件里还有这几项」的那一次启动出现。
            LteLog.LOGGER.info(
                    "[lte] decor config: moved {} legacy key(s) from {} to {} "
                            + "(route strip / line colours / door lights)",
                    Integer.valueOf(migrated), LEGACY_FILE, CONFIG_FILE);
        }
        return cfg;
    }

    /** {@code mtrlte.json} → 更早的旧文件名，取第一个存在且能解析的。 */
    private static JsonObject readLegacyObject(final Path dir) {
        final JsonObject primary = readObject(dir.resolve(LEGACY_FILE));
        if (primary != null) {
            return primary;
        }
        for (final String name : LEGACY_NAMES) {
            final JsonObject o = readObject(dir.resolve(name));
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    private static JsonObject readObject(final Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                return null;
            }
            final JsonElement e = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonElement.class);
            return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (IOException | RuntimeException e) {
            // 旧文件坏了不该拦住启动 —— 当它不存在。
            return null;
        }
    }

    /** 写回 {@code config/mtrlte_decor.json}。 */
    public void save() {
        save(FabricLoader.getInstance().getConfigDir());
    }

    /** 同上，配置目录由调用方给出（离线测试用）。 */
    public void save(final Path configDir) {
        final Path path = configDir.resolve(CONFIG_FILE);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LteLog.LOGGER.warn("[lte] could not write {}", path, e);
        }
    }

    /** 手改过的 JSON 也读得进来：钳/过滤一遍，坏一条只丢一条。 */
    private DecorConfig sanitized() {
        if (lineColors == null) {
            lineColors = new LinkedHashMap<>();
        }
        if (doorLightByLine == null) {
            doorLightByLine = new LinkedHashMap<>();
        }
        lineColors = sanitizeColorMap(lineColors);
        doorLightByLine = sanitizeDoorLightMap(doorLightByLine);
        doorLightColor = normalizeDoorLightColor(doorLightColor);
        return this;
    }

    // -----------------------------------------------------------------------------------------
    // 着色工具（原来在 HeadlightConfig 里，只服务条带 / 门灯，所以跟着搬过来）
    // -----------------------------------------------------------------------------------------

    /**
     * Parses {@code #RRGGBB} / {@code RRGGBB} (case-insensitive, {@code #}-optional) into
     * {@code 0xRRGGBB}, falling back when the string is missing or malformed.
     */
    public static int parseColor(final String text, final int fallback) {
        if (text == null) {
            return fallback;
        }
        String s = text.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Formats a colour the way the JSON stores it: {@code #RRGGBB}, upper case. */
    public static String formatColor(final int rgb) {
        return String.format("#%06X", Integer.valueOf(rgb & 0xFFFFFF));
    }

    /** 条带在「线路没设色」时用的灰白。 */
    public static int fallbackRgb() {
        return FALLBACK_RGB;
    }

    /** 把 {@code orange|green|blue} 之外的写法（大小写 / 空 / 拼错）收回橙色。 */
    public static String normalizeDoorLightColor(final String raw) {
        if (raw != null) {
            final String s = raw.trim().toLowerCase(Locale.ROOT);
            if (DOOR_LIGHT_GREEN.equals(s)) {
                return DOOR_LIGHT_GREEN;
            }
            if (DOOR_LIGHT_BLUE.equals(s)) {
                return DOOR_LIGHT_BLUE;
            }
            if (DOOR_LIGHT_ORANGE.equals(s)) {
                return DOOR_LIGHT_ORANGE;
            }
        }
        return DOOR_LIGHT_ORANGE;
    }

    /** 去掉空的/非法的 {@code #RRGGBB} 条目，键统一 trim。 */
    private static Map<String, String> sanitizeColorMap(final Map<String, String> raw) {
        final Map<String, String> out = new LinkedHashMap<>();
        if (raw != null) {
            raw.forEach((key, value) -> {
                if (key == null || value == null) {
                    return;
                }
                final String k = key.trim();
                if (k.isEmpty()) {
                    return;
                }
                final int parsed = parseColor(value, -1);
                if (parsed >= 0) {
                    out.put(k, formatColor(parsed));
                }
            });
        }
        return out;
    }

    /** 同上，但值必须是 {@code orange|green|blue}。 */
    private static Map<String, String> sanitizeDoorLightMap(final Map<String, String> raw) {
        final Map<String, String> out = new LinkedHashMap<>();
        if (raw != null) {
            raw.forEach((key, value) -> {
                if (key == null || value == null) {
                    return;
                }
                final String k = key.trim();
                if (!k.isEmpty()) {
                    out.put(k, normalizeDoorLightColor(value));
                }
            });
        }
        return out;
    }
}
