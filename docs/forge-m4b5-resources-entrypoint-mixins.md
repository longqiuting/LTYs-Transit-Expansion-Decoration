# Forge M4b-5：资源全量 + `@Mod` 入口接线 + mixin 配置/refmap

产物：`forge/build/libs/lted-forge-3.0.0.jar`（19 MB，1841 条目）
构建：`JAVA_HOME=<JDK17> ./gradlew -p forge clean build --offline` → BUILD SUCCESSFUL

## 1. 资源共享

`forge/build.gradle` 的 `sourceSets.main.resources` 现在是两个源目录：

```groovy
resources {
    srcDirs = ['src/main/resources', '../v1.20.1/src/main/resources']
    exclude 'fabric.mod.json'            // Fabric 专属
    exclude 'mtrlte.mtrdecor.mixins.json' // 目标是 yarn 名，Forge 用 mtrlte.forge.mixins.json
}
```

`forge/src/main/resources` 里只保留 Forge 独有的三样：`META-INF/mods.toml`、`pack.mcmeta`、
`assets/mtrlte/models/item/lte_tool.json`（M4a 样品物品的模型）。原先重复的 8 个占位资源
（clock / lte_logo 的 blockstate、model、贴图、lang）已删除，避免同一路径两份。

语言文件：Fabric 树的 `en_us/zh_cn` 是全量（各 ~49 KB，664/665 键）。Forge 独有的两条
（`item.mtrlte.lte_tool`、`tooltip.mtrlte.lte_tool`）放在 `forge/src/forge-lang/`，
由 `processResources.doLast` 在构建时合并进全量 JSON（已存在的键不覆盖）。

合并后的资源清单与 Fabric jar **逐文件一致**：

```
forge:  assets/ 1248 条目（1217 文件）  data/ 199 条目（190 文件）
fabric: assets/ 1247 条目（1216 文件）  data/ 199 条目（190 文件）
唯一差异：assets/mtrlte/models/item/lte_tool.json（Forge 独有样品物品模型）
missing in forge: 无
```

## 2. `@Mod` 入口

`forge/src/main/java/dev/js/mtrheadlight/forge/LtedForge.java`：

| 顺序 | 动作 | 说明 |
|---|---|---|
| 1 | `Reg.register(modBus)` | 6 个 `DeferredRegister` 先绑 mod 总线，否则后面 new 出来的对象无处排队 |
| 2 | `new LteServerDecor().onInitialize()` | 与 Fabric `ModInitializer` 同一份代码：方块/物品/BET/实体/音效/页签 + `Reg.tabEntries` 页签条目 + `Net.registerServer`（SimpleChannel）+ `Net.onJoin` |
| 3 | `modBus.addListener(Reg::onBuildTabContents)` | `BuildCreativeModeTabContentsEvent` 回放页签条目 |
| 4 | `modBus.addListener(LtedForge::onCommonSetup)` | `FMLCommonSetupEvent` → `enqueueWork` 里打一行 `[lte] common setup: N block(s) registered under mtrlte` |
| 5 | `Net.installCommonHooks()` | `PlayerLoggedInEvent` → 广播各频道初始数据 |
| 6 | `MinecraftForge.EVENT_BUS.addListener(Cmds::onRegisterServerCommands)` | `RegisterCommandsEvent`（双端安全，见下） |
| 7 | `Reg.item(lte_tool, …)` | M4a 样品物品 |
| 8 | `if (dist.isClient()) ClientBootstrap.init(modBus)` | `FMLClientSetupEvent` → `LtedClient#onInitializeClient`（渲染层、BER 排队、客户端频道、命令）＋ `EntityRenderersEvent.RegisterRenderers` 回放 BER ＋ `RegisterClientReloadListenersEvent` ＋ `RegisterClientCommandsEvent`（`/mtr_lte`） |

**修掉的一个真 bug**：原来 `Cmds::onRegisterClientCommands` 被写在 `LtedForge` 构造器里，
无条件挂到 Forge 总线。该方法的签名引用 `RegisterClientCommandsEvent`（客户端专属类），
专用服务端解析 `Cmds` 时会 `NoClassDefFoundError`。现在它挪进 `ClientBootstrap`，
只在 `Dist.CLIENT` 分支里接线；`Cmds` 另加 `onRegisterServerCommands`（`RegisterCommandsEvent`）
给以后的服务端命令用（本模组目前没有服务端命令，`/mtr_lte` 是客户端命令）。

## 3. Mixin

- `forge/src/main/resources/mtrlte.forge.mixins.json`：1 个通用 + 7 个 client mixin，
  `plugin = dev.js.mtrheadlight.mixin.mtr.MtrMixinPlugin`（按宿主类是否存在逐个 gate），
  `refmap = lted.refmap.json`，`injectors.defaultRequire = 0`。
- `forge/src/main/resources/lted.refmap.json`：`{"mappings":{},"data":{"searge":{}}}`。
  **为什么是空的**：Forge 1.20.1 产物里 MC 类名是 official（mojmap），成员是 SRG；本模组的
  mixin 选择器全部是「MTR 自己声明的方法名」（MTR 不混淆这些名字）或 `<init>`（构造器不改名），
  没有任何需要 named→SRG 映射的 MC 成员。所以注解处理器会产出的正是空表。真正的重映射
  由 FG 的 `reobfJar` 对**字节码**完成（已核对产物里的 `m_8055_` 等 SRG 名）。
- jar 清单：`MixinConfigs: mtrlte.forge.mixins.json`（Forge 1.20.1 由
  `MixinPlatformAgentDefault` 读这个清单属性发现配置）。
- `mods.toml` 也加了 `[[mixins]] config = "…"`：这是 NeoForge 的写法，1.20.1 的
  `ModFileInfo` 只按 key 取自己认识的字段（已用 javap 核对它的字符串常量表），未知段落被忽略。
  留着是为了以后迁 NeoForge 不用再改。

### 逐个目标核实（对 `forge/libs/MTR-forge-1.20.1-3.6.6.1.jar` 做 `javap -p`,在 Forge 生产 jar 里
### 成员名是 SRG、MTR 自己的方法名不变）

| Mixin | 目标类#方法 | 实测签名 | 状态 |
|---|---|---|---|
| `BlockPlatformDoorTypeMixin` | `mtr.block.BlockPlatform#getActualState` | `private BlockState getActualState(BlockGetter, BlockPos, BlockState)` | ✅ |
| `RenderPIDSStationMixin` | `mtr.render.RenderPIDS#getSchedules` | `public List<ScheduleEntry> getSchedules(T, BlockPos, Map<Long,String>)`；方法内 +7 处调用 `RailwayData.getStation(Set, DataCache, BlockPos)` | ✅ |
| `RenderPsdDoorTextureMixin` | `mtr.render.RenderPSDAPGDoor#render` | `public void render(T, float, PoseStack, MultiBufferSource, int, int)`；7 处 `String.format(String, Object[])` | ✅ |
| `RouteAsyncScopeMixin` | `mtr.client.ClientCache#getResource` | `private DynamicResource getResource(String, Supplier<NativeImage>, DefaultRenderingColor)`；+177 处 `CompletableFuture.supplyAsync(Supplier)`（单参，非带 Executor 的重载） | ✅ |
| `RouteMapCacheKeyMixin` | `ClientCache#getRouteMap / #getDirectionArrow / #getStationNameEntrance` | 三个方法内各有 `String.format`（偏移 46 / 88 / 26 …） | ✅ |
| `RouteMapArtMixin` | `mtr.client.RouteMapGenerator#generateRouteMap / #generateColorStrip / #generateDirectionArrow` | `(long,boolean,boolean,float,boolean)` / `(long)` / `(long,boolean,boolean,HorizontalAlignment,boolean,float,float,int,int,int)`，与 handler 形参逐位一致 | ✅ |
| `StationNameEntranceLogoMixin` | `RouteMapGenerator#drawResource` + `new ResourceLocation(String,String)` | `private static void drawResource(NativeImage, String, int,int,int,int, boolean, float,float,int,boolean)`；方法内 `new net/minecraft/resources/ResourceLocation("mtr", path)` | ✅（原选择器写的是 yarn 名 `Lnet/minecraft/util/Identifier;…` 且 `remap=true`，已改成 mojmap 名 + `remap=false`） |
| `RenderPidsTianjinStationMixin` | `ziyue.tjmetro.render.RenderPIDSTianjin#getSchedules` | **无法核实** —— 本机没有 Tianjin Metro 附属模组的 jar（`libs/` 里只有 MTR/ANTE/ModMenu/LDL），类名与 `getSchedules` 方法都只能沿用 Fabric 侧的写法 | ⚠️ TODO |

TODO 说明：`RenderPidsTianjinStationMixin` 用 `@Mixin(targets = "ziyue.tjmetro.render.RenderPIDSTianjin")`，
由 `MtrMixinPlugin` 在宿主类不存在时 gate 掉，所以**没装该附属模组的玩家不会受影响**；
但装了之后的注入是否正确、`getSchedules` 名字是否与 Forge 版附属一致，需要在有该 jar 的环境里
用 `javap -p ziyue.tjmetro.render.RenderPIDSTianjin` 补验。

### 为什么 Forge 侧的名字和 Fabric 侧不同

Forge 1.20.1 的 mod 产物是「official 类名 + SRG 成员名」（`reobfJar` 把 Mojang 名改成 SRG）。
例如 `BlockGetter#getBlockState` 在产物里是 `m_8055_`、`BlockPos#above` 是 `m_7494_`。
因此：

- mixin **选择器**里出现 MC 成员名时必须写 SRG（或干脆只引用 `<init>` / 非 MC 成员）；
- MTR 自己声明的方法（`getActualState`、`getSchedules`、`generateRouteMap` …）**不混淆**，名字照抄；
- 本轮的 8 个 mixin 里没有一处选择器引用 MC 成员名（只有 `ResourceLocation.<init>` 这一个构造器），
  所以选择器层面不需要 SRG 名。

## 4. 验证

```
$ unzip -Z1 forge/build/libs/lted-forge-3.0.0.jar | cut -d/ -f1 | sort | uniq -c
   1248 assets
    199 data
    385 dev
      1 icon.png
      1 lted.refmap.json
      5 META-INF
      1 mtrlte.forge.mixins.json
      1 pack.mcmeta
（共 1841 条目，19 MB；M4b-4 时是 410 条目 / assets 19 / 无 data）
```

```
$ unzip -p … META-INF/MANIFEST.MF
MixinConfigs: mtrlte.forge.mixins.json
```

```
$ javap -p dev/js/mtrheadlight/mixin/mtr/RouteMapArtMixin.class
private static void lte$routeMap(long, boolean, boolean, float, boolean, CallbackInfoReturnable<NativeImage>);
private static void lte$colorStrip(long, CallbackInfoReturnable<NativeImage>);
private static void lte$directionArrow(long, boolean, boolean, IGui$HorizontalAlignment, boolean, float, float, int, int, int, CallbackInfoReturnable<NativeImage>);
```

未做：实机启动 / 双端拆分（`ClientNet`）/ 改 `v1.20.1/` 源码。
