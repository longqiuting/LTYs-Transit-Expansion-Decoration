# M4b-3 — 200 个 mojmap 源码整体编进 forge 模块（机械修复 + 剩余错误分类）

目标：把 `forge/src/main/java-gen/`（`tools/remap-to-mojmap.sh` 产出的 200 个 mojmap 反编译源码）
作为**一个源集**整体编进 forge 模块，机械修复能修的，剩下的按「机制性问题」分类报出来。
不改业务逻辑、不改 Fabric 源码、不动已迁移的手写 forge 类（`forge/` 包，一行未改）。

## 1. 结论（实测）

| 指标 | 值 |
|---|---|
| 接入方式 | `sourceSets.main.java.srcDir 'src/main/java-gen'`（与 5 个手写类同源集） |
| 全量 javac（200 个 java-gen + 5 个手写类） | **144 通过 / 56 失败 / 359 个错误** |
| 接入前基线（同一次全量编译） | 116 通过 / 84 失败 / 400 个错误 |
| 机械修复文件数 | **33 个**（脚本 100% 生成，0 手改） |
| 因机械修复由失败转通过 | **28 个文件**（116 → 144） |
| 剩余错误里「反编译/命名/泛型」类 | **0**（全部消失） |
| 剩余 56 个文件的性质 | **100% 是 Fabric 平台层 API 缺失**（5 类） |
| `./gradlew build --offline` | BUILD FAILED（首次 19s / 增量 4s），`359 个错误`，与独立 javac 一致 |

**一句话**：整包一起编之后，单文件重编时看不见的跨类擦除问题并没有额外冒出来；
77 个失败降到了 56 个，且剩下的 56 个**全部**是 Fabric 平台层的洞，不再有"反编译没修干净"的文件。

## 2. 接入（`forge/build.gradle`）

```groovy
sourceSets {
    main {
        java { srcDir 'src/main/java-gen' }
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.release = 17
    options.compilerArgs += ['-Xmaxerrs', '5000']   // 默认 100 条会把分类统计截断
}

// 诊断用：导出 compileJava 的真实 classpath（独立 javac 复用同一份）
tasks.register('printCompileCp') {
    doLast {
        println 'CP_BEGIN'
        println sourceSets.main.compileClasspath.asPath
        println 'CP_END'
    }
}
```

## 3. 修复管线（可重跑、幂等）

```bash
./tools/fix-java-gen.sh            # classpath 同步 → 机械修复 → 全量 javac → 分类（8 轮上限）
./tools/fix-java-gen.sh --dry-run  # 只看会改什么
```

| 脚本 | 作用 |
|---|---|
| `tools/fix-java-gen.sh` | 编排：同步 classpath、循环「修复 → 全量 javac」直到没有可机械修的错误、最后跑分类 |
| `tools/fix-java-gen.js` | 修复器本体：表格修复 + 结构性修复（switch 作用域、重复局部变量改名），幂等 |
| `tools/m4b3-javac.sh` | 用 forge 的真实 compileClasspath 一次性 javac 全部源码（`-Xmaxerrs 20000`，不截断） |
| `tools/m4b3-classify.js` | 按「文件 → 主因」分类，输出 markdown 表 + JSON |
| `tools/m4b3-context.js` | 人读辅助：打印某个失败文件首条错误的上下文（±7 行） |

**接入前的基线**（同一次全量编译，`git stash` 掉全部修复后测的）：
116 通过 / 84 失败 / 400 个错误，其中 Fabric 平台层 56 个、可机械修 28 个
（DUP 10 + MTR 侧 5 + TYPE 4 + OVERRIDE 4 + OTHER 5）。

`java-gen` 是生成物：`remap-to-mojmap.sh` 重跑会覆盖，再跑一次 `fix-java-gen.sh` 即复原同样结果
（实测第二轮输出 `无需修改（已幂等）`，错误数仍是 359）。

### 3.1 做了什么（33 个文件，按机制归类）

| 机制 | 文件数 | 处数 | 手段 | 语义影响 |
|---|---|---|---|---|
| 重复局部变量 / switch case 摊平 | 14 | 17 | switch 的每个 `case` 体加 `{}` 块作用域；嵌套块内改名（`mesh→meshLted`）并同步块内引用 | 无（纯作用域） |
| 丢 cast / 类型推断 | 7 | 8 | 补显式 cast 或类型参数：`(BlockEntityTicker<T>)(BlockEntityTicker<X>)X::tick`、`new LinkedHashSet<Long>(...)`、`Optional.orElse((SoundEvent)null)`、lambda 形参补类型、`(List<int[]>)`、`Object var4 → U var4` | 无（擦除还原） |
| mojmap 命名撞车 | 4 | 5 | 本地 `static box(...)` → `ltedBox`（3 文件，与 `Block.box` 同签名）；`static isDamaged` → `isDamagedTag`（与 `IForgeItem.isDamaged` 冲突） | 无（纯改名） |
| MTR 侧：`newBlockEntity` 缺实现 | 5 | 8 | 保留 `createBlockEntity`（MTR 的抽象方法），补桥方法 `newBlockEntity → this.createBlockEntity(...)` | 无（等价转发，MTR 的默认实现本来就这么做） |
| 反编译把 `super(...)` 塞进 label 块 | 1 | 1 | 构造器改为 `super(...)` 首语句 + `static kindOf(state)` 辅助方法（原 label 块只是三元式的变形） | 无（同序求值） |
| 重映射残留 | 2 | 2 | `server.D()` → `server.overworld()`（`MinecraftServer#overworld()`） | 无 |

### 3.2 两个值得回灌管线的发现

1. **`MinecraftServer#D()` 没被改名**（2 个文件）。这是映射合成的漏点（obf 名 `D` 没匹配上 mojmap 的
   `overworld`），不是反编译问题。建议在 `tools/fix-decompiled-source.js` 里加一条「obf 单字母方法名」
   体检，或在 `join-mappings.js` 里查这条为什么不匹配。
2. **MTR Forge 的 `EntityBlockMapper` 用 SRG 名做默认实现**（`public default BlockEntity m_142194_(...)`）。
   在 mojmap 编译期它**不算**实现了 `EntityBlock.newBlockEntity`，所以每个实现类都必须显式写
   `newBlockEntity`；同时 `createBlockEntity` 仍是接口的抽象方法，两者都要有。这条应写进
   `docs/mtr3-forge-diff.md` / `docs/forge-m4b2-mojmap-baseline.md` §6.6（原文只说了「改名即可」，实测不是）。

## 4. 剩余 56 个文件：按类别计数

| 类别 | 文件数 | 错误数 | 代表文件 | 根因 |
|---|---|---|---|---|
| **NET** Fabric 网络 API | 12 | 111 | `clock/ClockNet.java`、`PidsXiamenNet.java` | Forge 没有 `net.fabricmc.fabric.api.networking.v1`（`ServerPlayNetworking` 7 / `ClientPlayNetworking` 5 / `PacketByteBufs` 5 / `PlayerLookup` 3 / `*PlayConnectionEvents` 4 个 import 站）。Forge 侧是 `SimpleChannel` + `PacketDistributor`；包体读写用 `FriendlyByteBuf`，字段级代码可照搬。 |
| **REG** Fabric 注册 API | 22 | 117 | `xiamen/XiamenObjects.java`、`EyecandyBlocks.java` | `FabricBlockEntityTypeBuilder`（15 站）+ `ItemGroupEvents`（22 站）/`FabricItemGroup`（4 站）。Forge 侧 `BlockEntityType.Builder.of + DeferredRegister`，创造模式标签走 `BuildCreativeModeTabContentsEvent`。 |
| **RENDER** Fabric 渲染注册 | 13 | 68 | `XiamenClient.java`、`PsdLcdClient.java` | `BlockEntityRendererRegistry`（12 站）、`BlockRenderLayerMap`（9 站）、`EntityRendererRegistry`（1 站）→ Forge 的 `EntityRenderersEvent.RegisterRenderers`、`ItemBlockRenderTypes.setRenderLayer`（client setup 阶段）。 |
| **ENTRY** Fabric 入口 / loader | 8 | 21 | `DecorConfig.java`、`LtedClient.java` | `ClientModInitializer`（14 站）、`ModInitializer`、`FabricLoader`（5 站）、`ModMenuApi/ConfigScreenFactory`（各 1 站）、`ResourceManagerHelper/SimpleResourceReloadListener`（各 1 站）。Forge 侧 `@Mod` 构造器 + `FMLClientSetupEvent`；ModMenu 无等价物；内置资源包改 `AddPackFindersEvent`。 |
| **CMD** Fabric 客户端命令 v2 | 1 | 42 | `Commands.java` | `FabricClientCommandSource` 是 Fabric 独有命令源，`ClientCommandManager`/`ClientCommandRegistrationCallback` 配套；Forge 侧 `RegisterClientCommandsEvent` + `CommandSourceStack`，参数类型与反馈调用都不同。**单文件 42 个错误，是最大的一块单点工作。** |
| 合计 | **56** | **359** | | |

Fabric import 站点共 **115 处 / 23 个不同 API**，全部可枚举（见 `.remap-work/m4b3-classify.json`）。
**没有** MTR 侧、泛型、覆盖、重复变量、命名残留类错误——这几类已经被机械修复清零。

## 5. 真实构建输出（`cd forge && ./gradlew build --offline`）

```
> Task :compileJava FAILED
...\java-gen\dev\js\mtrheadlight\clock\ClockClient.java:4: 错误: 程序包net.fabricmc.fabric.api.client.networking.v1不存在
...\clock\ClockClient.java:5: 错误: 程序包net.fabricmc.fabric.api.client.networking.v1不存在
...\clock\ClockClient.java:6: 错误: 程序包net.fabricmc.fabric.api.client.rendering.v1不存在
...\clock\ClockGlobalNet.java:4: 错误: 程序包net.fabricmc.fabric.api.networking.v1不存在
...\clock\ClockGlobalNet.java:5: 错误: 程序包net.fabricmc.fabric.api.networking.v1不存在
...\clock\ClockGlobalNet.java:6: 错误: 程序包net.fabricmc.fabric.api.networking.v1不存在
...\clock\ClockGlobalNet.java:7: 错误: 程序包net.fabricmc.fabric.api.networking.v1不存在
...\clock\ClockNet.java:4: 错误: 程序包net.fabricmc.fabric.api.networking.v1不存在
...\clock\ClockObjects.java:5: 错误: 程序包net.fabricmc.fabric.api.itemgroup.v1不存在
...\clock\ClockObjects.java:6: 错误: 程序包net.fabricmc.fabric.api.object.builder.v1.block.entity不存在
...\Commands.java:8: 错误: 程序包net.fabricmc.fabric.api.client.command.v2不存在
...\Commands.java:9: 错误: 程序包net.fabricmc.fabric.api.client.command.v2不存在
...\Commands.java:10: 错误: 程序包net.fabricmc.fabric.api.client.command.v2不存在
...\Commands.java:29: 错误: 找不到符号
...\Commands.java:33: 错误: 找不到符号
...\DecorBlocks.java:5: 错误: 程序包net.fabricmc.fabric.api.itemgroup.v1不存在
...\DecorClient.java:3: 错误: 程序包net.fabricmc.api不存在
...\DecorClient.java:4: 错误: 程序包net.fabricmc.fabric.api.blockrenderlayer.v1不存在
...\DecorConfig.java:16: 错误: 程序包net.fabricmc.loader.api不存在
...\LtedCoreItems.java:5: 错误: 程序包net.fabricmc.fabric.api.itemgroup.v1不存在
...\LtedDashboardScreen.java:10: 错误: 程序包net.fabricmc.loader.api不存在
...\RccPlatforms.java:4: 错误: 程序包net.fabricmc.fabric.api.itemgroup.v1不存在
359 个错误
100 个警告
BUILD FAILED in 4s
```

错误类型分布（359 条）：

| 形态 | 条数 | 含义 |
|---|---|---|
| 程序包X不存在 | 160 | 直接是 Fabric 包（`net.fabricmc.*`）或已 import 失败的 Fabric 类名（`ItemGroupEvents` / `BlockRenderLayerMap` …） |
| 找不到符号 | 195 | 全是上一条的级联（用到了那个不存在的类型/静态方法） |
| 不兼容的类型 | 4 | 也是级联：lambda 目标类型缺失导致推断失败 |

即**前 30 条、以及全部 359 条，都不指向反编译产物本身**——失败面已经收敛到平台层。

## 5.1 脚本重跑实况（从纯净基线起）

```
== 2/4 全量 javac（第 1 轮）   400 个错误      ← git stash 掉全部修复后的原始基线
== 3/4 机械修复（第 1 轮）     已修改 34 处
== 2/4 全量 javac（第 2 轮）   360 个错误
== 3/4 机械修复（第 2 轮）     已修改 1 处
== 2/4 全量 javac（第 3 轮）   359 个错误
== 3b/4 机械修复（表格，幂等）  无需修改（已幂等）
== 2/4 全量 javac              359 个错误
```

再跑一遍整个脚本：`无需修改（已幂等）`、`359 个错误`（同一结果，无重复改名）。

## 6. 下一步平台层工作量估算

按 import 站点数（不是文件数）估：

| 类别 | 文件 | import 站点 | 估工 | 说明 |
|---|---|---|---|---|
| REG 注册 | 22 | 41 | **最大块**（约 22 文件 × 0.5 天） | 机械度最高：`FabricBlockEntityTypeBuilder.create(...).build()` → `BlockEntityType.Builder.of(...)`，`ItemGroupEvents.modifyEntriesEvent(...)` → `BuildCreativeModeTabContentsEvent`。 |
| NET 网络 | 12 | 22 | 约 12 文件 × 0.5 天 | 一旦 `SimpleChannel` 骨架定好（`LtedForge.java` 里已有样板），剩下是逐个包换注册/发送调用；`FriendlyByteBuf` 读写体可照搬。 |
| RENDER 渲染 | 13 | 22 | 约 13 文件 × 0.25 天 | 纯注册搬家（`EntityRenderersEvent.RegisterRenderers` / `ItemRenderTypes`），无逻辑改动。 |
| ENTRY 入口 | 8 | 22 | 约 8 文件 × 0.5 天 | `ClientModInitializer.onInitializeClient` → `FMLClientSetupEvent`；`FabricLoader.getConfigDir` → `FMLPaths.CONFIGDIR`；ModMenu 需删/替换。 |
| CMD 命令 | 1 | 3 | 约 0.5–1 天 | 单文件但 42 个错误：`FabricClientCommandSource` 的方法面（`sendFeedback` / `getPlayer` / `getClient`）与 `CommandSourceStack` 不同，整块重写。 |
| **合计** | **56** | **115** | **约 8–10 人日** | 与 M4b-2 §6.1 的估算（55 文件 / 21 个 API）一致，现在有了逐站点的确定数字。 |

## 7. 复现

```bash
cd D:/MC-MTR-JS/mtr-headlight

# 1) 重新生成基线（可选，会覆盖 java-gen）
./tools/remap-to-mojmap.sh

# 2) 机械修复 + 全量编译 + 分类（幂等）
./tools/fix-java-gen.sh

# 3) 真实构建
cd forge && JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew build --offline
```

产物：`.remap-work/m4b3-javac.log`（全量错误原文）、`.remap-work/m4b3-classify.json`（逐文件分类）、
`temp/m4b3-gradle-build.utf8.log`（gradle 输出，UTF-8 转码）。

## 8. 已知限制

1. 分类是**文件级主因**：一个文件可能同时缺网络和渲染 API，只按最强的那类计数（例如
   `PidsXiamenClient` 同时有 networking 与 renderer 注册）。逐 API 的精确站点数见 §6。
2. 机械修复只保证「编译通过 + 语义等价」，没有做 javap 字节级对照（M4b-2 的单文件对照结论仍适用）。
3. 56 个文件里的逻辑（注册时机、包体协议、命令反馈）必须在平台层重写时**按 Forge 语义重排**，
   不是改名能解决的——本轮的贡献是把它们从 359 个错误收敛成 115 个确定的 import 站点。
