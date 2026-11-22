# 单 jar 双加载器（Fabric + Forge）结构实测与 LTED 可行性

> 对象：`D:\MC-MTR-JS\mtr-headlight`（LTED，Fabric，`v1.20.1` 为主）。
> 样本：`libs/MTR-ANTE-1.1.0+1.20.1.jar`（22.9 MB，已知单 jar 双端）、`libs/MTR-hANTE-1.1.4+1.20.1-universal.jar`、`libs/MTR-fabric-1.20.1-3.6.4.2.jar`、`attachments/MTR-forge-1.20.1-3.6.6.1.jar`。
> **本文只做取证，未改动任何代码。** 估算单列并标注「估算」。

---

## 0. 结论（一句话）

**能。** 一个 jar 里同时放 `fabric.mod.json` + `META-INF/mods.toml`、两套完整 class 树（`fabric/` 与 `forge/` 各自带自己的 mixin 配置和 refmap）、资源只放一份——这就是 ANTE / MTR 的真实做法，工具是 **Forgix**（`Forgix-Version: 1.2.9`，写在 MANIFEST 里）。
**代价不是"打包"，而是"编译两遍"**：ANTE 的 577 个类里 **566 个按名字重复**（96.9%），平台特化只有 18 个（3.1%）。对 LTED 就是"再做一棵 Forge 树"，`v1.20.1` 单版 **估算 1.5–2.5 人月**（其中 mixin 复核占 1/3），外加 Forgix 接入 **2–4 天**；打包成单 jar 本身不是瓶颈。

---

## 1. ANTE 结构实测表

### 1.1 根目录并存的两套元数据（实测 `unzip -l`）

| 项 | ANTE 1.1.0 | ANTE 1.1.4（hANTE 分支） | 说明 |
|---|---|---|---|
| `fabric.mod.json` | 931 B | 977 B | Fabric 入口在这里 |
| `META-INF/mods.toml` | 1 011 B | 1 060 B | Forge 入口在这边（`modLoader="javafml"`, `loaderVersion="[36,)"`） |
| `fabric-mtrsteamloco.mixins.json` | 1 268 B | 1 399 B | `package: fabric.cn.zbx1425...mixin` |
| `forge-mtrsteamloco.mixins.json` | 1 266 B | 1 397 B | `package: forge.cn.zbx1425...mixin` |
| `fabric-mixins.mtrsteamloco.refmap.json` | 11 445 B | 13 367 B | intermediary 名 |
| `forge-mixins.mtrsteamloco.refmap.json` | 12 689 B | 14 723 B | official + SRG 名 |
| `fabric-mtrsteamloco.accesswidener` | ✅ | ✅ | Fabric 侧扩权 |
| `META-INF/accesstransformer.cfg` | 618 B | ✅ | Forge 侧扩权 |
| `pack.mcmeta` | 84 B | ✅ | **只在根目录一份** |
| `META-INF/MANIFEST.MF` | `MixinConfigs: forge-mtrsteamloco.mixins.json` | 同 | Forge 靠 MANIFEST 发现 mixin；Fabric 靠 `fabric.mod.json` 的 `mixins` 数组 |

两套元数据、两套 mixin 配置的**类清单完全一致**（都是 23 个 client + 14 个 common = 37 个 mixin 类），差异只有 `package`、`refmap`、文件名。

### 1.2 三棵 class 树（`unzip -l` 全量统计）

| 前缀 | ANTE 1.1.0 | 大小（解压） | ANTE 1.1.4 | 性质 |
|---|---:|---:|---:|---|
| `fabric/cn/zbx1425/mtrsteamloco/**` | **573** 类 | 3.07 MB | 596 类 | Fabric 侧**完整**产物（含 mixin） |
| `forge/cn/zbx1425/mtrsteamloco/**` | **577** 类 | 3.17 MB | 600 类 | Forge 侧**完整**产物（含 mixin） |
| `vendor/cn/zbx1425/mtrsteamloco/**` | **468** 类 | 2.69 MB | 476 类 | **共享**：被打进来的第三方库（Rhino / mozilla classfile） |
| 合计 | 1 618 类 | 8.93 MB | 1 672 类 | jar 总 21.8 MB（其中 16.5 MB 是 `noto-sans-cjk-tc-medium.otf`） |

### 1.3 两份树的差异 = 平台特化只有 18 个类

把两棵树去掉前缀后 `comm` 比对：

| 类别 | 数量 | 类 |
|---|---:|---|
| 同名（两棵树各一份，重复编译） | **566** | 全部业务类、全部 mixin、全部 `data/`、`mappings/RegistryUtilities` 等 |
| 仅 fabric | **7** | `fabric/MainFabric`、`MainFabricClient`、`ModMenuApiImpl`、`RegistriesWrapperImpl`、`mappings/FabricRegistryUtilities`、+2 个 cloth-config demo |
| 仅 forge | **11** | `forge/MainForge`、`ClientProxy`(+2 内部类)、`RegistriesWrapperImpl`、`mappings/ForgeUtilities`(+5 内部类) |

**结论：双加载器不是"共享一份代码 + 薄适配层"，而是"两份编译产物 + 3% 的薄适配层"。**

### 1.4 同名类不是字节相同（不可去重）

```
RailExtraSupplier   DIFFERENT   fabric 4096 B / forge 4087 B
ShapeSerializer     DIFFERENT   fabric 4998 B / forge 5261 B
EyeCandyProperties  DIFFERENT   fabric 1777 B / forge 1813 B
```

原因是常量池里的 MC 名字不同 —— 见 §1.5。Forgix 的自动去重只对**字节完全相同**的类生效，因此只有第三方库进了 `vendor/`（`org/mozilla` 435 个类，`fabric/` 和 `forge/` 下 0 个）。

### 1.5 为什么"两套映射都能跑"——没有共享 MC 代码，各编译各的

同一个 `Main` 类，两棵树里的 MC 引用：

| 树 | 引用 |
|---|---|
| `fabric/.../Main.class` | `net/minecraft/class`（intermediary，`class_xxxx`） |
| `forge/.../Main.class` | `net/minecraft/core/BlockPos`、`net/minecraft/world/item/Item`、`net/minecraft/network/FriendlyByteBuf` …（**official/mojmap 类名**） |

refmap 里更清楚：

```
fabric: "pixels": "field_5200:Lnet/minecraft/class_1011;"
forge:  "pixels": "f_117977_:Lcom/mojang/blaze3d/platform/NativeImage;"
```

即 **Forge 1.20.1 = 类名用 Mojang official + 成员名用 SRG（`f_`/`m_` 前缀）**；Fabric = intermediary。

> 所以"共享部分如何做到两套映射都能跑"的答案是：**没有共享**。唯一的例外是第三方纯 Java 库（字节相同，可被 Forgix 抬到 `vendor/`）。

### 1.6 资源只有一份

`assets/` 在根目录 **265 条**，`fabric/` 与 `forge/` 下的 `assets/` 条目 **0**。locale、贴图、ogg 全部共享 —— Forgix 只把 **class** 分包，资源留在根。

---

## 2. 它为什么能跑（运行期机制，非玄学）

1. **JVM 惰性链接**：`fabric/` 下的类在 Forge 上永远不会被 `ClassLoader` 解析，因为它们只被 Fabric 入口引用；反之亦然。Forgix README 原话：*"by altering the packages slightly, we can make it such that each modloader calls its own package and does not interfere with other modloaders"*。
2. **每端只读自己的元数据**：Fabric 读 `fabric.mod.json` 的 `entrypoints`（值已被重写成 `fabric.cn.zbx1425...MainFabric`），Forge 走 `mods.toml` + 扫全 jar 找 `@Mod`。实测 `forge/cn/zbx1425/mtrsteamloco/forge/MainForge.class` 上确有 `net.minecraftforge.fml.common.Mod(value="mtrsteamloco")`。
3. **Forge 的扫描是 ASM 只读，不加载类**：`ModFileScanData` 扫字节码找注解，不会触发 `fabric/` 那半边的类初始化 —— 这是双份 class 树在 Forge 上安全的关键。

---

## 3. ANTE 的构造链：Architectury 双树 → Forgix 合并

证据链：

| 证据 | 含义 |
|---|---|
| `MANIFEST.MF` 里 `Forgix-Version: 1.2.9` | 用 Forgix 合并 |
| `mods.toml` 声明 `dependencies: architectury >= 1.26.37` | 上游是 Architectury 工程 |
| Fabric 侧 `mappings/FabricRegistryUtilities`、Forge 侧 `mappings/ForgeUtilities` | Architectury 的 `RegistryUtilities` 双实现模板 |
| 两套 `fabric.mod.json` / `mods.toml` 已由 Forgix 改写（入口带 `fabric.` 前缀） | 合并时重写了元数据 |
| refmap 的 **key** 也是 `fabric/cn/...` / `forge/cn/...` | 合并时重写了 refmap 的类名 key |
| `vendor/` 只有一个 `org/mozilla`，`fabric/`+`forge/` 下 0 个 | 字节相同的类被抬到共享前缀 |

**对照 MTR 自己**（同样是 Architectury 工程，但**不**合并）：

| | MTR-fabric 3.6.4.2 | MTR-forge 3.6.6.1 |
|---|---|---|
| 元数据 | `fabric.mod.json` | `META-INF/mods.toml` + `MANIFEST: MixinConfigs` |
| 主包 | `mtr/**` 1 799 类 | `mtr/**` 1 826 类 + `mtr/forge/**` 6 类 |
| 跨端抽象层 | `mtr/mappings/FabricRegistryUtilities` | `mtr/forge/mappings/ForgeUtilities` |
| mixin | `mtr.mixins.json`（5 个 client） | `mtr.mixins.json`（**4 个 client**：少了 `EntitySpawnMixin`/`UnknownPacketMixin`） |
| refmap | `mtr-refmap.json` | `mtr-refmap.json`（同名，内容不同） |

→ **即使是"有跨端抽象层"的项目，`mtr.mappings` 也照样整包编译两遍**；抽象层省的是*写代码*，不是*class 树*。MTR 选择出两个 jar，ANTE 选择用 Forgix 合成一个；两者内部结构完全相同。

---

## 4. LTED 基线（实测）

| 项 | 数值 | 命令 |
|---|---|---|
| `v1.20.1` Java 文件 / 行 | **300 / 79 238** | `find`+`wc -l` |
| `common` Java 文件 / 行 | 41（合计 96 125 行） | 同上 |
| 引用 `net.minecraft.*` 的文件 | **236 / 300（79%）** | `grep -rl '^import net\.minecraft'` |
| `net.minecraft` import 行 | 1 678（163 种类型） | 同上 |
| 引用 `net.fabricmc.*` 的文件 | **70**（154 行） | `grep -rl '^import net\.fabricmc'` |
| 有 MC 依赖但**无** Fabric 依赖的文件 | **178** | `comm` |
| `common` 里无 Fabric 依赖 | 35 / 41 | |
| `common` 里**完全不碰** `net.minecraft` 且不碰 Fabric（纯 Java） | **12 / 41** | 见 §7 方案 C |
| `@Mixin` 类 | **20** | |
| `remap = false` 出现次数 | **45** | 打在 MTR / 方速 / 天津内部 |
| mixin 配置 | **8** 份，共用 1 份 `lte.refmap.json` | |
| 带 `plugin` 的配置 | 3（`MtrMixinPlugin`×2 + `FangsuMixinPlugin`） | |
| 引用 `mtr.*` 的文件 | 96（其中 17 用 `mtr.mappings`） | |
| 可能产物 | **549 类 / 2.84 MB 解压**；资源 66.41 MB 解压；jar **18.91 MB** | `build/libs/lte-2.0.2.jar` |
| Gradle wrapper | 8.8；Loom 1.11.8；yarn 1.20.1+build.10 | |

**对比 ANTE 的关键比例**：ANTE 单树 577 类里 566 类要双份 → 双份成本 ≈ 2×3.1 MB。LTED 是 549 类 / 2.84 MB，双份后 jar 估算 **20–21 MB**（资源不翻倍），**体积完全不是问题**。

---

## 5. 三个方案

### 方案 A：单 jar = 两套完整编译产物 + 共享 assets/lang

**做法**（就是 ANTE 的做法）：
1. 新起一个独立的 Forge 工程（1.20.1，FG6，official+SRG），把 `common` + `v1.20.1` 按 Forge 映射/API 重写编译成 `forge 树`；
2. Fabric 树保持不变；
3. Forgix 把两个 jar 合成一个：`fabric()` + `forge()` + 共享 `packageGroup`，产出 `lte-<ver>-universal.jar`。

| 维度 | 评估 |
|---|---|
| 可行性 | **高**。样本已证明在 1.20.1 上成立，且两个不同作者的版本都用同一套结构 |
| 工作量（估算） | Forge 树 1.5–2.5 人月（借 `docs/multiloader-feasibility.md` §4b 的单树成本）+ Forgix 接入 2–4 天 + 每版回归 2–3 天 |
| class 冲突 | **无**（两棵树的包名互不相交）。前提是**必须**分包：`fabric.dev.js...` / `forge.dev.js...` |
| mixin 目标名 | 两端各写各的：`remap=true` 那 5 个原版 mixin 要用 official 名重写（`WorldRenderer`→`LevelRenderer`、`MatrixStack`→`PoseStack`…）；`remap=false` 那 15 个打在 MTR 内部的，描述符里的 MC 类型名也要逐条按 SRG 重写 |
| 资源重复 | 无（Forgix 只分包 class，资源留根）。**注意别把 assets 放进 `fabric/`、`forge/`** |
| 维护成本 | **高**：每加一个功能要在两棵树各写一次；但这就是现状 `v1.18.2/1.19.2/1.19.4/1.20.1` 的既有模式，团队已习惯 |

### 方案 B：Architectury（common + fabric + forge）

| 维度 | 评估 |
|---|---|
| 产出 | **两个 jar**，不是单 jar。要单 jar 还得叠 Forgix（= 方案 A 的打包步骤） |
| 可行性 | 中。理论优雅，但 LTED 的 236 个文件全是 yarn 名，Architectury 惯例要求两端统一 official → **全量映射改写** |
| 工作量（估算） | 3–5 人月/版（`multiloader-feasibility.md` §4a）；且 Architectury **不解决 mixin**：20 个 mixin 仍需两份 refmap、两份 `remap=false` 描述符 |
| 风险 | ① 12 个打在 MTR 内部的 mixin 依赖 MTR 双端同签名 ② 68 文件 / 11 个 Fabric API 模块要换成 Architectury API 或双端实现 ③ 133 处 `Registry.register` → `DeferredRegister` |

> 关键认知：**Architectury 只省"写"，不省"编译"**。ANTE 就是 Architectury 工程，产物照样是两棵完整的树（§3）。所以「上 Architectury 就能单 jar」是误解 —— Architectury 给的是两 jar，Forgix 才给单 jar。

### 方案 C：单 jar + 反射兼容层（共享 common 不碰 `net.minecraft` 类型）

| 维度 | 评估 |
|---|---|
| 可行性 | **低**。`common` 41 个文件里只有 **12 个**完全不碰 `net.minecraft`；`v1.20.1` 有 **236/300** 文件直接 `import net.minecraft.*`（字段、参数、泛型、`extends`） |
| 变成反射的代价 | 549 个类要逐个改写成 `MethodHandle`/`Class.forName`，且 `@Mixin` **无法反射化**（mixin 必须在编译期确定目标类和注入点），refmap 也必须逐端生成 |
| 工作量（估算） | ≥ 6 人月，且换来不可读、无编译期检查、启动期反射开销 |
| 结论 | **不成立**。就算做到极限，单 jar 里**仍然**有两份 mixin 类 + 两份 refmap + 两个入口 —— 反射只能勉强共享"业务逻辑"，而业务逻辑正是被 `net.minecraft` 类型绑得最死的部分 |

---

## 6. 方案对比总表

| | A 双树 + Forgix | B Architectury | C 反射兼容层 |
|---|---|---|---|
| 单 jar？ | ✅ | ❌（要再叠 Forgix） | ✅ |
| 可行性 | 高（有同类样本） | 中 | 低 |
| 工作量（1.20.1，估算） | **1.5–2.5 人月** + 2–4 天 | 3–5 人月 + 2–4 天 | ≥6 人月 |
| 类加载冲突 | 无（分包） | 无 | 无 |
| mixin 目标名 | 两套描述符 | 两套描述符 | 两套（不可避免） |
| refmap | 2 份 | 2 份 | 2 份 |
| 资源重复 | 无 | 无 | 无 |
| 包体增量 | +~3 MB | +~3 MB | +~3 MB |
| 维护成本 | 每功能 ×2 | 每功能 ×1.3 | 每功能 ×1（但代码不可读） |
| 与现有 4 版树的关系 | **一致**（现状就是 N 棵树） | 需重排目录 | 需重排目录 |

---

## 7. 构建工具链（问题 3）

### 7.1 Loom 与 ForgeGradle 不能同 project

两者都在 project 上创建 **同名 `minecraft` extension 与 `minecraft` configuration**（Loom 的 `minecraft { ... }` 与 FG 的 `minecraft { ... }`）。同 project 应用两个插件会直接抛 `Cannot add extension with name 'minecraft'`。因此：

- ✅ **多项目**：`settings.gradle` 里 `include ':fabric', ':forge', ':common'`，各自应用自己的插件；
- ✅ **复合构建**：`includeBuild` 也行（Architectury / MultiLoader 模板的做法）；
- ❌ **单 project 双插件**：不可能。

### 7.2 版本窗口（需实测）

- 仓库 wrapper 是 **Gradle 8.8**，Loom 1.11.8 在此版本下已产出 `lte-2.0.2.jar`；
- ForgeGradle 6（`net.minecraftforge.gradle [6.0,6.2)`）官方要求 Gradle 8（迁移文档写 8.1.1）；FG6 在新 Gradle（8.11+）上有已知兼容问题；
- **两者共用同一个 wrapper 时，版本窗口需要实测确认**（8.8 大概率同时满足；若否，退 Loom 1.9/1.10 或分 build）。这是唯一一个我未实测、但会影响方案落地的点。

### 7.3 refmap：两端必须各一份

| | Fabric | Forge |
|---|---|---|
| 生成方式 | Loom `loom.mixin.defaultRefmapName` | MixinGradle / FG 的 `reobf` |
| 映射 | dev(yarn/mojmap) → **intermediary** | dev → **official 类名 + SRG 成员** |
| 文件名 | `lte.refmap.json` | **必须另起名**（ANTE 用 `forge-*.refmap.json`） |

实测证据：ANTE 两份 refmap 的 key 也被改写成 `fabric/...` 与 `forge/...`。**如果手工合并 jar，这一步最容易漏**：refmap 的 key 是 mixin 类的全限定名，分包后必须同步改写；Mixin 配置里的 `package`、`plugin` 字段同理（LTED 有 3 份配置带 `plugin`，Forgix 是否自动改写 `plugin` 字段**需实测**）。

### 7.4 映射隔离：不要混 yarn 与 official

- Loom 支持 `loom.officialMojangMappings()`。**若要做双端共享源码，两端应统一到 official（mojmap）**，让各自 build 的 remap 阶段（intermediary vs SRG/reobf）去产生各自的运行时名字。Architectury 惯例正是如此。
- LTED 现状是 yarn，且 `libs/MTR-fabric-1.20.1-3.6.4.2-named.jar` 是 `tools/remap-mtr-named.sh` 现生成的 **yarn 名** MTR。切到 official 意味着 **MTR 也要重新 remap 一份 mojmap 名**（tiny-remapper 可做，但要重跑整套脚本与 mixin 目标复核）。
- **"官方映射与 yarn 的冲突怎么隔离"的直接答案：不隔离，二选一。** 一个编译单元只能有一份 MC 签名；双份 class 树正是因为"两个编译单元各选一套"。

---

## 8. 推荐

### 8.1 一句话

**要单 jar 就得编译两遍；ANTE 的 566/577 重复率说明这是结构性的，不是实现取巧。** 所以：**方案 A（两套完整产物 + Forgix）是唯一现实路径**，方案 B 是它的"更省写代码的前置步骤"，方案 C 不成立。

### 8.2 落地顺序（若决定做）

1. **先只做 `v1.20.1`**（YMTR 3.6.x 有 Forge 版，依赖不缺）。
2. 新起 `forge/` 子项目：FG6 + official/SRG + MTR-forge jar 作 `compileOnly`。
3. 把 `common` + `v1.20.1` 的源码**复制**过去，按 Forge API 逐模块替换（§2.3 的 11 个 Fabric 模块映射表已备在 `docs/multiloader-feasibility.md`）。
4. 20 个 mixin 逐条 `javap` 复核 Forge 侧签名，产出 `forge-lte.refmap.json`。
5. 根项目加 Forgix，`fabric()` + `forge()` 合成 `<ver>-universal.jar`。
6. 双端各跑一次：Forge 端重点验 `BeamLightMixin`（`WorldRenderer#getLightmapCoordinates` → `LevelRenderer#getLightColor`）—— 这条不成立则整个光束卖点消失。

### 8.3 代价

- 单版 1.5–2.5 人月（估算），**且此后每个新功能 ×2**；
- jar 增大约 3 MB（18.9 → ~21 MB），可接受；
- 额外维护 8 份 mixin 配置（Fabric）+ 8 份（Forge）+ 2 份 refmap。

---

## 9. 落地前必须实测的 6 项（本文未覆盖）

| # | 待验 | 影响 |
|---|---|---|
| 1 | Loom 1.11.8 与 FG6 在同一 Gradle wrapper 下的版本窗口 | 决定多项目还是分 build |
| 2 | Forgix 是否自动改写 mixin 配置里的 `plugin` 字段（LTED 有 3 份） | 不自动 → 手工双写 |
| 3 | Forge 侧 MTR 3.6.6.1 的 `mtr.mappings.*` 成员与 Fabric 3.6.4.2 是否签名一致 | LTED 有 17 个文件用 `mtr.mappings` |
| 4 | `BeamLightMixin` 在 `LevelRenderer#getLightColor` 上的可达性 | 核心功能生死 |
| 5 | 方速 / 天津 / JCM 的 Forge 包名前缀 | 5 个软目标 mixin |
| 6 | Forge 端 `mods.toml` 的 `mixin` 声明方式（MANIFEST `MixinConfigs` vs `mods.toml`） | 合并后 mixin 是否加载 |

---

## 10. 复现命令

```bash
# 双元数据并存
unzip -l libs/MTR-ANTE-1.1.0+1.20.1.jar | grep -Ei 'fabric.mod.json|mods.toml|mixins.*json|refmap|access'

# 三棵树的类数/体积
unzip -l libs/MTR-ANTE-1.1.0+1.20.1.jar |
  awk '$4 ~ /\.class$/ {n=$1; split($4,a,"/"); c[a[1]]++; s[a[1]]+=n}
       END {for (i in c) printf "%-8s %5d classes %7.2f MB\n", i, c[i], s[i]/1048576}'

# 平台特化面（566 同名 / 7 fabric-only / 11 forge-only）
unzip -l libs/MTR-ANTE-1.1.0+1.20.1.jar | awk '$4 ~ /\.class$/ {print $4}' > /tmp/a.txt
comm -23 <(grep '^fabric/' /tmp/a.txt|sed 's#^fabric/##'|sort) \
         <(grep '^forge/'  /tmp/a.txt|sed 's#^forge/##' |sort)

# 两套映射的证据
unzip -p libs/MTR-ANTE-1.1.0+1.20.1.jar fabric-mixins.mtrsteamloco.refmap.json | head -c 600
unzip -p libs/MTR-ANTE-1.1.0+1.20.1.jar forge-mixins.mtrsteamloco.refmap.json  | head -c 600

# 共享库被抬到 vendor/
unzip -l libs/MTR-ANTE-1.1.0+1.20.1.jar | grep 'org/mozilla' | awk '{print $4}' | cut -d/ -f1 | sort -u

# 资源不按包分（应输出 0）
unzip -l libs/MTR-ANTE-1.1.0+1.20.1.jar | awk '{print $4}' | grep -cE '^(fabric|forge)/.*assets/'

# Forge 入口真带 @Mod
"C:/Program Files/Java/jdk-17/bin/javap" -v -p forge/cn/zbx1425/mtrsteamloco/forge/MainForge.class | grep -A3 Mod

# LTED 基线
grep -rl '^import net\.minecraft' --include=*.java v1.20.1/src | wc -l      # 236
grep -rl '^import net\.fabricmc' --include=*.java v1.20.1/src | wc -l      # 70
unzip -l v1.20.1/build/libs/lte-2.0.2.jar | awk '$4 ~ /\.class$/ {s+=$1;c++} END {print c, s/1048576}'  # 549 2.84
```
