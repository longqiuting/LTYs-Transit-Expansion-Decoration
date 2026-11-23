#!/usr/bin/env bash
# 把 merge 产物同步到 5 个 mods 目录 —— 顺序与校验都在这儿，别手抄 cp。
#
#   tools/sync-jar.sh                # 同步 + 校验
#   tools/sync-jar.sh --check        # 只校验现有副本（不写）
#
# <h2>为什么不直接 cp 覆盖</h2>
# 2026-11-23 18:52 的实例日志就是这么坏的：客户端正在跑，jar 被覆盖写进去，
# 之后 Knot 读 class entry 拿到 {@code ZipException: invalid LOC header (bad signature)}，
# 抛 {@code RuntimeException: Failed to load class file for '...PidsXiamenAdSetScreen'} ——
# 玩家看到的是「聊天栏说已打开，界面没出来」。所以这里：
#   ① 有 Minecraft 客户端在跑就**拒绝同步**（写完也不会被正在跑的进程读到，只会坏在磁盘上）；
#   ② 先写同名 .tmp，再 mv 原子替换，任何时刻目录里的 jar 都是完整的；
#   ③ 每个副本 unzip -t 全量 CRC + 抽一个 class 验 CAFEBABE，不通过就报错退出。
set -euo pipefail

HERE="$(dirname "${BASH_SOURCE[0]}")"
ROOT="$(cd "$HERE/.." && pwd -W 2>/dev/null)" || ROOT="$(cd "$HERE/.." && pwd)"
cd "$ROOT"

JAR_NAME="$(grep -E '^archives_base_name=' v1.20.1/gradle.properties | cut -d= -f2 2>/dev/null || true)"
[ -n "${JAR_NAME:-}" ] || JAR_NAME="LTYs-Transit-Expansion-Decoration"
JAR_VER="$(grep -E '^mod_version=' v1.20.1/gradle.properties | cut -d= -f2)"
FILE="${JAR_NAME}-${JAR_VER}.jar"
SRC="merge/build/libs/$FILE"

PCL="/c/Users/AD/Desktop/PCL/.minecraft/versions"
TARGETS=(
  "$PCL/1.20.1-Fabric 0.19.3/mods"
  "$PCL/1.20.1-Forge 47.4.0/mods"
  "/d/MC-MTR-JS/forge-client/mods"
  "/d/MC-MTR-JS/forge-test/mods"
  "/d/MC-MTR-JS/fabric-test-lted/mods"
)

CHECK_ONLY=0
[ "${1:-}" = "--check" ] && CHECK_ONLY=1

if [ "$CHECK_ONLY" = 0 ]; then
  cd "$ROOT"
  [ -f "$SRC" ] || { echo "!! 找不到产物：$SRC（先跑 tools/merge-single-jar.sh）" >&2; exit 1; }

  # ① 有客户端在跑就别写：热替换 = 玩家那边读半个 zip（见文件头）。
  RUNNING="$(powershell -NoProfile -Command \
      "Get-CimInstance Win32_Process -Filter \"Name='java.exe' or Name='javaw.exe'\" | Where-Object { \$_.CommandLine -match 'gameDir|minecraft|fabric|forge' } | Select-Object -ExpandProperty ProcessId" \
      2>/dev/null | tr -d '\r' | grep -E '^[0-9]+$' || true)"
  if [ -n "$RUNNING" ]; then
    echo "!! 有 Minecraft / Gradle 相关 java 进程在跑（$RUNNING），拒绝同步 —— 先关掉客户端再跑。" >&2
    exit 2
  fi

  for t in "${TARGETS[@]}"; do
    mkdir -p "$t"
    cp -f "$SRC" "$t/$FILE.tmp"     # ② 先写 tmp
    mv -f "$t/$FILE.tmp" "$t/$FILE" #    再原子替换
    echo "同步 → $t"
  done
fi

# ③ 校验每个副本
fail=0
src_hash="$(sha256sum "$SRC" 2>/dev/null | cut -d' ' -f1 || true)"
for t in "${TARGETS[@]}"; do
  f="$t/$FILE"
  if [ ! -f "$f" ]; then echo "!! 缺文件：$f" >&2; fail=1; continue; fi
  if ! unzip -t "$f" >/dev/null 2>&1; then echo "!! zip 损坏：$f" >&2; fail=1; continue; fi
  magic="$(unzip -p "$f" fabric/dev/js/mtrheadlight/decor/LtedDashboardScreen.class 2>/dev/null \
           | head -c 4 | od -An -tx1 | tr -d ' \n')"
  if [ "$magic" != "cafebabe" ]; then echo "!! class 读不出来（$magic）：$f" >&2; fail=1; continue; fi
  hash="$(sha256sum "$f" | cut -d' ' -f1)"
  if [ -n "$src_hash" ] && [ "$hash" != "$src_hash" ]; then
    echo "!! 与产物不一致：$f（$hash ≠ $src_hash）" >&2; fail=1; continue
  fi
  printf "OK  %-64s %s\n" "$(echo "$t" | sed 's|.*/versions/||')" "${hash:0:16}"
done

[ "$fail" = 0 ] || { echo "!! 有副本没通过校验（见上）" >&2; exit 3; }
echo "5 个副本全部通过（zip CRC + class magic + sha256）。"
