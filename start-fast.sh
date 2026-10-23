#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME_DIR="$ROOT_DIR/.runtime-local/tools/jdk-17.0.19+10/Contents/Home"
JAVA_BIN="$JAVA_HOME_DIR/bin/java"
PORT="${SERVER_PORT:-18080}"

if [ ! -x "$JAVA_BIN" ]; then
  echo "未找到项目内置 Java：$JAVA_BIN"
  exit 1
fi

cd "$ROOT_DIR"

CLASSPATH="target/classes:$(find .m2repo -name '*.jar' ! -path '*/slf4j-api/1.7.*/*' | tr '\n' ':')"

echo "Starting card draw backend on http://localhost:$PORT"
exec "$JAVA_BIN" -cp "$CLASSPATH" com.fiftytalk.carddraw.CardDrawApplication --server.port="$PORT"
