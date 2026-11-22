#!/usr/bin/env bash
# M4b-3 诊断器：用 forge 模块的真实 compileClasspath，把 src/main/java + src/main/java-gen
# 的全部源文件**一次性**交给 javac（Gradle 默认 -Xmaxerrs 会被截断，这里放开到 20000），
# 输出全量错误清单供分类统计。
#
# 用法：
#   ./tools/m4b3-javac.sh            # 编译并输出摘要
#   ./tools/m4b3-javac.sh --raw      # 只跑 javac，保留日志路径
set -uo pipefail
mp() { cygpath -m "$1" 2>/dev/null || printf '%s' "$1"; }
R="$(mp "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)")"
W="$R/.remap-work"
JAVAC="C:/Program Files/Java/jdk-17/bin/javac"
CPFILE="$W/m4b3-cp.txt"
LOG="$W/m4b3-javac.log"
OUT="$W/m4b3-classes"

if [ ! -s "$CPFILE" ]; then
  echo "缺 $CPFILE —— 先在 forge 里跑 ./gradlew -q printCompileCp --offline" >&2
  exit 2
fi
CP="$(sed -n '/^CP_BEGIN$/,/^CP_END$/p' "$CPFILE" | sed '1d;$d' | tr '\\' '/')"

rm -rf "$OUT"; mkdir -p "$OUT"
: > "$W/m4b3-sources.txt"
find "$R/forge/src/main/java" "$R/forge/src/main/java-gen" -name '*.java' -print | tr '\\' '/' >> "$W/m4b3-sources.txt"

{
  printf -- '-proc:none\n-nowarn\n-encoding\nUTF-8\n-source\n17\n-target\n17\n-Xmaxerrs\n20000\n-Xdiags:verbose\n'
  printf -- '-d\n"%s"\n' "$OUT"
  printf -- '-cp\n"%s"\n' "$CP"
} > "$W/m4b3-args.txt"

"$JAVAC" "@$W/m4b3-args.txt" "@$W/m4b3-sources.txt" > "$LOG.gbk" 2>&1
rc=$?
iconv -f GBK -t UTF-8 "$LOG.gbk" 2>/dev/null > "$LOG" || cp "$LOG.gbk" "$LOG"
echo "javac exit=$rc  log=$LOG"
grep -E "个错误|个警告" "$LOG" | tail -2
exit $rc
