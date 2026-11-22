# LTE — Forge 1.20.1 模块（M4b-5：可用产物）

独立 root project，与 `../v1.20.1`（Fabric + Loom）互不引用。

```bash
# 在仓库根执行；ForgeGradle 6 需要 JDK 17，Gradle 用本目录自带的 8.1.1 wrapper
JAVA_HOME="C:/Program Files/Java/jdk-17" ./forge/gradlew -p forge clean build --offline
# → forge/build/libs/lted-forge-3.0.0.jar（1841 条目 / 19 MB）
```

- 映射：`official`（mojmap），`reobfJar` 把成员名改写成 SRG（生产环境格式）
- **资源**：`sourceSets.main.resources` 直接并入 `../v1.20.1/src/main/resources`
  → `assets/` 1248、`data/` 199，与 Fabric jar 逐文件对齐
  （唯一差异是 Forge 独有的 `assets/mtrlte/models/item/lte_tool.json`）。
  本目录 `src/main/resources` 只放 Forge 独有文件：`META-INF/mods.toml`、`pack.mcmeta`、
  `mtrlte.forge.mixins.json`、`lted.refmap.json`、`lte_tool.json`
- **语言合并**：`src/forge-lang/*.json` 里的 Forge 独有键在 `processResources` 时并进
  Fabric 的全量 `en_us/zh_cn`（不覆盖已有键）
- **入口**：`src/main/java/…/forge/LtedForge.java`（`@Mod("mtrlte")`）
  → `Reg`（DeferredRegister）→ `LteServerDecor` → 页签/BER/频道/命令事件；
  客户端部分在 `ClientBootstrap`（`Dist.CLIENT` 才加载）
- **mixin**：`mtrlte.forge.mixins.json`（1 通用 + 7 client）+ `lted.refmap.json`
  + jar 清单 `MixinConfigs`。8 个类里 7 个已在 MTR Forge 3.6.6.1 上核对过目标
  （`javap -p`）；天津 PIDS 那个附属模组的 mixin 无法本地核实，见文档里的 TODO
- 已迁移并注册：`LogoBlock`、`ClockBlock` + `ClockBlockEntity`、`LtedTooltipItem`
- 机器迁移基线：`src/main/java-gen/`（200 个 .java，由 `../tools/remap-to-mojmap.sh` 生成，
  重跑会整目录覆盖 —— 不要在里面手改；手改请同时加到 `../tools/fix-java-gen.js`）
- 编译期 MTR：`libs/MTR-forge-1.20.1-3.6.6.1.jar`（gitignored，需要时从附件拷进来）

文档：
- `../docs/forge-m4a-toolchain.md` —— 工具链与 yarn→mojmap 对照
- `../docs/forge-m4b2-mojmap-baseline.md` —— 反编译/重映射管线
- `../docs/forge-m4b5-resources-entrypoint-mixins.md` —— 资源、入口事件、mixin 目标核实表

版本组合（实测可用）：Gradle 8.1.1 + ForgeGradle 6.0.24 + Forge 1.20.1-47.4.0 + JDK 17。
