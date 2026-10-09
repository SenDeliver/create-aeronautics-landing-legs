#!/bin/bash
# usage: tools/run_headless.sh <scenario> <log> [gradle -P args...]
# KEEP_WORLD=1 keeps run/server/world (reload tests). Needs tools/setup_run.sh once.
cd "$(dirname "$0")/.."
SCEN=$1; LOG=$2; shift 2
tools/setup_run.sh > /dev/null
[ "$KEEP_WORLD" = "1" ] || rm -rf run/server/world
sed -i '' 's/^enable-rcon=false/enable-rcon=true/' run/server/server.properties
[ -f tools/local.env ] && . tools/local.env
./gradlew runServer "$@" > "$LOG" 2>&1 &
GPID=$!
for i in $(seq 1 150); do grep -q 'RCON running' "$LOG" && break; grep -qE 'BUILD FAILED|Crash|Exception in server tick loop' "$LOG" && break; sleep 2; done
python3 tools/rcon.py < "$SCEN" | tee "$LOG.rcon" | grep -E 'rror|nknown|stop|Incorrect|No ' || true
for i in $(seq 1 60); do kill -0 $GPID 2>/dev/null || break; sleep 2; done
kill $GPID 2>/dev/null
pkill -f 'run/server.*neoforge' 2>/dev/null
sed -i '' 's/^enable-rcon=true/enable-rcon=false/' run/server/server.properties
wait $GPID 2>/dev/null
echo "done: $LOG"
