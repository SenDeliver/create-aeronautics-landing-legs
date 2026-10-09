#!/bin/bash
# p2 suites on a dedicated server with a real client connected over localhost (the scenario runs over RCON after the client joins).
cd "$(dirname "$0")/.."
P=$1
python3 tools/gen_suites.py > /dev/null
for s in p2_phys p2_power p2_link_a; do
  CAMERA=tools/scenario-$s-client.txt tools/run_split.sh tools/scenario-op.txt tools/shots-wait.txt tools/logs/$P-$s > /dev/null 2>&1
done
for s in p2_phys p2_power; do echo "== $s dedicated+client"; python3 tools/analyze_p2.py $s tools/logs/$P-$s-server.log; done
echo "== p2_link dedicated+client (no reload part)"; python3 tools/analyze_p2.py p2_link tools/logs/$P-p2_link_a-server.log
