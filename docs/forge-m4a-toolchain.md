# M4a — Forge 工具链验证（1.20.1）

目标：证明「同一个仓库里 Fabric（Loom）与 Forge（ForgeGradle 6）各自可构建」，并真实编译出
2–3 个已迁到 official（mojmap）的类，产出合法 Forge jar。**本轮不追求功能完整**。

## 1. 实际可用的版本组合

| 角色 | 版本 | 说明 |
|---|---|---|
| Gradle（Forge 模块） | **8.1.1**（`forge/gradle/wrapper`，腾讯云镜像） | FG 6 在 Gradle 8.8 上会报 `because no repositories are defined`（见 §6） |
| Gradle（Fabric 模块） | **8.14**（`D:/MC-MTR-JS/temp/gradle/gradle-8.14`） | Loom 1.11.8 要求 **Gradle ≥ 8.14**；仓库根 wrapper 是 8.8，用根 wrapper 构建 v1.20.1 会直接失败（既有问题，本轮未改动） |
| ForgeGradle | **6.0.24** | 6.0.54 在本机拉 `net.minecraftforge:accesstransformers:8.2.14`（该坐标在 Forge maven 上不存在），不可用 |
| Forge | **1.20.1-47.4.0** | |
| Mappings | `channel: 'official', version: '1.20.1'` | Mojang 官方映射 = mojmap |
| JDK（编译/运行 Gradle） | **17.0.12**（`C:\Program Files\Java\jdk-17`） | `options.release = 17` |
| MC 依赖 | `net.minecraftforge:forge:1.20.1-47.4.0` | |
| MTR 依赖 | `forge/libs/MTR-forge-1.20.1-3.6.6.1.jar`（YMTR Forge 版，64 MB，`compileOnly`，已在 `.gitignore` 排除） | 本身是 mojmap 编译的，不需要 remap |

构建命令（均在**仓库根**执行，Fabric/Forge 各自独立）：

```bash
# Fabric（需要 Gradle >= 8.14，根 wrapper 8.8 不够）
D:/MC-MTR-JS/temp/gradle/gradle-8.14/bin/gradle -p v1.20.1 build

# Forge（自带 8.1.1 wrapper + JDK 17）
JAVA_HOME="C:/Program Files/Java/jdk-17" ./forge/gradlew -p forge build
```

## 2. `forge/build.gradle` 关键片段

```groovy
buildscript {
    repositories {
        maven { name = 'ForgeMirror'; url = 'https://maven.creeperhost.net' }   // 直连 forge maven 会读超时
        maven { name = 'Forge'; url = 'https://maven.minecraftforge.net/' }
        maven { name = 'Mojang'; url = 'https://libraries.minecraft.net/' }
        mavenCentral()
    }
    dependencies {
        classpath group: 'net.minecraftforge.gradle', name: 'ForgeGradle', version: '6.0.24', changing: true
    }
}
apply plugin: 'net.minecraftforge.gradle'
apply plugin: 'java'

minecraft {
    mappings channel: 'official', version: '1.20.1'
}

dependencies {
    minecraft "net.minecraftforge:forge:1.20.1-47.4.0"
    compileOnly files("libs/MTR-forge-1.20.1-3.6.6.1.jar")
}

processResources {
    filteringCharset = 'UTF-8'          // mods.toml 有中文，expand 默认按 GBK 读会乱码
    filesMatching('META-INF/mods.toml') {
        expand 'version': project.version, 'forge_version': ..., 'minecraft_version': ...
    }
}
```

模块布局：

```
forge/
  build.gradle  settings.gradle  gradle.properties
  gradlew  gradlew.bat  gradle/wrapper/      (Gradle 8.1.1)
  gradle-init-mirror.gradle                  (可选：把 FG 内部仓库也指向镜像)
  libs/MTR-forge-1.20.1-3.6.6.1.jar          (gitignored)
  src/main/java/dev/js/mtrheadlight/forge/   LtedForge(@Mod) + block/ + item/
  src/main/resources/META-INF/mods.toml, pack.mcmeta, assets/mtrlte/...
```

## 3. 迁移的类与 yarn → mojmap 对照

迁移文件（4 个类，`dev.js.mtrheadlight.forge.*`）：

| Forge 类 | 来源（Fabric/yarn） | 说明 |
|---|---|---|
| `block/LogoBlock` | `logo/LogoBlock` | 无覆写的方块，最简 |
| `block/ClockBlock` | `clock/ClockBlock` | 方块 + `EntityBlock` + ticker + 红石 + 刷子（`mtr.Items.BRUSH`） |
| `block/ClockBlockEntity` | `clock/ClockBlockEntity` | 子集：NBT、更新包、tick、停摆/追针、整点报时 |
| `item/LtedTooltipItem` | `security/LiquidScannerItem` | 无渲染物品 + tooltip |

名字对照（已用 1.20.1 `client.txt` 官方映射逐个核对，方法签名一致才迁移）：

| yarn | mojmap（official） |
|---|---|
| `net.minecraft.block.AbstractBlock.Settings` | `net.minecraft.world.level.block.state.BlockBehaviour.Properties` |
| `BlockEntityProvider` | `EntityBlock` |
| `createBlockEntity` | `newBlockEntity` |
| `getOutlineShape` | `getShape` |
| `getCollisionShape` | `getCollisionShape`（同名，参数类型换 `BlockGetter`/`CollisionContext`） |
| `canPlaceAt` | `canSurvive` |
| `getPlacementState` | `getStateForPlacement` |
| `ItemPlacementContext` | `BlockPlaceContext` |
| `ctx.getSide()` | `ctx.getClickedFace()` |
| `ctx.getBlockPos()` | `ctx.getClickedPos()` |
| `ctx.getWorld()` | `ctx.getLevel()` |
| `neighborUpdate` | `neighborChanged` |
| `onBlockAdded` | `onPlace` |
| `onUse` | `use` |
| `ActionResult` / `PASS` / `SUCCESS` | `InteractionResult` / `PASS` / `SUCCESS` |
| `Hand` | `InteractionHand` |
| `PlayerEntity` | `Player` |
| `ServerPlayerEntity` | `ServerPlayer` |
| `player.getStackInHand(hand)` | `player.getItemInHand(hand)` |
| `stack.isOf(item)` | `stack.is(item)` |
| `BlockView` | `BlockGetter` |
| `WorldView` | `LevelReader` |
| `World` | `Level` |
| `world.isClient` | `level.isClientSide` |
| `isReceivingRedstonePower` | `hasNeighborSignal` |
| `setBlockState` | `setBlock` |
| `Block.NOTIFY_ALL` / `NOTIFY_LISTENERS` | `Block.UPDATE_ALL` / `UPDATE_CLIENTS` |
| `Block.createCuboidShape` | `Block.box` |
| `VoxelShapes.empty` | `Shapes.empty` |
| `StateManager.Builder` | `StateDefinition.Builder` |
| `getStateManager()` | `getStateDefinition()` |
| `setDefaultState` / `getDefaultState` | `registerDefaultState` / `defaultBlockState` |
| `MapColor`（`IRON_GRAY`/`OFF_WHITE`） | `net.minecraft.world.level.material.MapColor`（`METAL`/`TERRACOTTA_WHITE`） |
| `BlockSoundGroup.WOOD` | `SoundType.WOOD` |
| `Settings.nonOpaque()` / `dynamicBounds()` | `Properties.noOcclusion()` / `dynamicShape()` |
| `writeNbt` / `readNbt` | `saveAdditional` / `load` |
| `markDirty` | `setChanged` |
| `toInitialChunkDataNbt` | `getUpdateTag` |
| `toUpdatePacket` | `getUpdatePacket` |
| `BlockEntityUpdateS2CPacket.create` | `ClientboundBlockEntityDataPacket.create` |
| `NbtCompound` | `CompoundTag` |
| `Registries.SOUND_EVENT` | `BuiltInRegistries.SOUND_EVENT` |
| `SoundCategory.BLOCKS` | `SoundSource.BLOCKS` |
| `SoundEvents.BLOCK_BELL_USE` | `SoundEvents.BELL_BLOCK` |
| `Identifier` | `ResourceLocation` |
| `Text` / `Formatting` | `Component` / `ChatFormatting` |
| `appendTooltip` / `TooltipContext` | `appendHoverText` / `TooltipFlag` |
| `world.getTime()` | `level.getGameTime()` |
| `world.updateListeners(...)` | `level.sendBlockUpdated(...)` |

## 4. 产物

- jar：`forge/build/libs/lted-forge-3.0.0.jar`（**30,205 B**，37 条目，**6 个 .class**）
- sources：`forge/build/libs/lted-forge-3.0.0-sources.jar`

```
$ unzip -l forge/build/libs/lted-forge-3.0.0.jar
      942  META-INF/mods.toml
       86  pack.mcmeta
     1531  META-INF/LICENSE-mtrlte
     9227  META-INF/NOTICE-mtrlte.md
      903  assets/mtrlte/blockstates/clock.json
       69  assets/mtrlte/blockstates/lte_logo.json
      176  assets/mtrlte/lang/en_us.json
      185  assets/mtrlte/lang/zh_cn.json
       88  assets/mtrlte/models/block/empty.json
     1084  assets/mtrlte/models/block/lte_logo.json
       37  assets/mtrlte/models/item/clock.json
      102  assets/mtrlte/models/item/lte_tool.json
     9150  assets/mtrlte/textures/block/lte_logo.png
       60  assets/mtrlte/textures/block/lte_logo.png.mcmeta
     6645  dev/js/mtrheadlight/forge/LtedForge.class
      891  dev/js/mtrheadlight/forge/block/ClockBlock$1.class
    10475  dev/js/mtrheadlight/forge/block/ClockBlock.class
     8411  dev/js/mtrheadlight/forge/block/ClockBlockEntity.class
      669  dev/js/mtrheadlight/forge/block/LogoBlock.class
     1832  dev/js/mtrheadlight/forge/item/LtedTooltipItem.class
---------  -------
    52768    37 files
```

`META-INF/mods.toml`：

```toml
modLoader = "javafml"
loaderVersion = "[47,)"
license = "LGPL-3.0-or-later"

[[mods]]
modId = "mtrlte"
version = "3.0.0"
displayName = "LTE (Forge, 1.20.1)"
authors = "LTE"
description = '''LTE 的 Forge 侧构建。M4a 阶段只用于验证工具链 …'''

[[dependencies.mtrlte]]   # forge [47,)
[[dependencies.mtrlte]]   # minecraft [1.20.1,1.20.2)
[[dependencies.mtrlte]]   # mtr [3.6,) mandatory = false
```

不是空壳的证据（`javap` 直接读 jar）：

```
public final class dev.js.mtrheadlight.forge.block.ClockBlock
        extends net.minecraft.world.level.block.Block
        implements net.minecraft.world.level.block.EntityBlock {
  public static final DirectionProperty FACING;
  public static final BooleanProperty POWERED;
  public VoxelShape m_5940_(BlockState, BlockGetter, BlockPos, CollisionContext);   // getShape
  public boolean m_7898_(BlockState, LevelReader, BlockPos);                        // canSurvive
  public BlockState m_5573_(BlockPlaceContext);                                     // getStateForPlacement
  public void m_6861_(BlockState, Level, BlockPos, Block, BlockPos, boolean);       // neighborChanged
  public BlockEntity m_142194_(BlockPos, BlockState);                               // newBlockEntity
  public InteractionResult m_6227_(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult);
}
```

> 注：编译产物里被覆写的方法显示为 SRG 名（`m_5940_` …），这是 Forge 1.20.1 的正常现象
> —— 类名/类型名是 official，方法名在 jar 里被 FG 的 reobf 换成 SRG，运行期由 Forge 统一。

MTR 侧验证：`mtr.Items.BRUSH` 在 YMTR Forge jar 里的类型是
`mtr.RegistryObject<net.minecraft.world.item.Item>`，`.get()` 后可直接与 `ItemStack.is(Item)` 比较，
官方映射下无需任何 shim。

## 5. 迁移 200 个类的工作量评估

现状：`v1.20.1/src/main/java` 198 个类 + `common` 2 个 = **200**。

| 类别 | 数量（实测） | 难度 | 说明 |
|---|---|---|---|
| 纯方块 / 物品 / 注册 | 39 个文件含 `Registry.register` | 低 | 机械替换成 `DeferredRegister`；`FabricBlockEntityTypeBuilder` → `BlockEntityType.Builder.of`；创造页签 `ItemGroupEvents` → `BuildCreativeModeTabContentsEvent`（26 个文件用到 `itemgroup`） |
| BER（方块实体渲染器） | 30 个文件 | 中 | `BlockEntityRenderer` 接口在 Forge 是同一套官方类，但注册点变成 `BlockEntityRenderers.register(type, ctx -> new X(ctx))` + `@Mod(value=..., dist=CLIENT)`；`ItemRenderer`/`BlockEntityRenderDispatcher` 构造器差异要逐个改 |
| 网络包 | 12 个文件 | **高** | Fabric `PlayNetworking`/`PacketByteBuf`/`PlayerLookup` → `SimpleChannel`/`FriendlyByteBuf`/手写 `ServerLevel` 玩家遍历；4 个书包（clock / global / security / pids）要重写成 message 类 + `registerMessage` 编号，S2C payload 的 `sendToPlayer`/`sendToNear` 都要自己写 |
| Mixin | 8 个文件 | **高** | refmap 从 yarn（`class_xxxx` / `method_xxxx`）换 mojmap；`mtrlte.mtrdecor.mixins.json` → `mods.toml` 的 `[[mixins]] config=`；目标类在 YMTR Forge jar 里包名相同但方法名是 SRG/官方名，每个 `@Inject(method=...)` 都要重新对签名（MTR-ANTE 的 `RailRenderDispatcher` 这类更是要拿 Forge 版 jar 重新反编译核对） |
| 依赖 MTR 的类型 | 48 个文件 | 中–高 | `mtr.mappings.*` 在两版里**类名相同**（31 个 class 都在），但签名不同：Forge 版 `BlockEntityMapper` 暴露的是 `readCompoundTag/writeCompoundTag`，yarn 侧是 `readNbt/writeNbt`；`BlockEntityRendererMapper` 构造器吃 `BlockEntityRenderDispatcher`。另有 `mtr.client.ClientData`、`mtr.block.BlockPSDAPGDoorBase` 等 1826 个 MTR 类，逐个核对即可，不能盲改 |
| 资源 / 内置资源 | 760 个 JSON + 352 个 PNG，34 MB | 低–中 | blockstates/models/lang 原样可搬；`fabric.mod.json` → `mods.toml`+`pack.mcmeta`；`data/minecraft/tags/*` 格式一致；`assets/*/rawmodels` 的自定义加载器（`RawModelJson`/`RawObjParser`）是纯 Java，可直接复用 |
| 客户端初始化 / 事件 | 35 个文件用到 Fabric client API | 中 | `ClientModInitializer` → `@Mod(dist=CLIENT)`；`ClientTickEvents` → `TickEvent.ClientTickEvent`；`BlockRenderLayerMap` → `ItemBlockRenderTypes.setRenderLayer`；`KeyBindingHelper` → `RegisterKeyMappingsEvent` |
| GUI / Screen | `Screen` 类基本是原版 API | 中 | `Screen`/`ButtonWidget` 同名同包，主要改 `ScreenHandler`/`MenuType` 注册与 `ExtendedScreenHandlerType` 的替代 |

粗估（含编译—调试—对齐签名）：

- 机械类（方块/物品/注册/资源搬运，约 90 个文件）：**0.5–1.5 h/个** → 60–120 h
- BER（30 个）：**2–4 h/个** → 60–120 h
- 网络（12 个）：**4–8 h/个** → 50–100 h（含协议对齐与双端联调）
- Mixin（8 个）：**4–10 h/个** → 30–80 h（取决于 YMTR Forge 内部类是否可反编译）
- 其余（Screen/事件/杂项，约 60 个）：**1–3 h/个** → 60–180 h

合计 **≈ 260–600 人时**（1 人全职 6–14 周）。最大不确定性不在 MC API，而在：

1. **MTR 侧 API 的 Forge 实现**（`mtr.mappings.*` 语义差异、`mtr.client.*` 的 Forge 分支）；
2. **Mixin 目标**（YMTR Forge 内部方法名的 SRG 化，需逐条反编译核对）；
3. **网络层**（Fabric 的通道语义 → SimpleChannel，4 个自定义协议要重写而非改名）。

## 6. 踩到的坑（都已在工程里处理）

1. **maven.minecraftforge.net 直连不可用**：`curl -sI` 30 s 读超时（多次重试仍超时），
   FG 拉 `ForgeAutoRenamingTool` / `accesstransformers` 时直接失败。
   实测 `https://maven.creeperhost.net`（完整镜像 Forge maven）可用：同一个 pom/jar 全部 200，
   FG jar 508 KB / 6 s。→ `build.gradle` 镜像优先、官方源保留兜底；FG 自己注册的内部仓库用
   `forge/gradle-init-mirror.gradle` 改写 URL（不清空任何仓库）。
2. **Gradle 8.8 + FG 6 → `because no repositories are defined`**：两个 FG 版本（6.0.54 / 6.0.24）
   在 Gradle 8.8 下都报这个错（仓库明明配了），换成 8.1.1 后立刻通过。FG 6 的
   `detachedConfiguration` 依赖 Gradle 8.1 的行为。
3. **FG 6.0.54 不可用**：它要求 `net.minecraftforge:accesstransformers:8.2.14`，该坐标在 Forge
   maven 上是 404（1.20.1 的 userdev 只要 8.0.4）。→ 固定 6.0.24。
4. **mods.toml 中文乱码**：Groovy `expand` 按平台编码读文件 → `filteringCharset = 'UTF-8'`。
5. **根 wrapper 8.8 撑不起 Loom 1.11.8**（既有问题，本轮未改）：Fabric 侧需 Gradle ≥ 8.14。

## 7. 本轮验证记录

| 命令 | 结果 |
|---|---|
| `JAVA_HOME=jdk-17 ./forge/gradlew -p forge build`（Gradle 8.1.1 / FG 6.0.24） | `BUILD SUCCESSFUL in 26s`，产出 `forge/build/libs/lted-forge-3.0.0.jar`（30,205 B / 37 条目 / 6 个 class） |
| `gradle-8.14 -p v1.20.1 build`（Loom 1.11.8） | `BUILD SUCCESSFUL in 41s`，产出 `v1.20.1/build/libs/lted-3.0.0.jar`（19,073,594 B），Fabric 侧未改动 |
| 首次 FG 冷构建（MCP + patch + 反编译重编译） | 7m 47s（仅一次，之后增量 16–26 s） |

