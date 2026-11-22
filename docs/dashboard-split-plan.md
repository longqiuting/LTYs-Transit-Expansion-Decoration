# 仪表板拆分方案（2026-11-22 记录，未动工）

## 现状

LTE 仪表板（`ConfigScreen` → `CategoryScreen` → `ParameterScreen`）的「其他」分区里，**混进了 3 个属于 LTED 的入口**（`ParameterScreen.java:548-556`）：

| 行 | 打开 | 归属 |
|---|---|---|
| 厦门 PIDS 设置 | `PidsXiamenSettingsScreen` | **LTED** |
| 全局广告集 | `PidsXiamenAdSetScreen` | **LTED** |
| 时钟全局配置 | `ClockConfigScreen`（全局模式） | **LTED** |

其余分区（光源 / 碰撞 / 粒子 / 伤害）全部是 LTE 的优化项，不涉及 LTED。

## 拆分目标

- **LTE 仪表板**只保留优化项：光源、碰撞、粒子、伤害、其他（内置车型屏蔽、重载）。
- **LTED 自建入口**，承载上述 3 项 + 现有的方块级配置（刷子右键）：PIDS/PSD-LCD、时钟、广告集、安检机、售卖机、充电宝机、装饰物件。

## LTED 入口方案（建议）

新建 `mtrlte:lted_dashboard`（物品，放「厦门地铁物件」页签靠前位置），右键打开 `LtedDashboardScreen`，分区：

| 分区 | 行 | 备注 |
|---|---|---|
| 显示屏 | 厦门 PIDS 设置 / 全局广告集 / PSD-LCD 全局面板参数 | 从 LTE 迁过来 |
| 时钟 | 时钟全局配置 | 从 LTE 迁过来 |
| 设备 | 安检机 / 售卖机 / 充电宝机 的全局项 | 新增（如无全局项则显示"单块用刷子配置"） |
| 其他 | 版本、内置资源包开关（纹理分级那条） | 新 |

**不做的**：不给 LTE 加"可选扩展点"API 让 LTED 反注仪表板——两边是独立模组，跨模组 UI 注入会把耦合又装回去，且 Yomi 接手 LTE 后更难维护。

## 连带要改的东西

1. `HeadlightConfig` 里的 `lineColors` / `doorLight*` / `parseColor`（约 120 行）→ 移到 LTED 自己的配置类与配置文件；LTE 的 `config/lte.json` 不再写这些键。
2. 语言键：`screen.mtrlte.pids_xiamen` / `pids_ad_set` / `clock_global` 等挪到 LTED 命名空间；LTE 侧删键。
3. 物品 id：`mtrlte:lte_dashboard` 属于**优化侧**，拆分后应为 `lte:lte_dashboard`（老创造栏物品会消失，属可接受损失——它是纯工具物品，不是方块）。
4. 创造页签：LTE 页签只留 仪表板 / 灯光编辑器 / 连接器；LTED 页签「厦门地铁物件」开头两位是 logo 方块与贴墙标识，往后才是各设备。

## 迁移风险

- 玩家存档里没有依赖仪表板物品掉落/合成，丢失影响仅限创造栏重新拿一次。
- 已有 `mtrlte` 配置文件中被 LTED 使用的键（线路色、门灯）**必须保留读旧键 + 写新文件**，否则升级后配色重置。
