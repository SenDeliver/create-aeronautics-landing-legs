#!/bin/bash
# Integrated (singleplayer) run of an RCON scenario: converts it to a DevScript and plays it in runClient on a fresh flat world.
# usage: tools/run_integrated.sh <scenario> <log> [keep]   ("keep" reuses the world of the previous run, e.g. for reload tests)
cd "$(dirname "$0")/.."
SCEN=$1; LOG=$2
tools/setup_run.sh > /dev/null
if [ ! -d run/world-template ]; then
  printf 'stop\n' > run/empty-scenario.txt
  tools/run_headless.sh run/empty-scenario.txt run/template.log > /dev/null
  cp -R run/server/world run/world-template
fi
if [ "$3" != "keep" ]; then rm -rf run/saves/p2int; mkdir -p run/saves; cp -R run/world-template run/saves/p2int; fi
python3 - "$SCEN" run/integrated-script.txt <<'PY'
import sys
out = ["wait 60"]
for line in open(sys.argv[1]):
    line = line.strip()
    if not line or line.startswith("# ") or line.startswith("#signal"):
        continue
    if line.startswith("#sleep"):
        out.append(f"wait {max(1, round(float(line.split()[1]) * 20))}")
    elif line == "stop":
        out.append("wait 40")
        out.append("quit")
    else:
        out.append("cmd " + line)
open(sys.argv[2], "w").write("\n".join(out) + "\n")
PY
[ -f tools/local.env ] && . tools/local.env
./gradlew runClient -PquickPlay=p2int -Plegworks.script="$PWD/run/integrated-script.txt" > "$LOG" 2>&1
echo "done: $LOG"
