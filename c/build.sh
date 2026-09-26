#!/bin/sh
# Builds each C plugin directory named into <dir>/build: <dir>.wasm from <dir>.c, and, for one with a
# manifest.toml, <dir>.hammerplugin. Freestanding wasm32, no libc or WASI, with the kit's headers on the
# include path. Needs clang, wasm-ld (lld), and zip.
# Usage: build.sh <plugin directory>...
set -e
kit=$(cd "$(dirname "$0")" && pwd)
[ $# -gt 0 ] || { echo "Usage: $0 <plugin directory>..." >&2; exit 2; }
for dir in "$@"; do
	name=$(basename "$(cd "$dir" && pwd)")
	mkdir -p "$dir/build"
	clang --target=wasm32-unknown-unknown -O2 -nostdlib -I "$kit/include" \
		-Wl,--no-entry -Wl,--export-dynamic \
		-o "$dir/build/$name.wasm" "$dir/$name.c"
	if [ -f "$dir/manifest.toml" ]; then "$kit/../tools/package.sh" "$dir" "$dir/build/$name.wasm"; fi
	echo "Built $name ($(wc -c < "$dir/build/$name.wasm") bytes)"
done
