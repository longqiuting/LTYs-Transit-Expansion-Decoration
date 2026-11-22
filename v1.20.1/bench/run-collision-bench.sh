#!/usr/bin/env bash
# Runs the server-collision benchmark / equivalence check for v1.20.1.
#
#   ./run-collision-bench.sh            # 4 timing scenarios, one JVM each
#   ./run-collision-bench.sh verify     # car-by-car equivalence vs the old builder
#
# Needs: build/classes/java/main (run `gradle build` first) and build/cp.txt (the runtime
# classpath; regenerate with the init script described in the report or `gradle -q dependencies`).
set -euo pipefail
cd "$(dirname "$0")/.."

MC_CLASSES="build/classes/java/main"
CP="${MC_CLASSES};$(cat build/cp.txt);../libs/MTR-fabric-1.20.1-3.6.4.2-named.jar"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"

mkdir -p bench/out
"$JAVAC" -nowarn -cp "$CP" -d bench/out bench/CollisionBench.java

if [ "${1:-}" = "verify" ]; then
  # cull radius huge: nothing may be culled, so every train has to be reproduced independently
  "$JAVA" -Dlte.cull.radius=100000 -cp "bench/out;$CP" CollisionBench verify
  exit 0
fi

for cfg in "40 running" "40 parked" "200 running" "200 parked"; do
  # shellcheck disable=SC2086
  "$JAVA" -cp "bench/out;$CP" CollisionBench $cfg
done
