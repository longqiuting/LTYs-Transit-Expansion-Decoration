# M4b-1：Forgix 合并 Fabric + Forge 为单 jar（`lted-3.0.0.jar`）

结论：**链路通了**。一个 jar 同时带 `fabric.mod.json` 与 `META-INF/mods.toml`，
两套 class 树被 Forgix 真实重定位（不是拼接），共享资源只留根目录一份。

- 产物：`merge/build/libs/lted-3.0.0.jar`（19,084,176 B / 1755 条目，其中 336 个 class）
- 输入：`v1.20.1/build/libs/lted-3.0.0.jar`（19,073,594 B / 1746 文件）、
  `forge/build/reobfJar/output.jar`（30,187 B / 21 文件）
- 版本：Forgix **1.2.9**（与 `libs/MTR-ANTE-1.1.0+1.20.1.jar` manifest 里的
  `Forgix-Version: 1.2.9` 对齐）

## 一条命令

```bash
tools/merge-single-jar.sh                # Fabric build → Forge build → Forgix 合并
SKIP_BUILD=1 tools/merge-single-jar.sh   # 只重跑合并
```

实测三个步骤各自 `BUILD SUCCESSFUL`（25s / 23s / 32s），连跑两次产物条目数与字节数一致。

## JDK / Gradle 分工

| 步骤 | JDK | Gradle | 说明 |
|---|---|---|---|
| Fabric `build` | 21 | **8.14**（`D:/MC-MTR-JS/temp/gradle/gradle-8.14`） | Loom 1.11.8 要求 ≥8.14；仓库根 wrapper 是 8.8，直接用会报 "requires at least Gradle 8.14"（既有问题，本轮未改） |
| Forge `build` | 17 | 8.1.1（`forge/` 自带 wrapper） | ForgeGradle 6.0.24 只认这一代 |
| Forgix `mergeJars` | 21 | 8.14 | 插件跑在 Gradle daemon 里，不编译任何 Java；两侧产物都是 17 字节码，21 读得动 |

## 接入方式：Gradle 插件 + 独立 `merge/` build

用插件（`io.github.pacifistmc.forgix`，Gradle Plugin Portal 上有 1.2.9），但
**不把 `v1.20.1` / `forge` 变成子项目**——两套工具链绑死在不同 Gradle 版本
（8.14 vs 8.1.1），塞进一个 build 只会互相牵制。

Forgix 1.2.9 的 `MergeJarsTask` 是这样定位输入的（反编译确认）：
先 `rootProject.getAllprojects()` 里按 `projectName` 找到子项目，再用
`new File(project.projectDir, jarLocation)` 拼路径。所以 `merge/settings.gradle`
`include` 了两个**空壳子项目**，`projectDir` 指向真实目录、`buildFileName`
指向一个不存在的文件——只为提供 `projectDir`，真实构建脚本不求值：

```groovy
// merge/settings.gradle
rootProject.name = 'lted-merge'
include ':fabric'
include ':forge'
project(':fabric').projectDir = file('../v1.20.1')
project(':forge').projectDir  = file('../forge')
project(':fabric').buildFileName = 'build.merge-stub.gradle'   // 不存在的文件 = 空项目
project(':forge').buildFileName  = 'build.merge-stub.gradle'
```

```groovy
// merge/build.gradle（关键片段）
plugins {
    id 'io.github.pacifistmc.forgix' version '1.2.9'
}

// 版本号取自两侧 gradle.properties，两边不一致直接失败
def readProp = { String path, String key ->
    def p = new Properties()
    new File(projectDir, path).withInputStream { p.load(it) }
    p.getProperty(key)
}
def fabricVersion = readProp('../v1.20.1/gradle.properties', 'mod_version')
def forgeVersion  = readProp('../forge/gradle.properties',  'mod_version')
if (fabricVersion != forgeVersion) { throw new GradleException("...") }

forgix {
    group          = 'dev.js.mtrheadlight'
    mergedJarName  = "lted-${fabricVersion}.jar"   // → lted-3.0.0.jar，无 -forge 后缀
    outputDir      = 'build/libs'

    fabric {
        projectName = 'fabric'
        jarLocation = 'build/libs/lted-3.0.0.jar'      // 相对 ../v1.20.1
    }
    forge {
        projectName = 'forge'
        jarLocation = 'build/reobfJar/output.jar'      // 相对 ../forge，用 reobf 产物
    }
}

// 输入缺失就失败，避免产出空包
tasks.named('mergeJars') {
    doFirst {
        [ new File(project(':fabric').projectDir, 'build/libs/lted-3.0.0.jar'),
          new File(project(':forge').projectDir,  'build/reobfJar/output.jar') ].each {
            if (!it.isFile() || it.length() == 0) throw new GradleException("缺少输入产物：${it}")
        }
    }
}
```

`mergeJars` 本身**不 dependsOn 两侧 build**（插件没有这个能力），所以由脚本按序调用。
`forge/build/reobfJar/output.jar` 与 `forge/build/libs/lted-forge-3.0.0.jar` 实测
**字节一致**：ForgeGradle 6 + 1.20.1（official 映射）编译期就是 SRG 名
（`javap -c` 里能看到 `m_61124_`），reobf 是直通，不是空包。

## 合并产物实际内容

```text
$ unzip -l merge/build/libs/lted-3.0.0.jar | grep -E "fabric.mod.json|mods.toml|mixins|refmap|pack.mcmeta|MANIFEST"
      563  2026-11-22 21:25   fabric-lte.refmap.json
      573  2026-11-22 21:25   fabric-mtrlte.mtrdecor.mixins.json
     1854  2026-11-22 21:25   fabric.mod.json
      616  2026-11-22 21:25   META-INF/MANIFEST.MF
      918  2026-11-22 21:25   META-INF/mods.toml
       86  2026-11-22 21:25   pack.mcmeta

条目按顶层目录：assets 1217 / fabric 330 / data 190 / META-INF 7 / forge 6 /
pack.mcmeta 1 / icon.png 1 / fabric.mod.json 1 / fabric-*.mixins.json 1 / fabric-*.refmap.json 1
```

- 两套 class：`fabric/dev/js/mtrheadlight/**`（330）与 `forge/dev/js/mtrheadlight/forge/**`（6），
  顶层不再有裸 `dev/`；两条入口在 metadata 里同步改写成 `fabric.dev.js...`
- `fabric.mod.json`：`entrypoints` / `mixins` 已改写（`fabric-` 前缀），其余字段原样
- `META-INF/mods.toml`：原样（`version = 3.0.0` 由 Forge 侧 processResources 展开），
  `@Mod("mtrlte")` 注解在重定位后仍在 `forge.dev.js.mtrheadlight.forge.LtedForge` 上
- `META-INF/MANIFEST.MF`：Forge 的 `Specification-*`/`Implementation-*` + Fabric 的
  `Fabric-*` 属性 + `Forgix-Version: 1.2.9` 合并成一份
- `assets/`、`data/`、`pack.mcmeta`、`icon.png` 只有根目录一份；zip 无重复条目（`uniq -d` 为空）

### 重定位是真改名，不是搬文件

```text
$ javap -classpath <merged> fabric.dev.js.mtrheadlight.clock.ClockBlock
public final class fabric.dev.js.mtrheadlight.clock.ClockBlock
    extends net.minecraft.class_2248 implements net.minecraft.class_2343
# javap -c 里的同包引用：fabric/dev/js/mtrheadlight/clock/ClockBlockEntity、ClockObjects …

$ javap -classpath <merged> forge.dev.js.mtrheadlight.forge.block.ClockBlock
public final class forge.dev.js.mtrheadlight.forge.block.ClockBlock
    extends net.minecraft.world.level.block.Block implements net.minecraft.world.level.block.EntityBlock
# 同包引用：forge/dev/js/mtrheadlight/forge/block/ClockBlockEntity、…/LtedForge
```

`MTR` / `net.minecraft` 这类外部引用不动；只改 jar 内部的 `dev.js.mtrheadlight.*`。
`tools/javap-all.sh` 对全部 336 个 class 逐个 javap：**336 parsed / 0 stderr**（没有坏字节、没有空包）。

## 已知限制

1. **资源同名冲突：Fabric 侧胜出。**实测 `assets/mtrlte/lang/en_us.json`：Fabric 49,553 B、
   Forge 176 B → 合并后是 49,553 B（Fabric 的）。Forgix 只做「后写覆盖」，没有合并 JSON 的能力；
   Forge 侧想特化某个共享资源（lang、blockstate、模型）目前做不到，只能让两边资源路径不同，
   或者接受 Fabric 版。`pack.mcmeta` 反过来——Fabric jar 里没有，所以保留了 Forge 的那份。
2. **所有 class 都会被复制一份**（`fabric/` + `forge/` 两棵树）。共享逻辑没有去重机制；
   本轮代码量小（330+6）所以无所谓，M4b 迁完 PIDS/PSD 后 jar 会接近「Fabric 体积 × 2 − 资源」。
3. **Forge 侧目前没有 mixin 配置**，所以只产出 `fabric-mtrlte.mtrdecor.mixins.json` +
   `fabric-lte.refmap.json`（`package` / `plugin` / `refmap` 都已改成 `fabric.*`）。
   Forge 侧一旦有自己的 mixins，Forgix 会对称地产出 `forge-<名>.mixins.json` +
   `forge-<名>.refmap.json`——这条用**临时夹具**验证过（拿 Forge jar 塞一个空的
   `lted-forge.mixins.json` + refmap 再合并，得到 `forge-lted-forge.mixins.json`，
   `package` 变成 `forge.dev.js.mtrheadlight.forge.mixin`）。夹具只在 `%TEMP%` 里，没进仓库。
4. **Forge 的 mixin 加载入口**目前是空的：`mods.toml` 没有 `[[mixins]]`，
   manifest 也没有 `MixinConfigs`。真上 Forge mixin 时要在 `META-INF/mods.toml` 或
   manifest 里指到 `forge-...mixins.json`（ANTE 用的是 manifest `MixinConfigs`）。
   这属于 M4b-2，不在本轮。
5. **不是字节级可复现**：每次合并 zip 条目时间戳都是当前时间，sha256 会变；条目数/大小一致。
6. `mergeJars` 不构建依赖，忘了先跑两侧 build 会在 `doFirst` 断言处直接报错（不会产出空包）。
7. 两侧 `mod_version` 必须相同，否则 `merge/build.gradle` 在配置阶段就失败。

## 未改动

`v1.20.1/` 源码与构建脚本、`forge/` 源码，全部未动；只新增 `merge/`（构建配置）与
`tools/merge-single-jar.sh`、`tools/javap-all.sh`。
