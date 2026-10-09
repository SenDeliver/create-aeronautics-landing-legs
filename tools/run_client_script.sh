#!/bin/bash
# Singleplayer client (fresh flat world copy) running a DevScript directly. usage: tools/run_client_script.sh <script> <log> [gradle -P args...]
cd "$(dirname "$0")/.."
SCRIPT=$1; LOG=$2; shift 2
tools/setup_run.sh > /dev/null
if [ ! -d run/world-template ]; then
  printf 'stop\n' > run/empty-scenario.txt
  tools/run_headless.sh run/empty-scenario.txt run/template.log > /dev/null
  cp -R run/server/world run/world-template
fi
rm -rf run/saves/p3ui; mkdir -p run/saves; cp -R run/world-template run/saves/p3ui
[ -f tools/local.env ] && . tools/local.env
./gradlew runClient -PquickPlay=p3ui -Plegworks.script="$PWD/$SCRIPT" "$@" > "$LOG" 2>&1
echo "done: $LOG"
