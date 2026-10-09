#!/bin/bash
# Re-runs the phase-1 suites headless and prints each analysis. usage: tools/regress.sh <log prefix> [gradle -P args...]
cd "$(dirname "$0")/.."
P=$1; shift
python3 tools/gen_suites.py > /dev/null
for s in r3 r4 r5 limbs remove; do
  tools/run_headless.sh tools/scenario-$s.txt tools/logs/$P-$s.log "$@" > /dev/null 2>&1
  echo "== $s"; python3 tools/analyze.py $s tools/logs/$P-$s.log
done
tools/run_headless.sh tools/scenario-reload_a.txt tools/logs/$P-reload_a.log "$@" > /dev/null 2>&1
KEEP_WORLD=1 tools/run_headless.sh tools/scenario-reload_b.txt tools/logs/$P-reload_b.log "$@" > /dev/null 2>&1
echo "== reload"; python3 tools/analyze.py reload tools/logs/$P-reload_a.log tools/logs/$P-reload_b.log
