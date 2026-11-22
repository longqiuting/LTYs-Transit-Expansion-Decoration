# 多加载器落地建议（2026-11-22，结论汇总）

> 依据：`docs/mtr3-forge-diff.md`（YMTR Forge 3.6.6.1 vs Fabric 3.6.4.2）、`docs/single-jar-multiloader.md`（ANTE 结构实测）、`docs/multiloader-feasibility.md`（耦合清单）。

## 1. 一个 jar 同时支持 Fabric + Forge：成立

- 样本：`MTR-ANTE-1.1.0+1.20.1.jar` —— 同 jar 内 `fabric.mod.json` + `META-INF/mods.toml`，`fabric/` 573 类 + `forge/` 577 类，两套 mixin 配置 + 两套 refmap，资源（`assets/`）只存一份在根。
- 打包工具：**Forgix 1.2.9**（ANTE MANIFEST 自证）；上游流程是 Architectury 出两个 jar → Forgix 合并。
- 关键认知：**不是"共享字节码 + 薄适配"，而是"同一份源码编译两遍"**。ANTE 566/577 类同名但字节不同（映射不同），无法去重。

## 2. MTR 3.6 两端差异：只差映射，不差 API

| 项 | Fabric 3.6.4.2 | Forge 3.6.6.1 |
|---|---|---|
| 包结构 | 扁平 `mtr.*` | 扁平 `mtr.*`（相同） |
| 类数 | 1799 | 1826（多 36：`mtr/forge/*` 胶水 + 新功能） |
| 同名同路径 | 1790 | 1790 |
| 映射 | intermediary（`class_` / `method_`） | official + SRG（`net/minecraft/world/...` / `m_xxxxx_`） |
| 共同类字节相同率 | 1308 / 1790 = **73.1%** | — |

结论：**源码共享成立**；LTED 用到的 65 条 `import mtr.*` 两端全在，13 个 `remap=false` 的 MTR mixin 目标方法在 Forge jar 里 13/13 存在（只是名字是 SRG）。

## 3. 真正的成本在 LTED 自己

- 300 文件 / 79k 行里 **236 个直接 import `net.minecraft`**（Yarn 名）；70 个用 Fabric API；129 处 `Registry.register`。
- 20 个 mixin 中 12 个打在 MTR 内部、描述符按 Yarn 写死 → Forge 侧要重新映射并各自生成 refmap。
- `common/` 41 个文件里只有 12 个完全不碰 MC。

## 4. 推荐路线（方案 A：双源码树 + Forgix 合并）

1. **确定映射策略**：两平台统一用 **Mojang official**（Loom 用 `officialMojangMappings()`，运行时仍 remap 到 intermediary）。这是唯一能让"同一份 common 源码"两端编译的路子；代价是一次性的 Yarn→official 源码迁移。
2. **工程骨架**：`common/`（无平台 API）+ `fabric/` + `forge/`（ForgeGradle 6）+ 顶层 `forgix` 任务；Loom 与 ForgeGradle 必须分项目（不能同 project，都抢 `minecraft` extension）。
3. **平台抽象**：把 70 个文件里的 Fabric API（`FabricItemGroup`、`ClientModInitializer`、`KeyBinding`、`ResourceManagerHelper`…）收进 `Platform` 接口，两端各实现一份。
4. **mixin 双份**：MTR 内部 12 个 + 原版 5 个，各自 refmap 与 `mixin` 插件字段；Forgix 合并时注意 `plugin` 字段改写（待实测）。
5. **资源**：`assets/` 只放一份（根目录），Forgix 不会复制到两棵树；内置资源包（第 5 条纹理分级）也放这里，天然两端共享。

**估算**：接入 2–4 天 + 平台抽象与映射迁移 1.5–2.5 人月；jar 从 18.9 MB 涨到约 21 MB。

## 5. 明确不推荐

- **方案 B 纯 Architectury**：产出的是**两个** jar，不满足"一个模组文件"。
- **方案 C 反射兼容层**：`@Mixin` 不能反射化，refmap 必须逐端生成，极限也就省掉一点胶水。

## 6. 待实测（动工前先验）

1. Loom 1.11.8 与 ForgeGradle 6 在 Gradle 8.8/8.14 的共存版本窗口；
2. Forgix 是否自动改写 mixin 的 `plugin` 字段（LTED 有 3 份插件）；
3. Forge 侧 `mtr.mappings.*` 签名一致性（只比对了类存在性，未逐方法）；
4. `BeamLightMixin` 在 Forge 的 `LevelRenderer#getLightColor` 上是否可达；
5. Yarn→official 源码迁移的自动化程度（能否用 tiny-remapper + 反编译批处理，还是必须人工）。
