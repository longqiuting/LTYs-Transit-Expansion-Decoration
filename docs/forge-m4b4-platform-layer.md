# M4b-4 — Forge 平台层（注册 / 渲染 / 网络 / 命令 shim），359 错 → 0

目标：在 `forge/` 侧补齐 Fabric API 的平台层，让 `src/main/java-gen`（200 个 mojmap 源码）
与 5 个手写 Forge 类一起 **0 编译错误**。原则是「写 shim 而不是逐文件重写」：
java-gen 的类名 / 包名 / 业务逻辑一行不动，只把调用平台 API 的那几行换成 shim。

## 1. 结论（实测）

| 指标 | M4b-3 基线 | M4b-4 |
|---|---|---|
| `./gradlew build --offline` | BUILD FAILED，**359 错 / 56 文件** | **BUILD SUCCESSFUL，0 错 / 0 失败** |
| `clean build --offline`（无增量） | — | BUILD SUCCESSFUL（17s，9 个任务全执行） |
| 产物 | — | `build/libs/lted-forge-3.0.0.jar`（816 KB，381 个 mtrheadlight class） |
| java-gen 里 Fabric API 引用 | 115 处 import / 23 个 API | **0 处** |
| java-gen 里直接 `Registry.register` | 103 处 | **0 处**（全部走 `Reg`） |
| java-gen 被改的行 | — | **258 行 / 27072 行 = 0.95%**（另加 88 行 shim import） |
| 被改写的 java-gen 文件 | — | **59 个**（58 个有调用点 + `ModMenuIntegration` 整文件替换） |

错误按类别全部清零：

| 类别 | 基线文件/错误 | 处理方式 |
|---|---|---|
| 网络 NET | 12 / 111 | 每个逻辑频道一个 `SimpleChannel`（`Net`） |
| 注册 REG | 22 / 117 | `DeferredRegister` 6 个 + 页签事件（`Reg`） |
| 渲染 RENDER | 13 / 68 | `EntityRenderersEvent` + `ItemBlockRenderTypes`（`Renderers` / `Layers`） |
| 命令 CMD | 1 / 42 | `RegisterClientCommandsEvent` + `CommandSourceStack`（`Cmds`） |
| 入口 / 环境 / ModMenu | 8 / 21 | `ClientInit` / `CommonInit` / `Env` / `Resources` |

## 2. shim 清单（`forge/src/main/java/dev/js/mtrheadlight/forge/platform/`）

| 文件 | 职责 | 替掉的 Fabric API |
|---|---|---|
| `Reg` | 6 个 `DeferredRegister`（Block / Item / BlockEntityType / EntityType / SoundEvent / CreativeModeTab）；`block/item/blockEntityType/entityType/sound/tab`；页签条目收集 + `BuildCreativeModeTabContentsEvent` 回放 | `Registry.register(BuiltInRegistries.*)`、`FabricBlockEntityTypeBuilder`、`FabricEntityTypeBuilder`、`FabricItemGroup`、`ItemGroupEvents` |
| `Layers` | `ItemBlockRenderTypes.setRenderLayer` | `BlockRenderLayerMap.INSTANCE.putBlock` |
| `Renderers` | BER / 实体渲染器**排队**，在 `EntityRenderersEvent.RegisterRenderers` 回放 | `BlockEntityRendererRegistry`、`EntityRendererRegistry` |
| `Net` | 每个逻辑频道一个 `SimpleChannel`；原始字节 payload；`canSend/send/sendToServer`；`tracking/allPlayers`；进服 / 断线钩子 | `ServerPlayNetworking`、`ClientPlayNetworking`、`PacketByteBufs`、`PlayerLookup`、`ServerPlayConnectionEvents`、`ClientPlayConnectionEvents` |
| `Cmds` | `Commands.literal/argument`、`feedback`、`RegisterClientCommandsEvent` 回调 | `ClientCommandManager`、`ClientCommandRegistrationCallback`、`FabricClientCommandSource` |
| `Env` | `configDir()` / `isModLoaded()` / `modVersion()` | `FabricLoader` |
| `Resources` | `SimpleResourceReloadListener` → `PreparableReloadListener` 适配器 | `ResourceManagerHelper`、`SimpleResourceReloadListener` |
| `ClientInit` / `CommonInit` | 入口接口（保持 `onInitializeClient()` / `onInitialize()` 方法名） | `ClientModInitializer`、`ModInitializer` |

手工接线（`forge/` 包）：

- `LtedForge`：`@Mod` 入口。`Reg.register(modBus)` → `new LteServerDecor().onInitialize()`
  → `Reg.onBuildTabContents` / `Net.installCommonHooks()` / `Cmds::onRegisterClientCommands`
  → `Dist.CLIENT` 时 `ClientBootstrap.init(modBus)`。
- `ClientBootstrap`：`FMLClientSetupEvent` 里跑 `new LtedClient().onInitializeClient()`；
  挂 `Renderers::onRegisterRenderers`、`Resources::onRegisterClientReloadListeners`；
  `ConfigScreenHandler` 扩展点替代 ModMenu。

## 3. 网络：12 个包、字节格式不变

- 每个 Fabric 频道 id（`clock_cfg` / `clock_global` / `pids_xiamen` / `pids_ad_set` /
  `liquid_scanner_cfg` …）对应 **一个 `SimpleChannel`**，版本串 `"1"`，双向强校验。
- 每个频道两条消息：`0` = C2S（`Net.ToServer`）、`1` = S2C（`Net.ToClient`）。
  两个方向必须是**两个不同的类**——Forge 的 `IndexedMessageCodec` 用 `Class` 做 handler 键，
  同一个类挂两个方向会互相覆盖。
- payload 不解析：发送端把 `FriendlyByteBuf` 剩余字节原样写入，接收端包回一个新的
  `FriendlyByteBuf` 交给原来的 lambda。**包体（kind 字节 / BlockPos / 变长字段）与 Fabric 侧逐字节一致**；
  只有 Forge 频道外层多一层分帧（discriminator + 长度），这是两套平台的必然差异。
- 接收器签名去掉了 Fabric 里没用到的 `handler` / `sender` 两个形参，lambda 体不动。
- `canSend` 用 `SimpleChannel.isRemotePresent(Connection)`，语义等同 Fabric 的 canSend。

## 4. 注册：DeferredRegister + 页签事件

- 103 处 `Registry.register(BuiltInRegistries.X, id, value)` → `Reg.block/item/...`：
  对象仍在 mod 构造期 `new` 出来（保持 Fabric 的写法），再交给 `DeferredRegister`
  在 `RegisterEvent` 阶段真正写入。
- 20 处 `FabricBlockEntityTypeBuilder.create(factory, blocks).build()` →
  `Reg.blockEntityType(id, factory, blocks)`（`BlockEntityType.Builder.of`）。
- 4 个创造页签 → `DeferredRegister<CreativeModeTab>` + `BuildCreativeModeTabContentsEvent`。
  shim 给每个页签维护一份 `List<ItemStack>`，按注册顺序跑完全部监听器再统一 `accept`，
  这样 `getDisplayStacks()`（LogoObjects 的置顶/去重逻辑）与 `method_45421` 都保持原语义。
- `displayItems` 给了一个空生成器：Forge 只负责「往页签里加」，不会补默认生成器，
  留 null 会在第一次打开页签时 NPE。

## 5. 可重跑

`java-gen` 是 `tools/remap-to-mojmap.sh` 的生成物，所以所有改写放在
**`tools/fix-java-gen-platform.js`**（幂等，`--dry-run` 可预演），并已接进
`tools/fix-java-gen.sh`（每轮修复后执行）。重跑反编译管线后跑一次脚本即可复原。

脚本只做机械替换：注册调用（括号配平解析，能处理 `blocksOf((s) -> s.door)` 这种嵌套）、
页签监听器、渲染器/渲染层、网络、环境、命令、入口接口，以及 4 处反编译残留
（`server.l()` → `isDedicatedServer()`、`completedFuture((Object)null)` → `(Void)null`、
`XiamenClient` 两处被丢掉的 raw provider cast）。

## 6. 决策与遗留 TODO

**决策**

- M4a 占位注册（`lte_logo` / `clock`）已从 `LtedForge` 移除：java-gen 的
  `LogoObjects` / `ClockObjects` 注册的是同一批 id，两处都注册会在 Registry 阶段撞名崩溃。
  M4a 的 sample 类仍保留在 `forge/block`、`forge/item` 下；`forge.block.ClockBlockEntity`
  的方块实体类型以 `mtrlte:legacy_clock` 单独注册（只服务 sample 自身，不参与内容）。
- `ModMenuIntegration` 改成 Forge 等价物：模组列表的「配置」按钮 → `LtedDashboardScreen`
  （`ConfigScreenHandler.ConfigScreenFactory` 扩展点），类名 / 包名不变。

**未做（编译不需要，实机需要）**

1. **Mixin 未接**：10 个 `dev.js.mtrheadlight.mixin.*` 类能编译，但 Forge 侧没有
   `mixins.mtrlte.json` 配置和 refmap，运行期不会生效。
2. **资源包**：`src/main/resources/assets/mtrlte` 目前只有 M4a 的 3 个 json + lang；
   java-gen 的 200 个类要用的模型 / 贴图 / 方块状态还没迁。
3. **`RegistrySelfCheck` 退化成盲区**：它靠 `BuiltInRegistries.BLOCK` 的
   `intrusiveValueToEntry` 反查「注册失败漏掉的方块」；改用 `DeferredRegister` 后，
   mod 构造期注册表里还没有我们的方块，这套自检只会报 0。诊断用，不影响功能。
4. **`Net` 引用了客户端类**（`Minecraft` / `ClientPacketListener`）：专用服务端加载
   `Net` 时理论上可能触发 `NoClassDefFoundError`。要做到完全干净，应把客户端收发拆成
   `ClientNet` 并由 `ClientBootstrap` 调用；当前以「编译 0 错」为目标没有拆。
5. **未实机验证**：本轮只验证编译 + 打包，未启动客户端/服务端。
