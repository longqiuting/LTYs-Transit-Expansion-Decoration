#!/usr/bin/env bash
# 归类 tools/verify-decompiled.sh --all 的失败原因
set -uo pipefail
LOG="${1:-/tmp/verify-all.log}"
echo "PASS(含 javap 有差异) = $(grep -c '^PASS' "$LOG")"
echo "FAIL                  = $(grep -c '^FAIL' "$LOG")"
echo
echo "## 失败类别"
grep '^   FAIL' "$LOG" | sed 's/^ *FAIL [^:]*: //' | while IFS= read -r r; do
  case "$r" in
    *"无法访问class_"*|*"程序包"*"不存在"*|*"找不到符号"*fabric*) echo "A_fabric_api" ;;
    *"中定义了变量"*) echo "B_dup_local" ;;
    *"条件表达式"*|*"方法引用"*|*"泛型"*|*"不兼容的类型"*) echo "C_generic" ;;
    *"TileEntityPIDS"*|*"FabricItemGroup"*|*"Door不是抽象"*|*"未覆盖"*) echo "D_mtr_or_abstract" ;;
    *) echo "E_other" ;;
  esac
done | sort | uniq -c | sort -rn
echo
echo "## 明细（文件: 首个错误）"
grep '^   FAIL' "$LOG" | sed 's/^ *FAIL //' | sed 's/\\MC-MTR-JS.*java-gen\\//; s/\\/\//g' | head -80
