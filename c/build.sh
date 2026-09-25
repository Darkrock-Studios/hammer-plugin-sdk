#!/bin/sh
# Builds each named plugin (default: all of them) into <plugin>/build: <plugin>.wasm, and, for one with
# a manifest.toml, <plugin>.hammerplugin with its settings.toml if it has one. Freestanding wasm32, no
# libc or WASI. Needs clang, wasm-ld (lld), and zip.
set -e
cd "$(dirname "$0")"
plugins="$*"
[ -n "$plugins" ] || plugins="upper wordfreq style plaintext simple-grammar"
for plugin in $plugins; do
	mkdir -p "$plugin/build"
	clang --target=wasm32-unknown-unknown -O2 -nostdlib \
		-Wl,--no-entry -Wl,--export-dynamic \
		-o "$plugin/build/$plugin.wasm" "$plugin/$plugin.c"
	if [ -f "$plugin/manifest.toml" ]; then
		rm -rf "$plugin/build/package"
		mkdir -p "$plugin/build/package"
		cp "$plugin/manifest.toml" "$plugin/build/package/"
		[ -f "$plugin/settings.toml" ] && cp "$plugin/settings.toml" "$plugin/build/package/"
		cp "$plugin/build/$plugin.wasm" "$plugin/build/package/plugin.wasm"
		rm -f "$plugin/build/$plugin.hammerplugin"
		(cd "$plugin/build/package" && zip -q -X "../$plugin.hammerplugin" *)
	fi
	echo "Built $plugin ($(wc -c < "$plugin/build/$plugin.wasm") bytes)"
done
