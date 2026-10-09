# Dev test harness

Not part of the mod jar. Drives a dev server/client from the `dev` source set (`/legworks` dev commands, TRACE logs, DevScript) and checks physics results.

Needs Python 3 and bash. The scripts use BSD `sed -i ''`, so they run on macOS as is.
Optional `tools/local.env` (gitignored) is sourced before Gradle runs, e.g. `export JAVA_HOME=/path/to/jdk`.

| File | What it does |
|---|---|
| `setup_run.sh` | Creates `run/` (gitignored): muted client options, flat creative dedicated server with RCON on 25598, Sable at 1 sub-step. RCON password from `$RCON_PASSWORD` or random. |
| `rcon.py` | Sends an RCON scenario from stdin to the dev server (reads port/password from `run/server/server.properties`). `#sleep N` and `#signal name` are harness directives. |
| `run_headless.sh <scenario> <log>` | `runServer` on a fresh world, plays the scenario over RCON, stops. `KEEP_WORLD=1` keeps the world (reload tests). |
| `run_integrated.sh <scenario> <log> [keep]` | Same scenario in singleplayer (`runClient`) via a generated DevScript. |
| `run_split.sh`, `run_split_suites.sh` | Dedicated server plus a real client over localhost. |
| `run_client_script.sh <script> <log>` | Singleplayer client running a DevScript (`shots-*.txt`: screenshots land in `run/screenshots/`). |
| `regress.sh <prefix>`, `run_all_p2.sh <prefix>` | Full regression runs; logs in `tools/logs/`, PASS/FAIL printed. |
| `gen_suites.py` | Generates the `scenario-*.txt` files. |
| `gen_ponder.py` | Builds Ponder scene schematics (see its docstring for the export command). |
| `gen_textures.py` | Generates the block and item textures into `src/main/resources` (needs Pillow). |
| `analyze.py`, `analyze_p2.py`, `analyze_p3.py` | Parse TRACE/STATUS logs and check pass thresholds (exit 1 on failure). |
| `results/` | Saved outputs of past regression runs. |
| `shots/` | Curated screenshots from DevScript runs. |

Quick start: `tools/setup_run.sh && tools/regress.sh mytest`.
