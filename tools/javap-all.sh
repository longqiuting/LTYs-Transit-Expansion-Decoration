#!/usr/bin/env bash
# 批量 javap：确认合并 jar 里每个 class 都能被 JVM 解析（不是空包 / 不是坏字节）。
# Usage: tools/javap-all.sh <jar> [jdk]
set -euo pipefail
JAR="$(cd "$(dirname "${1:?usage: javap-all.sh <jar> [jdk]}")" && pwd)/$(basename "$1")"
JDK="${2:-C:/Program Files/Java/jdk-17}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
(cd "$WORK" && unzip -oq "$JAR")
NAMES=$(cd "$WORK" && find . -name '*.class' | sed 's|^\./||;s|\.class$||;s|/|.|g')
COUNT=$(printf '%s\n' "$NAMES" | wc -l)
echo "classes in jar: $COUNT"
"$JDK/bin/javap" -classpath "$(cygpath -w "$WORK" 2>/dev/null || echo "$WORK")" $NAMES > "$WORK/out.txt" 2> "$WORK/err.txt" || true
echo "parsed: $(grep -c '^Compiled from' "$WORK/out.txt")"
echo "stderr lines: $(wc -l < "$WORK/err.txt")"
head -5 "$WORK/err.txt"
