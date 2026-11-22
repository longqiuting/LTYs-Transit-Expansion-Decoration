import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import dev.js.mtrheadlight.decor.DecorConfig;

/**
 * 离线用例：旧 {@code config/mtrlte.json} 里的装饰键能否被 {@link DecorConfig} 迁移到
 * {@code config/mtrlte_decor.json}，且第二次启动（旧键已被 LTE 的保存丢掉）仍读得到同样的值。
 *
 * <p>不依赖 Fabric / Minecraft：{@link DecorConfig#load(Path)} 与 {@code save(Path)} 的目录重载
 * 就是为这个用例留的。跑法见同目录的 {@code run-decor-config-migration-test.sh}。
 *
 * <h2>三个断言</h2>
 * <ol>
 *   <li><b>迁移</b>：旧文件里有 5 个键 → 新文件出现，值逐项相等（含 lineColors / doorLightByLine 的
 *       大小写与非法值清洗）；</li>
 *   <li><b>二次启动</b>：把 mtrlte.json 换成「已经没有这些旧键」的版本（模拟 LTE 保存后），
 *       读新文件仍然是同一份值；</li>
 *   <li><b>全新安装</b>：空目录 → 默认值，不抛异常。</li>
 * </ol>
 */
public final class DecorConfigMigrationTest {

    private static int failures;

    public static void main(final String[] args) throws Exception {
        final Path dir = Files.createTempDirectory("lte-decor-migration");

        // ------------------------------------------------------------------
        // 1) 旧 mtrlte.json：这 5 个键是 2.0.2 的写法（大小写混写 + 一个非法颜色）
        // ------------------------------------------------------------------
        Files.writeString(dir.resolve("mtrlte.json"), """
                {
                  "configVersion": 26,
                  "headLuminance": 15,
                  "xiamenRouteStrip": false,
                  "routeStripNamesBelow": true,
                  "lineColors": {
                    " 1 ": "00aa55",
                    "4号线": "#C0392B",
                    "bad": "zzzzzz"
                  },
                  "doorLightColor": "GREEN",
                  "doorLightByLine": {
                    "4": "blue",
                    "6": "nonsense"
                  }
                }
                """, StandardCharsets.UTF_8);

        final DecorConfig first = DecorConfig.load(dir);
        check("xiamenRouteStrip", false, first.xiamenRouteStrip);
        check("routeStripNamesBelow", true, first.routeStripNamesBelow);
        // 键 trim + 颜色归一成 #RRGGBB 大写
        check("lineColors[1]", "#00AA55", first.lineColors.get("1"));
        check("lineColors[4号线]", "#C0392B", first.lineColors.get("4号线"));
        check("lineColors 非法条目已丢弃", null, first.lineColors.get("bad"));
        check("doorLightColor 归一", "green", first.doorLightColor);
        check("doorLightByLine[4]", "blue", first.doorLightByLine.get("4"));
        check("doorLightByLine[6] 拼错收回橙色", "orange", first.doorLightByLine.get("6"));

        final Path decorPath = dir.resolve("mtrlte_decor.json");
        check("mtrlte_decor.json 已生成", true, Files.isRegularFile(decorPath));
        final JsonObject written = new GsonBuilder().create()
                .fromJson(Files.readString(decorPath, StandardCharsets.UTF_8), JsonObject.class);
        check("新文件带 lineColors", true, written.has("lineColors"));
        check("新文件带 doorLightColor", true, written.has("doorLightColor"));
        check("新文件带 xiamenRouteStrip", true, written.has("xiamenRouteStrip"));
        // 旧文件里的 LTE 键不该被抄进装饰文件
        check("新文件不含 headLuminance", false, written.has("headLuminance"));

        // ------------------------------------------------------------------
        // 2) 二次启动：mtrlte.json 已被 LTE 保存（旧键消失），新文件仍然生效
        // ------------------------------------------------------------------
        Files.writeString(dir.resolve("mtrlte.json"),
                "{\"configVersion\": 26, \"headLuminance\": 12}", StandardCharsets.UTF_8);
        final DecorConfig second = DecorConfig.load(dir);
        check("二次启动 strip=false", false, second.xiamenRouteStrip);
        check("二次启动 namesBelow=true", true, second.routeStripNamesBelow);
        check("二次启动 lineColors 保留", "#00AA55", second.lineColors.get("1"));
        check("二次启动 doorLightColor 保留", "green", second.doorLightColor);

        // ------------------------------------------------------------------
        // 3) 全新安装：空目录 → 默认值，不抛异常
        // ------------------------------------------------------------------
        final Path empty = Files.createTempDirectory("lte-decor-empty");
        final DecorConfig fresh = DecorConfig.load(empty);
        check("全新安装 strip 默认 true", true, fresh.xiamenRouteStrip);
        check("全新安装 namesBelow 默认 false", false, fresh.routeStripNamesBelow);
        check("全新安装门灯默认橙", "orange", fresh.doorLightColor);
        check("全新安装线路色为空", true, fresh.lineColors.isEmpty());
        check("全新安装也建了文件", true, Files.isRegularFile(empty.resolve("mtrlte_decor.json")));

        // ------------------------------------------------------------------
        // 4) LTE 的配置不再写这些键（"下次保存只写新位置"）
        // ------------------------------------------------------------------
        final String lteJson = new dev.js.mtrheadlight.HeadlightConfig().toJson();
        check("LTE 配置不写 lineColors", false, lteJson.contains("\"lineColors\""));
        check("LTE 配置不写 doorLightColor", false, lteJson.contains("\"doorLightColor\""));
        check("LTE 配置不写 doorLightByLine", false, lteJson.contains("\"doorLightByLine\""));
        check("LTE 配置不写 xiamenRouteStrip", false, lteJson.contains("\"xiamenRouteStrip\""));
        check("LTE 配置不写 routeStripNamesBelow", false, lteJson.contains("\"routeStripNamesBelow\""));
        check("LTE 配置仍然写自己的字段", true, lteJson.contains("\"headLuminance\""));

        // 清理
        deleteTree(dir);
        deleteTree(empty);

        if (failures > 0) {
            System.out.println("FAILED: " + failures + " assertion(s)");
            System.exit(1);
        }
        System.out.println("OK: decor config migration (12+ assertions)");
    }

    private static void check(final String what, final Object expected, final Object actual) {
        final boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (!ok) {
            failures++;
            System.out.println("  FAIL " + what + ": expected=" + expected + " actual=" + actual);
        } else {
            System.out.println("  ok   " + what + " = " + actual);
        }
    }

    private static void deleteTree(final Path root) {
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }
}
