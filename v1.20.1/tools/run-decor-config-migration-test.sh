#!/usr/bin/env bash
# 离线跑 DecorConfig 的旧键迁移用例（不需要启动 Minecraft）。
#
# 用法（仓库根目录）：
#   bash v1.20.1/tools/run-decor-config-migration-test.sh
#
# 前提：先 `gradle -p v1.20.1 compileJava`（脚本用 build/classes/java/main 里已经编译好的
# DecorConfig，再单独编译 tools/DecorConfigMigrationTest.java）。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd -W 2>/dev/null || pwd)"
MOD="$ROOT/v1.20.1"
JAVA_HOME="${JAVA_HOME:-C:/Program Files/Java/jdk-21.0.10}"
JAVAC="$JAVA_HOME/bin/javac"
JAVA="$JAVA_HOME/bin/java"

GRADLE_CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
find_jar () {
  local pattern="$1"
  local jar
  jar=$(find "$GRADLE_CACHE" -name "$pattern" 2>/dev/null | sort | tail -n 1)
  if [ -z "$jar" ]; then
    echo "missing jar: $pattern (looked in $GRADLE_CACHE)" >&2
    exit 1
  fi
  # native javac/java 不认 MSYS 的 /c/... 路径。
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -m "$jar"
  else
    printf '%s' "$jar"
  fi
}

GSON="$(find_jar 'gson-2.10*.jar')"
SLF4J="$(find_jar 'slf4j-api-2*.jar')"
LOADER="$(find_jar 'fabric-loader-*.jar')"

CLASSES="$MOD/build/classes/java/main"
if [ ! -d "$CLASSES" ]; then
  echo "compile first: gradle -p v1.20.1 compileJava" >&2
  exit 1
fi

OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

SEP=":"
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=";" ;;
esac

CP="$CLASSES$SEP$GSON$SEP$SLF4J$SEP$LOADER"
"$JAVAC" -encoding UTF-8 -cp "$CP" -d "$OUT" "$MOD/tools/DecorConfigMigrationTest.java"
"$JAVA" -cp "$OUT$SEP$CP" DecorConfigMigrationTest
