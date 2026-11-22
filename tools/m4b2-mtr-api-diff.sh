#!/usr/bin/env bash
# tools/m4b2-mtr-api-diff.sh —— 对比 mtr.mappings.* 在 Fabric(named) / Forge jar 里的公开签名
set -uo pipefail
mp() { cygpath -m "$1" 2>/dev/null || printf '%s' "$1"; }
R="$(mp "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)")"
JAVAP="C:/Program Files/Java/jdk-17/bin/javap"
FAB="$R/libs/MTR-fabric-1.20.1-3.6.4.2-named.jar"
FOR="$R/forge/libs/MTR-forge-1.20.1-3.6.6.1.jar"
TMP="$R/.remap-work/mtr-api"; rm -rf "$TMP"; mkdir -p "$TMP"

for cls in BlockEntityMapper BlockEntityRendererMapper ScreenMapper Utilities; do
  for side in fab for; do
    if [ "$side" = fab ]; then J="$FAB"; else J="$FOR"; fi
    "$JAVAP" -p -cp "$J" "mtr.mappings.$cls" 2>/dev/null | tail -n +2 \
      | sed 's/  */ /g' | sort > "$TMP/$cls.$side.sig"
  done
  echo "=== mtr.mappings.$cls ==="
  if diff -q "$TMP/$cls.fab.sig" "$TMP/$cls.for.sig" >/dev/null 2>&1; then
    echo "  两侧签名一致"
  else
    diff "$TMP/$cls.fab.sig" "$TMP/$cls.for.sig" | grep '^[<>]' | sed 's/^/  /'
  fi
done
