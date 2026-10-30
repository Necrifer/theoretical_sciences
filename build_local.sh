#!/bin/bash
# Offline build: compiles against jars in ../libs (copied from the user's CurseForge install)
set -e
cd "$(dirname "$0")"
export JAVA_TOOL_OPTIONS=
L=/home/claude/libs
CP="$L/neoforge-21.1.265-client.jar:$L/client-1.21.1-20240808.144430-srg.jar"
for j in $L/*.jar; do case "$j" in *neoforge-21.1.265-client.jar|*-srg.jar|*-extra.jar) ;; *) CP="$CP:$j";; esac; done
rm -rf build/classes build/jar && mkdir -p build/classes build/jar
find src/main/java -name '*.java' > build/sources.txt
set +e
javac --release 21 -proc:none -encoding UTF-8 -nowarn -Xmaxerrs 400 -d build/classes -cp "$CP" @build/sources.txt > build/javac.log 2>&1
JS=$?
set -e
grep -v -e '^Note:' -e 'unknown enum constant' -e 'reason: class file for' -e '^[0-9]* warning' build/javac.log || true
if [ $JS -ne 0 ]; then echo "COMPILE FAILED"; rm -f build/libs/*.jar; exit 1; fi
cp -r build/classes/* build/jar/
cp -r src/main/resources/* build/jar/
cp LICENSE build/jar/LICENSE
# round 36: only the mod's own Codex ships. The Patchouli entries stay in src/ as the Codex's source text (tools/
# round30_codex.py builds ts_codex from them), and the Modonomicon book is a build intermediate - neither goes in the jar.
rm -rf build/jar/assets/theoreticalsciences/patchouli_books build/jar/data/theoreticalsciences/patchouli_books build/jar/data/theoreticalsciences/modonomicon
python3 tools/pack_textures.py build/jar
VER=$(grep '^mod_version' gradle.properties | cut -d= -f2)
sed -i "s/\${mod_version}/$VER/g" build/jar/META-INF/neoforge.mods.toml
mkdir -p build/libs
rm -f build/libs/*.jar
# zip -9 (max deflate): keeps the jar under the 30 MB transfer limit to the user's PC
(cd build/jar && printf "Manifest-Version: 1.0\nAutomatic-Module-Name: theoreticalsciences\nImplementation-Title: theoreticalsciences\nImplementation-Version: $VER\n" > META-INF/MANIFEST.MF && zip -q -9 -X ../libs/theoreticalsciences-$VER.jar META-INF/MANIFEST.MF && zip -q -9 -X -r ../libs/theoreticalsciences-$VER.jar . -x META-INF/MANIFEST.MF)
ls -la build/libs
