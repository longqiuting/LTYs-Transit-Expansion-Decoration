#!/usr/bin/env bash
# 单文件重编证明：给指定源文件套同一份 classpath 编译，输出到 .remap-work/proof/out-<name>
set -uo pipefail
mp() { cygpath -m "$1" 2>/dev/null || printf '%s' "$1"; }
R="$(mp "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)")"
W="$R/.remap-work"
JAVAC="C:/Program Files/Java/jdk-17/bin/javac"
src="$1"; name="$2"
out="$W/proof/out-$name"; rm -rf "$out"; mkdir -p "$out"
{
  printf -- '-proc:none\n-implicit:none\n-nowarn\n-encoding\nUTF-8\n-source\n17\n-target\n17\n'
  printf -- '-d\n"%s"\n' "$out"
  printf -- '-cp\n"%s"\n' "$(cat "$W/verify-cp.txt")"
} > "$W/proof/args-$name.txt"
"$JAVAC" "@$W/proof/args-$name.txt" "$src" > "$out/javac.log" 2>&1
rc=$?
echo "exit=$rc out=$out"
[ $rc -ne 0 ] && iconv -f GBK -t UTF-8 "$out/javac.log" 2>/dev/null | head -6
exit $rc
