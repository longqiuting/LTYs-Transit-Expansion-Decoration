# LTE — Forge 1.20.1 模块（M4a：工具链验证）

独立 root project，与 `../v1.20.1`（Fabric + Loom）互不引用。

```bash
# 在仓库根执行；ForgeGradle 6 需要 JDK 17，Gradle 用本目录自带的 8.1.1 wrapper
JAVA_HOME="C:/Program Files/Java/jdk-17" ./forge/gradlew -p forge build
# → forge/build/libs/lted-forge-3.0.0.jar
```

- 映射：`official`（mojmap）
- 已迁移并注册：`LogoBlock`、`ClockBlock` + `ClockBlockEntity`、`LtedTooltipItem`
- 编译期 MTR：`libs/MTR-forge-1.20.1-3.6.6.1.jar`（gitignored，需要时从附件拷进来）
- 完整说明、yarn→mojmap 对照表、工作量评估：`../docs/forge-m4a-toolchain.md`

版本组合（实测可用）：Gradle 8.1.1 + ForgeGradle 6.0.24 + Forge 1.20.1-47.4.0 + JDK 17。
