#!/bin/bash
# Creates the local run dirs (gitignored): muted client, RCON-enabled dedicated server, Sable at 1 sub-step like prod.
cd "$(dirname "$0")/.."
mkdir -p run/config run/server/config
[ -f run/options.txt ] || cat > run/options.txt <<'OPT'
soundCategory_master:0.0
pauseOnLostFocus:false
onboardAccessibility:false
tutorialStep:none
joinedFirstServer:true
skipMultiplayerWarning:true
narrator:0
OPT
grep -q '^soundCategory_master:0.0' run/options.txt || { sed -i '' '/^soundCategory_master:/d' run/options.txt; echo 'soundCategory_master:0.0' >> run/options.txt; }
grep -q '^pauseOnLostFocus:false' run/options.txt || { sed -i '' '/^pauseOnLostFocus:/d' run/options.txt; echo 'pauseOnLostFocus:false' >> run/options.txt; }
echo 'eula=true' > run/server/eula.txt
[ -f run/server/server.properties ] || cat > run/server/server.properties <<'PROPS'
online-mode=false
enable-rcon=false
rcon.port=25598
server-port=25599
level-type=minecraft\:flat
spawn-protection=0
spawn-monsters=false
spawn-animals=false
spawn-npcs=false
difficulty=peaceful
gamemode=creative
view-distance=10
simulation-distance=10
motd=Legworks dev
PROPS
grep -q '^rcon.password=' run/server/server.properties || echo "rcon.password=${RCON_PASSWORD:-$(LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 20)}" >> run/server/server.properties
for d in run/config run/server/config; do
  cat > $d/sable-server.toml <<'SABLE'
sub_level_substeps_per_tick = 1
sub_level_storage_pruning = false
SABLE
done
echo "run dirs ready"
