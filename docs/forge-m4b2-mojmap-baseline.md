# M4b-2 — 机器迁移管线：Fabric jar → mojmap → 反编译源码基线

目标：把 Fabric（intermediary）构建产物的 **330 个 class / 200 个顶层类** 机器改名到
official（mojmap），并反编译成可读源码基线，把「改名字」这件事从手工量里彻底去掉，只剩平台层和
少量反编译瑕疵要人改。**本轮不要求全量编译通过**，但要求管线可重复、结论来自实测。

产物：

| 路径 | 内容 |
|---|---|
| `tools/remap-to-mojmap.sh` | 主管线（6 步，路径全参数化） |
| `tools/join-mappings.js` | intermediary(tiny v2) + mojmap(ProGuard) → 三命名空间 tiny |
| `tools/fix-decompiled-source.js` | 修 fernflower 二进制名渲染（按代码段/字面量段分别处理） |
| `tools/verify-decompiled.sh` | 单文件重编 + javap 对照（`--all` 可跑全量） |
| `tools/compile-one.sh` | 单文件重编（`verify-decompiled.sh` 内部用的同一套 classpath，也可单独跑） |
| `tools/m4b2-survey.sh` / `tools/m4b2-api-count.sh` / `tools/m4b2-mtr-api-diff.sh` | 人工改造量统计 / 逐 API 计数 / `mtr.mappings` 两侧签名 diff |
| `forge/src/main/java-gen/` | **基线源码**：200 个 .java，26 839 行，按包结构归位 |

---

## 1. 结论（实测）

| 指标 | 实测值 |
|---|---|
| 输入 | `v1.20.1/build/libs/LTYs-Transit-Expansion-Decoration-3.0.0.jar`（19 MB） |
| 重映射类数 | **330**（200 顶层 + 130 内部/匿名，内部类随外部类改名） |
| 残留 intermediary 名（`class_*`）文件 | **0** |
| 反编译出 .java | **200 个 / 26 839 行**（内部类合并进外部类） |
| forgeflower 报错 | **0 ERROR**（19 条 `Inconsistent generic signature` WARN，全部是枚举内部类的 `LEnum;` 签名，不影响编译） |
| 单文件重编（200 个逐一 javac） | **123 通过**（113 个 javap 成员签名与重映射后 class **完全一致**；10 个只差 1–2 个 synthetic bridge）/ **77 失败** |
| 三个指定类 | 见 §4 |

耗时（JDK 17，单机）：映射合成 ~1 s，MC 中间层 jar 命中 loom 缓存（否则 +3 s），重映射 **~2 s**，
反编译 **~4 s**，修名 + 落盘 < 1 s；全量重编验证 ~6.5 min（200 次 javac）。

**可重复性实测**：连续跑两次 `remap-to-mojmap.sh`，200 个 .java 的 md5 逐字节一致。

## 2. 管线怎么跑

```bash
cd D:/MC-MTR-JS/mtr-headlight

# 默认：自动发现 v1.20.1/build/libs 下最新的非 -sources/-dev jar
./tools/remap-to-mojmap.sh

# 指定输入 / 输出 / 中间目录 / JDK
./tools/remap-to-mojmap.sh --jar <fabric.jar> --out forge/src/main/java-gen \
                          --work .remap-work --java "C:/Program Files/Java/jdk-17/bin/java"
```

`--jar` 不是必须的：默认按 `v1.20.1/build/libs/*.jar`（排除 `-sources`/`-dev`）取最新，
所以 `archives_base_name` 改名（`lted` → `LTYs-Transit-Expansion-Decoration`）不需要动脚本。

六步（全部离线，只用本机 Gradle 缓存，不联网）：

1. **合成映射**：`tools/join-mappings.js` 把
   `net.fabricmc:intermediary:1.20.1` 的 `mappings/mappings.tiny`（`official → intermediary`）
   与 ForgeGradle 缓存里的 `client_mappings.txt`（mojmap ProGuard）
   用「obf 类 + obf 成员名 + obf 描述符」做键连接，输出**三命名空间** tiny：
   `tiny 2 0 official intermediary named`。
   匹配率：类 7388/7423、方法 35497/35743（99.3%）、字段 32747/32923（99.5%）。
   未匹配的 35 个类全是 `net/minecraft/server/Main`、`ClientBrandRetriever` 这类
   *本来就没被混淆* 的类，回退成原名，无影响。
2. **MC 中间层 jar**：优先用 loom 已经生成的
   `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-intermediary/...jar`；
   没有才用 tiny-remapper 从 `fabric-loom/1.20.1/minecraft-merged.jar`（obf）现做一份并缓存。
3. **重映射业务 jar**：`tiny-remapper 0.11.1`，`intermediary → named`，
   classpath = MC 中间层 jar + `libs/MTR-fabric-1.20.1-3.6.4.2.jar`。
4. **反编译**：`forgeflower 2.0.629.0`（ForgeGradle 缓存自带），
   `-dgs=1 -asc=1 -din=1 -rsy=1`（解泛型签名 / ASCII 转义 / 解内部类 / 隐藏 synthetic）。
5. **修名**：`tools/fix-decompiled-source.js`（见 §3.3；本次运行为 0 次改写，见 §3.2）。
6. **落盘**：解包 → 删非 .java → `forge/src/main/java-gen/`（默认先清空，可重复执行）。

JDK：重映射/反编译用 **JDK 17**（`C:/Program Files/Java/jdk-17`），输出 `-source/-target 17`。

> `forge/src/main/java-gen/` **不在** `forge/build.gradle` 的 sourceSet 里，
> 因此落盘不会影响 `./forge/gradlew -p forge build`；三个已迁移的 forge 类也没被改动。
> 实测：基线落盘后 `JAVA_HOME=jdk-17 ./forge/gradlew -p forge build --offline` 仍 **BUILD SUCCESSFUL**（23 s）。

## 3. 三个必须踩过的坑（都不是"应该"，是实测报错）

### 3.1 tiny-remapper 的 classpath 必须与输入**同命名空间**

第一版直接把 mojmap 版 MC jar 当 classpath，结果：**类名改对了，方法名一个没改**
（`ClockBlock` 变成 `extends Block` 但方法还是 `method_9515`）。因为 tiny-remapper 要用 classpath
在**源命名空间**里解析继承链；给 mojmap jar 它就解析不到 `class_2248` 的成员，方法重命名自然失效。
换成 `minecraft-merged-intermediary` 后立刻正确（`createBlockStateDefinition` / `getShape`）。

### 3.2 tiny v2 的类名必须是**内部名（斜杠）**

ProGuard 映射给的是点分名（`net.minecraft.world.level.block.Block`）。直接把点分名写进 tiny 的
`named` 列，tiny-remapper 会把**描述符写成点分**
（`(Lnet.minecraft.core.BlockPos;...)V`），class 文件看着正常、`javap -p` 也照样打印成点分，
但 javac 读这种描述符会把它当成嵌套类，报
`无法访问 net$minecraft$core$BlockPos`。修 `join-mappings.js` 后（`named.replace(/\./g,'/')`），
`fix-decompiled-source.js` 的改写次数从 83 文件降到 **0**：说明之前的 "net$minecraft" 现象
**根因是映射写错，不是 fernflower 的锅**。脚本仍保留该修复器，用于回归防护。

### 3.3 反编译器的类型名渲染

即使映射正确，若 fernflower 无法解析某个类型（这里没有），它会退化成二进制名
（`StateDefinition$Builder`）。javac 不接受源码里的 `$` 形式（`java.util.Map$Entry` 一样报错），
所以 `fix-decompiled-source.js` 会把 `X$Y` 改成 `X.Y`，但**只改代码段**：字符串字面量里的
`"mtr.data.IPIDS$TileEntityPIDS"`（共 4 处）和 `"price ${}"` 不会被碰。

## 4. 抽样验证（javac 重编 + javap 对照）

方法：`tools/verify-decompiled.sh` 对每个类**单独** javac（`-implicit:none`，
classpath = 重映射后的 jar（其余项目类）+ mojmap MC jar + MTR-forge jar + Gradle 缓存全部 jar，
460 条 classpath 走 `@argfile`，因为 `-cp "@file"` 不生效），再 `javap -p` 与重映射后的 class 比成员集合。

| 类 | 结果 | 说明 |
|---|---|---|
| `xiamen.XiamenStripPainter` | **PASS** | javac 通过，javap 成员签名**完全一致**（1 203 行） |
| `clock.ClockBlock` | **改 1 行后 PASS** | 原样 1 个错误：`getTicker` 结尾三元式丢了泛型 cast。补回 `(BlockEntityTicker<T>)(BlockEntityTicker<ClockBlockEntity>)` 后编译通过，javap **完全一致** |
| `xiamen.pids.PidsXiamenRenderer` | **改 3 行后 PASS** | 原样 1 个错误：`Block facing` 与内层 `Direction facing` 重名。改名（声明 + 2 处使用）后编译通过；javap 差 2 个 `ACC_BRIDGE/ACC_SYNTHETIC` 的 `shouldRender*`（编译器桥方法，语义无关） |
| `mixin.mtr.BlockPlatformDoorTypeMixin` | PASS | javap 完全一致 |
| `xiamen.dep.DepOnRoofBlockEntity` | PASS | javap 完全一致 |
| `xiamen.ticket.XiamenTicketGateSounds` | PASS | javap 完全一致 |
| `xiamen.fangsu.FangsuPsdGlassBlock` | 改 2 行后 PASS | `Block base` / `BlockState base` 重名，改名后编译通过 |
| `xiamen.pids.PidsXiamenObjects` | FAIL（平台层） | 注册代码用了 `FabricBlockEntityTypeBuilder`，其 Fabric API class 是 intermediary 名 ⇒ `无法访问 class_2586`，属 §6 的人工改造范围 |

全量（`tools/verify-decompiled.sh --all`，200 个文件逐一重编）：**123 PASS / 77 FAIL**。

## 5. 77 个重编失败的原因分类

| 类别 | 数量 | 典型报错 | 处理方式 |
|---|---|---|---|
| A. Fabric API 平台层 | **43** | `无法访问 class_2586` / `程序包 com.terraformersmc.modmenu.api 不存在` | 换 Forge 写法（DeferredRegister / SimpleChannel / IEventBus），不是反编译问题 |
| B. 重复局部变量 | **13** | `已在方法 render(...)中定义了变量 progress` | fernflower 作用域 bug；改个名即可（实测 2 例改 1–2 行后编译通过） |
| C. 泛型/类型推断 | **10** | `条件表达式中的类型错误`、`Object 无法转换为 SoundEvent`、`Set<Object> 无法转换为 Set<Long>` | 擦除导致的 cast/方法引用丢失，补 `(T)` 即可 |
| D. MTR Forge API 差异 | **3** | `TileEntityPIDS 构造器不适用`、`FabricItemGroup.builder 不适用` | 见 §6.6 |
| E. 覆盖/可见性冲突 | **8** | `ChargerBlock 中的 box(...) 无法覆盖 Block 中的 box(...)`、`PowerBankItem.isDamaged 无法实现 IForgeItem.isDamaged`、`PsdLcdBlock 不是抽象的，未覆盖 EntityBlock.newBlockEntity` | 见 §6.7 |

E 类里 **3 个**是 `box` 命名撞车（yarn 的 `Block.createCuboidShape` 在 mojmap 叫 **`Block.box`**，
而 mod 自己也有个 `private static VoxelShape box(...)`）、**1 个**是 `IForgeItem.isDamaged` 可见性、
**4 个**是 `createBlockEntity` 需要改名 `newBlockEntity`（见 §6.7）。

## 6. 人工改造清单（按类别与数量）

### 6.1 Fabric API 注册层 —— 55 个文件，21 个 `net.fabricmc` API

（数字为「出现次数 / 涉及文件数」，由 `tools/m4b2-api-count.sh` 实测）

| Fabric API | 次数 | 文件 | Forge 对应物 |
|---|---|---|---|
| `itemgroup.v1.ItemGroupEvents` | 72 | 22 | `CreativeModeTab` + `BuildCreativeModeTabContentsEvent` |
| `object.builder.v1.block.entity.FabricBlockEntityTypeBuilder` | 35 | 15 | `DeferredRegister<BlockEntityType<?>>` + `BlockEntityType.Builder.of(...)` |
| `client.rendering.v1.BlockEntityRendererRegistry` | 31 | 12 | `EntityRenderersEvent.RegisterRenderers` |
| `networking.v1.ServerPlayNetworking` | 30 | 7 | `SimpleChannel` + `NetworkDirection` |
| `api.ClientModInitializer` | 28 | 14 | `@Mod` 构造器 + `FMLClientSetupEvent` |
| `client.networking.v1.ClientPlayNetworking` | 24 | 5 | `SimpleChannel#sendToServer` |
| `client.command.v2.*`（`FabricClientCommandSource` 23 / `ClientCommandManager` 16 / `ClientCommandRegistrationCallback` 3） | 42 | 1 | `RegisterClientCommandsEvent` |
| `blockrenderlayer.v1.BlockRenderLayerMap` | 20 | 9 | `ItemBlockRenderTypes.setRenderLayer`（client setup） |
| `networking.v1.PacketByteBufs` | 19 | 5 | `FriendlyByteBuf`（同名，来自 MC） |
| `loader.api.FabricLoader` | 13 | 5 | `FMLPaths` / `ModList.get()` |
| `itemgroup.v1.FabricItemGroup` | 8 | 4 | `CreativeModeTab.builder()` |
| `networking.v1.PlayerLookup` | 6 | 3 | `PacketDistributor.TRACKING_ENTITY` |
| `networking.v1.ServerPlayConnectionEvents` | 6 | 2 | `PlayerEvent.PlayerLoggedInEvent/LoggedOutEvent` |
| `client.networking.v1.ClientPlayConnectionEvents` | 6 | 2 | `ClientPlayerNetworkEvent.LoggingIn/Out` |
| `api.ModInitializer` | 2 | 1 | `@Mod` 构造器 |
| `resource.SimpleResourceReloadListener` / `ResourceManagerHelper` | 2 / 2 | 1 | `AddReloadListenerEvent`（内置资源包见 §6.4） |
| `object.builder.v1.entity.FabricEntityTypeBuilder` | 2 | 1 | `DeferredRegister<EntityType<?>>` |
| `client.rendering.v1.EntityRendererRegistry` | 2 | 1 | `EntityRenderersEvent.RegisterRenderers` |
| `com.terraformersmc.modmenu.api.ModMenuApi` / `ConfigScreenFactory` | 2 / 2 | 1 | 删除（Forge 无 ModMenu 等价物，配置走 `ModConfigSpec` 或自带 GUI） |

`Registry.register(...)` 共 **123 处 / 37 文件**：Forge 侧统一改 `DeferredRegister`
（或保留 `Registry.register` + `BuiltInRegistries` 也能跑，但注册时机要放进 `FMLCommonSetupEvent`）。

### 6.2 事件

代码里没有 `ClientTickEvents` / `ServerTickEvents`（0 处），事件面很窄，主要就是
§6.1 里的 init / 网络 / 资源重载 / 命令四类。样板已在 `forge/.../LtedForge.java`。

### 6.3 网络包 —— 12 个文件，54 处

`ServerPlayNetworking`(7) + `ClientPlayNetworking`(5) + `PacketByteBufs`(5) + `PlayerLookup`(3)
→ Forge `SimpleChannel`（`NetworkRegistry.newSimpleChannel`）。
`net.minecraft.network.FriendlyByteBuf` 在两侧同名，读写代码可以照搬；
`PacketByteBuf` 在 Fabric 就是它的子类，字段级代码基本 1:1。

### 6.4 内置资源包

`ResourceManagerHelper` 2 处 / 1 文件（PIDS 广告贴图那套）。Forge 侧对应
`AddReloadListenerEvent` + `PackType.CLIENT_RESOURCES`；如果要"打包进 jar 的资源包"，
Forge 用 `AddPackFindersEvent`（`PackSource.BUILT_IN`）。**资源文件本身不用动**（`assets/` 原样）。

### 6.5 Mixin —— 8 个类

| 目标 | 类 |
|---|---|
| `mtr.render.RenderPIDS` / Tianjin 变体 | `RenderPIDSStationMixin`、`RenderPidsTianjinStationMixin` |
| `mtr.block.BlockPlatformDoorType` 等 | `BlockPlatformDoorTypeMixin`、`RenderPsdDoorTextureMixin` |
| MTR 路线图/缓存 | `RouteMapArtMixin`、`RouteMapCacheKeyMixin`、`RouteAsyncScopeMixin` |
| 站名入口 logo | `StationNameEntranceLogoMixin` |

8 个类全部 `remap = false`（22 处注解），目标是 **MTR 自己的类**（`mtr.*`），
所以重映射对它们**没有影响**：8 个文件里没有 `class_` 残留（实测）。
但 Forge 侧要 **第二份 mixin config + refmap**，且 MTR-Forge jar 的 MC 方法名是 **SRG**
（见 §6.6），`@Inject(method=...)` 里若写 MC 方法名要写 SRG 名（`m_xxxxx_`）。
当前 refmap `lte.refmap.json` 里是 `named:intermediary` 数据，Forge 那一份需要重新生成。

### 6.6 `mtr.mappings.*` 两侧签名差异（实测 diff）

`tools/m4b2-mtr-api-diff.sh` 对比 `libs/MTR-fabric-1.20.1-3.6.4.2-named.jar`（yarn 名）与
`forge/libs/MTR-forge-1.20.1-3.6.6.1.jar`（mojmap 类型 + SRG 方法名）：

| 类 | 差异 |
|---|---|
| `BlockEntityMapper` | 类名/构造器参数名同构；`readCompoundTag`/`writeCompoundTag` **两侧同名同签名**（只需把 yarn 类型换成 mojmap 类型）。注意 **Forge 版的 MC 侧钩子是 SRG 名** `m_142466_`(load) / `m_183515_`(save)，源码里 `super.` 调不到，必须走 `readCompoundTag`/`writeCompoundTag` |
| `BlockEntityRendererMapper` | 只差包名（`client.render.block.entity` → `client.renderer.blockentity`），`render`/`shouldRender` 名同 |
| `ScreenMapper` | 构造器 `Text` → `Component`；`addDrawableChild` 泛型 `ClickableWidget` → `AbstractWidget`（**mojmap 下 `ClickableWidget` 不存在，这是编译错误源之一**） |
| `Utilities` | 全部是 MC 类型/方法改名（`World`→`Level`、`PlayerEntity`→`Player`、`ItemGroup`→`CreativeModeTab`、`RegistryEntry$Reference`→`Holder`…），方法名不变 |

另外 `BlockEntityMapper` **没有** `createBlockEntity` 方法 —— 但基线里有 **5 个文件** 反编译出
`public BlockEntityMapper createBlockEntity(BlockPos, BlockState)`（yarn 名被 Loom 保留），
同时另有一个 `newBlockEntity` 桥方法。其中 4 个类自己 `implements EntityBlock`，因此直接编不过
（`不是抽象的，并且未覆盖 EntityBlock.newBlockEntity`）；剩下 1 个 `WallSignBlock` 由父类提供实现所以能编，
但源码里仍是残留名。**统一改名 `newBlockEntity` 即可**。

### 6.7 mojmap 命名撞车（E 类，8 例）

| 现象 | 原因 | 修法 |
|---|---|---|
| `box(...) 无法覆盖 Block 中的 box(...)`（ChargerBlock / DrinksMachineBlock / XiamenMachineBlock） | yarn `Block.createCuboidShape` 在 mojmap 叫 **`Block.box`**（static），而 mod 里也有 `private static VoxelShape box(...)`，静态方法同名同签名构成隐藏 | 把本地 `box` 改名（如 `shape`） |
| `PowerBankItem.isDamaged 无法实现 IForgeItem.isDamaged` | Forge 的 `IForgeItem` 用 **public** 默认方法覆盖同签名，mod 里的实现可见性更窄 | 改 `public` |
| `未覆盖 EntityBlock.newBlockEntity`（4 例） | 见 §6.6 末段 | 改名为 `newBlockEntity` |

### 6.8 反编译残留（B/C 类，23 例）

| 类别 | 数量 | 修法 | 实测 |
|---|---|---|---|
| 重复局部变量 | 13 | 改个名（1–2 行） | `PidsXiamenRenderer`、`FangsuPsdGlassBlock` 改后编译通过 |
| 泛型 cast/方法引用丢失 | 10 | 补 `(T)` / `(T2)` 强转 | `ClockBlock` 补 1 处后 javap 与原始 class **完全一致** |

这两类合计 23 个文件、每个 1–2 行，属于机械修复，可以写脚本批量做（改名用 scope 分析，cast 用
javac 报错行号定位），本轮没有做。

---

## 7. 复现 & 文件清单

```bash
# 1) Fabric 产物（已存在则跳过）
D:/MC-MTR-JS/temp/gradle/gradle-8.14/bin/gradle -p v1.20.1 build

# 2) 机器迁移
./tools/remap-to-mojmap.sh                      # → forge/src/main/java-gen/  (200 个 .java)

# 3) 抽样 / 全量重编验证
./tools/verify-decompiled.sh                    # 3 个指定类 + 5 个按步长抽取
./tools/verify-decompiled.sh --all              # 200 个逐一重编（~6.5 min）

# 4) 改造量统计
./tools/m4b2-survey.sh
./tools/m4b2-mtr-api-diff.sh
```

中间产物都在 `.remap-work/`（已加入 `.gitignore`）：
`intermediary-named.tiny`（三命名空间映射）、`LTYs-…-mojmap.jar`（重映射字节码）、
`ff/`（反编译产物）、`forgeflower.log`、`verify/`（每个类的 javac 日志与 javap 签名）。

## 8. 已知限制

1. **单文件重编不等于整包重编**：验证时其余项目类取自重映射后的 jar，只检验"这一个文件反编译得对不对"；
   整包一起编译会额外暴露跨类的擦除问题（数量未测）。
2. **Forge 侧 MTR 依赖是 SRG 的**（`m_142466_` 等），所以"基线能否直接编进 forge 模块"要等平台层改完才能整体验证；
   本轮只保证名字已到 official、单文件可重编。
3. **10 个 "javap 有差异" 全是同一件事**：MTR `BlockEntityRendererMapper` 的合成桥
   `shouldRender(BlockEntity, Vec3)` / `shouldRenderOffScreen(BlockEntity)`（`ACC_BRIDGE|ACC_SYNTHETIC`，
   逐个 `diff orig.sig gen.sig` 核对过）。`-rsy=1` 把它们从源码里藏了，重编时 javac 按需生成，
   不影响语义；只有要做字节级对齐时才需要关注。
4. 反编译器只有 forgeflower 一条路（缓存里没有 Vineflower/CFR）；若换 Vineflower，§5 的 B/C 类可能消失，
   管线第 4 步换 jar 即可，其余步骤不变。
5. **M4b-3 更正本条 §6.6/§6.7**：「把 `createBlockEntity` 统一改名 `newBlockEntity` 即可」实测不成立。
   MTR Forge 的 `EntityBlockMapper` 用 SRG 名 `m_142194_` 提供 `newBlockEntity` 的默认实现，在 mojmap
   编译期**不算**实现了 `EntityBlock.newBlockEntity`；而 `createBlockEntity` 仍是该接口的抽象方法。
   两者都要：保留 `createBlockEntity`，另补一个转发桥 `newBlockEntity`。
   详见 `docs/forge-m4b3-java-gen-compile.md`（整包接入 + 机械修复 + 剩余错误分类）。
