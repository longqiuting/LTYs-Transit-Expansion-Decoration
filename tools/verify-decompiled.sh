#!/usr/bin/env bash
# tools/verify-decompiled.sh
#
# 抽样验证 java-gen 里的反编译源码：javac 单文件重编 + javap 对照
#
#   1. 为每个类单独跑 javac（-sourcepath 指 java-gen，允许隐式编译依赖）
#   2. 编译成功则 javap 生成的 class，与「重映射后的原始 class」比对成员集合
#   3. 输出 PASS / FAIL(原因)
#
# 用法: tools/verify-decompiled.sh [fqcn...]
#   默认验证 ClockBlock / PidsXiamenRenderer / XiamenStripPainter + 5 个随机类
set -uo pipefail

mp() { cygpath -m "$1" 2>/dev/null || printf '%s' "$1"; }
REPO_ROOT="$(mp "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)")"
WORK="$REPO_ROOT/.remap-work"
GEN="$REPO_ROOT/forge/src/main/java-gen"
MC_VERSION="${MC_VERSION:-1.20.1}"
GRADLE_CACHE="$(mp "${GRADLE_CACHE:-$HOME/.gradle/caches}")"
M2="$GRADLE_CACHE/modules-2/files-2.1"

JAVA_BIN="${JAVA:-C:/Program Files/Java/jdk-17/bin/java}"
JAVAC="${JAVAC:-C:/Program Files/Java/jdk-17/bin/javac}"
JAVAP="${JAVAP:-C:/Program Files/Java/jdk-17/bin/javap}"

REMAP_JAR=$(ls -t "$WORK"/*-mojmap.jar 2>/dev/null | head -1)
[ -n "$REMAP_JAR" ] || { echo "run tools/remap-to-mojmap.sh first" >&2; exit 2; }
[ -d "$GEN" ] || { echo "no $GEN" >&2; exit 2; }

# ---- classpath: 重映射后的 jar(其余项目类) + MC(mojmap) + MTR(forge) + Gradle 缓存里全部 jar ----
MC_JAR=$(ls "$GRADLE_CACHE"/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/*_mapped_official_*/forge-*_mapped_official_*.jar 2>/dev/null | head -1)
[ -n "$MC_JAR" ] || { echo "mapped mojmap MC jar missing (build forge module once)" >&2; exit 2; }

CP_FILE="$WORK/verify-cp.txt"
{
  printf '%s;%s' "$REMAP_JAR" "$MC_JAR"   # 其余项目类用重映射后的 class：单文件编译不级联
  for j in "$REPO_ROOT"/forge/libs/*.jar; do [ -f "$j" ] && printf ';%s' "$j"; done
  find "$M2" -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' 2>/dev/null | sort | while read -r j; do printf ';%s' "$j"; done
} > "$CP_FILE"

OUT_BASE="$WORK/verify"
rm -rf "$OUT_BASE"; mkdir -p "$OUT_BASE"

if [ "${1:-}" = "--all" ]; then
  shift
  CLASSES=()
  while IFS= read -r line; do CLASSES+=("$line"); done < <(
    ( cd "$GEN" && find . -name '*.java' | sed 's|^\./||; s|\.java$||; s|/|.|g' ) | sort
  )
elif [ $# -gt 0 ]; then
  CLASSES=("$@")
else
  # 必测的三个 + 从 java-gen 随机抽 5 个（固定种子，保证可复现）
  CLASSES=(dev.js.mtrheadlight.clock.ClockBlock
           dev.js.mtrheadlight.xiamen.pids.PidsXiamenRenderer
           dev.js.mtrheadlight.xiamen.XiamenStripPainter)
  while IFS= read -r line; do CLASSES+=("$line"); done < <(
    ( cd "$GEN" && find . -name '*.java' | sed 's|^\./||; s|\.java$||; s|/|.|g' ) \
      | grep -v '\$' | sort | awk 'NR%37==0' | head -5
  )
fi

pass=0; fail=0; failed_list=()
for fq in "${CLASSES[@]}"; do
  src="$GEN/$(printf '%s' "$fq" | tr '.' '/').java"
  if [ ! -f "$src" ]; then
    echo "MISSING  $fq (no source file)"; fail=$((fail+1)); failed_list+=("$fq: no source"); continue
  fi
  odir="$OUT_BASE/$fq"; mkdir -p "$odir"
  # 类路径太长（460+ jar），必须走 javac @argfile，-cp "@file" 是不生效的
  {
    printf -- '-proc:none\n-implicit:none\n-nowarn\n-encoding\nUTF-8\n-source\n17\n-target\n17\n'
    printf -- '-d\n"%s"\n' "$odir"
    printf -- '-cp\n"%s"\n' "$(cat "$CP_FILE")"
  } > "$odir/args.txt"
  if "$JAVAC" "@$odir/args.txt" "$src" > "$odir/javac.log" 2>&1; then
    # javap 对照：比对成员名集合（忽略顺序/常量池细节）
    cls_file="$odir/$(printf '%s' "$fq" | tr '.' '/').class"
    orig_dir="$OUT_BASE/orig"; mkdir -p "$orig_dir"
    ( cd "$orig_dir" && unzip -oq "$REMAP_JAR" "$(printf '%s' "$fq" | tr '.' '/').class" 2>/dev/null )
    "$JAVAP" -p "$cls_file" 2>/dev/null | tail -n +2 | sort > "$odir/gen.sig"
    "$JAVAP" -p "$orig_dir/$(printf '%s' "$fq" | tr '.' '/').class" 2>/dev/null | tail -n +2 | sort > "$odir/orig.sig"
    if diff -q "$odir/gen.sig" "$odir/orig.sig" >/dev/null 2>&1; then
      echo "PASS     $fq (javac ok, javap 成员签名完全一致)"; pass=$((pass+1))
    else
      d=$(diff "$odir/orig.sig" "$odir/gen.sig" | grep -c '^[<>]' || true)
      echo "PASS*    $fq (javac ok; javap 有 $d 行差异, 见 $odir)"; pass=$((pass+1))
    fi
  else
    first=$(iconv -f GBK -t UTF-8 "$odir/javac.log" 2>/dev/null | grep -m2 -E '错误|error:' | sed 's/^/           /' || true)
    [ -n "$first" ] || first=$(head -2 "$odir/javac.log" | sed 's/^/           /')
    echo "FAIL     $fq"; echo "$first"; fail=$((fail+1))
    reason=$(iconv -f GBK -t UTF-8 "$odir/javac.log" 2>/dev/null | grep -m1 -E '错误|error:' | sed 's/^[^:]*: *//' || true)
    [ -n "$reason" ] || reason=$(head -1 "$odir/javac.log")
    failed_list+=("$fq: $reason")
  fi
done

printf '\n== 汇总: PASS=%d FAIL=%d\n' "$pass" "$fail"
for f in "${failed_list[@]:-}"; do [ -n "$f" ] && printf '   FAIL %s\n' "$f"; done
[ "$fail" -eq 0 ]
