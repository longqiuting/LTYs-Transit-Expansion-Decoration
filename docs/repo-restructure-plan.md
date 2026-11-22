# GitHub 仓库重组方案（2026-11-22 记录，未执行）

现状：`github.com/longqiuting/mtr-external-lights`，141 个提交、5 个 tag（最新 `v2.0.2`）、`.git` 约 210 MB；仓库名与内容（装饰 + 优化）已经不符。

## 方案对比

| | A. 旧仓改名 + 新建一个（推荐） | B. 两个全新仓 |
|---|---|---|
| LTED | 旧仓 **rename → `lted`**，保留全部 history / issues / releases / stars | 新建 `lted`，只带一个 squash 导入提交 |
| LTE | 新建 `lte` 仓 | 新建 `lte` 仓 |
| 历史 | LTED 完整；LTE 从"拆分点"新建（可写清出处） | 两边都只有导入提交 |
| 现有发布 | Modrinth `S4MLXR8U` / CurseForge `1786143` 继续指向 LTED（同一产品延续，方块 id 不变），用户升级无感 | 老项目页要废弃或改指向，用户容易跟错 |
| 旧链接 | GitHub 对 rename 自动 301 重定向，旧 URL 仍可用 | 旧仓库只能存档，链接会断（除非手动 redirect） |
| 风险 | 低 | 中（发布页与用户认知断裂） |

## 推荐落地步骤（方案 A）

1. **旧仓改名**：`mtr-external-lights` → `lted`；顺带把默认分支、README、仓库描述、topics 一起换掉（`minecraft`, `fabric`, `forge`, `mtr`, `decoration`）。
2. **新建 `lte` 仓**：把拆出来的优化部分（灯光/光束/碰撞/轨道床/连接器）作为首个提交导入；README 里写明「本仓由 `lted` 的前身拆分而来」，并链接 LTED。
3. **代码搬迁**：因为 A/B 代码在同一批目录里交织（`common/` 与 `v1.20.1/.../` 都混着），按路径切不干净。做法是：在 LTED 仓里先完成物理拆分（把 A 的类移到新 source set / 新工程），再用「复制目录 + 新仓库首次提交」的方式搬到 `lte`，历史以文字说明 + 旧仓链接代替（不做虚假的 subtree 历史）。
4. **发布页归属**：
   - LTED 沿用 Modrinth `S4MLXR8U`、CurseForge `1786143`（mod id `mtrlte` 不变，用户存档安全）；
   - LTE 另开新项目（Modrinth/CF 新建），版本从 1.0.0 起，避免与旧版本号混淆。
5. **Yomi 接手 LTE**：拆分完成、`lte` 仓稳定后，先在 `lte` 仓加 Yomi 为 collaborator 试跑；确认后做 **repository transfer**（GitHub 支持转给个人/组织），转移后我们保留只读或协作者权限。不要用 fork 代替转移——fork 会让 issue/PR/发布分散。
6. **许可与致谢**：两个仓都要各自的 `LICENSE` / `NOTICE.md`；LTED 保留资源来源（MTR/MSD/RCC/南滨等），LTE 只保留与代码相关的部分（MTR 下游、YMTR 声明）。

## 需要在执行前确定

1. LTED 版本号从 **3.0.0** 起（标记拆分后的新世代），LTE 从 **1.0.0** 起 —— 还是沿用 2.0.x？
2. LTE 仓先放我们账号（后转 Yomi），还是直接建在 Yomi 的组织下、我们提 PR？
3. 旧仓是否保留 `lte` 相关 tag（`v0.2.x`/`v1.0.0` 属于优化线）？建议：tag 全部留在 LTED（历史完整性），LTE 从 1.0.0 重新开始。
