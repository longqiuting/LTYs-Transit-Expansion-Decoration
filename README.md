# LTED (LTY's transit expansion — Decor)

**MTR（Minecraft Transit Railway）3.x 的装饰拓展模组** — Fabric，**1.20.1**。

基于 MTR 的列车 / 线路 / 站台数据，补上 MTR 本身没有的**站内装饰**：厦门风格的屏蔽门与站台方块、
PIDS / PSD-LCD 显示屏、发车倒数器、站外标志、时钟、售票机 / 闸机 / 充电宝 / 售卖机 / 安检机，
以及方速屏蔽门搬运版。

> 本仓库只做装饰。MTR 的灯光 / 光束 / 散射 / 碰撞 / 撞飞 / 道床判定等「优化」内容在独立工程
> **LTE**（`D:\MC-MTR-JS\lte`）里，两者可以同时安装，互不依赖。mod id 仍是 `mtrlte`，
> 方块 id 与拆分前完全一致（升级不会丢方块）。

- 仓库：<https://github.com/longqiuting/mtr-external-lights>
- 问题反馈：<https://github.com/longqiuting/mtr-external-lights/issues>
- 协议：MIT（屏蔽门部分移植自 MTR，同样 MIT）

---

## 功能

### 屏蔽门与站台

- 全高屏蔽门（PSD）与半高屏蔽门（APG），**1 / 2 / 3 / 4 / 6 号线**各一套
- 行为完整继承 MTR：列车到站自动开门、刷子切线路指示方向、剪刀切形态、门灯（1/2/3 橙色、4/6 绿色）、
  站台方块自动切换模型
- 线路条带按厦门实拍重绘：空心圆环站点、换乘图标（追逐箭头 + 线路牌）、终点三角箭头、45° 站名、
  颜色跟随线路（可用 `config/mtrlte_decor.json` 的 `lineColors` 手工覆盖）
- 大理石 / RCC 站台方块
- 方速（fangsu）屏蔽门搬运版：5 款旋转端门 + 2 款厦门滑动门，几何 / 贴图随 jar 走，**不需要装方速**

### 显示屏与发车倒数器

- 厦门 PIDS：地铁 / BRT 两款，广告表走资源包，支持全局广告集
- PSD-LCD 顶板挂板（1~N 格横向连成一条）
- 挂墙 / 挂顶发车倒数器（`dep_on_wall` / `dep_on_roof`）与挂杆

### 标志与设备

- 厦门 / 角美 / LTE logo 方块与贴墙标识、四件站外标志（挂墙 / 杆上）
- 时钟（代码画钟面 + 刷子配置 + 整点报时 + 红石控制）
- 售票机 / 余额查询机 / 闸机（进站 / 出站）/ 共享充电宝 / 饮料售卖机 / 液体安检机
- eyecandy：厦门风格物件包 26 件 + 南普陀 34 件（运行时 `.obj` 几何）
- 墙面与玻璃扶手

---

## 安装

1. 安装 **Fabric Loader** 与 **Fabric API**
2. 安装 **MTR 3.x**
3. 把 `lted-<version>.jar` 放进 `mods/`
4. 可选：**Mod Menu**（模组列表里的「配置」按钮会打开 LTED 仪表板）

服务器与客户端都装即可；服务端负责方块注册与数据，客户端负责渲染。

---

## 常用指令

主指令 `/mtr_lte`（旧别名 `/externallights`），无参数打开 LTED 仪表板。

```text
/mtr_lte dep                    倒数器读数方向翻到下一档
/mtr_lte dep status|next|normal|mirror|flip|rotate
/mtr_lte dep debug [on|off|status]   倒数器取证日志
```

其余配置都在**刷子右键**方块（单块 NBT）与 LTED 仪表板（PIDS / 时钟 / 装饰配色）里。

---

## 构建

需要 JDK 21 与 Gradle 8.14（工程自带 wrapper 是 8.8，fabric-loom 1.11.8 要求 ≥8.14）。

```bash
JAVA_HOME="C:/Program Files/Java/jdk-21.0.10" \
  "/d/MC-MTR-JS/temp/gradle/gradle-8.14/bin/gradle" -p v1.20.1 clean build --no-daemon
```

产物：`v1.20.1/build/libs/lted-<version>.jar`。

---

## 目录结构

```
common/    与版本无关的两端共享代码（LteLog / HostProbe）
v1.20.1/   1.20.1 工程：方块 / 渲染 / 界面 / 资源
libs/      MTR / ANTE / Mod Menu 的编译期 jar（不进分发物）
docs/      设计与排障文档
tools/     构建、发布、校验脚本
```

---

## 已知限制

- 活塞 / 爆炸 / `/setblock` 移除绑定方块不会触发自动解绑（只会空屏，不会崩）
- CurseForge 上传受网络限制，暂只发布 GitHub / Modrinth
