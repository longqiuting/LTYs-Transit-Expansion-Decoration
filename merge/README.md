# 单 jar（Forgix 1.2.9）

把 Fabric 与 Forge 两侧产物合并成一个同时能装两边的 jar。

    bash tools/merge-single-jar.sh              # 两侧 build + 合并
    SKIP_BUILD=1 bash tools/merge-single-jar.sh # 只重跑合并

产物：`merge/build/libs/LTYs-Transit-Expansion-Decoration-3.0.0.jar`
（名字由 `v1.20.1/gradle.properties` 的 `archives_base_name` + 两侧共同的 `mod_version` 决定；
两侧版本不一致时 build 直接失败。）

## 需要的前提

| 步骤 | JDK | Gradle |
|---|---|---|
| Fabric build | 21 | 8.14（Loom 1.11.8 要求 ≥8.14） |
| Forge build | 17 | forge 自带 8.1.1（离线） |
| Forgix merge | 21 | 8.14（不编译 Java，只合包） |

## 结构验收（M4b-11）

- `fabric.mod.json` 与 `META-INF/mods.toml` 同在根；`assets/`、`data/` 各只有一份。
- 条目数 2108（含目录），连跑两次 `unzip -Z1 | sort` 完全一致，`uniq -d` 为空。

## 实机

- Forge 1.20.1-47.4.0 专用服务端：`Done (14.545s)`，`mtrlte` 注册 172 方块。
- Fabric 1.20.1 + Loader 0.16.10 专用服务端：`Done (12.630s)`，`mtrlte 3.0.0` 加载，62 decorations。

## 与 LTE 单 jar 不能共存（重要）

LTED 与 LTE 共用 `dev.js.mtrheadlight.*` 包，Forge 的单 jar 会被当成两个自动模块，
两个合并 jar 同时放进 `mods/` 会直接崩：

    java.lang.module.ResolutionException:
      Modules lte and mtrlte export package fabric.dev.js.mtrheadlight.mixin.mtr to module minecraft

Forge 侧两个 jar 还有 17 个同名 class。因此测试环境只放 **LTED 单 jar + MTR + architectury**；
LTE 的单 jar 放在 `mods-off/`，需要时单独切换。
