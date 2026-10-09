#!/bin/bash
# Phase-2 full test run: phase-1 regression (dedicated), p2 suites on a dedicated server and on the integrated server.
# usage: tools/run_all_p2.sh <log prefix>   (results printed; logs in tools/logs/)
cd "$(dirname "$0")/.."
P=$1
tools/regress.sh $P
python3 tools/gen_suites.py > /dev/null
for s in p2_phys p2_power; do
  tools/run_headless.sh tools/scenario-$s.txt tools/logs/$P-$s.log > /dev/null 2>&1
  echo "== $s dedicated"; python3 tools/analyze_p2.py $s tools/logs/$P-$s.log
done
tools/run_headless.sh tools/scenario-p2_link_a.txt tools/logs/$P-p2_link_a.log > /dev/null 2>&1
KEEP_WORLD=1 tools/run_headless.sh tools/scenario-p2_link_b.txt tools/logs/$P-p2_link_b.log > /dev/null 2>&1
echo "== p2_link dedicated"; python3 tools/analyze_p2.py p2_link tools/logs/$P-p2_link_a.log tools/logs/$P-p2_link_b.log
for s in p2_phys p2_power; do
  tools/run_integrated.sh tools/scenario-$s.txt tools/logs/$P-int-$s.log > /dev/null 2>&1
  echo "== $s integrated"; python3 tools/analyze_p2.py $s tools/logs/$P-int-$s.log
done
tools/run_integrated.sh tools/scenario-p2_link_a.txt tools/logs/$P-int-p2_link_a.log > /dev/null 2>&1
tools/run_integrated.sh tools/scenario-p2_link_b.txt tools/logs/$P-int-p2_link_b.log keep > /dev/null 2>&1
echo "== p2_link integrated"; python3 tools/analyze_p2.py p2_link tools/logs/$P-int-p2_link_a.log tools/logs/$P-int-p2_link_b.log
tools/run_headless.sh tools/scenario-pit20.txt tools/logs/$P-pit20.log > /dev/null 2>&1
echo "== pit20 (tracked, 8x drop 20 + 2x drop 21 into the pit)"; python3 tools/analyze.py r5 tools/logs/$P-pit20.log
