#!/usr/bin/env bash
# 静态自检：每个 mixin 配置里的类都在 jar 里、且每个 mixin 类只出现在一个配置里（A/B 拆分后别重复/漏掉）。
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1

JAR="${1:-build/libs/lte-2.0.2.jar}"
if [ ! -f "$JAR" ]; then
  echo "no jar: $JAR (run: gradle build)" >&2
  exit 1
fi

UNZIP_LIST="$(unzip -l "$JAR" | tr -d '\r')"

fail=0
LISTED=""
TOTAL=0

for json in src/main/resources/*.mixins.json; do
  pkg=$(grep -o '"package"[[:space:]]*:[[:space:]]*"[^"]*"' "$json" | sed 's/.*"\([^"]*\)"$/\1/')
  classes=$(grep -oE '"[A-Za-z0-9_$]+"' "$json" \
    | tr -d '"' \
    | grep -vE '^(required|minVersion|package|compatibilityLevel|refmap|plugin|mixins|client|server|injectors|defaultRequire|JAVA_17|[0-9.]+)$' \
    | grep -E '^[A-Z][A-Za-z0-9_$]*$')
  echo "== $(basename "$json")  [$pkg]"
  for c in $classes; do
    path="${pkg//.//}/$c.class"
    TOTAL=$((TOTAL + 1))
    if grep -q " ${path}$" <<< "$UNZIP_LIST"; then
      printf '   ok   %s\n' "$c"
    else
      printf '   MISS %s (%s)\n' "$c" "$path"
      fail=1
    fi
    if grep -qx "$c" <<< "$LISTED"; then
      printf '   DUP  %s already listed in another config\n' "$c"
      fail=1
    fi
    LISTED="$LISTED$c
"
  done
done

echo
echo "total mixin classes: $TOTAL"
if [ "$fail" -eq 0 ]; then
  echo "OK: every mixin class exists exactly once"
else
  echo "FAILED"
  exit 1
fi
