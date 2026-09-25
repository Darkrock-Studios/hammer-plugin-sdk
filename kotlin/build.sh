#!/bin/sh
# Builds every plugin: <plugin>/build/<plugin>.hammerplugin for each with a manifest.toml, and the
# optimized module for the rest. Needs a JDK and Gradle 9; set GRADLE to a Gradle launcher if gradle is
# not on the path.
set -e
cd "$(dirname "$0")"
tasks=""
for dir in */; do
	plugin="${dir%/}"
	[ -d "$plugin/src/wasmWasiMain" ] || continue
	[ "$plugin" = hammer ] && continue
	if [ -f "$plugin/manifest.toml" ]; then
		tasks="$tasks :$plugin:hammerPlugin"
	else
		tasks="$tasks :$plugin:compileProductionExecutableKotlinWasmWasiOptimize"
	fi
done
"${GRADLE:-gradle}" -q $tasks
ls -1 */build/*.hammerplugin
