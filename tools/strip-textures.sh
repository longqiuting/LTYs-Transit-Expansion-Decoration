#!/usr/bin/env bash
# LTED 纹理瘦身：默认包降到「最大边 512」+ 生成内置高清资源包（幂等，可重跑）。
#
# 三件事，顺序固定：
#   1) 备份：assets/ 下的全部 PNG（含 .png.mcmeta）以「原始字节」复制到 hd/，
#      路径与 assets/ 下一致但不带 assets/ 前缀（assets/mtrlte/textures/a.png → hd/mtrlte/textures/a.png）。
#      只在 hd/ 不存在时执行 —— hd/ 是原图唯一副本，绝不能被已经缩过的 assets/ 覆盖。
#   2) 缩小：tools/TextureDownscale.java 读 hd/ 写 assets/，最大边降到 CAP（只缩不放，双三次，保留 alpha）。
#   3) 高清包：hd/ 里的原图原样复制到 resourcepacks/lted_hd/assets/（Fabric/Forge 通用内置包，
#      游戏里手动勾选，默认关闭）。
#
# 不动 assets/mtrsteamloco/rails（另一项任务的部分，且 rails 贴图不在 hd/ 里，
# 所以既不会被缩、也不会进高清包）。
#
# 用法：
#   bash tools/strip-textures.sh              # 全流程
#   CAP=1024 bash tools/strip-textures.sh     # 换上限
#   bash tools/strip-textures.sh --audit-only # 只体检，不改文件
set -euo pipefail

mp() { cygpath -m "$1" 2>/dev/null || printf '%s' "$1"; }
ROOT="$(mp "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)")"
RES="$ROOT/v1.20.1/src/main/resources"
ASSETS="$RES/assets"
HD="$RES/hd"
PACK="$RES/resourcepacks/lted_hd"
CAP="${CAP:-512}"
JAVA="${JAVA:-C:/Program Files/Java/jdk-17/bin/java}"
SKIP_REL="mtrsteamloco/rails"

cd "$ROOT"

[ -d "$ASSETS" ] || { echo "找不到 $ASSETS" >&2; exit 2; }
[ -x "$JAVA" ] || JAVA="$(command -v java || true)"
[ -n "${JAVA:-}" ] || { echo "找不到 java（用 JAVA=... 指定）" >&2; exit 2; }

audit() { "$JAVA" -Dfile.encoding=UTF-8 tools/TextureAudit.java "$@"; }

if [ "${1:-}" = "--audit-only" ]; then
  echo "== 默认包（assets）"
  audit "$ASSETS"
  echo "== 高清包（resourcepacks/lted_hd/assets）"
  audit "$PACK/assets"
  echo "== 原始备份（hd）"
  audit "$HD"
  exit 0
fi

# ---------------------------------------------------------------- 1) 备份
if [ -d "$HD" ] && [ -n "$(ls -A "$HD" 2>/dev/null)" ]; then
  echo "== 1/3 备份：hd/ 已存在，跳过（原图只备份一次，避免把已缩小的图当原图）"
else
  echo "== 1/3 备份：assets → hd/（原始字节）"
  n=0
  while IFS= read -r -d '' f; do
    rel="${f#"$ASSETS"/}"
    case "$rel" in "$SKIP_REL"/*) continue ;; esac
    mkdir -p "$HD/$(dirname "$rel")"
    cp -p "$f" "$HD/$rel"
    n=$((n + 1))
  done < <(find "$ASSETS" -type f \( -name '*.png' -o -name '*.png.mcmeta' \) -print0)
  echo "   备份 $n 个文件"
fi

# ---------------------------------------------------------------- 2) 缩到 CAP
echo "== 2/3 缩小：hd/ → assets/（最大边 $CAP）"
"$JAVA" -Dfile.encoding=UTF-8 tools/TextureDownscale.java "$HD" "$ASSETS" "$CAP"

# ---------------------------------------------------------------- 3) 高清包
echo "== 3/3 高清包：hd/ → resourcepacks/lted_hd/assets/（原始字节）"
mkdir -p "$PACK/assets"
n=0
while IFS= read -r -d '' f; do
  rel="${f#"$HD"/}"
  case "$rel" in "$SKIP_REL"/*) continue ;; esac
  mkdir -p "$PACK/assets/$(dirname "$rel")"
  cp -p "$f" "$PACK/assets/$rel"
  n=$((n + 1))
done < <(find "$HD" -type f \( -name '*.png' -o -name '*.png.mcmeta' \) -print0)
echo "   写入 $n 个文件"

# ---------------------------------------------------------------- 体检
echo
echo "== 体检"
audit "$ASSETS" "$CAP"
echo
audit "$PACK/assets"
