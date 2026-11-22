# M1 解耦报告（配置 / 仪表板 / mixin / entrypoint）

> 分支 `split/decouple`，基线 tag `pre-split-2.0.2`。**不拆仓库、不改 mod id、不改其它三个 MC 版本**。
> 验收：`gradle -p v1.20.1 build` 通过 + 旧配置迁移用例通过（本文件末节的脚本）。

## 1. 配置拆分：`lineColors` / 门灯配色 → `DecorConfig`

| 动作 | 文件 | 关键位置 |
|---|---|---|
| 新建装饰侧配置类 | `v1.20.1/.../decor/DecorConfig.java` | `class DecorConfig`(42)、`CONFIG_FILE = "mtrlte_decor.json"`(46)、`load()`(144) / `load(Path)`(151) |
| LTE 侧删字段 | `common/.../HeadlightConfig.java` | 原 774–849 行整块（`xiamenRouteStrip` / `routeStripNamesBelow` / `lineColors` / `DOOR_LIGHT_*` / `doorLightColor` / `doorLightByLine` / `normalizeDoorLightColor`）、`sanitized()` 里三行、`sanitizeColorMap` / `sanitizeDoorLightMap` 两个方法；留一行说明注释(775) |
| 改引用 | `xiamen/XiamenRouteStyle.java` | `lineColor` / `overrideColor` / `lookup` / `doorLightColor` 的参数类型 `HeadlightConfig` → `DecorConfig` |
| 改引用 | `xiamen/XiamenRouteStrip.java` | `paint` / `colorStrip` / `directionArrow` 里的 `cfg` 改 `DecorConfig.get()` |
| 改引用 | `xiamen/XiamenDoorLights.java` | 两个 `colorFor` 改 `DecorConfig.get()` |
| 改引用 | `xiamen/pids/PsdLcdStrip.java` | `rgbOf` 的 `lineColor(DecorConfig.get(), …)` |

- `DecorConfig` 自带 `parseColor` / `formatColor` / `normalizeDoorLightColor` / 两套 sanitize，**不引用
  `HeadlightConfig`**（B 侧不再依赖 A 侧）。
- 归属判据按引用判：`xiamenRouteStrip`、`routeStripNamesBelow`、`lineColors`、`doorLightColor`、
  `doorLightByLine` 只被条带 / 门灯渲染读，全部跟着走。
- 旧配置迁移（`DecorConfig.load`）：
  1. `config/mtrlte_decor.json` 存在 → 直接读（读不到不报错，退回默认）；
  2. 不存在 → 读 `config/mtrlte.json`（以及更早的 `mtr-external-lights.json` / `mtr-headlight.json`）
     按**键名**取那 5 个键，`sanitized()` 之后写新文件，打印**一行** INFO：
     `[lte] decor config: moved 5 legacy key(s) from mtrlte.json to mtrlte_decor.json (route strip / line colours / door lights)`；
  3. 一个键都没有（全新 / 已迁过）→ 出厂默认 + 建文件，**不报错、不打日志**。
- 「下次保存只写新位置」：`HeadlightConfig` 已经没这些字段，Gson 序列化自然丢弃；用例第 4 段直接断言
  `new HeadlightConfig().toJson()` 里不存在这 5 个键。
- 迁移时机：`main` 入口 `LteServer` 在调 A 侧之前先 `LteServerDecor.loadConfig()`（Fabric 先跑 main
  再跑 client），保证**早于** `HeadlightConfig.load()` 可能的那次整份重写。

## 2. 仪表板：3 行迁到 `mtrlte:lted_dashboard`

| 动作 | 文件 | 关键位置 |
|---|---|---|
| 移除 3 行 + 3 个 open 方法 | `ParameterScreen.java` | `OTHER` 分区只剩 `reloadTrains`（原 548–556 行删除，`openXiamenPids` / `openXiamenAdSet` / `openClockGlobal` 删除），类注释行表同步 |
| 新界面 | `decor/LtedDashboardScreen.java` | 分区：显示屏 / 时钟 / 设备 / 其他；动作行复用三个子界面；装饰配色三行带 ↺ |
| 新物品 | `decor/LtedDashboardItem.java` + `decor/LtedCoreItems.java` | `mtrlte:lted_dashboard`，反射调 `LtedDashboardScreen.open()`（dedicated server 上只 `PASS`） |
| 注册进 main | `server/LteServerDecor.java` | `module("lted dashboard item", LtedCoreItems::register)`(54)、`module("lted dashboard tab", LtedCoreItems::pinCoreTabOrder)`(107) |
| 资源 | `models/item/lted_dashboard.json`（沿用 `mtrlte:item/lte_dashboard` 贴图）、`lang/zh_cn.json` + `en_us.json` 共 21 个新键 | |

- 页签位置：`pinCoreTabOrder()` 在 `LogoObjects.pinCoreTabOrder()` **之后**注册，把自己的条目插到
  角美 logo 后一格 → 厦门 logo / 角美 logo / **LTED 仪表板** / 贴墙标识 / 站外标志 …
- 排版：开关在上（本页没有滑块，装饰数值都是单块 NBT）＋ 每个可改行末尾方形 ↺；「复原」写回
  `DecorConfig.defaults()` 同名字段并立即落盘。
- 设备的全局项确实不存在（安检机 / 售卖机 / 充电宝机 都是单块 NBT），所以「设备」分区只留一行
  说明、不建空行；PSD-LCD 同理（没有全局面板参数）。
- 旧物品 `mtrlte:lte_dashboard` 不变（仍开 `ConfigScreen`），只是少了那 3 行。

## 3. mixin 拆分（A = 改 MTR/原版的优化类；B = 改我们自己的方块 / 渲染）

| json | 侧 | 包含的 mixin 类 |
|---|---|---|
| `mtrlte.beamlight.mixins.json` | A | `BeamLightMixin` |
| `mtrlte.collision.mixins.json` | A | `MixinEntityMove` |
| `mtrlte.scatter.mixins.json` | A | `GameRendererScatterMixin` |
| `mtrlte.rain.mixins.json` | A | `WorldRendererRainMixin` |
| `mtrlte.registry.mixins.json` | A | `SimpleRegistryFreezeMixin` |
| `mtrlte.fangsu.mixins.json` | A | `FangsuTurnoutRenderMixin`（动态光照）、`FangsuTrafficLightRenderMixin`（信号联动） |
| `mtrlte.mtr.mixins.json` | A | `TrainClientRegistryMixin`（内置车型）、`RenderSignalBaseMixin`（信号连接器） |
| `mtrlte.mtrserver.mixins.json` | A | `TrainSimulateCaptureMixin`、`RailwayDataCaptureMixin` |
| **`mtrlte.mtrdecor.mixins.json`（新）** | B | `BlockPlatformDoorTypeMixin`(mixins) + `RenderPIDSStationMixin`、`RenderPidsTianjinStationMixin`、`RenderPsdDoorTextureMixin`、`RouteMapArtMixin`、`RouteMapCacheKeyMixin`、`RouteAsyncScopeMixin`、`StationNameEntranceLogoMixin`(client) |
| **`mtrlte.mtrserverdecor.mixins.json`（新）** | B | `PidsScheduleScopeMixin` |

- `mtr` / `mtrserver` 两份原先是 A+B 混装，拆成上面 4 份；`package` 与 `plugin`（`MtrMixinPlugin` /
  `MtrServerMixinPlugin`）保持原值，两份同 package 的配置各自列类（Mixin 支持同 package 多配置）。
- `fabric.mod.json` 的 `mixins` 现在列 10 份（A 8 + B 2）。
- 静态自检：`bash v1.20.1/tools/check-mixin-split.sh` → 20 个 mixin 类，每个都在 jar 里且只出现一次。

## 4. entrypoint 拆分

**main（选了「兼容壳」方案）**：`fabric.mod.json` 仍只有一个 `LteServer`。

| 类 | 侧 | 负责 |
|---|---|---|
| `server/LteServer` | 壳 | 先 `LteServerDecor.loadConfig()`，再 A、再 B，最后 `installRegistrySelfCheck()`。拆仓库时删 B 那一行 |
| `server/LteServerLights` | A | 配置加载、`ServerAuthority`、握手、`LteCoreItems`、`/ltebind`、解绑钩子、`ServerCollision.tick`、信号绑定进服同步、注册表自检 |
| `server/LteServerDecor` | B | 厦门物件页签 / logo / 贴墙标识 / 扶手 / 装饰方块 / RCC / PIDS / PSD-LCD / 两个网络通道 / DEP / 站外标志 / 页签重排 / LTED 仪表板物品 / 售票机 / 充电宝 / 售卖机 / 安检 / 时钟 / 闸机 / eyecandy / 方速屏蔽门 + PIDS 数据范围两 tick |

**client**：18 个入口收成 2 个，顺序与拆分前一致。

| 类 | 侧 | 负责（按注册顺序） |
|---|---|---|
| `LtedClient` | B | `DecorConfig.load()` → `XiamenClient` → `WallSignClient` → `RailingGlassClient` → `DecorClient` → `PidsXiamenClient` → `PsdLcdClient` → `DepOnWallClient` → `LogoClient` → `XiamenMachinesClient` → `ChargerClient` → `DrinksMachineClient` → `SecurityClient` → `FangsuPsdClient` |
| `LteClient` | A | `MtrHeadlightClient` → `CollisionBootstrap` → `BuiltInTrainsInit` → `VehicleLightsJsonClient` → `SignalLinkClient` |

- 跨侧依赖只有两条，都保留原语义：A 先建「LTE 工具」页签 → B 的 `LogoObjects` / `WallSignObjects` 往里加条目；
  B 的 `LogoObjects.pinCoreTabOrder()` 必须在它自己那几个模块之后。
- `VehicleLightsJsonServer`（main）与 `ModMenuIntegration`（modmenu）未动。

## 5. 验证

```
# 构建
JAVA_HOME="C:/Program Files/Java/jdk-21.0.10" gradle -p v1.20.1 build --no-daemon -q        # OK
# 旧配置迁移用例（离线，不需要 Fabric / MC）
bash v1.20.1/tools/run-decor-config-migration-test.sh                                       # OK: decor config migration (12+ assertions)
# mixin 分组静态自检
bash v1.20.1/tools/check-mixin-split.sh                                                     # OK: 20 classes, each exactly once
```

迁移用例覆盖：旧键 → 新文件（含键 trim、非法颜色丢弃、`GREEN` → `green`、拼错的门灯色收回橙）、
「mtrlte.json 已被重写」的二次启动仍读到同值、空目录全新安装不报错、LTE 配置不再写这 5 个键。

## 6. 已知影响 / 后续

- **其它三个 MC 版本**（1.18.2 / 1.19.2 / 1.19.4）的 `XiamenRouteStyle` / `XiamenDoorLights` /
  `XiamenRouteStrip` 仍按 `HeadlightConfig.lineColors` 写，而 `HeadlightConfig` 在 `common/` 里共享 ——
  这三个版本要等各自的 `DecorConfig` 移植（本轮按约定不动它们，所以它们暂时编译不过）。
- 界面视觉 / 物品右键没有在游戏里跑过：本仓库没有 `run/` 配置，MTR 是 `compileOnly`，`runClient`
  起不来。逻辑与资源（模型 / lang / 物品注册 / 页签 pin）已静态核对。
- LTED 仪表板暂时沿用 `mtrlte:item/lte_dashboard` 贴图（没有新美术资源）。
