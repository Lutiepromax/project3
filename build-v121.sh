#!/usr/bin/env bash
set -euo pipefail

git fetch origin obsidian-tnt-build-20260805 --depth=1
git show FETCH_HEAD:.github/workflows/build-obsidian-tnt.yml > /tmp/build-v110.yml
cp /tmp/build-v110.yml .github/workflows/build-obsidian-tnt.yml

git fetch origin ded47bf92a3ae8c9f18f4fb8af635bf699ca8c50 --depth=1
git show FETCH_HEAD:.github/workflows/build-obsidian-v120-pr.yml > /tmp/build-v120.yml

python - <<'PY'
from pathlib import Path
import subprocess

text = Path('/tmp/build-v120.yml').read_text()
step_names = [
    'Recreate the previously compiled v1.1.0 source',
    'Apply v1.2.0 visibility, hitbox, knockback and durability UI changes',
]

for index, name in enumerate(step_names, 1):
    marker = f'      - name: {name}\n'
    start = text.index(marker)
    run_marker = '        run: |\n'
    start = text.index(run_marker, start) + len(run_marker)
    lines = []
    for line in text[start:].splitlines():
        if line.startswith('      - name:'):
            break
        if line.startswith('          '):
            lines.append(line[10:])
        else:
            lines.append(line)

    script_text = '\n'.join(lines) + '\n'
    if index == 2:
        script_text = script_text.replace("'''          ", "'''")

    script = Path(f'/tmp/v120-step-{index}.sh')
    script.write_text(script_text)
    subprocess.run(['bash', str(script)], check=True)
PY

python - <<'PY'
from pathlib import Path

java = Path('forge-work/src/main/java/com/radel/obsidiantnt/ObsidianTNTMod.java')
source = java.read_text()
needle = '                  ENTITIES.register(bus);\n'
insert = needle + '''
                  // Direct listener registration fixes old Forge + OptiFine
                  // launchers skipping the nested automatic subscriber.
                  bus.addListener(ClientEvents::onClientSetup);
'''
if needle not in source:
    raise RuntimeError('ENTITIES registration anchor not found')
source = source.replace(needle, insert, 1)
java.write_text(source)

properties = Path('forge-work/gradle.properties')
text = properties.read_text().replace('mod_version=1.2.0', 'mod_version=1.2.1')
properties.write_text(text)
PY

cd forge-work
./gradlew clean build --no-daemon --stacktrace
cd ..

rm -rf artifact-v121
mkdir artifact-v121
JAR="$(find forge-work/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*sources*' | head -n 1)"
test -n "$JAR"
cp "$JAR" artifact-v121/ObsidianTNT-1.16.5-1.2.1.jar
