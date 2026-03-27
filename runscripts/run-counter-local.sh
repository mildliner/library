#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   runscripts/run-counter-local.sh [client_id] [increment] [ops]
# Example:
#   runscripts/run-counter-local.sh 1001 1 10
# Notes:
#   SKIP_BUILD=1 runscripts/run-counter-local.sh ...   # skip ./gradlew installDist

CLIENT_ID="${1:-1001}"
INCREMENT="${2:-1}"
OPS="${3:-10}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_DIR="$ROOT_DIR/build/install/library"
LOG_DIR="${TMPDIR:-/tmp}/bftsmart-counter-logs"

mkdir -p "$LOG_DIR"

cleanup() {
  for id in 0 1 2 3; do
    local pid_file="$LOG_DIR/replica-${id}.pid"
    if [[ -f "$pid_file" ]]; then
      kill "$(cat "$pid_file")" 2>/dev/null || true
      wait "$(cat "$pid_file")" 2>/dev/null || true
      rm -f "$pid_file"
    fi
  done
}
trap cleanup EXIT

if [[ "${SKIP_BUILD:-0}" == "1" ]]; then
  echo "[1/5] Skipping build because SKIP_BUILD=1"
else
  echo "[1/5] Building distribution (installDist)..."
  ( cd "$ROOT_DIR" && ./gradlew installDist >/dev/null )
fi

echo "[2/5] Preparing config/currentView..."
rm -f "$DIST_DIR/config/currentView"

cd "$DIST_DIR"

echo "[3/5] Starting 4 replicas..."
for id in 0 1 2 3; do
  nohup ./smartrun.sh bftsmart.demo.counter.CounterServer "$id" \
    >"$LOG_DIR/replica-${id}.log" 2>&1 &
  echo $! > "$LOG_DIR/replica-${id}.pid"
  echo "  - replica $id pid=$(cat "$LOG_DIR/replica-${id}.pid")"
done

echo "[4/5] Waiting replicas to be ready..."
for id in 0 1 2 3; do
  ready=0
  for _ in $(seq 1 60); do
    if grep -q "Ready to process operations" "$LOG_DIR/replica-${id}.log"; then
      ready=1
      break
    fi
    sleep 1
  done

  if [[ "$ready" -ne 1 ]]; then
    echo "Replica $id did not become ready in time. Last log lines:"
    tail -n 40 "$LOG_DIR/replica-${id}.log" || true
    exit 1
  fi
done

echo "[5/5] Running client (id=$CLIENT_ID, increment=$INCREMENT, ops=$OPS)..."
./smartrun.sh bftsmart.demo.counter.CounterClient "$CLIENT_ID" "$INCREMENT" "$OPS" \
  | tee "$LOG_DIR/client.log"

echo "Done. Logs are in: $LOG_DIR"
