#!/usr/bin/env bash
# Regenerates every generated file in src/ (Java content lists, assets, data, lang, Codex) from tools/*.py, then
# checks the result: progression (every item reachable, also after the Shutdown), file references, structures.
# Needs Python 3.10+ with Pillow, numpy and scipy. Takes a few minutes. Textures are NOT redrawn (see BUILDING.md).
set -e -o pipefail
export PYTHONHASHSEED=0      # the same output on every run (Python otherwise orders sets differently each time)
cd "$(dirname "$0")"
LOG=out/logs; mkdir -p "$LOG"
for s in add_tools_content add_complexity_content add_concepts_content add_extreme_content add_round3_content add_round4_content \
         add_round5_content add_round7_content add_round8_content add_round10_content add_round11_content add_round14_content \
         add_round15_content add_round16_content add_round17_content add_round19_content add_round20_content add_round21_content \
         add_round22_content add_round24_content add_round25_content add_round26_content add_round27_content \
         add_round32_branches_content add_round32_gear_content add_round34_anomaly_content add_round35_content add_round37_content add_round38_content add_round39_content add_round40_content add_round41_content add_round42_content add_round44_content add_round45_content add_round46_content add_round47_content add_round48_content add_round49_content add_round50_content add_round51_content add_round52_content add_round53_content add_round54_content add_round55_content add_round56_content add_round57_content \
         gen_java gen_block_sounds gen_data chaincheck gen_entities gen_sounds; do
  echo "== $s"
  python3 "$s.py" > "$LOG/$s.log" 2>&1 || { echo "FAILED: $s"; tail -20 "$LOG/$s.log"; exit 1; }
  tail -2 "$LOG/$s.log"
done
echo "== music";               python3 gen_music.py --register | tail -2
echo "== data (second pass)";  python3 gen_data.py | tail -3
echo "== progression";         python3 chaincheck.py | tail -4
echo "== after the Shutdown";  TS_PHASE=sundered python3 chaincheck.py | tail -1
echo "== ores";                python3 ore_textures.py | tail -2
echo "== custom textures";     python3 custom_textures.py | tail -1
echo "== animations";          python3 trim_animations.py | tail -1
echo "== codex";               python3 gen_codex_space.py | tail -2
echo "== flavour";              python3 flavour.py | tail -1     # 1.1.0: descriptions for what had placeholders
echo "== text overrides";      python3 text/textdoc.py apply | tail -3       # 1.1.0: the rewritten text (tools/text/overrides.json)
echo "== found";                python3 found.py | tail -1       # 1.1.0: where each material is found (tooltips and Codex)
echo "== tags";                 python3 gen_tags.py | tail -1    # 1.1.0: c: and family tags for every item
cd ..
echo "== validate";            python3 tools/validate.py | tail -5
echo "== controllers";         python3 tools/controller_check.py | tail -3
