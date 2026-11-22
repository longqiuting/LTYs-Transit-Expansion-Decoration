# MTR3 / MTR4 双版本适配可行性（LTED 装饰模组）

> 调研日期：2026-11-22 · 仓库 `D:\MC-MTR-JS\mtr-headlight` · 只做调研，未改任何代码
> 全部结论均有 `Modrinth API` / `unzip -l` / `javap -p` 实测支撑，见文末「证据附录」。

---

## 0. 结论摘要（TL;DR）

1. **MTR4 存在且是上游主线**：Modrinth 上 `4.0.5`（2026-06-14）覆盖 **1.16.5 / 1.17.1 / 1.18.2 / 1.19.2 / 1.19.4 / 1.20.1 / 1.20.4**，加载器 **fabric + forge**；`4.1.0-beta.2` 覆盖 1.21.1 / 1.21.4，加载器 **fabric + neoforge**。稳定版 `4.0.0` 于 2025-07-31 发布，最早的 4.x 构建 `4.0.0-beta-8` 可追到 2024-06-20。
2. **本仓库 libs 里的「MTR 3.6.4.2」不是上游**：其 `fabric.mod.json` 写着 `"name": "Yomi's Minecraft Transit Railway"` / `"license": "All Rights Reserved"` / sources `yomi-china/Yomis-Minecraft-Transit-Railway`。官方 Modrinth 的 3.x 线**止于 3.2.2-hotfix-2（2023-10-08）**，没有 3.6.x。即：上游已全面转向 4.x，「MTR3」实际指的是 Yomi 中文分支。
3. **MTR4 是一次 API 重写，不是小版本升级**：包根 `mtr.*` → `org.mtr.{core,mod,mapping}`；方块/BE/BER 全部改成 **holder 包装类型**（`org.mtr.mapping.holder.*`）并叠加 `xxx2()` 方法名；`Train/TrainClient` 整类消失换成 `Vehicle/VehicleExtension`；`IPIDS`、`IPIDSRenderChild`、`ClientData` 的静态集合**全部不存在**。
4. **可行，但只能做「编译期双后端」**：一套 jar 靠反射兼容 MTR3/MTR4 **不可行**（核心是 20 个类要 `extends` 两边不同的基类，反射解决不了继承）。推荐 **common（无 MTR 依赖）+ mtr3 后端 + mtr4 后端** 的三模块架构。
5. **MTR4 上必须降级/重写的功能**：PIDS 厦门屏与 PSD-LCD 的自绘渲染链、全部 10 个 MTR mixin、数据绑定层（`IPIDS$TileEntityPIDS` 类名硬编码）。**能原样带走的功能**：时钟（3671 行，0 处 MTR import）、扶手/装饰小物件/rawmodel/scatter、纯 vanilla 装饰方块。

---

## 1. 版本矩阵（Modrinth API 实测）

### 1.1 MTR4 发布线

| 版本 | 首次发布 | MC 版本 | 加载器 | 备注 |
|---|---|---|---|---|
| `4.0.0-beta-8` | 2024-06-20 | 1.16.5 / 1.17.1 / 1.18.2 / 1.19.2 / 1.19.4 / 1.20.1 / 1.20.4 | fabric, forge | 最早的 4.x 公开构建 |
| `4.0.0` | 2025-07-31 | 同上 7 个 | fabric, forge | 首个稳定版 |
| `4.0.1` | 2025-08-24 | 同上 7 个 | fabric, forge | |
| `4.0.2-hotfix-1` | 2025-10-18 | 同上 7 个 | fabric, forge | |
| `4.0.3` | 2026-01-29 | 同上 7 个 | fabric, forge | |
| `4.0.4` | 2026-04-12 | 同上 7 个 | fabric, forge | |
| **`4.0.5`** | **2026-06-14** | 同上 7 个 | **fabric, forge** | **当前 4.0.x 最新** |
| `4.1.0-beta.1` | 2026-06-04 | 1.21.1 / 1.21.4 | fabric, neoforge | |
| `4.1.0-beta.2` | 2026-07-04 | 1.21.1 / 1.21.4 | fabric, neoforge | 换 NeoForge，1.20.1 未跟进 |

### 1.2 MTR3 发布线（官方 Modrinth）

| 版本 | 日期 | MC | 加载器 |
|---|---|---|---|
| `1.20.1-3.2.2-hotfix-2` | 2023-10-08 | 1.20 / 1.20.1 | forge |
| `3.2.2-hotfix-1` 系列 | 2023-06-09 | 1.18.2 / 1.19.2 / 1.19.3 / 1.19.4 / 1.20.1 | fabric + forge |
| `3.2.2` 及更早 | 2022–2023 | 1.16.2–1.20.1 | fabric + forge |

> **3.6.x 在 Modrinth 上不存在**（全库 496 个 version 里 `/3\.6/` 命中 0）。本仓库 `libs/MTR-fabric-1.20.1-3.6.4.2.jar` 的 `fabric.mod.json` 为 `"name": "Yomi's Minecraft Transit Railway"`、`"license": "All Rights Reserved"`、`sources: yomi-china/Yomis-Minecraft-Transit-Railway` —— 是 Yomi_307 的中文分支（仓库 `NOTICE.md` L22 也记录了这件事）。

### 1.3 对本项目的直接含义

| MC | 项目现有模块 | MTR3（Yomi fork） | MTR4 fabric | 双后端同盘共存 |
|---|---|---|---|---|
| 1.18.2 | `v1.18.2` | 3.2.2-hotfix-1 | 4.0.5 ✓ | ✓ |
| 1.19.2 | `v1.19.2` | 3.2.2-hotfix-1 | 4.0.5 ✓ | ✓ |
| 1.19.4 | `v1.19.4` | 3.2.2-hotfix-1 | 4.0.5 ✓ | ✓ |
| 1.20.1 | `v1.20.1` | 3.6.4.2（Yomi） | 4.0.5 ✓ | ✓ |
| 1.20.4 | — | ✗ | 4.0.5 ✓ | 只有 MTR4 |
| 1.21.x | — | ✗ | 4.1.0-beta.2（fabric/neoforge） | 只有 MTR4 |

**同一 MC 版本上两代 MTR 都有构建**，所以「同盘跑双后端」不是理论假设，是实际可分发形态（`fabric.mod.json` 的 `depends.mtr` 用区间即可）。

---

## 2. MTR3 ↔ MTR4 API 差异（LTED 使用面）

### 2.1 整体结构

| 项 | MTR3 3.6.4.2 | MTR4 4.0.5 | 判定 |
|---|---|---|---|
| 类总数 | 2172 | **5558** | — |
| 包根 | `mtr.*` | `org.mtr.*` | **全量改名** |
| 包结构 | `mtr.{block,client,data,render,screen,mappings,packet,model,path,item,entity,…}` | `org.mtr.core`(297) / `org.mtr.mod`(585) / `org.mtr.mapping`(356) / `org.mtr.libraries`(4514，shaded 依赖) / `org.mtr.legacy`(30) | 按「数据核心 / 游戏层 / 加载器抽象」三分 |
| 跨加载器 | 无统一抽象；`mtr.mappings` 是薄封装 | `org.mtr.mapping.{holder,mapper,registry,tool}` 自建 holder 层 + 自带 `mtr.accesswidener` | 这是它能同时出 fabric+forge 的原因 |
| 依赖 fabric-api | `depends: {fabric, minecraft}`（未列 fabric-api） | `depends: {fabric, minecraft}`（未列 fabric-api） | 都不硬依赖 |
| 上游维护 | 官方 3.x 已停更（3.2.2，2023） | 4.0.5，2026-06 仍在更新 | MTR4 是唯一活跃线 |

### 2.2 方块基类（javap 实测签名）

| 用途 | MTR3 | MTR4 | 同名同签名？ |
|---|---|---|---|
| 屏蔽门顶板 | `mtr.block.BlockPSDTop`<br>`extends mtr.mappings.BlockDirectionalMapper`<br>重写 **vanilla** 方法：`method_9534(BlockState, World, BlockPos, PlayerEntity, Hand, BlockHitResult)` | `org.mtr.mod.block.BlockPSDTop`<br>`extends org.mtr.mapping.mapper.BlockExtension`<br>重写 **holder** 方法：`onUse2(holder.BlockState, holder.World, …)` | ✗ 基类不同、方法名不同、参数类型不同 |
| 屏蔽门门体 | `mtr.block.BlockPSDAPGDoorBase`（abstract）<br>内类 `TileEntityPSDAPGDoorBase`<br>属性 `END/UNLOCKED/TEMP`（`class_2746`） | `org.mtr.mod.block.BlockPSDAPGDoorBase`<br>内类 **`BlockEntityBase`**（原名消失）<br>属性仅 `END/UNLOCKED`（`holder.BooleanProperty`），`TEMP` 不在方块类及其父类上 | ✗ |
| PIDS 横屏基类 | `mtr.block.BlockPIDSBaseHorizontal`<br>`implements … IPIDS`，**无参构造** | `org.mtr.mod.block.BlockPIDSHorizontalBase`<br>`extends BlockPIDSBase`，**构造 `(int maxArrivals)`**，另需 `canStoreData` / `getBlockPosWithData` | ✗ 改名 + 构造签名变 |
| 竖屏 PIDS | `BlockPIDSBaseVertical` | `BlockPIDSVerticalBase` | ✗ 改名 |
| 站台 | `mtr.block.BlockPlatform(Settings, boolean)`<br>静态 `DOOR_TYPE` / `SIDE` 在**类上** | `org.mtr.mod.block.BlockPlatform(holder.BlockSettings, boolean)`<br>静态属性搬进 **`PlatformHelper` 接口** | ✗ 构造与常量位置都变 |
| 属性注册 | `protected void method_9515(StateManager.Builder<Block,BlockState>)` | `public void addBlockProperties(List<HolderBase<?>>)` | ✗ |
| 工具接口 | `mtr.block.IBlock`：`getVoxelShapeByDirection(…, class_2350)`、`getStatePropertySafe(BlockView, BlockPos, Property<T>)`、`checkHoldingBrush(World, PlayerEntity, Runnable…)` | `org.mtr.mod.block.IBlock`：同名方法，但全部换 holder 类型 + `EnumProperty`/`BooleanProperty` 重载爆炸（1 个方法变 4 个） | ~ 名字在、签名全换 |
| 玻璃/闸机/站名牌 | `BlockAPGGlass` `BlockTicketBarrier` `BlockStationNameEntrance` `BlockPoleCheckBase` | 同名存在于 `org.mtr.mod.block` | ~ 名在、基类与签名换 |

**结论**：类名基本保留，但**没有一个可直接编译**。所有 `@Override` 的目标方法都从「vanilla 方法」变成「`xxx2()` holder 方法」。

### 2.3 数据入口

| 用途 | MTR3 | MTR4 | 判定 |
|---|---|---|---|
| 客户端数据总入口 | `mtr.client.ClientData` — **纯静态**：`STATIONS` `PLATFORMS` `SIDINGS` `ROUTES` `DEPOTS` `LIFTS` `SIGNAL_BLOCKS` `OCCUPIED_RAILS` `RAILS`，另有 `TRAINS` map | `org.mtr.core.data.ClientData` — **实例类**，`extends Data`，只有 `simplifiedRoutes`；真正的客户端单例是 `org.mtr.mod.client.MinecraftClientData`（`getInstance()`，字段 `vehicles` = `ObjectArraySet<VehicleExtension>`） | ✗ 静态 API 消失，改单例 + fastutil 集合 |
| 列车 | `mtr.data.Train`（abstract，`trainCars`/`spacing`/`width`/`doorValue`…）/ `mtr.data.TrainClient`（+`trainRenderer`、`SpeedCallback`） | **都不存在**。`org.mtr.core.data.Vehicle`（`extends VehicleSchema`，schema 生成）+ `org.mtr.core.data.VehicleCar` / `VehiclePosition` + `org.mtr.mod.data.VehicleExtension` / `PersistentVehicleData` | ✗ 整层重写 |
| 线路/车站/站台/机务段 | `mtr.data.Route` / `Station` / `Platform` / `Siding` / `Depot`（`extends NameColorDataBase`） | `org.mtr.core.data.*` 同名，但由 `org.mtr.core.generated.data.*Schema` 生成；客户端另走 `SimplifiedRoute` / `RoutePlatformData` | ✗ 同名不同物 |
| 线路图 | `mtr.client.RouteMapGenerator` | `org.mtr.mod.client.RouteMapGenerator` | ~ 角色保留，需重验签名 |
| **PIDS 契约** | `mtr.data.IPIDS` + **`IPIDS$TileEntityPIDS`**（abstract，`setData`/`getPlatformIds`/`getDisplayPage`/`messages`）+ **`IPIDSRenderChild`**（`getDisplayPage()`） | **`IPIDS` 与 `IPIDSRenderChild` 全库不存在**（`unzip -l | grep -i IPID` 零命中）。替代：`org.mtr.mod.block.BlockPIDSBase$BlockEntityBase`（`setData` / `getPlatformIds` / `getDisplayPage` / `getMessage(int)` / `getHideArrival(int)` / `abstract showArrivalNumber()`），渲染靠 `org.mtr.mod.render.RenderPIDS<T extends BlockEntityBase>` 或自定义 `BlockEntityRenderer` | ✗ **契约被拆解重造** |
| 票务 | `mtr.data.TicketSystem` + `BALANCE_OBJECTIVE` / `addObjectivesIfMissing` | `org.mtr.mod.data.TicketSystem` | ~ 同名，需重验 |

### 2.4 渲染接口

| 用途 | MTR3 | MTR4 | 判定 |
|---|---|---|---|
| 方块实体基类 | `mtr.mappings.BlockEntityMapper extends class_2586`<br>`read/writeCompoundTag(class_2487)` | `org.mtr.mapping.mapper.BlockEntityExtension extends holder.BlockEntityAbstractMapping`<br>`read/writeCompoundTag(holder.CompoundTag)`，新增 `blockEntityTick()` / `markDirty2()` / `getRenderDistance2()` | ✗ 基类与 NBT 类型都换 |
| BER 基类 | `mtr.mappings.BlockEntityRendererMapper`（**自身无方法**）→ 子类直接重写 vanilla `render(T, float, MatrixStack, VertexConsumerProvider, int, int)` | `org.mtr.mapping.mapper.BlockEntityRenderer<T>` → 必须实现 `render(T, float, **GraphicsHolder**, int, int)`（`MatrixStack`+`VertexConsumerProvider` 由 `GraphicsHolder` 接管） | ✗ **抽象方法签名不同**，全部 BER 要按 GraphicsHolder 重写 |
| 渲染层 | `MoreRenderLayers.getLight/getInterior/getExterior(Identifier, boolean)` 返回 `net.minecraft.class_1921` | 同名方法返回 `holder.RenderLayer`，参数 `holder.Identifier`；新增 `getLight2` / `removeFromCache` | ~ 名字同、类型换 |
| 屏蔽门/顶板渲染器 | `mtr.render.RenderPSDTop extends RenderRouteBase<BlockPSDTop$TileEntityPSDTop>`，构造 `(class_824)` | `org.mtr.mod.render.RenderPSDTop extends RenderRouteBase<BlockPSDTop$BlockEntity>`，构造 `(BlockEntityRenderer$Argument)`；`getRenderType(holder.World, holder.BlockPos, holder.BlockState)` | ~ 结构同、泛型与参数换 |
| 门/玻璃/站名牌 | `RenderPSDAPGDoor` / `RenderAPGGlass` / `RenderStationNameTiled` / `RenderRouteBase` / `StoredMatrixTransformations` | 同名在 `org.mtr.mod.render` | ~ |
| 屏幕 UI | `mtr.mappings.ScreenMapper extends class_437` | `org.mtr.mapping.mapper.ScreenExtension` | ✗ 改名 |
| 控件 | `mtr.screen.WidgetBetterCheckbox` / `WidgetBetterTextField` / `PIDSConfigScreen` | 前两个**不存在**（仅 `WidgetShorterSlider` / `WidgetColorSelector`）；`PIDSConfigScreen` 在 | ✗ |
| 主类 | `mtr.MTRClient` | `org.mtr.init.MTRClient` | ✗ 换包 |

### 2.5 注册 / 网络

| 项 | MTR3 | MTR4 |
|---|---|---|
| 方块注册 | MTR **不提供** addon 注册 API；本项目直接走 vanilla/Fabric `net.minecraft.registry.Registry` + `Registries.BLOCK`（见 `decor/DecorBlocks.java`） | `org.mtr.mapping.registry.Registry`：`registerBlock` / `registerBlockWithBlockItem` / `registerBlockEntityType` / `registerItem` / `registerSoundEvent` / `registerParticleType` / `registerPacket(Class<T>, Function<PacketBufferReceiver,T>)` / `sendPacketToClient` / `createCreativeModeTabHolder`，配 `EventRegistry` |
| 网络包 | `mtr.packet.PacketTrainDataGuiClient/Server` + `IPacket` | `org.mtr.mod.packet.*`（45 个）→ `PacketUpdatePIDSConfig`、`PacketOpenPIDSConfigScreen`、`PacketRequestData` … 全改名 |
| 判断 | 注册靠 loader，MTR 只管行为 | **MTR4 自带一套公共注册 API**（21 个 public 方法），但输入输出是 holder 类型 |

> 这是 MTR4 唯一「比 MTR3 更好」的地方：`org.mtr.mapping.registry.Registry` 是公共可调用的注册面（是否为官方承诺的 addon 契约，未在其 fabric.mod.json / 文档中确认）。代价是它要求你的方块就是 `holder.Block` 包装对象，无法与 vanilla `Block` 子类混用。

---

## 3. 耦合面量化（本仓库实测）

| 指标 | 数值 | 口径 |
|---|---|---|
| v1.20.1 + common 总规模 | 324 文件 / **91,746 行** | `find … -name *.java \| xargs cat \| wc -l` |
| 触碰 `mtr.*` 的文件 | **96 文件 / 39,285 行（43%）** | `grep -rl "^import mtr\."` |
| 继承/实现 MTR 类型的类 | **20 个**（见下表） | AST-lite 扫描 import + `extends/implements` |
| MTR mixin | **10 个**（`mtrlte.mtr.mixins.json`，`required:false` + `defaultRequire:0`） | 配置文件 |
| 完全无 MTR 依赖的子系统 | 时钟 3,671 行、扶手 552 行、rawmodel 1,454 行、net 209 行、client 40 行 = **5,926 行** | 0 处 `import mtr.` |

**20 个继承点**（每个都是 MTR4 上必须重写的单元）：

| 次数 | MTR3 超类 | 本项目类 |
|---|---|---|
| 4 | `mtr.mappings.BlockEntityRendererMapper` | `DepOnRoofRenderer` `DepOnWallRenderer` `PidsXiamenRenderer` `PsdLcdRenderer` |
| 2 | `mtr.block.BlockPlatform` | `MarblePlatformBlock` `RccPlatformBlock` |
| 2 | `mtr.mappings.BlockEntityMapper` | `DepOnRoofBlockEntity` `DepOnWallBlockEntity` |
| 2 | `mtr.mappings.ScreenMapper` | `PidsXiamenAdsScreen` `PidsXiamenConfigScreen` |
| 2 | `mtr.render.RenderPSDAPGDoor` | `RenderPsdApgDoor` `RenderPsdDoor` |
| 1 | `mtr.block.BlockPSDAPGDoorBase` | `FangsuPsdDoorBlock`（Fangsu 兼容，MTR4 上直接放弃） |
| 1 | `mtr.block.BlockPIDSBaseHorizontal` | `PidsXiamenBlock` |
| 1 | `mtr.block.BlockPoleCheckBase` | `PidsXiamenPoleBlock` |
| 1 | `mtr.block.BlockStationNameEntrance` | `WallSignBlock` |
| 1 | `mtr.block.BlockTicketBarrier` | `XiamenTicketGate` |
| 1 | `mtr.render.RenderStationNameTiled` | `WallSignRenderer` |
| 1 | `mtr.render.RenderAPGGlass` | `XiamenApgGlassRenderer` |
| 1 | `mtr.render.RenderPSDTop` | `XiamenPsdTopRenderer` |

**20 个继承点里有 15 个落在 `xiamen/`（136 文件 / 31,352 行 / 103 处 MTR import）** —— 厦门风格子系统就是 MTR4 适配的主战场。

---

## 4. 方案对比

### a) 单 jar + 运行时反射 / 软依赖兼容层

**不可行（作为主方案）。**

- 致命点：LTED 的 20 个类需要 `extends mtr.block.BlockPSDTop` **或** `extends org.mtr.mod.block.BlockPSDTop`。Java 的继承在 class 文件里是写死的常量池项，反射无法在运行时改变父类。
- 若强行单 jar，只能：① 运行时用 ASM 生成两套字节码（等价于在 jar 里塞两套实现 + 自建类加载器，工作量 ≥ 方案 b，风险远高）；② 把每个继承点改成「组合 + 代理 vanilla Block」——那就放弃了 MTR 提供的全部行为（站台识别、门体状态机），功能大幅缩水。
- 反射只在一处成立：**数据读取**（`TrainClient` → `VehicleExtension`）可以做一层 `MtrDataPort` 反射门面，避免 76 个消费文件各自写分支。这可以作为方案 b 的**内部配件**，不能作为整体方案。

**工作量**：N/A（无法收敛）· **风险**：极高（ASM/类加载器、混淆差异、调试不可读） · **结论**：❌

### b) 编译期多后端（common + mtr3 + mtr4）

**唯一现实可行。**

```
common/            ← 无 MTR 依赖：时钟、扶手、rawmodel、scatter、碰撞数学、网络封装
                     + 定义 MtrPort 接口（站点/列车/线路图/PIDS 数据）
mtr3/  (v1.20.1-mtr3 …) ← 现有代码 + MtrPort 的 MTR3 实现
mtr4/  (v1.20.1-mtr4 …) ← MtrPort 的 MTR4 实现（holder API）+ 20 个继承点重写
```

- 现有仓库**已经是这个形状**（`common/` + `v1.18.2 v1.19.2 v1.19.4 v1.20.1` 四模块共享 `common`），只是四模块都对着 MTR3。加一个维度（MTR 世代）而不是从零搭架构。
- 分发：`mtr4` 变体的 `fabric.mod.json` 写 `depends: { "mtr": ">=4.0.5 <5" }`，`mtr3` 变体写 `"mtr": ">=3.2.2 <4"`。Fabric loader 只会装载匹配的那个变体（不同 mod 版本共存时需 `provides`/不同 mod id 或依赖区间，二者不会同时在同一个实例里）。
- 关键前置：把 `common` 里已有的 5,926 行（时钟/扶手/rawmodel/net）真正摘干净——目前 `clock`、`railing`、`rawmodel`、`net`、`client` 已经是 **0 处 MTR import**，只需从 `v1.20.1/src` 移到 `common/src`。

**工作量（估算，1 人）**：
| 阶段 | 内容 | 估算 |
|---|---|---|
| 1 | `common` 抽离 + `MtrPort` 接口定义 | 1–2 周 |
| 2 | mtr4 骨架：`Registry` 注册、方块基类移植（PSD 顶板/门/站台/闸机/站名牌） | 2–3 周 |
| 3 | mtr4 数据层适配（`MinecraftClientData.getInstance()`、`VehicleExtension`、`SimplifiedRoute`）+ 76 个消费文件改类型 | 2–3 周 |
| 4 | PIDS 厦门屏 + PSD-LCD 渲染链重写（`BlockEntityRenderer` + `GraphicsHolder`） | 3–4 周 |
| 5 | 10 个 MTR mixin 重新定位（或标记 mtr4 不提供） | 1–2 周 |
| **合计** | 达到「MTR3 功能对等」 | **9–14 周** |
| 仅做装饰（阶段 1–2 + 时钟/物件） | 无 PIDS/LCD/PSD 门 | **3–4 周** |

**风险**：中。主要是 PIDS/LCD 的渲染重写（holder `GraphicsHolder` 立即模式与现在的 `MatrixStack`+`VertexConsumer` 写法差异大）；MTR4 内部仍在演进（4.0.5 → 4.1.0 换到 NeoForge）。

### c) 只用稳定子集（放弃继承，纯 vanilla 方块 + 降级）

**可作「先落地」的过渡，但不能满足现有功能。**

- 双版真正同名同签名的东西只有：**方块 ID、资源路径（models/textures/lang/blockstates）、vanilla Block/BlockEntity/BER 注册流程**。类型层面交集 ≈ 0（一边 vanilla 类型，一边 holder 包装）。
- 落地形态：LTED 的装饰方块全部做成**纯 vanilla 方块**（`decor/DecorBlocks` 里的大理石地板已经是），不继承任何 MTR 类；需要 MTR 行为的地方（站台识别、PSD 门联动、PIDS 数据）在 MTR 侧只做「存在就注册」的可选联动。
- 代价：站台方块失去 `side`/`door_type` 自动推导（本项目 `MarblePlatformBlock` 依赖 `BlockPlatform` 的父类逻辑）；PSD/APG 门、PIDS、LCD 全部降级为静态装饰。

**工作量**：2–3 周 · **风险**：低 · **结论**：⚠️ 只能作为 MTR4 的首发最小可用集，不是终态。

### 推荐

**b 为主、c 为第一步，a 只作为 b 内部的「数据反射门面」局部手段。**

具体路径：先把 `common` 里无 MTR 依赖的 5,926 行抽出来 + 定义 `MtrPort`（1–2 周），然后发布一个 **MTR4 装饰版**（方案 c 形态，覆盖时钟/扶手/装饰物件/大理石站台）验证分发链路，再按阶段 2–5 补齐屏蔽门与 PIDS。这样 MTR4 用户先有可用版本，同时不阻塞 MTR3 主线的维护。

---

## 5. 功能降级清单（MTR4 上）

| 功能 | MTR3 | MTR4 | 判定 |
|---|---|---|---|
| 装饰方块（大理石地板） | 纯 vanilla | 纯 vanilla | ✅ 0 改动 |
| 站台方块（大理石/RCC，靠 `BlockPlatform` 自动 `side`+`door_type`） | `extends mtr.block.BlockPlatform` | `extends org.mtr.mod.block.BlockPlatform`，常量搬进 `PlatformHelper` | ✅ 可保留，需重写约 80 行/类 |
| 时钟（3,671 行） | 0 处 MTR import | 同 | ✅ 原样移植 |
| 扶手 / rawmodel / scatter / 装饰小物件 | 0–3 处 MTR import | 同 | ✅ 基本原样 |
| 信号灯 / 碰撞 / 喷雾 | 依赖 `Train`/`Rail` 位置 | `Vehicle` + `SimplifiedRoute` | ⚠️ 数据层适配后可保留 |
| PSD/APG 屏蔽门（门体 + 自绘 renderer） | `BlockPSDAPGDoorBase` + `RenderPSDAPGDoor` + `TileEntityPSDAPGDoorBase` | 同名类但 `TileEntityPSDAPGDoorBase` → `BlockEntityBase`，BER 改 `GraphicsHolder` | ⚠️ 重写，功能可保留 |
| **PIDS 厦门屏**（`PidsXiamenRenderer` 1,270 行 + 37 个配套文件） | `BlockPIDSBaseHorizontal` + `IPIDS$TileEntityPIDS` + `IPIDSRenderChild` | `IPIDS`/`IPIDSRenderChild` **不存在**；改 `BlockPIDSHorizontalBase` + `BlockPIDSBase$BlockEntityBase` + `RenderPIDS<T>` 或自建 `BlockEntityRenderer` | 🔻 **必须重写渲染与 BE 基类**；数据（platformIds/messages/displayPage）仍在 |
| **PSD-LCD**（`PsdLcdRenderer` 898 行） | 同上 | 同上 | 🔻 同上 |
| 数据绑定层（`bind/`：`MtrTiles` 硬编码 `"mtr.data.IPIDS$TileEntityPIDS"`、`PidsLinkStore`） | 按类名 + `instanceof` 双路判定 | 类名变更 | 🔻 需加 MTR4 分支（常量级改动，但要重测） |
| 10 个 MTR mixin（`RouteMapArtMixin`、`RenderPIDSStationMixin`、`RenderSignalBaseMixin`、`TrainClientRegistryMixin`、`StationNameEntranceLogoMixin`…） | 目标 `mtr.*` | 目标 `org.mtr.mod.*`，字段/方法名全变 | 🔻 **每个都要重新找注入点**；架构上已有 `required:false` + `defaultRequire:0`，缺目标会静默跳过 |
| Fangsu（方速）PSD 门兼容 | `mtr.block.BlockPSDAPGDoorBase` | Fangsu 自身是否跟 MTR4 未知 | 🔻 MTR4 上建议直接不提供 |
| 创造页签 / 注册 | vanilla + fabric-api `ItemGroupEvents` | 可用 `org.mtr.mapping.registry.Registry.createCreativeModeTabHolder`，也可继续用 fabric-api | ✅ |

---

## 6. 未验证事项（明确标注）

| 事项 | 状态 | 原因 |
|---|---|---|
| MTR4 `4.1.0-beta.2`(1.21.1) 与 `4.0.5`(1.20.1) 的 `org.mtr.mapping` 签名差异 | **未验证** | 111.6 MB jar 从 `cdn.modrinth.com` 多次下载均被截断（最远 5.8 MB / 111 MB），Range 请求返回 501。**推断**（非证据）：`org.mtr.mapping` 由 `org.mtr.mapping.mixin.CreateAccessWidener` 等工具按 MC 版本生成，holder 包装会随 MC 版本暴露不同方法，故 1.21 上大概率仍有签名漂移；需拿到 jar 才能定论。 |
| `4.0.5` 在 1.18.2 / 1.19.2 / 1.19.4 上与 1.20.1 的 API 差异 | **未验证** | 只需一份 jar 即可，但同为 84.8 MB，本轮只取了 1.20.1。 |
| MTR4 forge 变体的 API 是否与 fabric 完全一致 | **未验证** | 只取 fabric jar。按其自建 mapping 层的设计意图应当一致。 |
| MTR4 是否对 addon 提供官方「mixin 白名单 / AW」 | 已知自带 `mtr.accesswidener` + `minecraft-mappings.accesswidener`，未查文档 | — |
| Yomi fork 3.6.4.2 与官方 3.2.2 的 API 差异 | 未验证 | 但 3.2.2 与 3.6.4.2 的方块类名集合一致（`BlockPIDS{1,2,3,4}`、`BlockPSD*` 全同），项目现有四模块已能同时编译，说明差异在项目可承受范围内。 |

---

## 7. 证据附录

```bash
# 1) MTR4 是否存在 / 加载器 / MC 版本
curl -s "https://api.modrinth.com/v2/project/minecraft-transit-railway/version"
#   → 496 个 version；4.0.5 覆盖 1.16.5…1.20.4 [fabric,forge]；
#     4.1.0-beta.2 覆盖 1.21.1/1.21.4 [fabric,neoforge]；3.6.x 命中 0

# 2) 取 MTR4 jar（1.20.1 fabric, 84,780,475 bytes）
curl -L -o MTR-4.0.5-1.20.1.jar \
  "https://cdn.modrinth.com/data/XKPAmI6u/versions/5cxaj3g8/MTR-fabric-4.0.5%2B1.20.1.jar"

# 3) 包根搬迁
unzip -l MTR-4.0.5-1.20.1.jar | grep -cE "\.class$"        # 5558
unzip -l MTR-4.0.5-1.20.1.jar | grep -oE " org/mtr/[a-z]+/" # libraries/mod/mapping/core/legacy/init/mixin

# 4) 签名对比（MTR3 生产 jar vs MTR4 生产 jar，同为 1.20.1，intermediary 名一致）
javap -p -classpath <mtr3> mtr.block.BlockPSDTop
javap -p -classpath <mtr4> org.mtr.mod.block.BlockPSDTop
#   → method_9534(BlockState…)  vs  onUse2(holder.BlockState…)

# 5) IPIDS 契约消失
unzip -l MTR-4.0.5-1.20.1.jar | grep -i IPID   # 无输出

# 6) 本项目耦合面
grep -rl "^import mtr\." --include=*.java common v1.20.1/src | wc -l   # 96
```

原始数据留档：`%LOCALAPPDATA%\Temp\mtr_versions.json`（Modrinth 全量 version JSON）、
`%LOCALAPPDATA%\Temp\mtr4\MTR-4.0.5-1.20.1.jar`、`%LOCALAPPDATA%\Temp\cmp\{mtr3,mtr4}\`（解包后的 class 树）。
