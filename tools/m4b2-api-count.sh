#!/usr/bin/env bash
# m4b2-api-count.sh —— 逐 API 统计（次数 / 文件数，按 import 的简单类名）
# 用法: tools/m4b2-api-count.sh
#：次数 / 文件数（按 import 的简单类名）
set -uo pipefail
GEN="D:/MC-MTR-JS/mtr-headlight/forge/src/main/java-gen"
cd "$GEN" || exit 1
grep -rh '^import net.fabricmc\|^import com.terraformersmc' --include='*.java' . \
  | sed 's/^import //; s/;$//' | sort -u | while read -r imp; do
    simple="${imp##*.}"
    n=$(grep -rho "\b${simple}\b" . --include='*.java' | wc -l | tr -d ' ')
    f=$(grep -rl "\b${simple}\b" . --include='*.java' | wc -l | tr -d ' ')
    printf '%-70s %5s %5s\n' "$imp" "$n" "$f"
  done | sort -k2 -nr
