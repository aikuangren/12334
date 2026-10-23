#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PORT="${SERVER_PORT:-18080}"
LOG_DIR="$ROOT_DIR/.runtime-local"
BACKEND_LOG="$LOG_DIR/share-backend.log"
NGROK_LOG="$LOG_DIR/share-ngrok.log"

mkdir -p "$LOG_DIR"

has_service() {
  curl -fsS --max-time 2 "http://localhost:${PORT}/api/ping" >/dev/null 2>&1
}

echo "Card Draw online sharing"
echo "Local port: ${PORT}"

if has_service; then
  echo "Backend is already running."
else
  echo "Starting backend..."
  nohup "$ROOT_DIR/start-fast.sh" >"$BACKEND_LOG" 2>&1 &

  for _ in $(seq 1 60); do
    if has_service; then
      break
    fi
    sleep 1
  done

  if ! has_service; then
    echo "Backend failed to start. Please check log:"
    echo "$BACKEND_LOG"
    exit 1
  fi
fi

if ! command -v ngrok >/dev/null 2>&1; then
  echo "ngrok is not installed. Please install ngrok first."
  exit 1
fi

if curl -fsS --max-time 2 "http://127.0.0.1:4040/api/tunnels" >/dev/null 2>&1; then
  echo "ngrok is already running."
else
  echo "Starting ngrok..."
  nohup ngrok http "$PORT" >"$NGROK_LOG" 2>&1 &
  sleep 3
fi

PUBLIC_URL=""
for _ in $(seq 1 20); do
  PUBLIC_URL="$(curl -fsS --max-time 2 "http://127.0.0.1:4040/api/tunnels" 2>/dev/null \
    | sed -n 's/.*"public_url":"\([^"]*https:\/\/[^"]*\)".*/\1/p' \
    | head -1)"
  if [ -n "$PUBLIC_URL" ]; then
    break
  fi
  sleep 1
done

if [ -z "$PUBLIC_URL" ]; then
  echo "ngrok started, but no public URL was found yet. Please check log:"
  echo "$NGROK_LOG"
  exit 1
fi

echo ""
echo "Public test URLs:"
echo "H5:        ${PUBLIC_URL}/h5-c.html?userId=10001"
echo "Admin:     ${PUBLIC_URL}/admin.html"
echo "Prototype: ${PUBLIC_URL}/prototype.html"
echo "API ping:  ${PUBLIC_URL}/api/ping"
echo ""
echo "Keep this terminal and your computer running while others test."
echo "Backend log: $BACKEND_LOG"
echo "ngrok log:   $NGROK_LOG"
