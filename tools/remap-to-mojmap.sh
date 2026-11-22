#!/usr/bin/env bash
# tools/remap-to-mojmap.sh
#
# 机器迁移管线：Fabric(intermediary) jar -> official/mojmap 字节码 -> 反编译源码基线
#
#   1. 取 MC 中间层 jar（优先 loom 已生成的 minecraft-merged-intermediary，没有才现做一份）
#      —— tiny-remapper 解析继承关系要求 classpath 与输入同命名空间，
#         所以「MC 中间层 jar」是必须的中间产物，不能直接拿 mojmap 版 MC 当 classpath。
#   2. 把 intermediary tiny v2 与 mojmap(ProGuard tv2, client_mappings.txt) 合成三命名空间
#      tiny 文件（official / intermediary / named），见 tools/join-mappings.js
#   3. tiny-remapper: intermediary -> named(mojmap)
#   4. forgeflower 反编译 -> java 源码
#   5. 修 fernflower 无 classpath 时的二进制名渲染（net$minecraft.x -> net.minecraft.x 等），
#      见 tools/fix-decompiled-source.js
#   6. 按包结构落到 <out>
#
# 依赖（全部离线取自本机 Gradle 缓存，脚本不联网）：
#   - net.fabricmc:tiny-remapper (>=0.10) + asm + mapping-io + slf4j-api
#   - net.fabricmc:intermediary:<mc>  (v2 jar 内的 mappings/mappings.tiny)
#   - ForgeGradle 缓存里的 mojmap ProGuard 映射 (client_mappings.txt)
#   - fabric-loom 缓存里的 minecraft-merged.jar (obf) / minecraft-merged-intermediary
#   - net.minecraftforge:forgeflower (ForgeGradle 缓存)
#
# 用法:
#   tools/remap-to-mojmap.sh [选项]
#     --jar <path>       输入 Fabric jar（默认自动取 v1.20.1/build/libs 最新产物，
#                        排除 -sources/-dev；archives_base_name 改名不用动脚本）
#     --out <dir>        源码输出目录（默认 forge/src/main/java-gen）
#     --work <dir>       中间产物目录（默认 .remap-work）
#     --java <path>      java 可执行文件（默认 jdk-17 → JAVA_HOME → PATH）
#     --keep             不清空 --out（默认会先删干净，保证可重复执行）
#     -h|--help
#
# 环境变量覆盖: MC_VERSION (默认 1.20.1), GRADLE_CACHE (默认 ~/.gradle/caches)
set -euo pipefail

# git-bash 下 pwd 给的是 /d/... ，原生 node/java 不认，统一转成 D:/... 混合式路径
mp() {
  if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"
  else printf '%s' "$1" | sed -E 's#^/([A-Za-z])/#\1:/#'; fi
}

REPO_ROOT="$(mp "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)")"
MC_VERSION="${MC_VERSION:-1.20.1}"
GRADLE_CACHE="$(mp "${GRADLE_CACHE:-$HOME/.gradle/caches}")"

JAR=""   # 空 = 自动发现 v1.20.1/build/libs 下最新的产物（排除 -sources/-dev）
OUT="$REPO_ROOT/forge/src/main/java-gen"
WORK="$REPO_ROOT/.remap-work"
KEEP=0
JAVA_BIN=""

while [ $# -gt 0 ]; do
  case "$1" in
    --jar) JAR="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    --work) WORK="$2"; shift 2 ;;
    --java) JAVA_BIN="$2"; shift 2 ;;
    --keep) KEEP=1; shift ;;
    -h|--help) sed -n '2,/^set -euo/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done

# ---- java ----
if [ -z "$JAVA_BIN" ]; then
  # 重映射/反编译用 JDK 17（产物 --release 17 兼容）；没装就用 JAVA_HOME / PATH
  if [ -x "C:/Program Files/Java/jdk-17/bin/java" ]; then JAVA_BIN="C:/Program Files/Java/jdk-17/bin/java"
  elif [ -x "$HOME/.jdks/jdk-17/bin/java" ]; then JAVA_BIN="$HOME/.jdks/jdk-17/bin/java"
  elif [ -n "${JAVA_HOME:-}" ] && [ -x "$(mp "$JAVA_HOME")/bin/java" ]; then JAVA_BIN="$(mp "$JAVA_HOME")/bin/java"
  else JAVA_BIN="$(command -v java)"; fi
fi
JAVA_BIN="$(mp "$JAVA_BIN")"

say() { printf '%s\n' "== $*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

# 输入 jar：默认取 v1.20.1/build/libs 最新产物，名字随 gradle.properties 的
# archives_base_name 变化，不写死（排除 -sources / -dev 变体）
if [ -z "$JAR" ]; then
  JAR=$(ls -t "$REPO_ROOT"/v1.20.1/build/libs/*.jar 2>/dev/null \
        | grep -v -e '-sources\.jar$' -e '-dev\.jar$' | head -1)
  [ -n "$JAR" ] || die "no Fabric jar in v1.20.1/build/libs (build the Fabric module first)"
fi
[ -f "$JAR" ] || die "input jar not found: $JAR"
[ -f "$REPO_ROOT/tools/join-mappings.js" ] || die "tools/join-mappings.js missing"
command -v node >/dev/null 2>&1 || die "node not found (needed to join mappings)"

M2="$GRADLE_CACHE/modules-2/files-2.1"
FG="$GRADLE_CACHE/forge_gradle"

find_one() { # <pattern...> ; returns first match or empty
  local p
  for p in "$@"; do
    local hit
    hit=$(ls $p 2>/dev/null | head -1 || true)
    if [ -n "$hit" ]; then printf '%s' "$hit"; return 0; fi
  done
  return 1
}

say "1/6 定位工具与映射"
TINY_REMAPPER=$(ls $M2/net.fabricmc/tiny-remapper/*/*/tiny-remapper-*.jar 2>/dev/null | grep -v -- '-sources' | sort -V | tail -1)
[ -n "$TINY_REMAPPER" ] || die "tiny-remapper jar not in Gradle cache"
MAPPING_IO=$(find_one "$M2/net.fabricmc/mapping-io/0.7.*/*/mapping-io-*.jar") || MAPPING_IO=$(find_one "$M2/net.fabricmc/mapping-io/*/*/mapping-io-*.jar") || die "mapping-io not in Gradle cache"
SLF4J=$(find_one "$M2/org.slf4j/slf4j-api/2.*/*/slf4j-api-*.jar") || SLF4J=$(find_one "$M2/org.slf4j/slf4j-api/*/*/slf4j-api-*.jar")
ASM_VER=$(ls $M2/org.ow2.asm/asm/ 2>/dev/null | sort -V | tail -1)
ASM_JARS=$(ls $M2/org.ow2.asm/asm*/$ASM_VER/*/asm*-$ASM_VER.jar 2>/dev/null | grep -v -- '-sources' | tr '\n' ';' | sed 's/;$//')
[ -n "$ASM_JARS" ] || die "ASM jars not in Gradle cache"

INTERMEDIARY=$(find_one "$M2/net.fabricmc/intermediary/$MC_VERSION/*/intermediary-$MC_VERSION-v2.jar") || die "intermediary $MC_VERSION not in Gradle cache"
PROGUARD="$FG/minecraft_repo/versions/$MC_VERSION/client_mappings.txt"
[ -f "$PROGUARD" ] || die "mojmap proguard mappings not found: $PROGUARD (run the forge module build once)"
MC_OBF="$GRADLE_CACHE/fabric-loom/$MC_VERSION/minecraft-merged.jar"
[ -f "$MC_OBF" ] || die "loom merged (obf) jar not found: $MC_OBF"

FORGEFLOWER=$(find_one "$FG/maven_downloader/net/minecraftforge/forgeflower/*/forgeflower-*.jar" \
                       "$FG/maven_downloader/net/minecraftforge/forgeflower/*/*/forgeflower-*.jar") \
  || die "forgeflower not in ForgeGradle cache"

TR_CP="$TINY_REMAPPER;$ASM_JARS;$MAPPING_IO;$SLF4J"
printf '   java        : %s\n' "$JAVA_BIN"
printf '   tiny-remapper: %s\n' "$(basename "$TINY_REMAPPER")"
printf '   forgeflower : %s\n' "$(basename "$FORGEFLOWER")"

mkdir -p "$WORK/mappings" "$WORK/ff"
CP_FILE="$WORK/mappings/intermediary-named.tiny"

# ---- 2. 合成三命名空间映射 ----
say "2/6 合成 official/intermediary/named 三命名空间映射"
rm -rf "$WORK/intermediary-extract"
mkdir -p "$WORK/intermediary-extract"
( cd "$WORK/intermediary-extract" && unzip -oq "$INTERMEDIARY" "mappings/mappings.tiny" )
node "$REPO_ROOT/tools/join-mappings.js" \
  "$WORK/intermediary-extract/mappings/mappings.tiny" \
  "$PROGUARD" \
  "$CP_FILE" | sed 's/^/   /'

# ---- 3. MC 中间层 jar ----
# 优先用 loom 已经生成好的 minecraft-merged-intermediary（省一次 3s 重映射），
# 没有就自己从 obf 合并 jar 生成一份缓存到 work 目录。
MC_INT=$(ls "$GRADLE_CACHE"/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-intermediary/"$MC_VERSION"-*/*.jar 2>/dev/null \
         | grep -v '\.backup$' | head -1)
if [ -n "$MC_INT" ]; then
  say "3/6 使用 loom 预置的 MC 中间层 jar: $(basename "$MC_INT")"
else
  MC_INT="$WORK/mc-$MC_VERSION-intermediary.jar"
  if [ -f "$MC_INT" ]; then
    say "3/6 MC 中间层 jar 已缓存，跳过 ($(basename "$MC_INT"))"
  else
    say "3/6 生成 MC 中间层 jar（obf -> intermediary，供 tiny-remapper 解析继承）"
    "$JAVA_BIN" -Xmx2G -cp "$TR_CP" net.fabricmc.tinyremapper.Main \
      "$MC_OBF" "$MC_INT" "$WORK/intermediary-extract/mappings/mappings.tiny" official intermediary \
      > "$WORK/remap-mc.log" 2>&1 || { tail -20 "$WORK/remap-mc.log" | sed 's/^/   /'; die "MC remap failed (log: $WORK/remap-mc.log)"; }
    tail -3 "$WORK/remap-mc.log" | sed 's/^/   /'
  fi
fi

# ---- 4. 重映射业务 jar ----
say "4/6 重映射 $(basename "$JAR")  intermediary -> named(mojmap)"
REMAP_JAR="$WORK/$(basename "${JAR%.jar}")-mojmap.jar"
CLASS_CP=("$MC_INT")
# 同命名空间的依赖 mod jar 参与继承解析（可选，存在就带上）
for extra in "$REPO_ROOT"/libs/MTR-fabric-$MC_VERSION-*.jar; do
  case "$extra" in *-named.jar) continue ;; esac
  [ -f "$extra" ] && CLASS_CP+=("$extra")
done
"$JAVA_BIN" -Xmx3G -cp "$TR_CP" net.fabricmc.tinyremapper.Main \
  "$JAR" "$REMAP_JAR" "$CP_FILE" intermediary named "${CLASS_CP[@]}" \
  > "$WORK/remap-mod.log" 2>&1 || { tail -20 "$WORK/remap-mod.log" | sed 's/^/   /'; die "mod remap failed (log: $WORK/remap-mod.log)"; }
tail -5 "$WORK/remap-mod.log" | sed 's/^/   /'
[ -f "$REMAP_JAR" ] || die "remap produced no jar"

# ---- 5. 反编译 ----
say "5/6 forgeflower 反编译"
rm -rf "$WORK/ff"; mkdir -p "$WORK/ff"
"$JAVA_BIN" -Xmx3G -jar "$FORGEFLOWER" -dgs=1 -asc=1 -din=1 -rsy=1 \
  "$REMAP_JAR" "$WORK/ff" > "$WORK/forgeflower.log" 2>&1 || true
SRC_JAR=$(ls "$WORK"/ff/*.jar 2>/dev/null | head -1)
[ -n "$SRC_JAR" ] || die "forgeflower produced no source jar, see $WORK/forgeflower.log"

# ---- 6. 落地 ----
say "6/6 解包 + 修正 fernflower 嵌套类型渲染 + 落盘"
rm -rf "$WORK/plain"; mkdir -p "$WORK/plain"
( cd "$WORK/plain" && unzip -q "$SRC_JAR" )   # unzip 的 * 不跨 '/'，整包解开再删非源码
find "$WORK/plain" -type f ! -name '*.java' -delete
find "$WORK/plain" -type d -empty -delete 2>/dev/null || true
# fernflower 在无 classpath 时会退化成二进制名（net$minecraft.x / StateDefinition$Builder），
# 交给 join 脚本按「代码段/字面量段」分别修，字符串里的 $ 原样保留
FIXED=$(node "$REPO_ROOT/tools/fix-decompiled-source.js" "$WORK/plain" | wc -l | tr -d ' ')

if [ "$KEEP" -eq 0 ]; then
  rm -rf "$OUT" 2>/dev/null || { find "$OUT" -mindepth 1 -delete 2>/dev/null || true; }
fi
mkdir -p "$OUT"
cp -r "$WORK/plain/." "$OUT/"

N_CLASSES=$(unzip -l "$REMAP_JAR" | grep -c '\.class$' || true)
N_JAVA=$(find "$OUT" -name '*.java' | wc -l | tr -d ' ')
N_ERR=$(grep -c 'ERROR' "$WORK/forgeflower.log" 2>/dev/null || true)
LEFT_INTERMEDIARY=$(grep -rlE 'class_[0-9]+' "$OUT" --include='*.java' 2>/dev/null | wc -l | tr -d ' ' || true)

printf '\n'
printf '== 完成\n'
printf '   重映射类数         : %s\n' "$N_CLASSES"
printf '   反编译 .java 文件  : %s   (内部类合并进外部类)\n' "$N_JAVA"
printf '   fernflower 修正文件: %s\n' "$FIXED"
printf '   仍含 class_ 的文件 : %s\n' "$LEFT_INTERMEDIARY"
printf '   反编译日志         : %s (ERROR 行: %s)\n' "$WORK/forgeflower.log" "$N_ERR"
printf '   输出目录           : %s\n' "$OUT"
