#!/bin/bash
# Dedicated-server split test: runServer (fresh world, class-load log) + runClient connected over localhost running a DevScript.
# usage: [CAMERA=<rcon timeline>] tools/run_split.sh <rcon scene> <client script> <log prefix>
cd "$(dirname "$0")/.."
SCENE=$1; SCRIPT=$2; OUT=$3
tools/setup_run.sh > /dev/null
rm -rf run/server/world run/signal
sed -i '' 's/^enable-rcon=false/enable-rcon=true/' run/server/server.properties
[ -f tools/local.env ] && . tools/local.env
./gradlew runServer -PclassLog="$PWD/$OUT-server-classes.txt" > "$OUT-server.log" 2>&1 &
SPID=$!
for i in $(seq 1 150); do grep -q 'RCON running' "$OUT-server.log" && break; sleep 2; done
python3 tools/rcon.py < "$SCENE" > "$OUT-rcon.txt"
./gradlew runClient -Pconnect=localhost:25599 -Plegworks.script="$PWD/$SCRIPT" > "$OUT-client.log" 2>&1 &
CPID=$!
for i in $(seq 1 150); do grep -q 'Dev joined the game' "$OUT-server.log" && break; sleep 2; done
[ -n "$CAMERA" ] && python3 tools/rcon.py < "$CAMERA" >> "$OUT-rcon.txt"
wait $CPID
printf 'legworks trace 0\nstop\n' | python3 tools/rcon.py >> "$OUT-rcon.txt"
for i in $(seq 1 60); do kill -0 $SPID 2>/dev/null || break; sleep 2; done
kill $SPID 2>/dev/null
sed -i '' 's/^enable-rcon=true/enable-rcon=false/' run/server/server.properties
wait $SPID 2>/dev/null
echo "done: $OUT"
