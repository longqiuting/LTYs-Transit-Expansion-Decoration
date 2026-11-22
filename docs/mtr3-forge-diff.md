# YMTR Forge 3.6.6.1 vs Fabric 3.6.4.2 —— 类 / 映射 / API 差异取证

> 取证日期：2026-11-22
> 对象 A（Fabric，旧一小版）：`libs/MTR-fabric-1.20.1-3.6.4.2.jar`（64,413,784 B）
> 对象 B（Forge/YMTR，新一小版）：`attachments/MTR-forge-1.20.1-3.6.6.1.jar`（64,180,174 B）
> 方法：`unzip -l` 全量清单 + `javap -p` 签名对比 + 类文件常量池字符串扫描 + 全类二进制逐字节比较。
> 全部结论均由上述命令输出支撑，未做源码级推测。临时工作目录：`%LOCALAPPDATA%\Temp\mtrdiff`。

---

## 0. 结论摘要（TL;DR）

| 问题 | 结论 |
|---|---|
| 包结构是否一致 | **一致**。两端都是扁平 `mtr.*`（`mtr.block` / `mtr.data` / `mtr.client` / `mtr.render` / `mtr.mappings` …），**不存在** `mtr.core` / `mtr.mod` 之类的分层差异。 |
| 类集合重叠 | `mtr.*` 共 1790/1826 个类名相同（98.0%）；Forge 多 36 个，Fabric 多 9 个，全部是**平台胶水**或**3.6.6.1 新增功能**，无一处是同名类搬家。 |
| 映射命名空间 | **不同，且是本任务最关键的一条**：Fabric jar = intermediary（`net/minecraft/class_1937`），Forge jar = official/mojmap（`net/minecraft/world/level/Level`）。证据见 §2。 |
| 共享一套编译产物能否两端跑 | **不能**。1790 个共同类里只有 1308 个（73.1%）字节完全一致；剩下 482 个把 MC 类名写死进了常量池。源码级可以共享，但必须走 Architectury Transformer 那一类的**编译期重映射**（MTR 自己就是这么做的）。 |
| MTR 自己的双端做法 | Architectury 多加载器工程：一份 common 源码 → 两个平台 jar；`mtr/forge/*`（11 类）与 `mtr/fabric/*`（2 类）是平台缝；Forge 侧 `mods.toml` 强制依赖 Architectury ≥ 1.26.37；两端都不 shade Architectury API。 |
| LTED 用到的 MTR API 是否还在 | **全部在**。65 条 `import mtr.*` 全部在 Forge jar 中找到同名类；65 个类里 52 个成员集完全等价，13 个有差异，但**差异全部是「新增」+「映射改名」，没有一条真实删除**。 |
| LTED 会不会被 3.6.4.2 → 3.6.6.1 的新增 API 影响 | **不会**。新增 33 项成员（`REDSTONE_DOOR_CONTROL`、`getBrakingDistance`、`getRecommendedSpeed` 等）抽查 15 个代表名，在 LTED 代码中命中均为 **0**。 |
| 真正的移植阻塞点 | 不在 MTR API，而在 LTED 自身：283 个源文件里 234 个 `import net.minecraft.*`（**Yarn 名**），70 个 `import net.fabricmc.*`（35 个 Fabric API 入口），129 处 `Registry.register(...)`。 |

一句话：**MTR 这边几乎是免费兼容，LTED 那边是硬阻塞。**

---

## 1. 类集合对比

### 1.1 总量

| 指标 | Fabric 3.6.4.2 | Forge 3.6.6.1 |
|---|---|---|
| jar 内 `.class` 总数 | 2173 | 1936 |
| `mtr/**` 类数 | 1799 | 1826 |
| `mtr/libraries/**`（shade 的 jetty/msgpack 等） | 1169 | 1169 |
| 顶层 `mtr/*.class`（入口/注册表） | 27 | 23 |
| 资源 `assets/**` | 3964 | 3978 |
| 数据 `data/**` | 517 | 517 |

Fabric 比 Forge 多出的 237 个类**全部在非 MTR 侧**，MTR 业务代码反而是 Forge 更多（1826 > 1799）：

| 非 `mtr/` 类 | Fabric | Forge |
|---|---|---|
| 两者共有 | 109 | 109 |
| 仅本侧独有 | **265**（`javazoom` 89、`junit` 102、`org` 73、architectury inject 标记 1） | **1**（自己的 architectury inject 标记） |

→ Fabric jar 只是额外打包了 jlayer/junit/jetty 相关类才显得更大，与 API 无关。

### 1.2 包前缀

| 包 | Fabric | Forge | 说明 |
|---|---|---|---|
| `mtr/block` | 170 | 170 | 完全一致 |
| `mtr/data` | 82 | 84 | +2（`TrainClient$ConnectionCache`、`$TranslucentCarRender`） |
| `mtr/model` | 80 | 80 | 一致 |
| `mtr/screen` | 53 | 53 | 一致 |
| `mtr/client` | 39 | 41 | +`AnteCompat`、`DynamicTrainModel$RenderPart` |
| `mtr/render` | 36 | 38 | +`TrainRenderCulling`、`RenderDrivingOverlay$GeometryBuilder` |
| `mtr/mappings` | 31 | 30 | Fabric 多 `FabricRegistryUtilities`，Forge 换成 `mtr/forge/mappings/ForgeUtilities` |
| `mtr/webdashboard` | 29 | 47 | +18（`tunnel/*`、`WebDashboardTrackService` 等，3.6.6.1 新功能） |
| `mtr/mixin` | 7 | 6 | 平台缝，见 §3.4 |
| `mtr/fabric` | 2 | — | `RegistryImpl`、`RegistryClientImpl` |
| `mtr/forge` | — | 11 | `MTRForge`、`RegistryImpl`、`RegistryClientImpl`、`ForgeUtilities` + 内部类 |
| 顶层 `mtr/*` | 27 | 23 | Fabric 多 `MTRFabric`、`MTRFabricClient`(+内部类)、`ModMenuConfig`；Forge 多 `CreativeModeTabs`、`Blocks`、`BlockEntityTypes` |

`comm` 集合运算结果：

- **同名同路径：1790 个**（`mtr/` 全集口径 1799 vs 1826）
- **仅 Fabric 存在：9 个**
  ```
  mtr/fabric/RegistryClientImpl, mtr/fabric/RegistryImpl,
  mtr/mappings/FabricRegistryUtilities,
  mtr/mixin/EntitySpawnMixin, mtr/mixin/UnknownPacketMixin,
  mtr/ModMenuConfig, mtr/MTRFabric, mtr/MTRFabricClient(+$CustomResourcesWrapper)
  ```
- **仅 Forge 存在：36 个**
  ```
  mtr/forge/**（11）                        ← 平台胶水
  mtr/mixin/LevelRendererMixin              ← 平台缝（Fabric 侧换成 EntitySpawn/UnknownPacket 两个）
  mtr/client/AnteCompat, DynamicTrainModel$RenderPart
  mtr/data/TrainClient$ConnectionCache, TrainClient$TranslucentCarRender
  mtr/render/TrainRenderCulling, RenderDrivingOverlay$GeometryBuilder
  mtr/webdashboard/**（18：tunnel/、TracksServletHandler、WebDashboardTrackService…）
  ```
  → 后 24 个都不引用 `net/minecraftforge`（全局仅 9 个类引用 Forge，全在 `mtr/forge/**` + `mtr/MaterialColor`），所以它们是 **3.6.6.1 的版本增量**，不是 Forge 特化。

### 1.3 字节级同一性（最强的一条证据）

对 1790 个同名 `mtr.*` 类做逐字节 `cmp`：

| | 数量 | 占比 |
|---|---|---|
| **完全字节相同** | **1308** | **73.1%** |
| 字节不同（常量池里带 MC 名/映射名） | 482 | 26.9% |

说明「同一份 common 源码，编译两次」这件事是实锤：**七成以上的 MTR 业务类在两个 jar 里是同一个文件**（数据结构、GUI 常量、几何算法、路径计算等完全不碰 MC 类型）。剩下 482 个正是把 MC 类型写进签名的那批。

只有 473/1826（Fabric）与 484/1826（Forge）个类**任何位置**出现 `net/minecraft/`。

### 1.4 任务点名的十个类

| 类 | Fabric | Forge | 判定 |
|---|---|---|---|
| `mtr.block.BlockPSDTop` | 16 pub/prot 成员 | 16 | **同名可用**。成员集在归一化后完全一致；差异只有 8 个 MC override 被改名（`method_9534` ↔ `m_6227_` 等）。 |
| `mtr.block.BlockPIDSBase` | **不存在** | **不存在** | 两端都没有这个类。真实存在的是 `BlockPIDSBaseHorizontal`（8 成员，归一化后一致）与 `BlockPIDSBaseVertical`。任务清单里的这个名字是猜的。 |
| `mtr.client.ClientData` | 39 | 40 | 同名可用；Forge 多 1 个 `public static boolean REDSTONE_DOOR_CONTROL`（3.6.6.1 新增）。 |
| `mtr.data.Train` | 107 | 119 | 同名可用；Forge 多 3 字段（`decelerationConstant`/`enableDelayedAcceleration`/`heldTicks`）、5 方法（`getBrakingDistance`/`getDecelerationConstant`/`getAccelerationConstant`/`getManualNotchSpeedChange`/`invalidatePositionCache`）、**2 个 12/15 参构造重载**。旧构造签名全部保留。 |
| `mtr.render.MoreRenderLayers` | 6 | 6 | 公开 API 完全一致（`getLight`/`getInterior`/`getInteriorTranslucent`/`getExterior`/`getExteriorTranslucent`）。**私有实现被重写**：`LIGHT_CACHE` 由 `Map<String, MC>` 改为 `Map<MC, MC[]>`，`EXTERIOR_CACHE` 换成 `CUTOUT_CACHE`。→ 只影响反射/ASM 直接摸私有字段的代码，LTED 没摸。 |
| `mtr.client.RouteMapGenerator` | 16 | 16 | 同名可用，归一化后零差异（`NativeImage` 在 Fabric 侧是 `class_1011`，Forge 侧是 `com/mojang/blaze3d/platform/NativeImage`，属命名空间差异而非 API 差异）。 |
| `mtr.data.TrainClient` | 34 | 37 | 同名可用；Forge 多 `public final TrainProperties trainProperties`、`getRecommendedSpeed()`、`scanDoorsForRender(...)`。 |
| `mtr.block.IBlock` | 15 | 15 | **归一化后零差异**，纯静态常量+工具方法，最稳的一个。 |
| `mtr.data.TrainServer` | 15 | 18 | 同名可用；Forge 多 2 个 13/16 参构造重载，私有 `suspendedDwellTicks()` 被移除（私有，无影响）。 |
| `mtr.packet.Packet` | **不存在** | **不存在** | 两端都没有。包络基类实际是 `mtr.packet.PacketTrainDataBase`（3 成员，归一化后**零差异**）。`mtr/packet/` 下 15 个类名两端清单完全一致（`diff` 无输出）。 |

---

## 2. 映射命名空间（决定性证据）

### 2.1 类文件常量池

`mtr/data/Train.class`：

```
FABRIC 3.6.4.2:  (Lnet/minecraft/class_1937;DDD)Z          ()Lnet/minecraft/class_2248;
                 #net/minecraft/class_2689$class_2690      (Lnet/minecraft/class_1799;)V
FORGE  3.6.6.1:  Lnet/minecraft/world/level/Level          Lnet/minecraft/world/level/block/Block
                 (DDD)Lnet/minecraft/core/BlockPos         net/minecraft/core/registries/BuiltInRegistries
```

`mtr/block/BlockPSDTop.class`：

```
FABRIC: (Lnet/minecraft/class_2680;)Lnet/minecraft/class_2680;
FORGE : (Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;…)
        Lnet/minecraft/world/level/block/state/BlockState; Lnet/minecraft/world/phys/shapes/VoxelShape;
```

### 2.2 全量统计

| 扫描项（`mtr/**` 范围内） | Fabric | Forge |
|---|---|---|
| 含 `net/minecraft/class_`（intermediary）的类文件 | **471** / 1826 | **0** / 1826 |
| 含 `net/minecraft/world/level`（mojmap 包路径）的类文件 | **0** / 1826 | **287** / 1826 |
| 含 `net/minecraft/core/`（mojmap）的类文件 | 0 | 279 |
| 含任意 `net/minecraft/` 的类文件 | 473 | 484 |
| 含重命名成员标记的类文件 | 311（`method_NNN` / `field_NNN`） | 322（`m_NNN_` / `f_NNN_`） |

Fabric 侧 `META-INF/MANIFEST.MF` 直接自报：

```
Fabric-Mapping-Namespace: intermediary
```

Forge 侧 `mtr-refmap.json`（mixin 重映射表）全部指向 **mojmap**：

```json
"mtr/mixin/PlayerRendererOffsetMixin": {
  "getRenderOffset(...)": "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;m_7860_(...)"
},
"mtr/mixin/PlayerTeleportationStateAccessor": { "isChangingDimension": "f_8927_:Z" }
```

Fabric 侧同一文件的 refmap 全指向 intermediary：

```json
"mtr/mixin/EntitySpawnMixin": { "handleAddEntity": "Lnet/minecraft/class_634;method_11112(Lnet/minecraft/class_2604;)V" }
```

### 2.3 结论

- Fabric jar：**intermediary**（`class_XXXX` / `method_XXXX` / `field_XXXX`）。这是 Fabric Loom `remapJar` 的产物，运行时由 Loom/Fabric Loader 用 Yarn 名混入。
- Forge jar：**official (Mojang mappings)**，即 `net.minecraft.world.level.*` / `m_xxxxx_`。无任何 intermediary 残留（0 个类）。
- 因此 **同一个 `.class` 不可能同时被两个加载器接受**：它引用的 MC 类型在另一端根本不存在。共享必须在**编译期**用 Architectury Transformer（或 Stonecutter/Preprocessor + 两套 gradle 目标）完成，而不是运行时。

---

## 3. 双端结构分析（MTR 自己的做法）

### 3.1 元数据对照

| | Fabric 3.6.4.2 | Forge 3.6.6.1 |
|---|---|---|
| 描述文件 | `fabric.mod.json` | `META-INF/mods.toml` + `pack.mcmeta` |
| 入口 | `main: mtr.MTRFabric`<br>`client: mtr.MTRFabricClient`<br>`modmenu: mtr.ModMenuConfig` | `MixinConfigs: mtr.mixins.json,mtr.voxy.mixins.json`（mods.toml 无 `[[mixins]]`，走 Manifest 属性） |
| 平台类 | `mtr/MTRFabric`, `mtr/MTRFabricClient` | `mtr/forge/MTRForge`(+`$MTRModEventBus`) |
| 依赖 | `fabric: *`, `minecraft: *` | `forge [36,)`, `minecraft [1.16.5,)`, **`architectury [1.26.37,)` mandatory** |
| License | All Rights Reserved | `MIT AND GPL-3.0-or-later`（`META-INF/licenses/YMTR-*` 7 份） |
| 映射 | `Fabric-Mapping-Namespace: intermediary` | 无（= official） |

### 3.2 Architectury 证据

两个 jar 里都有同一个注入标记（Architectury Loom 的 common dev-jar 产物）：

```
architectury_inject_MinecraftTransitRailway_common_a1f42693..._c957d270...common12013642devjar/PlatformMethods.class   ← Fabric
architectury_inject_MinecraftTransitRailway_common_a1f42693..._22b5c421...common12013661devjar/PlatformMethods.class   ← Forge
```

前缀 `MinecraftTransitRailway_common` + 两端 `PlatformMethods` 说明：**一份 common 源码、两次平台化编译**；注入目录名末尾的 `12013642` / `12013661` 是项目版本 `1.20.1-3.6.4.2` / `1.20.1-3.6.6.1` 的压缩编码，正好对上两个 jar。

但 **Architectury API 本身不被 shade**：两端 `find -path '*dev/architectury*'` 结果均为 **0 个 class 文件**，只在 9（Forge）/ 7（Fabric）个类里留下对外引用（`Registry`、`RegistryClient`、`MTRClient`、`mtr/mappings/RegistryUtilities*`、`DeferredRegisterHolder`、`NetworkUtilities`）。Forge 端因此用 `mods.toml` 强制要求用户装 Architectury。

### 3.3 平台缝（platform seam）

| 层 | Fabric | Forge |
|---|---|---|
| 注册表实现 | `mtr/fabric/RegistryImpl`、`RegistryClientImpl` | `mtr/forge/RegistryImpl`、`RegistryClientImpl`(+`$StationColor`) |
| 工具桥 | `mtr/mappings/FabricRegistryUtilities` | `mtr/forge/mappings/ForgeUtilities`(+4 内部类：`$Events`、`$ClientsideEvents`、`$CreativeModeTabWrapper`、`$EntityRendererPair`、`$RegisterCreativeTabs`) |
| 事件总线 | `net/fabricmc/fabric/api/event/Event`、`CommandRegistrationCallback`（7 个类引用 `net/fabricmc`） | `net/minecraftforge/**`（9 个类引用） |
| 抽象层 | 共用 `mtr/mappings/*Mapper`（30 个同名类：`BlockMapper`/`BlockEntityMapper`/`ScreenMapper`/`RenderLayerMapper`/`Utilities`…，Fabric 侧多 1 个 `FabricRegistryUtilities`） | 同左 |

`mtr/mappings/*` 这层是 MTR 给下游 addon 用的跨平台抽象（`BlockEntityMapper implements BlockEntity`、`ScreenMapper extends Screen` 等），LTED 大量依赖它——但它**自身也不是平台中立的二进制**：`BlockEntityMapper` 在两端只有 `method_11007`↔`m_142466_` 这类改名差异（见 §4.2），源码层面对齐即可。

### 3.4 Mixin

| | Fabric | Forge |
|---|---|---|
| 配置文件 | `mtr.mixins.json` + `mtr.voxy.mixins.json` | **完全相同两份**（同名、同 `package: mtr.mixin`、同 refmap 名） |
| mixin 类 | 7：`PlayerTeleportationStateAccessor`、`EntitySetOldPosAndRotMixin`、**`EntitySpawnMixin`**、`GameRendererMixin`、`PlayerRendererOffsetMixin`、**`UnknownPacketMixin`**、`VoxyFogCompatMixin` | 6：`PlayerTeleportationStateAccessor`、`EntitySetOldPosAndRotMixin`、`GameRendererMixin`、`PlayerRendererOffsetMixin`、**`LevelRendererMixin`**、`VoxyFogCompatMixin` |
| refmap | `mtr-refmap.json`（intermediary） | `mtr-refmap.json`（mojmap） |

→ mixin 配置是**源码级共享**的（Architectury 的 common mixin 机制），真正的差异只在 refmap 与个别平台专属注入点。

### 3.5 对 LTED 的意义

MTR 已经证明了「多加载器 + 一份 common 源码」这条路可行，但它依赖三个前置条件，缺一不可：

1. Gradle 里接 Architectury Loom（`architectury_inject_*common*devjar`）；
2. 用 `architectury-transformer` 把 common 字节码在构建期改写成 intermediary / official 两套；
3. Forge 端声明并依赖 Architectury 运行时。

---

## 4. LTED 受影响清单

### 4.1 覆盖面

`v1.20.1` 主源集：283 个 `.java`。

| 项 | 数量 |
|---|---|
| `import mtr.*` 唯一语句 | **65**（无 wildcard） |
| 引用 MTR 的文件 | 78 / 283 |
| `import net.minecraft.*` 唯一语句 | **162（全部 Yarn 名：`net.minecraft.block` / `util` / `registry` / `world` …）** |
| 引用 MC 的文件 | 234 / 283 |
| `import net.fabricmc.*` 唯一语句 | 35（`fabric.api` 125 处、`ClientModInitializer` 17、`loader.api` 7…） |
| 引用 Fabric API 的文件 | 70 / 283 |

MTR 依赖密度最高的 10 个类：
`mtr.client.ClientData`(26 文件)、`mtr.data.TrainClient`(19)、`mtr.block.IBlock`(15)、`mtr.data.RailwayData`(12)、`mtr.data.TransportMode`(11)、`mtr.mappings.BlockEntityMapper`(9)、`mtr.data.Station`(9)、`mtr.data.Siding`(7)、`mtr.client.TrainClientRegistry`(7)、`mtr.block.BlockPSDAPGDoorBase`(7)。

### 4.2 成员级差异（65 个类的全量 javap 对比）

做法：对每个类取 `javap -p` 的 public/protected 成员，把 `method_NNN`/`m_NNN_`/`field_NNN`/`f_NNN_` 与 MC 类型统一归一化后做集合比较。

| 结果 | 类数 |
|---|---|
| 归一化后**完全等价** | **52 / 65** |
| 有差异 | 13 / 65 |

13 个有差异的类：`ClientData`、`TrainClient`、`RailwayData`、`Siding`、`Train`、`TrainServer`、`BlockPSDAPGDoorBase`、`BlockPSDAPGDoorBase$TileEntityPSDAPGDoorBase`、`JonModelTrainRenderer`、`DynamicTrainModel`、`ModelTrainBase`、`WidgetBetterTextField`、`PacketTrainDataGuiClient`。

**关键判定**：把这 13 个类的 diff 里所有「仅 Fabric 有」的成员抽出来统计——

```
< method_9515#1  ×12     < method_9530#4  ×9      < method_9605#1 ×8
< method_9559#6  ×7      < method_9534#6  ×6      < method_9568#4 ×3
…（共 34 条，全部形如 method_NNN#arity）
```

**没有一条是真实删除**——34 条全部是 `method_NNN` → `m_NNNN_` 的映射改名。也就是说：**3.6.4.2 → 3.6.6.1 对 LTED 用到的 API 是纯增量，零破坏。**

Forge 侧新增成员共 **33 项**（含 4 个构造重载）：

```
ClientData.REDSTONE_DOOR_CONTROL          Train.getAccelerationConstant()
RailwayData.getRedstoneDoorControl()      Train.getDecelerationConstant()
RailwayData.isRedstoneDoorControlEnabled()Train.getBrakingDistance(float,float)
RailwayData.setRedstoneDoorControl(int)   Train.getManualNotchSpeedChange(int,float)
Siding.getDecelerationConstant()          Train.invalidatePositionCache()
Siding.getEnableDelayedAcceleration()     Train.scanDoorsForRender(...)
Siding.setEnableDelayedAcceleration(..)   TrainClient.getRecommendedSpeed()
Siding.setUnlimitedTrains(10 args)        TrainClient.scanDoorsForRender(...)
                                          TrainClient.trainProperties
JonModelTrainRenderer.hasTranslucentParts()
DynamicTrainModel.hasTextDisplays() / hasRenderStage(int)
ModelTrainBase.hasTextDisplays() / hasRenderStage(int)
WidgetBetterTextField.setFieldHeight(int)
PacketTrainDataGuiClient.send/recvRedstoneDoorControl…
```

在 LTED 1.20.1 源码里逐个 grep（`-w` 全词，抽查 15 个代表 token）：

| token | 出现次数 |
|---|---|
| `getRecommendedSpeed` / `scanDoorsForRender` / `REDSTONE_DOOR_CONTROL` | 0 / 0 / 0 |
| `isRedstoneDoorControlEnabled` / `getBrakingDistance` / `getDecelerationConstant` / `getAccelerationConstant` | 0 / 0 / 0 / 0 |
| `enableDelayedAcceleration` / `hasTranslucentParts` / `hasTextDisplays` / `hasRenderStage` | 0 / 0 / 0 / 0 |
| `setFieldHeight` / `setUnlimitedTrains` / `heldTicks` / `trainProperties` | 0 / 0 / 0 / 0 |

→ **抽查的 15 个 token 全为 0 命中**，新增 API 对 LTED 完全无害。

### 4.3 三类清单

#### A. 同名可用（直接复用，无需分支）—— 52 个类 + 全部 MTR 自有方法

- 全部 65 个 import 的**类名**在 Forge jar 中都存在（含内部类 `BlockPSDAPGDoorBase$TileEntityPSDAPGDoorBase`）。
- 52 个类成员集等价。LTED 实际调用的 MTR 自有方法名（`getActualState`、`generateRouteMap`、`generateColorStrip`、`generateDirectionArrow`、`simulateTrain(s)`、`isCloseToSavedRail`、`getNodePos`、`getOccupiedAspect`、`register(String, TrainProperties)`、`Rail`/`PathData`/`Route`/`Station`/`Siding`/`Depot` 的 getter、`IGui` 常量、`TrainProperties` 全字段…）在两端**同名同签名**——MTR 自己的方法不会被加载器改名。
- **结论**：13 个 `@Mixin(value = <MTR 类>, remap = false)` 的目标方法**两端逐个核对通过**（对每个类跑 `javap -p` 并 grep）：

| LTED mixin | 目标类 | 目标方法 | Fabric | Forge |
|---|---|---|---|---|
| `BlockPlatformDoorTypeMixin` | `BlockPlatform` | `getActualState` | ✓ | ✓ |
| `RouteMapArtMixin` | `RouteMapGenerator` | `generateRouteMap` / `generateColorStrip` / `generateDirectionArrow` | ✓ | ✓ |
| `StationNameEntranceLogoMixin` | `RouteMapGenerator` | `drawResource` | ✓ | ✓ |
| `RouteAsyncScopeMixin` | `ClientCache` | `getResource` | ✓ | ✓ |
| `RouteMapCacheKeyMixin` | `ClientCache` | `getRouteMap` / `getDirectionArrow` / `getStationNameEntrance` | ✓ | ✓ |
| `RenderPIDSStationMixin` | `RenderPIDS` | `getSchedules` | ✓ | ✓ |
| `RenderPidsTianjinStationMixin` | `ziyue.tjmetro…`（第三方） | `getSchedules` | n/a | n/a |
| `RenderPsdDoorTextureMixin` | `RenderPSDAPGDoor` | `render` | ✓ | ✓ |
| `RenderSignalBaseMixin` | `RenderSignalBase` | `getNodePos` / `getOccupiedAspect` | ✓ | ✓ |
| `TrainClientRegistryMixin` | `TrainClientRegistry` | `reset()V` / `register(String,TrainProperties)V` | ✓ | ✓ |
| `PidsScheduleScopeMixin` | `SavedRailBase` | `isCloseToSavedRail` | ✓ | ✓ |
| `RailwayDataCaptureMixin` | `RailwayData` | `simulateTrains` | ✓ | ✓ |
| `TrainSimulateCaptureMixin` | `TrainServer` | `simulateTrain` | ✓ | ✓ |

→ 13/13 的 MTR 侧注入点在 Forge 3.6.6.1 上依然存在，`remap = false` 的写法不需要改。

#### B. 需要分支（按平台/版本分叉）

| 项 | 为何要分支 | 触发条件 |
|---|---|---|
| **所有覆盖 MC 方法的地方** | 名字随映射变：Yarn `onUse`/`getPlacementState`/`getStateForNeighborUpdate`/`getCollisionShape`/`getOutlineShape`/`getCullingShape`/`asItem`/`appendProperties`，Forge 侧是 `m_6227_`/`m_49966_`/`m_24196_`… | **已触发**：`FangsuPsdDoorBlock`、`FangsuPsdEndDoorBlock`、`FangsuPsdGlassBlock`、`XiamenPsdBlocks`、`MarblePlatformBlock`、`RccPlatformBlock` 等直接 `extends BlockPSDAPGDoorBase / BlockPlatform` 并 `@Override onUse / getPlacementState / getStateForNeighborUpdate / getCollisionShape`。这些 override 在 Forge 上必须改名为 `m_xxx_`（或交给 Architectury Transformer / Mojmap remap 处理）。 |
| **`@Mixin` 到原版类**（`remap = true`） | `Entity.class`、`WorldRenderer.class`、`GameRenderer.class`、`SimpleRegistry.class` 用 Yarn 名 | 4 个 mixin 文件；两端需各自的 refmap/映射名 |
| **`ScreenMapper` / `WidgetMapper` 子类的 override** | `init`/`render`/`close`/`shouldPause` 是 Yarn 名，Forge 侧 `m_7856_`/`m_88315_`/`m_7379_`/`m_7043_`（见 `WidgetBetterTextField`、`PIDSConfigScreen` 的成员 diff） | `PidsXiamenAdsScreen`、`PidsXiamenConfigScreen` 等 |
| **`BlockEntityRendererMapper` 子类** | `render`/`isInRenderDistance`/`rendersOutsideBoundingBox`/`getRenderDistance` 的 MC 侧名字不同 | `DepOnRoofRenderer`、`PidsXiamenRenderer`、`PsdLcdRenderer`、`RenderPsdDoor`、`RenderPsdApgDoor` |
| **`MoreRenderLayers` 私有缓存** | 3.6.6.1 把 `LIGHT_CACHE: Map<String,MC>` 改成 `Map<MC,MC[]>`，`EXTERIOR_CACHE` → `CUTOUT_CACHE` | 目前 LTED 未触碰私有字段 → **暂不需要分支**，但任何反射/`@Accessor` 都不能碰这几个字段 |

#### C. 不存在（任务清单里的名字需要更正）

| 清单里的名字 | 实际情况 |
|---|---|
| `mtr.block.BlockPIDSBase` | 两个 jar 都**没有**。真实基类：`BlockPIDSBaseHorizontal`（8 成员）、`BlockPIDSBaseVertical`（4 成员），两端成员数各自相同、归一化后等价。 |
| `mtr.packet.Packet` | 两个 jar 都**没有**。真实基类：`PacketTrainDataBase`（3 成员，两端零差异）；接口是 `mtr.packet.IPacket`。 |

> 这两条不是版本差异，是任务描述里的类名本身不存在——查证后应以此为准。

### 4.4 LTED 自身的平台耦合（真正的移植成本）

| 耦合面 | 规模 | Forge 对应物 |
|---|---|---|
| Yarn 名 MC 类型 | 162 个唯一 import / 234 文件 | mojmap 全量改名（`net.minecraft.block.Block` → `net.minecraft.world.level.block.Block` 等） |
| `Registry.register(...)` | **129 处** | `DeferredRegister` / `RegistryEvent` |
| Fabric 网络 API | `ServerPlayNetworking` 46、`ClientPlayNetworking` 30、`PacketByteBufs` 30 | `SimpleChannel` / `NetworkRegistry` |
| `BlockEntityRendererRegistry.register` | 31 | `EntityRenderersEvent.RegisterRenderers` |
| `FabricBlockEntityTypeBuilder` | 35 | `BlockEntityType.Builder` |
| `ItemGroupEvents` | 53 | `BuildCreativeModeTabContentsEvent` |
| `FabricLoader` | 18 | `FMLPaths` / `ModList` |
| Fabric 入口/事件 | `ModInitializer`、`ClientModInitializer`、`ClientTickEvents`/`ServerTickEvents`、`CommandRegistrationCallback`、`PlayerBlockBreakEvents`、`AttackBlockCallback`… | Forge 事件总线 |

MTR 提供了跨平台的 `mtr.mappings.RegistryUtilities` / `DeferredRegisterHolder`，但 LTED **基本没用**（0 处 `RegistryUtilities`、0 处 `FabricRegistryUtilities`）——注册走的是 `net.minecraft.registry.Registry` + Fabric API 直连。这是移植的主要工作量。

---

## 5. 对「共享一套 class 能不能两端跑」的最终回答

1. **编译产物层面：不能。** Fabric 类 471 个含 `net/minecraft/class_*`，Forge 类 0 个；Forge 类 322 个含 `m_xxxxx_`，Fabric 0 个。同一个 `.class` 在另一端会 `NoClassDefFoundError`。
2. **但七成是免费的。** 1790 个共同类里 1308 个（73.1%）在两 jar 中**逐字节相同**，482 个才有 MC 常量池差异。真正需要「双份编译」的只有那 482 个 + LTED 侧 234 个碰 MC 的文件。
3. **正确的共享粒度是源码，不是 class。** 参照 MTR：Architectury 三段式（`common` + `fabric` + `forge`）+ 构建期 transformer。LTED 若要跟进，最低成本路径是：
   - 把 41 个已存在的 `common/src/main/java` 文件、以及所有**不 import `net.minecraft`** 的类（283 - 234 = 49 个文件）先归入 common；
   - 平台缝按 MTR 的形状抽：`Registry`/`RegistryClient`/`Network`/`Renderer`/`ItemGroup`；
   - MC 类型在 common 里统一用**一套名字**（建议 mojmap，与 Forge 对齐、与 Architectury 默认一致），Fabric 侧交给 transformer。
4. **不要试图「一份 jar 双端」。** MTR 自己都没这么做：它的两个 jar 是两次独立编译的产物。

---

## 6. 未验证 / 风险

- **无法区分「平台差异」与「版本差异」的少数字段**：本次只有一个 Forge 版本（3.6.6.1）和一个 Fabric 版本（3.6.4.2），没有交叉样本。判定依据是「是否引用 `net/minecraftforge`」——24 个纯业务类（webdashboard/tunnel 等）均不引用，故归为版本增量；但严格说这仍是归纳而非对照实验。
- **Forge 3.6.6.1 的运行时行为未验证**：本次只做静态取证，未在 Forge 环境启动、未跑 LTED jar。
- **`MoreRenderLayers` 的缓存语义变化**（`getLight` 从 `Map<String,MC>` 改 `Map<MC,MC[]>`）暗示光照渲染行为可能调整过；若 LTED 依赖 `MoreRenderLayers.getLight` 的具体返回（2 处引用），建议在 Forge 上实测一次观感。
- LTED 尚未有任何 Forge/Architectury 构建目标（`v1.20.1/gradle.properties` 只有 `yarn_mappings=1.20.1+build.10`），因此「Forge 端能不能编译通过」目前**没有证据**，需另立任务。

---

## 附：复现命令

```bash
SCRATCH="$LOCALAPPDATA/Temp/mtrdiff"
F="D:/MC-MTR-JS/mtr-headlight/libs/MTR-fabric-1.20.1-3.6.4.2.jar"
G="D:/Hermes Agent CN Desktop/data/hermes-home/attachments/MTR-forge-1.20.1-3.6.6.1.jar"
mkdir -p "$SCRATCH/allf" "$SCRATCH/allg"
unzip -qq -o "$F" -d "$SCRATCH/allf"; unzip -qq -o "$G" -d "$SCRATCH/allg"

# 类集合
unzip -l "$F" | awk '{print $4}' | grep '\.class$' | sort > "$SCRATCH/fab.classes"
unzip -l "$G" | awk '{print $4}' | grep '\.class$' | sort > "$SCRATCH/forge.classes"
comm -12 "$SCRATCH/fab.classes" "$SCRATCH/forge.classes" | grep '^mtr/' | wc -l   # 1790

# 命名空间
grep -rl 'net/minecraft/class_'  "$SCRATCH/allf/mtr" --include='*.class' | wc -l   # 471
grep -rl 'net/minecraft/class_'  "$SCRATCH/allg/mtr" --include='*.class' | wc -l   # 0
grep -rl 'm_[0-9]\{3,\}_'        "$SCRATCH/allg/mtr" --include='*.class' | wc -l   # 322

# 字节同一性
while read f; do cmp -s "$SCRATCH/allf/$f" "$SCRATCH/allg/$f" && echo same; done \
  < <(comm -12 "$SCRATCH/fab.classes" "$SCRATCH/forge.classes" | grep '^mtr/') | wc -l  # 1308

# 签名
JAVAP="C:/Program Files/Java/jdk-17/bin/javap"
"$JAVAP" -p -cp "$SCRATCH/allf" mtr.data.Train > /tmp/f.txt
"$JAVAP" -p -cp "$SCRATCH/allg" mtr.data.Train > /tmp/g.txt
diff /tmp/f.txt /tmp/g.txt
```
