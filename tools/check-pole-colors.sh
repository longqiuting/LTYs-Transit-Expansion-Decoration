#!/usr/bin/env bash
# 厦门地铁柱子「车站色判定链」的离线检查：不需要世界、不需要客户端。
#
#   tools/check-pole-colors.sh
#
# 做法：用 Fabric 侧的编译产物（v1.20.1/build/classes/java/main）+ yarn 映射的 MC jar +
# MTR 的 named jar + 那份 Fabric 专用服务端的 libraries/（MC 运行期的全部依赖：DFU / guava /
# gson / fastutil / log4j ...）拼一个 classpath，把 tools/PoleColorCheck.java 编出来跑一遍。
#
# 覆盖 5 条定位规则（精确 / 就近 / 边界 / 超范围 / 远离）+ 4 条线路色规则（跳过 color=0 /
# 跳过别的车站 / 取线路 id 最小 / 一条都没有 = 0）。改 PoleColors 之后先跑这个。
#
# 前提：先 `gradle -p v1.20.1 build`（要 build/classes），以及 SERVER_DIR 那个服务端目录存在
# （只借它的 libraries/，不启动它）。
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -W 2>/dev/null)" || HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -W 2>/dev/null)" || ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SERVER_DIR="${SERVER_DIR:-D:/MC-MTR-JS/fabric-test-lted}"
JAVAC="${JAVAC:-C:/Program Files/Java/jdk-17/bin/javac}"
JAVA="${JAVA:-C:/Program Files/Java/jdk-17/bin/java}"
CLASSES="$ROOT/v1.20.1/build/classes/java/main"
NAMED_MC="$ROOT/v1.20.1/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-487d1d375f/1.20.1-net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2/minecraft-merged-487d1d375f-1.20.1-net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2.jar"
MTR="$ROOT/libs/MTR-fabric-1.20.1-3.6.4.2-named.jar"

for f in "$CLASSES" "$NAMED_MC" "$MTR" "$SERVER_DIR/libraries"; do
  if [ ! -e "$f" ]; then
    echo "!! 缺少 $f（先跑 gradle -p v1.20.1 build；SERVER_DIR 指到一个 Fabric 服务端目录）" >&2
    exit 2
  fi
done

CP="$CLASSES;$NAMED_MC;$MTR"
while IFS= read -r j; do
  CP="$CP;$SERVER_DIR/$j"
done < <(cd "$SERVER_DIR" && find libraries -name '*.jar')

OUT="${OUT:-C:/Users/AD/AppData/Local/Temp/mtrx/polecheck}"
rm -rf "$OUT"
mkdir -p "$OUT"

"$JAVAC" -encoding UTF-8 -nowarn -d "$OUT" -cp "$CP" "$ROOT/tools/PoleColorCheck.java" || {
  echo "!! 编译 PoleColorCheck 失败" >&2
  exit 3
}

echo "== PoleColorCheck（车站色判定链）"
"$JAVA" -Dfile.encoding=UTF-8 -cp "$OUT;$CP" PoleColorCheck
