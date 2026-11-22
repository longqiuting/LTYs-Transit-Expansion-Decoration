#!/usr/bin/env bash
# M4b-1：一条命令产出「同时可装 Fabric 与 Forge」的单 jar。
#
#   tools/merge-single-jar.sh                # 全量：Fabric build → Forge build → Forgix 合并
#   SKIP_BUILD=1 tools/merge-single-jar.sh   # 只重跑合并（两侧产物已在）
#
# 产物：merge/build/libs/LTYs-Transit-Expansion-Decoration-3.0.0.jar
#
# JDK / Gradle 分工（三个步骤各自不同，别混）：
#   Fabric  build : JDK 21 + Gradle 8.14（Loom 1.11.8 要求 ≥8.14；仓库根 wrapper 是 8.8，会直接报错）
#   Forge   build : JDK 17 + forge 自带 Gradle 8.1.1（ForgeGradle 6.0.24 只认这一代）
#   Forgix  merge : JDK 21 + Gradle 8.14（插件跑在 daemon 里，不编译任何 Java 代码）
set -euo pipefail

HERE="$(dirname "${BASH_SOURCE[0]}")"
ROOT="$(cd "$HERE/.." && pwd -W 2>/dev/null)" || ROOT=""
[ -n "$ROOT" ] || ROOT="$(cd "$HERE/.." && pwd)"

JDK21="${JDK21:-C:/Program Files/Java/jdk-21.0.10}"
JDK17="${JDK17:-C:/Program Files/Java/jdk-17}"
GRADLE="${GRADLE:-D:/MC-MTR-JS/temp/gradle/gradle-8.14/bin/gradle}"

cd "$ROOT"

if [[ ! -x "$GRADLE" ]]; then
  echo "!! 找不到 Gradle 8.14：$GRADLE（Loom 1.11.8 要求 ≥8.14）" >&2
  echo "   用 GRADLE=/path/to/gradle-8.14/bin/gradle 覆盖" >&2
  exit 1
fi

if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  echo "== [1/3] Fabric 侧（JDK 21 + Gradle 8.14）"
  JAVA_HOME="$JDK21" "$GRADLE" -p v1.20.1 build --console=plain

  echo "== [2/3] Forge 侧（JDK 17 + Gradle 8.1.1）"
  JAVA_HOME="$JDK17" bash forge/gradlew -p forge build --console=plain
fi

echo "== [3/3] Forgix 合并（JDK 21 + Gradle 8.14）"
JAVA_HOME="$JDK21" "$GRADLE" -p merge mergeJars --console=plain

OUT="merge/build/libs/LTYs-Transit-Expansion-Decoration-3.0.0.jar"
echo
echo "产物：$OUT"
ls -l "$OUT"
unzip -Z1 "$OUT" | wc -l | xargs echo "条目数（含目录）："
