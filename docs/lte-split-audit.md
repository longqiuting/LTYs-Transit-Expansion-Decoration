# LTE 拆分审计：A 优化 / B 装饰 / C 共享

> 只读扫描 `D:\MC-MTR-JS\mtr-headlight`。统计口径：`v1.20.1/src/main/java/dev/js/mtrheadlight` + `common/src/main/java/dev/js/mtrheadlight`（`common` 不是独立模块，见 §6.3）。
> 行数 = `wc -l`。耦合 = 解析 `import dev.js.mtrheadlight.*` 得到的**文件级**边（不含注释/字符串）。

---

## 1. 总量

| 类 | 文件 | 行数 | 占比 |
|---|---:|---:|---:|
| **A 优化**（灯光/光束/散射/碰撞/轨道床/车灯 JSON） | 81 | 36,616 | 39.8% |
| **B 装饰**（xiamen/clock/decor/railing/rawmodel/security/logo/bind/signal/siding） | 213 | 47,940 | 52.1% |
| **C 共享基础**（配置/日志/兼容/GUI 框架/mixin 插件/网络/入口） | 30 | 7,514 | 8.2% |
| 合计 | 324 | 92,070 | |

**一句话结论：代码量 B > A，但资源量 A ≪ B（A ≈ 70 KB，B ≈ 30.4 MB）。两个模组的重心完全不同。**

---

## 2. 包/类清单

### 2.1 A — 优化（对 MTR 本体的增强）

| 包 / 类 | 文件 | 行数 | 职责 |
|---|---:|---:|---|
| `v1.20.1` 根 · Beam*（`BeamEditor3DScreen` 2517、`BeamPackWriter` 1027、`BeamSession` 587、`BeamGizmo` 423、`BeamListScreen` 384、`BeamEditScreen` 603、`BeamVehicleScreen` 391、`BeamPackScope` 396、`BeamSnap` 140、`BeamEditorOpener` 28） | 10 | 6,496 | 3D 光束编辑器：编辑/打包/导入车灯束布局 |
| `v1.20.1` 根 · `ConfigScreen` 251 + `ParameterScreen` 1595 | 2 | 1,846 | 车灯/碰撞配置界面（用 C 的 GUI 框架） |
| `v1.20.1` 根 · `LightFlare` 914、`GlowCompat` 261、`LightBackends` 84、`LightTest` 189、`RenderHooks` 194 | 5 | 1,642 | 灯晕渲染 / 发光兼容 / 后端桥 / 探针 / 世界渲染回调注册 |
| `v1.20.1` 根 · `RainSpray` 1008、`SprayParticle` 340、`SprayType` 144 | 3 | 1,492 | 雨中车灯溅水（粒子 + tick） |
| `v1.20.1` 根 · `TrainPreview` 1222、`TrainNames` 238 | 2 | 1,460 | 编辑器里的车型预览 / 车型名 |
| `v1.20.1` 根 · `CustomPack` 779、`PackImportScreen` 221 | 2 | 1,000 | 车灯资源包（`lte_vehicle_lights.json`）管理 / 导入界面 |
| `v1.20.1` 根 · `VehicleLightsJsonClient` 94 + `VehicleLightsJsonServer` 49 | 2 | 143 | 车灯 JSON 的 C2S/S2C 同步 |
| `v1.20.1` 根 · `FangsuTurnoutLight` 96 | 1 | 96 | 方速道岔灯（渲染增强） |
| `v1.20.1/collision`（`CollisionSystem` 2215、`CollisionImpact` 1382、`CarBand` 417、`MobCollision` 266、`CollisionBootstrap` 94、`OnboardToolBypass` 85、`DriverKeyBypass` 79、`TrainImpactDamageSource` 71） | 8 | 4,617 | 列车/车厢碰撞、撞飞、伤害（客户端推演） |
| `v1.20.1/sss`（`ScatterPass` 1215、`ScatterProgram` 510、`ScatterLights` 247、`DepthCopy` 208、`ColorCopy` 292） | 5 | 2,477 | 屏幕空间次表面散射（自编译 core shader） |
| `v1.20.1/server` · `ServerCollision` 765、`CarScan` 359、`ServerTrackbed` 331 | 3 | 1,458 | 服务端碰撞结算 / 车厢扫描 / 服务端轨道床 |
| `v1.20.1/mixin` · A 部分（beamlight、collision、rain、scatter、TrainClientRegistry、RailwayDataCapture、TrainSimulateCapture） | 7 | 469 | 见 §4 |
| `v1.20.1/mtr` · `BuiltInTrainsInit` | 1 | 31 | 1.20.1 专属入口：内置车型过滤 |
| `common` 根 A · `ProtoBeamCone` 1273、`BeamGlow` 642、`BeamRelight` 435、`BeamBlockLight` 298、`ConeVolumeBeam` 49、`BeamGeom` 89、`EndLight` 63、`LightBackend` 51 | 8 | 2,900 | 光束几何 / 光带 / 方块重打光 / 锥体光束 |
| `common` 根 A · `VehicleLights` 1125、`VehicleLightOverrides` 1091、`VehicleLightJson` 430、`VehicleLightPacks` 136、`BuiltinLights` 276 | 5 | 3,058 | 动态车型灯位绑定 / 覆盖库 / JSON / 包加载 / 内置表 |
| `common` 根 A · `LightController` 537、`TrainLamps` 418、`TrainPose` 432、`HeadlightSwitch` 185、`DepartureState` 417 | 5 | 1,989 | 每列车灯位计算 / 姿态采样 / 开关 / 发车判定 |
| `common` 根 A · `RailWeather` 405、`RainHush` 365、`ParticleCull` 179、`ShaderPackProbe` 142、`DebugProbe` 544、`ClearDiag` 58 | 6 | 1,693 | 天气判定 / 车内不画雨 / 粒子裁剪 / shader 探测 / 探针 |
| `common/MtrHeadlightClient` | 1 | 85 | A 的客户端初始化（config load / tick / Commands / RenderHooks）+ `config()` 访问器 |
| `common/collision`（`RailBed` 1877、`Trackbed` 673、`CarGeometry` 497、`TrainTickCapture` 80） | 4 | 3,131 | 道床几何 / 轨道床门面 / 车厢几何 / 列车 tick 快照 |
| `common/mtr` · `BuiltInTrains` | 1 | 533 | 内置车型灯位表 |
| **A 合计** | **81** | **36,616** | |

### 2.2 B — 装饰（自含内容）

| 包 / 类 | 文件 | 行数 | 职责 |
|---|---:|---:|---|
| `xiamen`（`XiamenStripPainter` 1876、`PsdLcdPainter` 1775、`PidsXiamenRenderer` 1270、`PidsXiamenConsist` 964、`PsdLcdRenderer` 898、`XiamenPsdBlocks` 509、`XiamenRouteStrip` 508、`PsdLcdLayout` 494 …） | 136 | 31,488 | 厦门风格：全高/半高屏蔽门、PIDS、PSD-LCD、线路条带、厦门/角美 logo、标志牌 |
| └ `xiamen` 本层（20 个） | 20 | 5,606 | 屏蔽门方块 / 线路条带 / 客户端接线 / 厦门对象注册 |
| └ `xiamen/fangsu` | 16 | 3,199 | 方速 BRT 屏蔽门（14 门 + 玻璃板） |
| └ `xiamen/pids` | 38 | 12,347 | 厦门 PIDS / PSD-LCD / 广告集 / 排版 |
| └ `xiamen/dep` | 19 | 3,359 | 发车倒数器（挂墙/挂顶/挂杆） |
| └ `xiamen/sign` | 8 | 866 | 站外标志 / 贴墙标识 |
| └ `xiamen/ticket` | 7 | 865 | 闸机 |
| └ `xiamen/machine` | 8 | 1,036 | 售票 / 余额查询机 |
| └ `xiamen/charger` | 7 | 1,317 | 充电宝租借机 |
| └ `xiamen/drinks` | 6 | 1,122 | 饮料售卖机 |
| └ `xiamen/eyecandy` | 7 | 1,771 | eyecandy 摆件方块 |
| `clock`（`ClockConfigScreen` 1046、`ClockRenderer` 495、`ClockConfig` 481、`ClockBlockEntity` 408 …） | 13 | 3,684 | 时钟方块（纯代码画钟面 + 配置） |
| `bind`（`ProjectorConnectorItem` 443、`PidsConnectorItem` 414、`MtrTiles` 347、`BindTargets` 254 …） | 15 | 2,988 | PIDS/投影机/信号连接器物品、绑定存储、LTE 仪表板 |
| `signal`（`SignalLinkItem` 537、`ConnectorNodeHighlight` 393、`FangsuTrafficLightMode` 266、`FangsuSignalSync` 257、`LteCoreItems` 216 …） | 10 | 2,524 | 信号连接器 + 方速信号灯同步 + **创造页签注册** |
| `security`（`LiquidScannerBlock` 465、`LiquidScannerConfigScreen` 364 …） | 7 | 1,361 | 液体扫描安检机 |
| `rawmodel`（`RawObjParser` 748、`RawModelJson` 485、`RawMesh` 118、`RawModelRenderer` 103） | 4 | 1,458 | OBJ/JSON 原始模型加载 + 渲染（装饰专用） |
| `decor`（`RccPlatforms` 171、`DecorBlocks` 140 …） | 5 | 503 | 大理石/RCC 站台方块 |
| `railing`（`RailingGlassBlocks` 191、`RailingWallBlock` 173 …） | 4 | 556 | 玻璃/墙扶手 |
| `logo`（`LogoObjects` 241 …） | 3 | 319 | LTE / 角美地铁 logo 方块 |
| `v1.20.1` 根 · `SidingScreen` 738、`SidingMapScreen` 956、`SidingInfo` 19、`TabIcons` 71 | 4 | 1,784 | MTR 侧线浏览器 + 创造页签图标（仅 B 用） |
| `v1.20.1/mixin` · B 部分 | 10 | 763 | 见 §4 |
| `common/bind` · `LteMtrLookup` | 1 | 478 | 反射查 MTR 轨道/车站/站台（连接器用） |
| `common/signal` · `SignalSourceCapture` | 1 | 34 | 信号灯源快照（配合 B 的 mixin） |
| **B 合计** | **213** | **47,940** | |

### 2.3 C — 共享基础

| 包 / 类 | 文件 | 行数 | 职责 |
|---|---:|---:|---|
| `common/HeadlightConfig` | 1 | 2,420 | **上帝配置对象**：246 个 public 字段，单一 `config/mtrlte.json`。A 用 ~240 项，B 只用 3 项（`lineColors`、`doorLightColor`、`doorLightByLine`）+ 2 个静态 helper |
| `common/LteLog` | 1 | 21 | 日志前缀 `[lte]`（全仓 86 处引用） |
| `common/MtrCompat` | 1 | 61 | MTR 跨版本反射封装（`Rail`/`RailwayData`） |
| `common/ModMenuIntegration` | 1 | 13 | ModMenu 入口 → 打开配置界面 |
| `common/net`（`LteChannel` 98、`ServerAuthority` 108） | 2 | 208 | `mtrlte:hello` 握手通道 / 服务端权威判定 |
| `common/server`（`MtrServerData` 117、`ServerConfig` 42） | 2 | 161 | 服务端轨道数据入口 / 服务端配置源 |
| `common/mixin/HostProbe` | 1 | 143 | 探测 ANTE/NTE 轨道宿主 mod |
| `v1.20.1` 根 GUI 框架 · `ConfigWriteTarget` 1484、`ColorPickerScreen` 316、`CategoryScreen` 235、`ConfigCategory` 55、`ResetConfirmScreen` 91 | 5 | 2,181 | 配置界面框架（A 的车灯界面 + B 的侧线界面共用） |
| `v1.20.1` 根 · `Commands` | 1 | 821 | `/mtr_lte` 单棵命令树，同时挂 A（碰撞/光束）与 B（dep debug）子命令 |
| `v1.20.1` 根 · `FangsuCompat` 111、`KeyHooks` 44、`LteDebug` 67、`McCompat` 19 | 4 | 241 | 方速探测 / 键位 / 诊断总开关 / MC API 兼容（`McCompat` 在 1.20.1 无引用） |
| `v1.20.1/client` · `RenderDistance` | 1 | 41 | BER 视距（**只被 15 个 B 渲染器使用**） |
| `v1.20.1/net` · `ClientHandshake` 117、`ServerHandshake` 92 | 2 | 211 | 握手两端实现 |
| `v1.20.1/server` · `LteServer` 303、`RegistrySelfCheck` 243 | 2 | 548 | **主服务端入口（注册 A+B 全部模块）** / 注册表自检（B 的方块注册也在用） |
| `v1.20.1/mixin` · 插件与 C 归属 | 6 | 444 | 见 §4 |
| **C 合计** | **30** | **7,514** | |

---

## 3. 耦合量化

### 3.1 跨类 import 边（文件级）

| 方向 | 边数 | 说明 |
|---|---:|---|
| A → B | **1** | `RenderHooks` → `signal/ConnectorNodeHighlight`（注册信号节点高亮） |
| B → A | **4** | 全部是 `xiamen/*` → `MtrHeadlightClient`（读配置） |
| A → C | 23 | `HeadlightConfig` 10、`LteLog` 4、`LteDebug` 4、`ServerAuthority` 2、`ClientHandshake` 1、`ServerConfig` 1、`MtrServerData` 1 |
| B → C | **108** | `LteLog` 82、`RenderDistance` 15、`RegistrySelfCheck` 5、`HeadlightConfig` 3、`ColorPickerScreen` 1、`LteDebug` 1、`MtrCompat` 1 |
| C → A | 7 | `Commands`（碰撞）、`FangsuTurnoutRenderMixin` |
| C → B | 3 | `Commands`（dep debug）、`FangsuTrafficLightRenderMixin` |
| A→A / B→B / C→C | 49 / 79 / 17 | 内部内聚 |

**结论：A 与 B 几乎零 import 耦合（1 + 4 条边）。真正的问题不在 A↔B，而在两边都压在同一个 C 上（131 条 A/B→C 边）。**

### 3.2 隐藏耦合（编译器/import 图看不到）

| 类型 | 位置 | 说明 |
|---|---|---|
| 同包引用 | `ConfigScreen`(A) → `SidingScreen`(B) 第 167 行 | A 的配置主界面有一个「侧线」按钮直接 new B 的界面 |
| 静态方法 | `SidingScreen`(B) / `SidingMapScreen`(B) → `ParameterScreen`(A) 的 `describe` / `stats` / `nameOf`（约 50 行，第 1252–1300 行） | B 的侧线界面借用了 A 配置界面里的 3 个 siding 文本 helper |
| 反射 | `bind/BeamEditorItem`(B) → `Class.forName("dev.js.mtrheadlight.BeamEditorOpener")` | B 的物品打开 A 的 3D 光束编辑器 |
| 反射 | `bind/LteDashboardItem`(B) → `Class.forName("dev.js.mtrheadlight.MtrHeadlightClient")` | B 的物品打开 A 的配置界面 |
| 注册 | `signal/LteCoreItems`(B) 注册 `bind/BeamEditorItem`(B→A 反射) | **A 的光束编辑器物品挂在 B 的创造页签里** |
| 入口 | `LteServer`(C) 第 76–152 行 | 一个 main 入口同时注册 xiamen/railing/clock/ticket/pids + ServerCollision |

### 3.3 共享配置类被谁用

| 共享类 | 行数 | A 用 | B 用 | 结论 |
|---|---:|---|---|---|
| `HeadlightConfig` | 2,420 | 10 个文件（碰撞、散射、beam mixin、服务端） | 3 个：`XiamenDoorLights`、`XiamenRouteStrip`、`XiamenRouteStyle` | **必须切**：B 只要 `lineColors` / `doorLightColor` / `doorLightByLine` / `normalizeDoorLightColor` / `parseColor`（约 120 行），其余 ~240 字段全是 A |
| `MtrHeadlightClient` | 85 | 11（客户端初始化 + `config()`） | 4（`config()`） | 必须各留一份；`config()` 被调用 **59 次** |
| `LteLog` | 21 | 4 | 82 | 复制 21 行即可 |
| `MtrCompat` | 61 | 0 | 1（`bind/LteMtrLookup`）+ C 的 `MtrServerData` | 复制 61 行 |
| `TabIcons` | 71 | 0 | 3（decor/signal/xiamen） | **归 B**（A 不引用），但 A 也需要自己的页签 → 各一份 |
| `client/RenderDistance` | 41 | 0 | 15 | **归 B**（或 C），与 A 无关 |
| `RegistrySelfCheck` | 243 | C 入口调用 | 5 个 B 注册模块 | 复制或抽公共 |
| `ColorPickerScreen` | 316 | A 的光束编辑器 2 处 | `ClockConfigScreen` 1 处 | 复制 |

### 3.4 拆开后「必须复制 / 改接口」的清单

| 类 | 行数 | 动作 |
|---|---:|---|
| `common/HeadlightConfig` | 2,420 | **拆两个类**：`HeadlightConfig`(A) + `LineStyleConfig`(B，约 120 行含序列化)。同时拆配置文件 |
| `common/MtrHeadlightClient` | 85 | 拆：A 保留初始化；B 新建自己的 config holder + `openConfigScreen` |
| `v1.20.1/Commands` | 821 | 拆两棵命令树（A：`beam3d`/`collision`/`lighttest`；B：`dep`/`clock`/`pids`） |
| `v1.20.1/server/LteServer` | 303 | 拆两个 `main` 入口 |
| `v1.20.1` GUI 框架（5 个类） | 2,181 | 复制到两边，或抽成 ~2k 行的小公共库（**建议抽库**，否则 2k 行重复） |
| `common/LteLog` | 21 | 复制 |
| `common/MtrCompat` | 61 | 复制 |
| `v1.20.1/TabIcons` | 71 | 复制（A/B 各一份页签图标） |
| `v1.20.1/client/RenderDistance` | 41 | 只给 B（A 不需要） |
| `v1.20.1/net/*`（4 类） | 415 | 归 A（碰撞权威）；B 不需要 |
| `fabric.mod.json` | — | entrypoints 拆：A = `LteServer`/`VehicleLightsJsonServer`/`MtrHeadlightClient`/`CollisionBootstrap`/`BuiltInTrainsInit`/`RenderHooks`；B = 其余 16 个 client 入口 |
| `mtrlte.mtr.mixins.json` | — | **拆两个**：A 只留 `TrainClientRegistryMixin`；B 留其余 8 个 |
| `mtrlte.mtrserver.mixins.json` | — | 拆：A = `TrainSimulateCapture` + `RailwayDataCapture`；B = `PidsScheduleScope` |
| 反射/同包 3 处 | — | 改字符串类名 + `ConfigScreen` 的侧线按钮 |

小计：**必须新写的胶水约 250–400 行**（两份小工具类 + 配置拆分 + 命令/入口拆分），**可选抽公共库约 2.2k 行**（GUI 框架）。

---

## 4. mixin 归属

判定依据按任务口径：**改 MTR/原版类且服务于 MTR 增强 → A；改 MTR 类但服务于自家装饰内容 → B；插件/兼容探测 → C**。

| mixin | 目标类 | 行数 | 归属 | 理由 |
|---|---|---:|---|---|
| `mixin/beamlight/BeamLightMixin` | `WorldRenderer` | 121 | **A** | 光束写进区块光照 |
| `mixin/collision/MixinEntityMove` | `Entity` | 51 | **A** | 碰撞推开 |
| `mixin/rain/WorldRendererRainMixin` | `WorldRenderer` | 55 | **A** | 车内不画雨 |
| `mixin/scatter/GameRendererScatterMixin` | `GameRenderer` | 51 | **A** | 散射 pass 注入 |
| `mixin/mtr/TrainClientRegistryMixin` | `TrainClientRegistry` | 107 | **A** | 内置车型过滤（车灯） |
| `mixin/mtrserver/RailwayDataCaptureMixin` | `RailwayData` | 36 | **A** | 抓轨道数据供碰撞 |
| `mixin/mtrserver/TrainSimulateCaptureMixin` | `TrainServer` | 48 | **A** | 抓列车 tick 供碰撞 |
| `mixin/mtr/RenderSignalBaseMixin` | `RenderSignalBase` | 176 | **B** | 信号灯颜色（方速/线路色） |
| `mixin/mtr/RenderPIDSStationMixin` | `RenderPIDS` | 68 | **B** | 厦门 PIDS 站名 |
| `mixin/mtr/RenderPidsTianjinStationMixin` | `RenderPIDS`（天津） | 63 | **B** | 天津 PIDS |
| `mixin/mtr/RenderPsdDoorTextureMixin` | `RenderPSDAPGDoor` | 57 | **B** | 屏蔽门贴图替换 |
| `mixin/mtr/BlockPlatformDoorTypeMixin` | `BlockPlatform` | 60 | **B** | 让自家全高门被识别为 `platform_psd` |
| `mixin/mtr/RouteMapArtMixin` | `RouteMapGenerator` | 95 | **B** | 线路图条带 |
| `mixin/mtr/RouteMapCacheKeyMixin` | `ClientCache` | 43 | **B** | 线路图缓存键 |
| `mixin/mtr/RouteAsyncScopeMixin` | `ClientCache` | 70 | **B** | 线路图异步作用域 |
| `mixin/mtr/StationNameEntranceLogoMixin` | `RouteMapGenerator` | 82 | **B** | 站名入口 logo |
| `mixin/mtrserver/PidsScheduleScopeMixin` | `SavedRailBase` | 49 | **B** | PIDS 班次作用域 |
| `mixin/FangsuMixinPlugin` | — | 119 | **C** | 插件（按方速是否安装决定） |
| `mixin/fangsu/FangsuTurnoutRenderMixin` | `com.fangsu...TurnoutBlockEntityRenderer` | 81 | **C** | 第三方兼容（但被 A 的 `FangsuTurnoutLight` 使用） |
| `mixin/fangsu/FangsuTrafficLightRenderMixin` | `com.fangsu...BlockEntityTrafficLight` | 54 | **C** | 第三方兼容（B 信号用） |
| `mixin/mtr/MtrMixinPlugin` | — | 81 | **C** | MTR 版本门控，两边都要 |
| `mixin/mtrserver/MtrServerMixinPlugin` | — | 62 | **C** | 同上 |
| `mixin/registry/SimpleRegistryFreezeMixin` | `SimpleRegistry` | 47 | **C** | 注册表冻结自检（两边注册块都用） |
| `common/mixin/HostProbe` | — | 143 | **C** | ANTE/NTE 探测 |

**A = 7 个（469 行），B = 10 个（763 行），C = 7 个（586 行）。**

⚠️ `mtrlte.mtr.mixins.json` 和 `mtrlte.mtrserver.mixins.json` 各把 A、B 混在一个文件里，**必须拆**。目标类无重叠，拆开后两个模组不会抢同一个注入点（前提是各自保留 `MtrMixinPlugin`）。

---

## 5. 资源归属（`v1.20.1/src/main/resources/assets/mtrlte`，30.79 MB / 1,208 文件）

| 类 | 体积 | 文件数 | 内容 |
|---|---:|---:|---|
| **A** | **0.07 MB** | **13** | `shaders/core/{light_scatter,lte_cone_volume}.{fsh,vsh,json}`（41 K）、`builtin_lights.json`（44 K）、`textures/vehicle/light_flare.png`、`textures/particle/spray*.png`、`models/item/{beam_editor,lte_dashboard}.json` |
| **B** | **30.42 MB** | **1,183** | `eyecandies/` 11 M、`textures/eyecandies/` 11 M、`textures/block/` 6.4 M、`models/{block,item}` 1.7 M、`blockstates/` 551 K、`advertisements/` 904 K、`rawmodels/` 452 K、`fangsu_psd/` 351 K、`textures/fangsu_psd/` 195 K、`sounds/` 76 K、`pids_xiamen_*.json` |
| **C** | **0.30 MB** | **12** | `lang/{en_us,zh_cn}.json`（119 K，两边都要）、`textures/gui/**`（PIDS 车厢图 / PSD-LCD 素材，201 K）、`sounds.json`、`models/item/tab_{core,tools}.json` |

`data/`（267 K）全部是 B 的 loot table / recipe；`icon.png` 各一份。

**结论：A 只带走 13 个文件 / 70 KB；B 留下 30.4 MB。资源不是拆分难点。**

---

## 6. 结论：最小切面

### 6.1 一句话

**这不是「按 import 图切」的问题——A↔B 只有 5 条 import 边；真正的切面是「谁拥有配置、入口、命令树、创造页签、命名空间」。把这 5 处从共享层里拔出来，两边就能独立编译。**

### 6.2 最小切面（按改动顺序）

| # | 切点 | 涉及文件 | 行数 | 必要性 |
|---|---|---|---:|---|
| 1 | **拆 `HeadlightConfig`** | 2420 行拆成 A 配置 + B 的 `LineStyleConfig`（~120 行）；`config/mtrlte.json` 拆成两个文件 | 2,420 | 必须 |
| 2 | **拆配置访问器** | `MtrHeadlightClient` 85 → A 的一份 + B 的一份；B 的 4 处 `config()` 调用改指向 | 85 | 必须 |
| 3 | **拆命令树** | `Commands` 821 → `/lte`（A）+ `/mtrlte`（B） | 821 | 必须 |
| 4 | **拆服务端入口** | `LteServer` 303 → A/B 两个 `main` | 303 | 必须 |
| 5 | **拆 mixin json** | `mtrlte.mtr.mixins.json`、`mtrlte.mtrserver.mixins.json` 各拆两个；`fabric.mod.json` 的 mixins/entrypoints 数组拆开 | — | 必须 |
| 6 | **页面归属** | 光束编辑器物品 `bind/BeamEditorItem` + 页签注册 `signal/LteCoreItems` 归 A（或保留反射） | ~284 | 必须 |
| 7 | **摘掉侧线按钮** | `ConfigScreen` 第 167 行的 `new SidingScreen(...)`（A→B 同包耦合） | 1 处 | 必须 |
| 7b | **抽走 siding 文本 helper** | `ParameterScreen` 的 `describe`/`stats`/`nameOf`（约 50 行）移到 B 的 `SidingInfo`/新 `SidingText`，断掉 B→A 的静态引用 | ~50 | 必须 |
| 8 | **复制小工具** | `LteLog`(21)、`MtrCompat`(61)、`TabIcons`(71)、`RenderDistance`(41，只给 B) | 194 | 必须 |
| 9 | **命名空间** | A 改 `mtrlte:` → 新 namespace：7 处代码字面量 + 13 个资源文件；**B 保留 `mtrlte`**（避开重命名 1,183 个资源） | ~20 处 | 必须 |
| 10 | **GUI 框架**（可选） | `ConfigWriteTarget`(1484)/`CategoryScreen`(235)/`ConfigCategory`(55)/`ColorPickerScreen`(316)/`ResetConfirmScreen`(91) 共 2,181 行：抽公共库或两边各复制一份（B 的侧线界面已经 `new CategoryScreen`） | 2,181 | 强烈建议抽库 |

### 6.3 迁移量估算

| 项 | 行数 |
|---|---:|
| A 侧整体迁出（81 文件） | **36,616** |
| B 侧留下（213 文件） | 47,940 |
| C 必须新写/改的胶水 | **≈ 250–400**（配置拆分 + 两份 config holder + 命令/入口拆分 + 3 处反射改字符串） |
| 可选抽公共 GUI 库 | ≈ 2,200 |
| 必须拆动的总行数（含被拆的类内部重排） | **≈ 7,000–7,500**（几乎等于整个 C 层） |

**注意 `common` 不是独立模块**：`v1.20.1/build.gradle:38` 把 `../common/src/main/java` 直接并进 `main` 的 `srcDirs`。所以没有模块边界可依赖，拆分必须靠包名 + 两个独立构建。

### 6.4 风险点

| 风险 | 等级 | 说明 / 对策 |
|---|---|---|
| **`HeadlightConfig` 上帝对象** | 🔴 高 | 246 个 public 字段 + 单一 JSON + 默认值迁移逻辑（`sanitize*`）。拆字段容易漏掉序列化/老配置兼容。建议：先按「A/B 字段」加注释分组，再用一个只含 B 字段的投影类，**不动 A 的 240 字段** |
| **单一配置文件 `config/mtrlte.json`** | 🔴 高 | 两边都写同一个文件 → 后写覆盖。必须拆成两个 path，并处理老文件迁移 |
| **循环依赖** | 🟠 中 | `ConfigScreen`(A)→`SidingScreen`(B) 同包、`LteCoreItems`(B)→`BeamEditorItem`(A 反射)、`LteServer`(C) 同时注册两边。拆前先断这几条，否则拆不动 |
| **同包名 / 同 namespace** | 🟠 中 | 两个模组若都留在 `dev.js.mtrheadlight` + `mtrlte:`，**同时安装会类冲突/注册冲突**。必须一方换包名和 namespace（建议 A 换，成本 13 文件） |
| **反射耦合静默失效** | 🟠 中 | `Class.forName` 两处 + `MtrHeadlightClient.openConfigScreen`，编译器不报错，运行时才 NPE。拆完要跑冒烟 |
| **mixin 冲突** | 🟡 低 | 当前 A/B 的 `@Mixin` 目标**无重叠**；拆 json 时保留各自 `MtrMixinPlugin` 即可。真正风险在 `SimpleRegistryFreezeMixin`（两边都注册块，冻结自检必须都保留一份） |
| **`common` 无模块边界** | 🟡 低 | 只能靠包名拆分；建议拆完把 A/B 各建独立 Gradle 工程，不要再共享 `srcDirs` |
| **B 侧 30 MB 资源 / 1,183 文件** | 🟡 低 | 只是体积问题，不影响切面；但若 B 改 namespace，这 1,183 个文件全部要改名 → 因此**让 B 保留 `mtrlte`** |

### 6.5 给 yomi 的 A 包边界（建议）

```
A 模块应包含：
  dev.js.mtrheadlight.beam.*      ← BeamEditor* / Beam* (10 类, 6.5k)
  dev.js.mtrheadlight.light.*     ← BeamGlow/Relight/BlockLight/ProtoBeamCone/LightController/LightFlare/...
  dev.js.mtrheadlight.collision.* ← 12 类 (7.7k)
  dev.js.mtrheadlight.sss.*       ← 5 类 (2.5k)
  dev.js.mtrheadlight.train.*     ← VehicleLights*/TrainLamps/TrainPose/DepartureState/HeadlightSwitch/BuiltInTrains
  dev.js.mtrheadlight.server.*    ← ServerCollision/CarScan/ServerTrackbed (+自己的入口)
  dev.js.mtrheadlight.net.*       ← 握手/权威
  dev.js.mtrheadlight.config.*    ← HeadlightConfig(A 部分) + 自己的 config holder
  assets/<new-ns>/{shaders,builtin_lights.json,light_flare,spray,beam_editor}
B 模块保留：
  xiamen/ clock/ decor/ railing/ rawmodel/ security/ logo/ bind/ signal/ siding
  + 自己的 LineStyleConfig、TabIcons、RenderDistance、LteLog、MtrCompat、GUI 框架
```

---

*审计脚本：`temp/lte-audit.js`、`temp/asset-audit.js`；机器可读结果：`temp/lte-audit.json`。*
