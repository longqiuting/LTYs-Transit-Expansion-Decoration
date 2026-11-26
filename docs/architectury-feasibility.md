# LTED 迁移 Architectury 可行性评估

> 评估日期：2026-11-26　范围：LTED（`mtrlte`，1.20.1，Fabric + Forge 单 jar）
> 只做评估，不动任何代码 / 构建脚本。所有"现在是什么样"的结论都来自本仓库代码与已有文档；
> 所有"Architectury 是什么样"的结论都来自 9.2.14 的 jar 与上游源码（见附录 A 的证据清单）。

## 0. 结论速览

**有条件推荐。**

- **推荐的部分**：把构建骨架换成 Architectury Loom 的三工程（`common/` + 两个平台工程）+ 继续 Forgix 合单 jar。
  这能一次性消掉本项目最大的痛点——「改一行共享代码 → 重跑 5 分钟 remap/反编译 → 再跑两个 fix 脚本」，
  变成「改 `common/` 里一份源码，编译一次」。共享代码的日常改法从"生成物上打补丁"变回"改源码"。
- **不推荐的部分**：把平台差异整体外包给 Architectury API。逐项核对下来，它有 4 处对不上本项目
  （BER 注册时序、内置高清资源包、ModMenu 配置入口、ctrl+中键拾取），而这 4 处恰好是现在
  Forge 侧最容易出事的地方。
- **一句话理由**：Architectury 能替掉的是**构建管线**（这是真收益，一次 26–34 人日换掉永久性的 5 分钟循环），
  替不掉的是**平台行为**（那 1.6% 的代码该手写还是得手写），而且它不会让 jar、依赖、mixin 冲突面变简单。

前置条件（不满足就不建议动工）：

1. P0 验证 spike 通过——`architectury-loom 1.17.x + Gradle 8.14 + JDK 21/17 + MC 1.20.1 + Forge 47.4.0`
   能跑通一个带方块 / BER / 包 / mixin 的最小三工程，且 Forgix 1.2.9 能把它合回单 jar。
2. 只维护 1.20.1（与 `docs/lte-split-decisions.md` §3.5 已定口径一致）。多版本矩阵会让这套结构的收益迅速归零。

---

## 1. 现状盘点（评估的事实基础）

### 1.1 代码规模

| 项 | 数值 | 来源 |
|---|---|---|
| Fabric 源码 | **211 个 `.java` / 44,400 行** | `v1.20.1/src/main/java` |
| Forge 生成物（java-gen） | **210 个 `.java`** | 反编译产物 = Fabric 树 − 3（`MinecraftClientPickMixin` / `BuiltinPacks` / `PoleRawRenderer`）+ 2（`common/` 的 `LteLog` / `HostProbe`） |
| Forge 手写平台层 | 9 个 `forge/platform/*`（929 行）+ 8 个 `forge/*` 类（733 行），合计 1,662 行 | `forge/src/main/java` |
| 资源 | assets 1217 文件 + data 190 文件（两端逐文件一致） | `docs/forge-m4b5` |
| 产物 | Fabric 25.4 MB / Forge 25.4 MB / 合并 26.2 MB | `build/libs` 实测 |

### 1.2 平台 API 用量（要迁移的真实接触面）

| 平台 API | 用量 | 分布 |
|---|---|---|
| `Registry.register(Registries.X, …)` | **108 处**：BLOCK 40 / ITEM 39 / BLOCK_ENTITY_TYPE 21 / ITEM_GROUP 4 / SOUND_EVENT 3 / ENTITY_TYPE 1 | 约 60 个 `*Objects` 类 |
| `FabricBlockEntityTypeBuilder` | 20 处 `create(…).build()` | 16 个文件 |
| `ItemGroupEvents.modifyEntriesEvent` | 26 处（23 个文件）；`FabricItemGroup.builder()` 4 处 | 页签条目 + 置顶重排 |
| `BlockEntityRendererRegistry.register` | **20 处** | 12 个 `*Client` 类 |
| `EntityRendererRegistry.register` | 1 处（充电宝投射物） | `ChargerClient` |
| `BlockRenderLayerMap.INSTANCE.putBlock` | 13 处 | 9 个文件 |
| 网络：`registerGlobalReceiver` / 发送点 | **12 个频道 / 12 个接收器 / 20 个发送点** | 11 个文件 |
| `ResourceManagerHelper` + `SimpleResourceReloadListener` | 1 处（PIDS 广告表） | `PidsXiamenClient` |
| `ColorProviderRegistry.BLOCK` | 1 处（柱子染色） | `PoleTint` |
| `ClientCommandRegistrationCallback` | 1 处（`/mtr_lte dep`） | `Commands` |
| 内置资源包 | 1 处（`resourcepacks/lted_hd`） | `platform/BuiltinPacks` |
| mixin | **10 个文件**：8 个 mixin 打 MTR 内部（`BlockPlatform` / `RouteMapGenerator`×2 / `RenderPIDS` / `RenderPSDAPGDoor` / `ClientCache`×2 / `ziyue.tjmetro.RenderPIDSTianjin`）+ 1 个打原版 `MinecraftClient` + 1 个 config plugin | 2 份 mixin json |

### 1.3 按"能不能搬进 common"分桶（这是成本估算的骨架）

| 桶 | 文件 | 行数 | 说明 |
|---|---|---|---|
| **A. 零改动** | 18 | 6,799 | 不 import `net.minecraft` / `mtr` / `net.fabricmc`（布局 / 排版 / 解析 / 数学） |
| **B. 只需重命名** | 125 | 27,513 | 有 MC 或 MTR import，但**没有** Fabric API、没有 mixin；只需 Yarn→mojmap |
| **C. 换 API 后可搬** | 57 | ≈9.5k | 54 个有 Fabric API 调用点 + 3 个单点（`PoleTint` 染色、`PidsXiamenClient` 资源重载、`LtedClient` 入口）；`PoleClient` 也在这一桶，但它内部的 ctrl+中键要留平台钩子 |
| **D. 必须留平台层** | 11 | 712 | 10 个 mixin 文件（657 行）+ `platform/BuiltinPacks`（55 行）= **1.6% 的代码** |

合计 211 / 44,400 行。**没有一行业务逻辑必须留在平台层**——留的只有 1 个资源包注册和 11 个 mixin 相关文件。

---

## 2. 逐项对照：Architectury 能不能覆盖本项目的平台差异

判定口径：
- ✅ **覆盖** = 有对应 API，替换调用即可，两平台行为一致；
- ⚠️ **部分** = 有 API 但语义/时序对不上，要额外兜底；
- ❌ **不覆盖** = 没有对应 API，必须手写平台层。

| # | 平台差异 | 现在怎么做（Fabric / Forge） | Architectury 对应 | 判定 |
|---|---|---|---|---|
| 1 | 方块 / 物品 / 音效 / 实体类型注册（108 处） | Fabric：`Registry.register` 立即写；Forge：`Reg` 6 个 `DeferredRegister`，且**对象实例化被推迟到 `RegisterEvent` 里**（Forge 的 `Block` 构造器会自注册，在 `@Mod` 构造器里 `new` 会撞 "registry already frozen"） | `DeferredRegister.create(modId, key)` + `register(id, Supplier)` → `RegistrySupplier<T>`；Fabric 立即写，Forge 走 `DeferredRegister` | ✅ 但**语义要改**：返回值从 `T` 变成 `RegistrySupplier<T>`，55 处 `*Objects.foo()` holder 访问点要变成 `.get()`（或保留自家 holder，见 §3） |
| 2 | 方块实体类型（20 处） | Fabric `FabricBlockEntityTypeBuilder.create(...).build()`；Forge `BlockEntityType.Builder.of(...).build(null)` | 无专用 API；原版 `BlockEntityType.Builder` 两端都能用，配合 `DeferredRegister` 即可 | ✅ |
| 3 | 创造页签 + 条目顺序（4 个页签 / 26 处条目 / 置顶重排） | Fabric：`FabricItemGroup.builder()` + `ItemGroupEvents`，重排靠 `entries.getDisplayStacks()` 拿**可变列表**再 `set()`；Forge：`Reg` 自建 `List<ItemStack>` 在 `BuildCreativeModeTabContentsEvent` 回放 | `CreativeTabRegistry.modify(tab, callback)`，回调拿到 `CreativeTabOutput`（`accept` / `acceptBefore` / `acceptAfter`） | ✅ 但**重排写法要重写**：`acceptBefore(anchor, stacks)` 代替 `getDisplayStacks()` 的原地改写（`LogoObjects.pinCoreTabOrder` 一处，约 20 行） |
| 4 | 方块实体渲染器（20 处） | Fabric：`BlockEntityRendererRegistry.register` 在 client init 里直接调；Forge：`Renderers` **排队**，在 `EntityRenderersEvent.RegisterRenderers` 回放，且 `Renderers.ensureClientInit()` 把整个 client init 拉进这个事件（因为 Forge 47.4.0 实测 `RegisterRenderers` **早于** `FMLClientSetupEvent`，否则队列永远空、当时 19 个 BER 全丢） | `BlockEntityRendererRegistry.register(type, provider)`；**Forge 实现是裸调 `BlockEntityRenderers.register(type, provider)`，没有任何延后**（`BlockEntityRendererRegistryImpl.java`）；而 `ClientLifecycleEvent.CLIENT_SETUP` 在 Forge 就是 `FMLClientSetupEvent`（`EventHandlerImplClient.java:341`） | ⚠️ **部分**：按官方写法在 CLIENT_SETUP 里注册，Forge 上比 Forge 官方指定点（`RegisterRenderers`）更晚。原版 1.20.1 的 provider 表在 `BlockEntityRenderDispatcher.reload(ResourceManager)` 里重读（javap 已核：该类实现 `SynchronousResourceReloader`），所以"更晚"**可能**仍然生效——但这取决于首次资源重载与 `FMLClientSetupEvent` 的先后，属环境相关，**必须在 spike 里实测**。保守做法：保留现有 `Renderers` 的 `ensureClientInit` 逻辑（20 行 `@ExpectPlatform`） |
| 5 | 实体渲染器（1 处） | 同 4 | `EntityRendererRegistry.register` | ⚠️ 同 4（同一个时序问题） |
| 6 | 渲染层（13 处） | Fabric `BlockRenderLayerMap`；Forge `ItemBlockRenderTypes.setRenderLayer` | `RenderTypeRegistry.register(type, blocks...)`；Forge 实现就是裸调 `setRenderLayer` | ✅（只要在 CLIENT_SETUP 及之后调用，与现在一致） |
| 7 | 方块染色（1 处） | Fabric `ColorProviderRegistry.BLOCK`；Forge `RegisterColorHandlersEvent.Block` + `event.getBlockColors()` | `ColorHandlerRegistry.registerBlockColors(color, blocks...)`；Forge 实现：`Minecraft.getBlockColors()` 为 null 才排队到事件，否则直写 live 实例 | ✅（CLIENT_SETUP 时实例已存在 → 直写，安全） |
| 8 | 网络 C2S / S2C（12 频道 / 12 接收器 / 20 发送点） | Fabric：`ServerPlayNetworking` / `ClientPlayNetworking` / `PacketByteBufs` / `PlayerLookup`；Forge：`Net` 给**每个逻辑频道建一个 `SimpleChannel`**，双向各一条消息，payload 原样字节透传 | `NetworkManager.registerReceiver(Side, id, receiver)` + `sendToPlayer` / `sendToPlayers` / `sendToServer` / `canPlayerReceive` / `canServerReceive`；Forge 实现是**单频道 `architectury:network`**，payload 前写 `ResourceLocation` 前缀，另有 `SYNC_IDS` 握手 | ✅ 覆盖面完整，但**实现层要重写**：12 个频道的收发不解析字节的部分可以直接套用，`canSend` 语义要对齐（现在用 `isRemotePresent`） |
| 9 | 客户端命令 `/mtr_lte` | Fabric `ClientCommandRegistrationCallback` + `FabricClientCommandSource`；Forge `RegisterClientCommandsEvent` + `CommandSourceStack` | `ClientCommandRegistrationEvent.EVENT`，source 类型是 `ClientCommandSourceStack`（不是 `CommandSourceStack`） | ✅（`Commands.java` 一处，`sendFeedback` 换 `source.sendFeedback`） |
| 10 | 客户端资源重载（1 处） | Fabric `ResourceManagerHelper` + `SimpleResourceReloadListener`；Forge `Resources` 适配器 → `RegisterClientReloadListenersEvent` | `ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, PreparableReloadListener, id)`；Forge 实现直调 `ReloadableResourceManager.registerReloadListener` | ✅（`SimpleResourceReloadListener` 三段式要改成 `PreparableReloadListener` 的 `reload(barrier,…)`，约 15 行） |
| 11 | 内置资源包（`mtrlte:lted_hd`，默认关闭） | Fabric `ResourceManagerHelper.registerBuiltinResourcePack(ModContainer,…)`；Forge `AddPackFindersEvent` + `PathPackResources` + `Pack.readMetaAndCreate` | **无** | ❌ **不覆盖** → 必须 `@ExpectPlatform` 两份实现（现成的 55 + 88 行可直接搬） |
| 12 | mixin（10 个文件，含 2 份 json / 2 份 refmap） | Fabric json 用 yarn 描述符（`lte.refmap.json`）；Forge json 独立一份（`lted.refmap.json`，实测为空表），且 `StationNameEntranceLogoMixin` 的 `ResourceLocation.<init>` 描述符被 `fix-java-gen.js` 改成 mojmap | Loom 按平台各自生成 refmap；common 只写一份 mixin 类。8 个 MTR mixin 全部 `remap=false`、选择器是 **MTR 自己声明的方法名**（不混淆，两端同名） | ⚠️ **大部分覆盖**：8 个 MTR mixin 里 7 个可原样进 common；唯一分叉点是 `StationNameEntranceLogoMixin` 里两处 `Lnet/minecraft/util/Identifier;<init>` vs `Lnet/minecraft/resources/ResourceLocation;<init>`（注解值必须是编译期常量，一份源码产不出两种）→ 该文件按平台各一份，或抽成平台常量文件 |
| 13 | 客户端事件（进服广播 / 断线清理 / tick） | Fabric `ServerPlayConnectionEvents.JOIN` / `ClientPlayConnectionEvents.DISCONNECT`；Forge `PlayerLoggedInEvent` / `ClientPlayerNetworkEvent.LoggingOut` | `PlayerEvent.PLAYER_JOIN`、`ClientPlayerEvent.CLIENT_PLAYER_QUIT`、`ClientTickEvent.CLIENT_PRE` | ✅ |
| 14 | **客户端初始化时序**（`RegisterRenderers` 早于 `FMLClientSetupEvent`） | Forge 侧专门写了 `Renderers.ensureClientInit()`：谁先到谁跑客户端初始化，保证 BER 队列在回放前填满 | 无。`ClientLifecycleEvent.CLIENT_SETUP` 就挂在 `FMLClientSetupEvent` 上，架构上无法表达"比 RegisterRenderers 更早" | ❌ **不覆盖** → 保留 `Renderers` 那 20 行调度逻辑（这是踩过坑换来的） |
| 15 | ModMenu / 模组列表"配置"按钮 | Fabric `ModMenuApi`；Forge `ConfigScreenHandler.ConfigScreenFactory` 扩展点 | 无（Architectury 的 modmenu 兼容只服务它自己） | ❌ **不覆盖** → 两个小类（各 ~15 行） |
| 16 | 配置目录 / mod 版本 / isModLoaded | Fabric `FabricLoader`（5 个文件）；Forge `Env`（`FMLPaths` / `ModList`） | `Platform.getConfigFolder()` / `getMod(id).getVersion()` / `isModLoaded(id)` | ✅ |
| 17 | ctrl+中键复制柱底属性 | Fabric：`MinecraftClientPickMixin`（`doItemPick` HEAD）；Forge：`InputEvent.InteractionKeyMappingTriggered`（不需要 mixin） | 无 | ❌ **不覆盖**（本来就该分平台：mixin vs 事件） |

**小结**：17 项里 ✅ 10、⚠️ 3、❌ 4。触目惊心的是 ❌ 那 4 项——**BER 时序、内置资源包、ModMenu、拾取键**，正好是现在 Forge 侧花了最多时间调的四件事。

---

## 3. 必须继续手写平台层的部分（迁移后也省不掉）

| 平台层小块 | 现在的实现 | 迁移后形态 | 估算行数 |
|---|---|---|---|
| 渲染器注册时序 | `forge/platform/Renderers`（138 行，含排队 + `ensureClientInit` + 迟到兜底） | `@ExpectPlatform` 的 `PlatformRenderers`：Fabric 直接注册；Forge 在 `RegisterRenderers` 里回放 + 兜底 | 40–60（两端合计） |
| 内置高清资源包 | Fabric 55 行 + Forge 88 行 | 原样搬成两个平台实现（Architectury 无对应 API） | ~140 |
| ModMenu / 配置按钮 | Fabric `ModMenuIntegration` + Forge `ConfigScreenHandler` | 两个平台类不变 | ~30 |
| 客户端命令的 side 隔离 | Forge 侧曾因 `Cmds` 引用客户端事件类在专用服务端 `NoClassDefFoundError` | `ClientCommandRegistrationEvent` 只在客户端接线；`@Environment(CLIENT)` 纪律自守 | ~10 |
| ctrl+中键 | Fabric mixin / Forge `InputEvent` | 不变 | ~60（含 mixin） |
| MTR mixin 描述符分叉 | `fix-java-gen.js` 表格里 2 条改写 | `StationNameEntranceLogoMixin` 两平台各一份（或抽常量） | ~80×2 |
| 两份 mixin json + plugin 接线 | 2 份 json | 不变（Forgix 会改名成 `fabric-/forge-` 前缀） | ~40 |
| 入口点与元数据 | `fabric.mod.json` / `mods.toml` / manifest `MixinConfigs` | 不变（Architectury 只提供 Gradle 插件与 common 模板） | — |

**合计约 500–700 行手写平台代码**（上表各项相加约 490–570 行，含注释与兜底日志取上限）。对比现在 Forge 侧手写的 1,662 行（9 个 shim 929 行 + 8 个类 733 行），**减少约 60–70%**——省掉的只是注册/网络/命令/资源重载那几层胶水，而真正难的四件事本来就没有 API 可依。

---

## 4. 迁移成本估算

### 4.1 人日（单人，熟悉本项目代码的前提下）

| 阶段 | 内容 | 涉及面 | 人日 |
|---|---|---|---|
| **P0 验证 spike** | 三工程骨架跑通；确定 loom/Gradle/JDK 组合；实测 BER 在 `CLIENT_SETUP` 注册是否生效；实测 Forgix 1.2.9 合并后的 mixin json / `@ExpectPlatform` 重定位 | 空工程 | **2–3** |
| **P1 骨架 + 映射迁移** | `common/` / `fabric/` / `forge/` 建立；211 文件 Yarn→mojmap（含 MTR 编译依赖换成 mojmap 命名版）；资源归位 | A 桶 18 + B 桶 125（34.3k 行） | **6–8** |
| **P2 注册层** | 108 处 `Registry.register` → `DeferredRegister`；20 处 BET；4 个页签 + 26 处条目；页签置顶改 `acceptBefore`；holder → `RegistrySupplier` | C 桶 57 | **4–5** |
| **P3 客户端渲染与 GUI** | 20 BER + 1 实体渲染器 + 13 渲染层 + 1 染色 + 1 资源重载 + 23 个 `Screen` | ~35 文件 | **5–6** |
| **P4 网络** | 12 频道 → `NetworkManager`；join/disconnect；`canSend` 语义；字节格式回归 | 11 文件 | **3–4** |
| **P5 mixin** | 8 个 MTR mixin 进 common；描述符分叉；两平台 json/plugin/refmap 验证（含 `ziyue.tjmetro` 的 Forge 侧 TODO） | 10 文件 | **2–3** |
| **P6 打包与双端验收** | Forgix 合单 jar；双端客户端 + 专用服务端起服；40 个方块 / 23 个界面的冒烟回归 | — | **4–5** |
| | | **合计** | **26–34 人日** |

日历换算：单人约 **6–8 周**（含回归与返工）；两人并行（一人平台层 / 一人业务层）约 **4–5 周**。

对照：如果只做「一次性 Yarn→mojmap + 保留现有双树与 `forge/platform/*` shim」（即 `docs/multiloader-plan.md` 的方案 A 改良版），估 **9–13 人日**，且不需要新依赖。

### 4.2 分阶段迁移顺序（每一步都能独立编译 + 起服，出问题二分范围小）

1. **先搬零依赖层**：A 桶 18 个文件（6.8k 行）进 `common/`。理由：不碰 MC 也不碰平台 API，用来验证工程骨架、包名、Gradle 配置，失败成本最低。
2. **再做"只重命名"的 125 个文件**：一次性 Yarn→mojmap（Loom 的 `migrateMappings` 类工具做批量，人工收尾编译错误）。这一步做完，**代码已经在 common 里能编译**，但还没接线——是一个天然的安全检查点。
3. **然后注册层**（108 处 + 20 BET + 4 页签）：此时服务端能起、方块能放、页签能开。**必须逐 id 对比**：迁移前后 `RegistrySelfCheck` 的方块计数（现在日志里有 `common setup: N block(s) registered`）要完全一致。
4. **再客户端渲染层**（BER 20 / 渲染层 13 / 染色 1 / GUI 23）：这一步是唯一可能被 Architectury API 坑到的地方（BER 时序），先按 §2#4 的保守方案写，spike 结论出来后再决定要不要简化。
5. **再网络**（12 频道）：跨端，验证成本最高，放后面；此时两端其它功能已稳定，联机问题不会和渲染问题混在一起。
6. **最后 mixin + 打包**：mixin 只在两端都跑起来之后才需要，且 Forgix 会改名、加前缀，放在最后一次性处理。

顺序原则：**先"不需要 MC 的" → 再"只需改名的" → 再"服务端可验证的" → 再"客户端渲染" → 最后"跨端 + 打包"**。

---

## 5. 风险清单

### 5.1 版本兼容性（已核，不是风险，是结论）

| 项 | 结论 | 证据 |
|---|---|---|
| Architectury API 有没有 1.20.1 版本 | 有，**9.2.14**（2024-02-26 发布，是 1.20.1 最后一个版本） | Modrinth API |
| 与 Forge 47.4.0 | ✅ 兼容（要求 `forge [46,)`、`minecraft [1.20,)`） | `architectury-9.2.14-forge.jar` 的 `META-INF/mods.toml` |
| 与 Fabric API 0.92.12 / loader 0.16.9 | ✅ 兼容（要求 `fabric-api >=0.66.0`、`fabricloader >=0.14.0`） | `architectury-9.2.14-fabric.jar` 的 `fabric.mod.json` |
| 与 MTR 的关系 | **Forge 侧 MTR 3.6.6.1 已经把 `architectury [1.26.37,)` 写成硬依赖** → Forge 玩家本来就装着 Architectury，LTED 加这个依赖的实际增量是 0 | `forge/libs/MTR-forge-1.20.1-3.6.6.1.jar` 的 `mods.toml` |
| Fabric 侧增量 | 新增一个 0.58 MB 硬依赖（MTR 的 fabric 版不依赖 Architectury） | 同上 |

### 5.2 风险表

| # | 风险 | 概率 / 影响 | 缓解 |
|---|---|---|---|
| R1 | **Loom / Gradle / JDK 组合**：现在 Fabric 侧是 Loom 1.11.8 + Gradle **8.14**（Loom 强要求 ≥8.14）；Architectury 1.20 分支自己是用 `architectury-loom 1.1-SNAPSHOT` + Gradle **8.0.1** 构建的；maven 上可用的 architectury-loom 从 1.4.384（2024 初）到 1.17.493（2025） | 中 / 高 | P0 spike 第一件事就是实测 `architectury-loom 1.17.x + Gradle 8.14`。若不行，退回老 Gradle（<8.14）——本项目**已经**是"两套 Gradle"（8.14 给 Fabric、8.1.1 给 Forge），再加一套的边际成本可接受 |
| R2 | **Architectury API 已停止更新**：9.2.14 之后 1.20.1 线不再发版 | 低 / 中 | 1.20.1 已冻结，正常只需修自己遇到的 bug；真踩到 API bug 只能 fork 或绕开（本项目已有 4 处不用它的 API，绕开能力足够） |
| R3 | **BER 注册时序**：`ClientLifecycleEvent.CLIENT_SETUP` = `FMLClientSetupEvent`，而 Forge 47.4.0 实测 `RegisterRenderers` 更早；Architectury 的 BER API 在 Forge 上没有任何延后 | 中 / 高（20 个方块实体渲染器一起失效） | 保留 `Renderers.ensureClientInit` 那套调度；P0 里做对照实验（CLIENT_SETUP 注册 vs 事件注册，看 20 个 BER 是否都在） |
| R4 | **MTR mixin 选择器**：8 个 MTR mixin 全 `remap=false`；名字两端一致，但描述符里出现 MC 类型时必须分平台 | 高（必现）/ 低 | 已知且已有机械方案（`fix-java-gen.js` 那 2 条改写），迁移时改成"该文件两份"即可 |
| R5 | **第三方 mixin 冲突面**（Architectury 不会改变，但共享 common 后会更容易踩） | 中 / 中 | 见 §5.3 |
| R6 | **Forgix 仍然必须用**：Architectury 产出的是**两个** jar | 确定 / 低 | 继续用 1.2.9；MTR-ANTE 1.1.0 就是"Architectury + Forgix 1.2.9 + 单 jar"的现成活样本，与本项目同 MC、同 Forge 47、同 MTR 依赖 |
| R7 | **Forgix 重定位对 `@ExpectPlatform` / mixin json 的影响**：Forgix 会把 `dev.js.mtrheadlight.*` 整体重定位成 `fabric.` / `forge.` 前缀，并改写 mixin json 的 `package` / `plugin` / `refmap`（现有文档已用夹具验证过行为） | 中 / 中 | P0 里用最小工程验证一次；已知限制不变：资源同名时 Fabric 胜出、class 全量复制 |
| R8 | **构建时间**：现在全量 `remap + 反编译 + 两个 fix 脚本` ≈ 5 分钟（改共享代码每次都要跑）；Forge 侧 `clean build` 17 s、Forgix 合并 21 s | — | 迁移后：共享代码只编译一次（common），平台工程只编译薄平台层；**日常改代码的循环 5 min → 约 20–40 s**；全量 clean build 估计持平或略慢（多两个 transform 步骤），净收益在增量循环上 |
| R9 | **Fabric 侧的 MTR 编译依赖**：共享代码要用 mojmap，而现有 `libs/MTR-fabric-1.20.1-3.6.4.2-named.jar` 是 **Yarn named**（用 `tools/remap-mtr-named.sh` 从 intermediary 转的） | 确定 / 中 | 需要一份 **mojmap 命名**的 MTR fabric jar 作编译依赖；仓库已有 remap 工具链与 mappings，估 0.5–1 天。**这是最容易在 P1 卡住的一项，应放在 spike 里一起验** |
| R10 | **专用服务端**：现在 Forge 的 `Net` 引用了客户端类（已有 TODO），迁移到 common 后误用客户端类的概率更高 | 中 / 中 | common 里统一用 `Platform.getEnvironment()` / `@Environment(CLIENT)` 守卫；P6 起一次专用服务端 |
| R11 | **注册顺序回归**：厦门页签的置顶顺序、BET 与 block 的先后、`lte_logo` 的 id 不能变（存档兼容） | 中 / 高 | P1/P2 完成后逐 id 对比 `RegistrySelfCheck` 的计数；页签顺序逐个截图比对 |

### 5.3 与第三方 mixin 的冲突面（Architectury 既不加也不减）

| 对象 | 冲突点 | 现状 |
|---|---|---|
| MTR 本体（12+ mixin） | 本项目 8 个注入里 7 个落在 MTR 的 5 个类上（另 1 个在天津地铁）：`BlockPlatform#getActualState`、`RouteMapGenerator#generateRouteMap/generateColorStrip/generateDirectionArrow/drawResource`、`RenderPIDS#getSchedules`、`RenderPSDAPGDoor#render`、`ClientCache#getResource/getRouteMap/…` | 均为 `@Inject(HEAD, cancellable)`，且 `injectors.defaultRequire = 0`（注入失败不崩）。与其它 MTR 附属（ANTE / NTE / 天津地铁）撞同一方法的概率存在，但迁移到 Architectury **不改变**这一点 |
| 天津地铁 `ziyue.tjmetro` | `RenderPidsTianjinStationMixin` 打 `RenderPIDSTianjin#getSchedules` | **Forge 侧至今未核实**（`docs/forge-m4b5` 标注 ⚠️ TODO：本机没有该 jar）。迁移前应先补这一条，否则会误判"迁移引入的问题" |
| 方速（fangsu）PSD | `BlockPlatformDoorTypeMixin` 改站台门型判定；方速的同源方块可能也走这条判定 | 现在靠行为对齐，无 mixin 冲突记录；迁移不改变 |
| 客户端 mixin（`MinecraftClientPickMixin`） | 打原版 `doItemPick` | Forge 侧已用 `InputEvent` 替代，不在 mixin 冲突面上 |
| Architectury API 自身的 mixin | 它的 `architectury.mixins.json` / `architectury-common.mixins.json` 会更早进链 | 只影响优先级顺序，未见与本项目目标类重叠 |

---

## 6. 结论

### 6.1 推荐度：**有条件推荐**

**推荐做的**：换成 Architectury 的**构建骨架**——`common/`（一份 mojmap 源码）+ `fabric/` + `forge/` 两个平台工程 + 继续 Forgix 合单 jar。

理由（正面）：
- 一次消灭 `remap-to-mojmap.sh` + `fix-java-gen.js` + `fix-java-gen-platform.js` 这套"改生成物"的循环，共享代码回到"改源码"；
- 共享 mixin 只写一份，refmap 由 Loom 按平台生成——现在两份 json + 手动改描述符的维护成本归零（除 1 个描述符分叉）；
- **200 / 211 个文件（95%）能搬进 common**（A+B+C 桶，其中 143 个连 API 都不用换），平台层只剩 11 个文件、约 500–700 行；
- Forge 侧用户**本来就在装 Architectury**（MTR forge 硬依赖它），分发成本几乎为零；
- 与本生态已有先例同构：MTR 自己就是 Architectury 工程，MTR-ANTE 是"Architectury + Forgix 1.2.9 + 单 jar"的现成样本。

**不推荐做的**：把平台差异交给 Architectury API 后就撒手。17 项对照里 4 项它做不到，而且都是踩过坑的地方——尤其 `CLIENT_SETUP` 与 `RegisterRenderers` 的时序，Architectury 的抽象正好抹掉了这个差异。

**条件（缺一不可）**：
1. P0 spike（2–3 人日）验证 `architectury-loom 1.17.x + Gradle 8.14 + Forge 47.4.0` 与 BER 时序；
2. 接受一次性 26–34 人日投入，并且**只维护 1.20.1**（多版本会把这套结构的收益吃光）；
3. 接受 `mtrlte:` 命名空间与所有 id 不变（存档兼容），页签顺序逐个回归。

### 6.2 一句话理由

> Architectury 换来的是「共享代码只写一遍、只编译一次」，换不来「平台差异不用管」——它值这 26–34 人日，但只在你顺手把 `platform/` 里那 4 块（BER 时序、内置资源包、ModMenu、拾取键）继续自己写的前提下才成立。

### 6.3 如果不做 Architectury（对照选项）

| 方案 | 人日 | 日常改共享代码 | 新依赖 | 主要缺点 |
|---|---|---|---|---|
| 现状（反编译管线） | 0 | **5 分钟**（remap + 反编译 + 2 个 fix 脚本） | 无 | 手改必被覆盖；两份源码不同步；java-gen 无注释、局部变量名丢失 |
| **B. 一次性 Yarn→mojmap + 双树 + 手写 shim** | 9–13 | 约 1–2 分钟（两次编译同一份源码） | 无 | 仍是"同一份源码编译两遍"；mixin/refmap 两份维护不变 |
| **C. Architectury（本评估）** | 26–34 | **约 20–40 秒**（common 编译一次） | Forge 已有 / Fabric +0.58 MB | 一次性成本最高；API 已停更；4 项平台差异仍手写 |

若近期没有"再加一个加载器 / 再扩一个 MC 版本"的计划，**B 的性价比高于 C**；若打算长期维护、并且后面还要接 NeoForge 或第二个 MC 版本，**C 的摊销更划算**（Architectury 的结构天然支持加 `neoforge/` 工程）。

---

## 附录 A：证据清单

| 结论 | 证据位置 |
|---|---|
| 211 文件 / 44,400 行；108 处注册（BLOCK 40 / ITEM 39 / BET 21 / TAB 4 / SOUND 3 / ENTITY 1） | `find` / `grep` 统计 `v1.20.1/src/main/java`（本次评估实测） |
| 20 BER / 1 实体渲染器 / 13 渲染层 / 12 频道 / 12 接收器 / 20 发送点 / 26 处页签条目 | 同上 |
| 分桶 18 / 125 / 57 / 11（末位 11 = 必须留平台层） | 按 `import net.minecraft` / `import mtr` / `import net.fabricmc` / mixin 路径逐文件分类 |
| Forge 事件顺序（`RegisterRenderers` 早于 `FMLClientSetupEvent`） | `forge/src/main/java/dev/js/mtrheadlight/forge/platform/Renderers.java` 类注释（实测于 Forge 47.4.0） |
| Architectury BER 在 Forge 无延后 | `architectury-api` 1.20 分支 `forge/.../rendering/forge/BlockEntityRendererRegistryImpl.java` |
| `CLIENT_SETUP` = `FMLClientSetupEvent` | `architectury-api` 1.20 分支 `forge/.../event/forge/EventHandlerImplClient.java:341-342` |
| `ColorHandlerRegistry` / `RenderTypeRegistry` 的 Forge 实现细节 | 同分支 `.../rendering/forge/ColorHandlerRegistryImpl.java` / `RenderTypeRegistryImpl.java` |
| `ReloadListenerRegistry` 的 Forge 客户端走直调 | 同分支 `forge/.../registry/forge/ReloadListenerRegistryImpl.java` |
| `NetworkManager` 单频道 `architectury:network` + `SYNC_IDS` | 同分支 `forge/.../networking/forge/NetworkManagerImpl.java` |
| `DeferredRegister` → `RegistrySupplier`；`CreativeTabOutput.acceptBefore/After` | 同分支 `common/.../registries/DeferredRegister.java`；9.2.14 jar `javap` |
| 9.2.14 的版本约束（forge ≥46、fabric-api ≥0.66） | `architectury-9.2.14-{forge,fabric}.jar` 的 `mods.toml` / `fabric.mod.json` |
| MTR forge 硬依赖 architectury `[1.26.37,)`；MTR 是 Architectury 工程 | `forge/libs/MTR-forge-1.20.1-3.6.6.1.jar` 的 `mods.toml`；`libs/MTR-fabric-1.20.1-3.6.4.2.jar` 内 `architectury_inject_..._devjar/PlatformMethods.class` |
| 现有 MTR fabric 编译依赖是 Yarn named | `tools/remap-mtr-named.sh` 头注释；`libs/MTR-fabric-1.20.1-3.6.4.2-named.jar` |
| Forgix 重定位 / mixin json 改名行为 | `docs/forge-m4b-single-jar.md` §"重定位是真改名"、"已知限制" |
| MTR-ANTE 的 Architectury + Forgix 结构 | `libs/MTR-ANTE-1.1.0+1.20.1.jar`（`fabric/` 573、`forge/` 577、`vendor/` 471、assets 265） |
| 原版 1.20.1 BER 表在资源重载时重建 | `javap` 于 Loom 缓存 MC jar：`BlockEntityRenderDispatcher implements SynchronousResourceReloader`，`reload` 调 `BlockEntityRendererFactories.reload(Context)` |
| 现有 java-gen 管线成本（5 分钟 / 200 类） | 任务上下文 + `docs/forge-m4b2-mojmap-baseline.md`、`docs/forge-m4b3-java-gen-compile.md` |
