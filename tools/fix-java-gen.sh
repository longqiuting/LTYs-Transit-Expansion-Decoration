#!/usr/bin/env bash
# M4b-3 机械修复流水线：java-gen（200 个 mojmap 反编译源码）+ forge 手写 3 类整体编译。
#
# 步骤：
#   1. 同步 compile classpath（forge 模块的真实 compileClasspath，含 Forge + MTR-forge）
#   2. tools/fix-java-gen.js  —— 表格修复 + 结构性修复（switch case 作用域 / 重复局部变量改名）
#   3. tools/m4b3-javac.sh    —— 一次性 javac 全部源码（-Xmaxerrs 20000，不截断）
#   4. 若仍有「重复局部变量」，重复 2–3（最多 3 轮），直到不再变化
#   5. tools/m4b3-classify.js —— 输出按类别的错误统计表
#
# 幂等：java-gen 可被 tools/remap-to-mojmap.sh 覆盖，重跑本脚本即得到同样的修复结果。
#
# 用法：
#   ./tools/fix-java-gen.sh              # 修复 + 全量编译 + 分类
#   ./tools/fix-java-gen.sh --dry-run    # 只看会改什么（不改文件、不编译）
set -uo pipefail
mp() { cygpath -m "$1" 2>/dev/null || printf '%s' "$1"; }
R="$(mp "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)")"
JAVAC_HOME="C:/Program Files/Java/jdk-17"
DRY=0
[ "${1:-}" = "--dry-run" ] && DRY=1

cd "$R" || exit 2

if [ $DRY -eq 1 ]; then
  node tools/fix-java-gen.js --dry-run
  exit 0
fi

echo "== 1/4 同步 compile classpath"
(cd forge && JAVA_HOME="$JAVAC_HOME" ./gradlew -q printCompileCp --offline 2>/dev/null) \
  | sed -n '/^CP_BEGIN$/,/^CP_END$/p' > "$R/.remap-work/m4b3-cp.txt"
[ -s "$R/.remap-work/m4b3-cp.txt" ] || { echo "classpath 导出失败" >&2; exit 2; }

for round in 1 2 3 4 5 6 7 8; do
  # 先编再修：保证修复器读到的行号来自当前源码，绝不吃旧日志
  echo "== 2/4 全量 javac（第 $round 轮）"
  ./tools/m4b3-javac.sh | tail -2
  if ! grep -q "中定义了变量" "$R/.remap-work/m4b3-javac.log"; then break; fi
  echo "== 3/4 机械修复（第 $round 轮）"
  node tools/fix-java-gen.js
  node tools/fix-java-gen-platform.js | head -1
done

# 表格类修复（丢 cast / 命名撞车 / MTR 桥）幂等，最后统一过一遍，再编一次
echo "== 3b/4 机械修复（表格，幂等）"
node tools/fix-java-gen.js
# M4b-4：Fabric API → Forge 平台 shim（注册 / 页签 / 渲染 / 网络 / 命令 / 环境）
node tools/fix-java-gen-platform.js | head -1
./tools/m4b3-javac.sh | tail -2

echo "== 4/4 分类"
node tools/m4b3-classify.js
