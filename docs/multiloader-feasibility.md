# Fabric / Forge 多加载器可行性调研（LTED）

> 调研对象：`D:\MC-MTR-JS\mtr-headlight`，四套源码树 `v1.18.2` / `v1.19.2` / `v1.19.4` / `v1.20.1` + 共享 `common/`。
> **本文只做取证与评估，未改动任何代码。** 所有结论都带文件/行号或外部 API 数据；估算值单独标注为「估算」。

---

## 0. 结论（先说不好听的）

1. **「把 LTED 移植到 Forge」不是一个加载器问题，是三个问题叠在一起**：加载器 API（68 个文件）、映射命名空间（262 个文件）、以及 20 个 mixin 类里 **12 个打在 MTR 内部**（字符串目标按 yarn 名/描述符写死）。
2. **MTR Forge 产物不缺**：`v1.20.1` 编译期与运行期都绑定 **MTR 3.6.4.2（YMTR，`libs/MTR-fabric-1.20.1-3.6.4.2.jar`）**，YMTR **同时发布 Forge 版本**（作者 2026-11-22 确认；本机未取到该 jar，无法做字节码比对）。因此 Forge 版 LTED 属于**工程成本问题，不是依赖缺口**——真正的成本在加载器抽象、yarn→official 映射、12 个打在 MTR 内部的 mixin 描述符，以及第三方 addon 的 `fabric.`/`forge.` 包名前缀探测。
3. **Forge 上真正能成立的组合只有两种**：a) 1.18.2/1.19.2/1.19.4 对齐 MTR Forge 3.2.2；b) 任意版本对齐 MTR 4 Forge —— 而 b 等于**重写**（MTR 4 取消了 Train 概念，§3.1）。
4. **推荐：只做 Fabric（方案 c）**。若确有 Forge 需求，正确做法是**另起一个范围小得多的 MTR 4-Forge 新模组**（光束 + PSD/APG 门 + 扶手），复用纯数学部分，而不是把 74,859 行搬过去。

---

## 1. 取证方法

| 手段 | 用途 |
|---|---|
| 源码检索（`grep`/`read_file`） | 统计 import、注册点、mixin 目标、事件挂载 |
| `unzip -l` + `javap -p`（JDK 17） | 比对 MTR 3.2.2 与 3.6.4.2 的类/方法签名；确认 ANTE 双端包结构 |
| Modrinth API `/project/{slug}/version` | 逐 MC 版本、逐 loader 统计真实发版（本项目环境无法直连 `cdn.modrinth.com` 下载 jar，故只用元数据 + 仓库内 jar） |
| 仓库内已有 jar 的元数据 | `ref/nte-jar/*__fabric.mod.json`、`*__META-INF_mods.toml`、`libs/` |

规模基线（实测）：

| 项 | 数值 |
|---|---|
| `v1.20.1` Java 文件 / 行 | 283 / 74,859 |
| `common` Java 文件 / 行 | 41 / 16,887 |
| 其它三套（含 common 复用） | ~62–63k 行；与 1.20.1 相比 131–171 个文件不同 |
| 引用 `net.minecraft.*` 的文件 | **262**（324 个文件里的 81%） |
| 引用 `mtr.*` 的文件 | 96 |
| 引用 `net.fabricmc.fabric.api` 的文件 | **68** |
| `@Mixin` 类 | 每版 20 个（四版一致） |
| 注入点 | `@Inject` 24 / `@Redirect` 5 / `@ModifyVariable` 4 / `@ModifyArgs` 3 / `@ModifyArg` 2 |

---

## 2. 加载器耦合清单（`v1.20.1`）

### 2.1 构建层

| 位置 | 内容 | Forge 侧对应 |
|---|---|---|
| `v1.20.1/build.gradle:2` | `id 'fabric-loom' version '1.11.8'` | ForgeGradle / NeoGradle |
| `build.gradle:44-47` | `minecraft` + `yarn` + `fabric-loader` + `fabric-api` | `net.minecraftforge:forge` + `official` 映射 |
| `build.gradle:29-34` | `loom.mixin.defaultRefmapName = "lte.refmap.json"` | 需另一份 Forge refmap（SRG/official） |
| `build.gradle:36-41` | sourceSets 把 `../common/src/main/java` 并入 main | 同（Architectury 下需挪到 common 子项目） |
| `build.gradle:58` | MTR 用 **named jar** 编译（`tools/remap-mtr-named.sh` 现场生成） | Forge 侧需要 SRG/official 名的 MTR jar，脚本不适用 |
| `build.gradle:81-85` | `-AMSG_MIXIN_SOFT_TARGET_NOT_FOUND=disabled`（方速/天津目标只在运行期存在） | 同（这是 javac 选项，非加载器） |
| `settings.gradle:3` | 插件仓库 `maven.fabricmc.net` | 需加 Forge maven |

> `tools/remap-mtr-named.sh:3-13` 明确写了：MTR 是 **Architectury dev jar，Loom 自己的 remap 会失败**（"unfixable conflicts"），所以只能自己用 tiny-remapper 转一份具名 jar。这条流水线在 Forge 侧要**整套重做**。

### 2.2 元数据 `src/main/resources/fabric.mod.json`

| 行 | 内容 | 说明 |
|---|---|---|
| 30-39 | 8 个 mixin 配置 | Mixin 库两端同源，但 `refmap` 和 `remap` 语义不同 |
| 40-44 | `main`: `LteServer`, `VehicleLightsJsonServer` | = `ModInitializer`（`server/LteServer.java:7`、`VehicleLightsJsonServer.java:4`） |
| 45-64 | `client`: **18 个** `ClientModInitializer` | 全部要换成 Forge 的 `@Mod` + `FMLClientSetupEvent`/`ClientTickEvent` 分派 |
| 65-67 | `modmenu`: `ModMenuIntegration` | Mod Menu 只有 Fabric/Quilt（§3） |
| 69-75 | `depends`: `fabricloader / minecraft / java / fabric-api / mtr` | Forge 用 `mods.toml`，且无 `fabric-api` 概念 |

### 2.3 Fabric API 模块（逐个，带出现次数与代表文件）

| Fabric 模块 | 用到的类 | 次数 | 代表位置 |
|---|---|---|---|
| `fabric-item-group-api-v1` | `ItemGroupEvents`, `FabricItemGroup` | 26 | `clock/ClockObjects.java:6-7`、`xiamen/XiamenObjects.java:10-11` |
| `fabric-networking-api-v1` | `ServerPlayNetworking`, `ClientPlayNetworking`, `PacketByteBufs`, `PlayerLookup`, `Server/ClientPlayConnectionEvents` | 33 | `net/ClientHandshake.java:6-9`、`clock/ClockGlobalNet.java:5-8` |
| `fabric-object-builder-api-v1` | `FabricBlockEntityTypeBuilder`(15), `FabricEntityTypeBuilder`(2) | 17 | `xiamen/charger/ChargerObjects.java:7-8` |
| `fabric-rendering-v1` | `BlockEntityRendererRegistry`(12), `WorldRenderContext`(5), `WorldRenderEvents`, `EntityRendererRegistry` | 19 | `BeamGlow.java:6`、`xiamen/XiamenClient.java:7` |
| `fabric-lifecycle-events-v1` | `ClientTickEvents`(6), `ClientLifecycleEvents`(2), `ServerTickEvents`(2), `ServerLifecycleEvents`(2) | 12 | `KeyHooks.java:3`、`server/LteServer.java:8-9` |
| `fabric-blockrenderlayer-v1` | `BlockRenderLayerMap` | 9 | `decor/DecorClient.java:5`、`xiamen/pids/PsdLcdClient.java:6` |
| `fabric-command-api-v2` | `CommandRegistrationCallback`, `ClientCommandManager`, `ClientCommandRegistrationCallback`, `FabricClientCommandSource` | 5 | `Commands.java:12-14`、`bind/LteBindCommand.java:5` |
| `fabric-resource-loader-v0` | `ResourceManagerHelper`, `SimpleResourceReloadListener` | 4 | `VehicleLightsJsonClient.java:5-6`、`xiamen/pids/PidsXiamenClient.java:12-13` |
| `fabric-particles-v1` | `FabricParticleTypes`, `ParticleFactoryRegistry` | 2 | `SprayType.java:3-4` |
| `fabric-events-interaction-v0` | `AttackBlockCallback`, `PlayerBlockBreakEvents` | 2 | `bind/BindUnbind.java:6-7` |
| `fabric-key-binding-api-v1` | `KeyBindingHelper` | 1 | `KeyHooks.java:4` |
| （`fabric-loader` 本体，非模块） | `FabricLoader`(14 处)、`EnvType`、`Environment`、`ModInitializer`/`ClientModInitializer` | 32 | `HeadlightConfig.java:1953`、`server/LteServer.java:59-60`、`xiamen/pids/PidsXiamenSettings.java:98` |

**汇总**：11 个 Fabric API 模块 + loader 本体；68 个文件；模块级替换在 Forge 侧的对应关系是 `ItemGroup`→`CreativeModeTab`（DeferredRegister）、`FabricBlockEntityTypeBuilder`→`BlockEntityType.Builder`、`BlockEntityRendererRegistry`→`EntityRenderersEvent.RegisterRenderers`、`BlockRenderLayerMap`→`ItemBlockRenderTypes.setRenderLayer`（`FMLClientSetupEvent`）、`ServerPlayNetworking`/`ClientPlayNetworking`→`SimpleChannel`、`*TickEvents`→`TickEvent.*`、`ResourceManagerHelper`→`AddReloadListenerEvent`、`CommandRegistrationCallback`→`RegisterCommandsEvent`（客户端命令 → `RegisterClientCommandsEvent`）、`KeyBindingHelper`→`RegisterKeyMappingsEvent`。

### 2.4 注册量（机械但量大）

| 项 | 数量 | 证据 |
|---|---|---|
| `Registry.register(...)` | **133** | `grep -c` 全量 |
| `FabricBlockEntityTypeBuilder.create` | 20 | 同上 |
| `FabricEntityTypeBuilder` | 2 | `xiamen/charger/ChargerObjects.java:8` |
| `FabricItemGroup` 页签 | 3 | `signal/LteCoreItems.java:13`、`decor/RccPlatforms.java:5`、`xiamen/eyecandy/EyecandyBlocks.java:20` |
| `ItemGroupEvents.modifyEntriesEvent` | 22 | 各 `*Objects.java` |
| 自定义网络通道（`mtrlte:*`） | **18** | `grep -oE 'new Identifier\("mtrlte", "[a-z_]+"\)'` |
| 发包/收包调用点 | 36 | `ServerPlayNetworking.send` / `registerGlobalReceiver` 等 |
| GUI `*Screen` 类 | 12 | `ls *Screen*.java` |
| 资源重载监听器 | 2 | `VehicleLightsJsonClient.java:66`、`PidsXiamenClient.java:108` |

### 2.5 Mixin（真正难搬的部分）

8 份配置（`src/main/resources/mtrlte.*.mixins.json`），全部 `refmap: lte.refmap.json`；`required: false` 的 4 份都挂了自定义 `IMixinConfigPlugin`（`mixin/mtr/MtrMixinPlugin.java`、`mixin/mtrserver/MtrServerMixinPlugin.java`、`mixin/FangsuMixinPlugin.java`）。

| 目标类别 | 数量 | 文件 | 移植难度 |
|---|---|---|---|
| **MTR 内部**（`remap = false`） | 12 | `mixin/mtr/*`(9) + `mixin/mtrserver/*`(3) | 中高：类名不变，但**方法描述符里的 MC 类型名在 Forge 侧是 official 名**，字符串常量全部要重写并逐条 `javap` 复核 |
| **天津地铁**（`remap = false`，软目标） | 1 | `mixin/mtr/RenderPidsTianjinStationMixin.java:39` | 中：类名 `ziyue.tjmetro.*` 若 Forge 侧有前缀变化则整条失效（当前无 Forge 探测分支） |
| **方速**（`remap = false`，软目标） | 2 | `mixin/fangsu/FangsuTrafficLightRenderMixin.java:42`、`FangsuTurnoutRenderMixin.java:57` | 中：目标 `com.fangsu.*`，方速 Forge 端包名需实测 |
| **原版渲染/注册表**（`remap = true`） | 5 | `BeamLightMixin`(WorldRenderer)、`WorldRendererRainMixin`、`MixinEntityMove`(Entity)、`SimpleRegistryFreezeMixin`(SimpleRegistry)、`GameRendererScatterMixin` | **高**：yarn 名在 Forge 侧不存在 |

原版目标的命名差异（Forge 1.20.1 = official/Mojang 名）：

| LTED 里的 yarn 名 | Forge 侧 |
|---|---|
| `WorldRenderer.getLightmapCoordinates` / `renderWeather` / `tickRainSplashing` | `LevelRenderer.getLightColor` / `renderSnowAndRain` / `tickRain` |
| `GameRenderer.renderWorld(FJ Lnet/minecraft/client/util/math/MatrixStack;)V` | `GameRenderer.renderLevel(... PoseStack ...)` |
| `SimpleRegistry.freeze` | `Registry.freeze` |
| `ChunkRendererRegion` | `RenderChunkRegion` |
| `MatrixStack` / `LightmapTextureManager` / `BlockRenderView` | `PoseStack` / `LightTexture` / `BlockAndTintGetter` |

> 其中 `BeamLightMixin` 是**整个模组的核心**（`common/.../BeamBlockLight.java:8-19` 说明：原版唯一把坐标变成光值的入口就是 `WorldRenderer#getLightmapCoordinates`，一个 `@Inject(at = RETURN)` 覆盖全部地形顶点）。这条 mixin 在 Forge 必须重新定位并重新验证 —— 它一旦不成立，车头灯光束就完全不存在。

### 2.6 其它 Fabric 专属点

| 位置 | 内容 |
|---|---|
| `common/.../ModMenuIntegration.java:3-4,8` | `ModMenuApi` + `ConfigScreenFactory`（Mod Menu 仅 Fabric/Quilt） |
| `v1.20.1/.../KeyHooks.java:3-4` | `KeyBindingHelper.registerKeyBinding` + `ClientTickEvents` |
| `common/.../net/LteChannel.java:29` | `Identifier("mtrlte","hello")` 作为通道名（Forge 用 `ResourceLocation` + `SimpleChannel` 的 `NetworkRegistry.newSimpleChannel`) |
| `common/.../mixin/HostProbe.java:11-16` | ANTE 探测写死 **`fabric.cn.zbx1425...`**；Forge 端实测是 `forge.cn.zbx1425...`（见 §3.3）——探测本身是加载器相关的 |
| `common/.../FangsuCompat.java:73`、`xiamen/machine/FangsuTicketUi.java:55` | `FabricLoader.isModLoaded("fangsu")` |
| `common/.../ShaderPackProbe.java:109,122` | `Class.forName("net.irisshaders.iris.api.v0.IrisApi")` / OptiFine（纯反射，两端都能用） |

---

## 3. 第三方依赖归属性

### 3.1 MTR（硬依赖）— **本报告的核心结论来源**

`fabric.mod.json:74` 只硬依赖 `mtr`；代码里 96 个文件 import `mtr.*`，16 个文件用 `mtr.mappings.*`（MTR 自己的跨加载器抽象层，这是唯一"能白拿"的部分）。

Modrinth 实测（`/project/minecraft-transit-railway/version`，按 MC 版本 × loader 汇总）：

| MC | Fabric 最新 | Forge 现状 |
|---|---|---|
| 1.18.2 | 4.0.5 | 3.0.0 → 3.2.2-hotfix-1，然后 **直接跳到 4.0.0-beta-8** → 4.0.5 |
| 1.19.2 | 4.0.5 | 3.1.0 → 3.2.2-hotfix-1，然后 4.0.0-beta-8 → 4.0.5 |
| 1.19.4 | 4.0.5 | 3.2.1 → 3.2.2-hotfix-1，然后 4.0.x |
| **1.20.1** | 4.0.5 | **`1.20.1-3.2.2-hotfix-2` / `FORGE-1.20-3.2.2-hotfix-1`，然后 4.0.0-beta-8 → 4.0.5** |

关键事实：
- ~~**MTR 3.6.x 完全没有 Forge 产物**~~ → **更正（2026-11-22）：YMTR 3.6.x 有 Forge 版**（作者确认）。此前提到的「Forge 只有 ≤3.2.2 / 4.0.x」只适用于上游 MTR，不适用于 YMTR。
- 并且 `libs/MTR-*.jar` 里 `mtr/mappings/` 只有 **`FabricRegistryUtilities.class`，没有 Forge 变体** —— 说明同一个 jar 不可能两端通用，MTR 自己是按加载器分别编译的。
- 字节码比对（`javap -p`）：`3.2.2` 与 `3.6.4.2` 的 `mtr.client.ClientCache#getResource`、`RouteMapGenerator#{generateRouteMap,generateColorStrip,generateDirectionArrow,drawResource}` **签名完全一致**；`TrainClient` 3.6 是 3.2.2 的**超集**（74 vs 63 个成员，无删除）。所以"降到 3.2.2 能否编译"的答案偏向"能"，但：
  - 仍需为 3.6 独有特性做降级（例：`v1.20.1/.../SidingInfo.java:10` 注释 "MTR 3.6.x only; older versions report 0"）；
  - 每个 mixin 目标都要在 3.2.2 上重新 `javap` 复核（3.2.2 的 `RouteMapGenerator` 里 lambda 编号已经不同）。
- MTR 4 是**概念级重写**：`ref/mtr4-research.txt` 摘录官方 wiki —— "In MTR 4, the concept of a Train no longer exists"，改为 carriage/consist。LTED 的 `TrainClientRegistryMixin`、`TrainSimulateCaptureMixin`(`TrainServer#simulateTrain`)、按列车取 `trainCars/spacing` 的整条链路（`collision/CarBand.java:195`、`LightFlare.java:404`、`RainSpray.java:386`、`sss/ScatterLights.java:212` …）全部要重写。

### 3.2 依赖可用性总表

| 模组 | LTED 用它做什么（证据） | 必需 | Fabric | Forge / NeoForge | 备注 |
|---|---|---|---|---|---|
| **MTR** | 核心：12 个 mixin + `mtr.mappings`(16 文件) + 96 文件 import | **中** | ✅ 3.2.2/3.6.4.2 | ✅ YMTR 3.6.x 有 Forge 版（未取证） | 无依赖缺口；成本在 loader/mixin 移植 |
| NTE (`mtr-nte`) | 1.18.2–1.19.4 轨道亮度 mixin，探测 `cn.zbx1425...`（`HostProbe.java:15-16`） | 可选 | ✅ 0.3.3（`libs/MTR-NTE-fabric-*`） | ✅ 0.5.x 有 Forge | Forge 端包名为 `forge.cn.zbx1425...`，探测要加分支 |
| ANTE 1.1.0 | 1.20.1 轨道 chunk 亮度，探测 `fabric.cn.zbx1425...`（`HostProbe.java:12-13`） | 可选 | ✅ | ✅ **同一个 jar 双端**：`libs/MTR-ANTE-1.1.0+1.20.1.jar` 内同时有 `fabric/cn/zbx1425/**`(573 类) 与 `forge/cn/zbx1425/**`(577 类) + `META-INF/mods.toml`（依赖 architectury ≥1.26.37） | 说明上游做得到，代价是类和 mixin 都要双份 |
| 方速 fangsu | 2 个 mixin + `FangsuCompat`/`FangsuTicketUi` 反射（`FangsuCompat.java:73`、`FangsuTicketUi.java:72`） | 可选 | ✅ 1.3.2 (`[MTR3]...FABRIC`) | ✅ **1.0.0 起就有 `+forge.` 产物**（Modrinth：1.18.2–1.20.1 每个版本都有 fabric+forge，MTR3/MTR4 两条线也各有） | Fabric jar 结构：`com/fangsu/**` 公共类 + `com/fangsu/fabric/**` 加载器类 → Forge 端 `com.fangsu.blockEntities.*` 大概率同名，但仍需实测 |
| JCM (`jsblock`) | PIDS 绑定按类名沿父类链找（`bind/BindTargets.java:40-42`、`bind/MtrTiles.java:96`） | 可选 | ✅ | ✅ 1.2.x 起 Forge，1.20.1 还有 NeoForge | 按类名字符串判断 → 依赖 JCM Forge 的包名前缀是否一致 |
| 天津地铁 (`ziyue.tjmetro`) | PIDS 渲染 mixin（`HostProbe.java:24`、`RenderPidsTianjinStationMixin.java:39`） | 可选 | ✅ | ✅ Modrinth `tianjin-metro` = fabric,forge | 同上，需核对类名 |
| MSD（MTR 车站装饰） | **只用了素材**：11 个墙上扶手 + 11 个玻璃扶手模型、`railing.png` 等（`NOTICE.md` §4） | **否** | 素材已复制进 `mtrlte:` 命名空间 | 不需要 | `ref/nte-jar/MTR-MSD-Addon-fabric-...__fabric.mod.json`、Modrinth 续作也只有 fabric → **但因为不产生运行期依赖，不构成阻塞** |
| LambDynamicLights | **已弃用**：`LightBackends.java:7-13` 明确 "LambDynamicLights is gone"，当前唯一后端是自研 `BeamBlockLight`；`build.gradle:38`（1.18.2）与 `tools/typecheck/stubs/**` 是残留 | 否 | — | — | LDL 本身只有 fabric/neoforge，**没有 forge**；好在已不需要 |
| Mod Menu | 配置入口（`ModMenuIntegration.java`） | 可选 | ✅ | ❌ 仅 fabric/quilt | Forge 用 `IConfigScreenFactory`/`ConfigScreenHandler` |
| **Titanium** | **全仓库无任何引用**（代码、build.gradle、docs、jar 元数据均无 `titanium`） | — | — | — | 任务清单里提到的这个依赖在本仓库**不存在** |

---

## 4. 三个方案对比

### a) Architectury（common + fabric + forge 子项目）

必须做的事（按实测规模）：

| 工作 | 量 |
|---|---|
| 把 `common`(16.9k 行) + `v1.20.1`(74.9k 行) 重排成 `common/` + `fabric/` + `forge/` | 324 个文件搬迁；**本轮先要统一映射**（Architectury 常规做法：两端都用 official，才能共用源码）—— 即 262 个 `net.minecraft.*` 文件的 import/调用名整体改写 |
| Fabric API → Architectury API 或双端实现 | 68 个文件、11 个模块 |
| 注册层 | 133 处 `Registry.register` + 20 个 BE 类型 → DeferredRegister |
| 网络层 | 18 条通道 / 36 个调用点 → `NetworkManager` |
| **Mixin** | 20 个类；Architectury **不解决 mixin**：需要两套 refmap、`remap=false` 的 MTR 目标要按两端分别写、5 个原版目标要按 yarn/official 各写一份 |
| GUI / Mod Menu / KeyBinding | 12 个 Screen + Forge 的配置屏 + `RegisterKeyMappingsEvent` |
| MTR 目标版本 | **1.20.1 Forge 只能是 3.2.2 或 4.0** → 要么降级特性、要么重写 |

估算（**估算，非实测**）：单个 MC 版本 **3–5 人月**（其中 mixin 复核占 1/3），四版 ≈ **12–20 人月**；此后每个新功能都要在 common + 两端 mixin 各验证一次。
风险排序：① 12 个打在 MTR 内部的 mixin 要在 Forge 映射下重写描述符 ② 原版 mixin 的 yarn/official 双份维护 ③ 第三方 addon 包名前缀（`fabric.`/`forge.`）导致所有软目标探测都要双写。

### b) 各加载器独立源码树（现有复制式多版本 ×2）

现状已经是 4 棵树 + 脚本化改写的移植流水线（`tools/port-stage1.js` … `port-stage10-imports.js`，`port-stage1.js:19-26` 就是一串 import 替换表）。再 ×2 = **8 棵树**。

| 项 | 现状 | ×2 后 |
|---|---|---|
| 源码树 | 4 | 8 |
| 单树规模 | 62–75k 行 | 同上 |
| 每功能落地次数 | 4 | **8** |
| Mixin 维护 | 20×4 = 80 个 | 160 个 |
| 版本一致性 | 已经有 131–171 文件漂移 | 漂移翻倍，脚本要写 Forge 专用分支 |

估算：初次 **1.5–2.5 人月/树**（Forge 树比 Fabric 树贵，因为 mixin 与 API 都要重写），四版 ≈ **6–10 人月**；长期维护成本是现有模式的 2 倍以上，且**没有任何共享收益**（Architectury 至少还能把 common 收拢）。
风险：① mixin 双份漂移（最痛，`remap=false` 的 MTR/方速目标在两棵树里写法不同）② 现在靠 `tools/remap-mtr-named.sh` 生成的 named jar 在 Forge 侧不适用，需要另一套脚本。

### c) 只发 Fabric（Forge 不做）

| 项 | 值 |
|---|---|
| 工作量 | 0 |
| 维护成本 | 不变（4 棵树，脚本化） |
| 风险 | 无新增；仅承担"放弃 Forge 用户"这一产品决策 |

---

## 5. 组合矩阵（四版 × 两加载器 × MTR 3/4）

图例：**✅ 现状可用** · **🟡 可行但需重做/降级** · **❌ 依赖不存在**

| MC | Fabric × MTR3 | Fabric × MTR4 | Forge × MTR3 | Forge × MTR4 |
|---|---|---|---|---|
| **1.18.2** | ✅ 已发布（MTR 3.2.2） | 🟡 MTR 4.0.5 fabric 存在；代码要按 MTR4 重写（Train→consist） | 🟡 MTR Forge 3.2.2 存在，NTE/方速/JCM/天津 Forge 都存在 → **依赖齐，但 20 个 mixin + 68 文件 Fabric API 全要重做**（≈ 新工程） | 🟡 依赖齐（MTR Forge 4.0.5），但叠加 MTR4 重写 |
| **1.19.2** | ✅ 已发布（3.2.2） | 🟡 同上 | 🟡 同上（MTR Forge 3.2.2 存在） | 🟡 同上 |
| **1.19.4** | ✅ 已发布（3.2.2） | 🟡 同上 | 🟡 同上（MTR Forge 3.2.2 存在） | 🟡 同上 |
| **1.20.1** | ✅ 已发布（**YMTR 3.6.4.2**） | 🟡 MTR 4.0.5 fabric 存在；需 MTR4 适配 | 🟡 **YMTR 3.6.x 有 Forge 版**（作者确认）→ 可行，成本是多加载器工程 | 🟡 MTR Forge 4.0.5 存在，但必须先完成 MTR4 重写 |

**没有任何一格是"改个 build.gradle 就能出 jar"。** 但也没有"依赖不存在"的死格：Forge × 1.20.1 × YMTR 3.6.x 是成立的，代价是加载器移植工程量。

---

## 6. 推荐

### 推荐：方案 c（只做 Fabric），并把 Forge 需求当作"新产品"而不是"移植"

理由（按权重）：
1. ~~主力版本 1.20.1 在 Forge 上没有对应的 MTR~~ → **更正：YMTR 3.6.x 有 Forge 版**，Forge 是工程量问题不是硬约束。
2. 即使退到 MTR Forge 3.2.2，"光束"这个核心卖点依赖的 `BeamLightMixin` 是一条**原版渲染 mixin**，Forge 侧必须整体重定位并重新做视觉验证（`BeamBlockLight.java:8-19` 的整段论证在 Forge 上要重来一遍）。
3. 20 个 mixin 里 **12 个打的是 MTR 内部**，`remap=false` + 描述符里带 MC 类型名 —— 两端映射不同意味着**每个注入点都要两份字节码证据**。这正是本项目一贯的工作方式（见 `docs/port-*.md` 里成篇的 `javap` 取证），成本会随版本数线性叠加。
4. 收益侧：Forge 的 MTR 玩家基数集中在 **MTR 4**（3.2.2 是 2023 年的老版本），而 MTR 4 上 LTE 的功能要重写才有意义 —— 既然如此，不如一开始就按 MTR 4 设计。

### 如果确实要覆盖 Forge：建议的替代路线（成本远低于移植）

做一个**独立的新模组**，不搬 LTED：

- 目标：**MTR 4 Forge（1.20.1）/ 或直接上 NeoForge**，范围只保留三块：
  1. 车头灯光束（`BeamBlockLight` + `BeamGeom` + `ConeVolumeBeam` + `EndLight` 是**纯数学，零加载器依赖**，`common/` 里 23 个文件不含 `net.fabricmc`，可直接复用核心算法）；
  2. PSD/APG 屏蔽门与玻璃（`extends BlockPSDAPGDoorBase` 7 个、`BlockPSDTop` 3 个、`BlockAPGGlass` 3 个 —— 依赖 MTR 的类，MTR 4 里对应类改名，需重写）；
  3. 扶手 / 站牌 / 装饰方块（11+11 个模型已在 `mtrlte:` 命名空间里，素材可直接复用）。
- 明确不做：PIDS/投影机绑定、JCM/天津/方速联动、3D 光束编辑器、厦门一整套机器（这些是 LTED 里耦合最深的部分）。

估算（**估算**）：**1.5–2.5 人月**能把 1+3 做出来（含 mixin 复核与视觉验证），约为方案 b 单树成本，但换来的是一个可维护的、目标明确的产物。

---

## 7. 附：本次取证明细（可复现）

```bash
# 加载器耦合
grep -rn 'net\.fabricmc' v1.20.1/src/main/java common/src/main/java        # 68 文件 / 11 模块
grep -rh '^@Mixin' v1.20.1/src/main/java --include=*.java | wc -l           # 20
grep -rhoE '@(Inject|Redirect|ModifyArg|ModifyArgs|ModifyVariable)' ...    # 38 个注入
grep -rho 'Registry.register' ... | wc -l                                   # 133

# MTR 版本缺口（外部数据）
curl -s https://api.modrinth.com/v2/project/minecraft-transit-railway/version
#   → 1.20.1 forge: 1.20.1-3.2.2-hotfix-2 / FORGE-1.20-3.2.2-hotfix-1, 然后 FORGE-4.0.0-beta-8

# MTR 自身按加载器编译
unzip -l libs/MTR-fabric-1.20.1-3.6.4.2.jar | grep mappings/   # 只有 FabricRegistryUtilities
unzip -p libs/MTR-fabric-1.20.1-3.6.4.2.jar fabric.mod.json    # name = Yomi's Minecraft Transit Railway

# ANTE 双端
unzip -l libs/MTR-ANTE-1.1.0+1.20.1.jar | grep MeshBuildingRailChunk
#   fabric/cn/zbx1425/.../MeshBuildingRailChunk.class
#   forge/cn/zbx1425/.../MeshBuildingRailChunk.class

# 3.2.2 vs 3.6.4.2 签名
javap -p -classpath <extracted-3.2.2>  mtr.client.RouteMapGenerator
javap -p -classpath <extracted-3.6.4.2> mtr.client.RouteMapGenerator
```
