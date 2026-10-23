#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -d "$ROOT_DIR/.runtime-local/tools/jdk-17.0.19+10" ]; then
  export JAVA_HOME="$ROOT_DIR/.runtime-local/tools/jdk-17.0.19+10/Contents/Home"
  export MAVEN_HOME="$ROOT_DIR/.runtime-local/tools/apache-maven-3.9.9"
else
  export JAVA_HOME="$ROOT_DIR/.runtime/tools/jdk-17.0.19+10/Contents/Home"
  export MAVEN_HOME="$ROOT_DIR/.runtime/tools/apache-maven-3.9.9"
fi
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
export SERVER_PORT="${SERVER_PORT:-18080}"
export MAVEN_REPO_LOCAL="$ROOT_DIR/.m2repo"

echo "Using Java: $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"
echo "Using Maven: $("$MAVEN_HOME/bin/mvn" -version | head -1)"
echo "Starting card draw backend on port $SERVER_PORT..."

exec "$MAVEN_HOME/bin/mvn" -Dmaven.repo.local="$MAVEN_REPO_LOCAL" spring-boot:run -Dspring-boot.run.arguments="--server.port=$SERVER_PORT"
