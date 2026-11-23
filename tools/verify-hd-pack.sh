#!/usr/bin/env bash
# 验收：单 jar 里的纹理是否「默认 ≤512 + 内置高清包是原图」。
#
#   bash tools/verify-hd-pack.sh [jar 路径]
#
# 默认查 merge/build/libs/ 下那个合并 jar。检查项：
#   1) assets/**  里每张 PNG 最大边 ≤ 512（TextureAudit 断言）
#   2) resourcepacks/lted_hd/pack.mcmeta 存在且 pack_format = 15
#   3) resourcepacks/lted_hd/assets/** 的条目数
#   4) 抽查 3 张高清贴图与 hd/ 备份逐字节一致（md5）
#   5) hd/ 备份本身没有被打进 jar（只是构建输入）
set -euo pipefail

mp() { cygpath -m "$1" 2>/dev/null || printf '%s' "$1"; }
ROOT="$(mp "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)")"
JAR="${1:-}"
JAVA="${JAVA:-C:/Program Files/Java/jdk-17/bin/java}"

cd "$ROOT"

if [ -z "$JAR" ]; then
  JAR="$(ls -1 merge/build/libs/*.jar 2>/dev/null | grep -v sources | head -1 || true)"
fi
[ -f "$JAR" ] || { echo "找不到 jar：${JAR:-merge/build/libs/*.jar}" >&2; exit 2; }

echo "jar: $JAR ($(du -m "$JAR" | cut -f1) MB)"

WORK="$LOCALAPPDATA/Temp/hd-pack-verify"
rm -rf "$WORK"; mkdir -p "$WORK"
unzip -q -o "$JAR" -d "$WORK"

# 1) 默认包
echo
echo "== 1) 默认包 assets/**"
"$JAVA" -Dfile.encoding=UTF-8 tools/TextureAudit.java "$WORK/assets" 512

# 2) 包元数据
echo
echo "== 2) 内置包元数据"
META="$WORK/resourcepacks/lted_hd/pack.mcmeta"
[ -f "$META" ] || { echo "  ✗ 缺 resourcepacks/lted_hd/pack.mcmeta" >&2; exit 1; }
FMT=$(tr -d ' \r\n' < "$META" | sed -n 's/.*"pack_format":\([0-9]*\).*/\1/p')
echo "  pack.mcmeta 存在，pack_format=$FMT"
[ "$FMT" = "15" ] || { echo "  ✗ pack_format 应为 15" >&2; exit 1; }

# 3) 条目数
HDN=$(find "$WORK/resourcepacks/lted_hd/assets" -name "*.png" | wc -l)
echo "  resourcepacks/lted_hd/assets: PNG $HDN 张"
[ "$HDN" -gt 0 ] || { echo "  ✗ 高清包里没有 PNG" >&2; exit 1; }

# 4) 与备份逐字节抽查
echo
echo "== 3) 高清贴图 md5 抽查（对比 hd/ 备份）"
for f in mtrlte/textures/eyecandies/texturexlt.png \
         mtrlte/textures/eyecandies/stone_zhu.png \
         mtrlte/advertisements/scenery1.png; do
  # md5sum 在文件名含反斜杠时会用 "\<hash>" 转义输出，统一去掉前缀再比
  A=$(md5sum "$WORK/resourcepacks/lted_hd/assets/$f" | awk '{print $1}' | sed 's/^\\//')
  B=$(md5sum "v1.20.1/src/main/resources/hd/$f" | awk '{print $1}' | sed 's/^\\//')
  if [ "$A" = "$B" ]; then echo "  ✓ $f  $A"; else echo "  ✗ $f  jar=$A 备份=$B"; exit 1; fi
done

# 5) hd/ 没进 jar
echo
echo "== 4) 备份目录不进 jar"
if unzip -Z1 "$JAR" | grep -q "^hd/"; then
  echo "  ✗ jar 里混进了 hd/（构建 exclude 失效）" >&2; exit 1
fi
echo "  ✓ jar 里没有 hd/ 条目"

echo
echo "全部通过。"
