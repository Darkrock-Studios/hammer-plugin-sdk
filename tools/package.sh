#!/bin/sh
# Packs a plugin directory into <dir>/build/<dir>.hammerplugin: its manifest.toml, its settings.toml,
# locales/, and resources/ if it has them, and a built module. Needs zip.
# Usage: package.sh <plugin directory> <module.wasm>
set -e
[ $# -eq 2 ] || { echo "Usage: $0 <plugin directory> <module.wasm>" >&2; exit 2; }
dir="$1"
wasm="$2"
name=$(basename "$(cd "$dir" && pwd)")
package="$dir/build/package"
rm -rf "$package"
mkdir -p "$package"
cp "$dir/manifest.toml" "$package/"
if [ -f "$dir/settings.toml" ]; then cp "$dir/settings.toml" "$package/"; fi
if [ -d "$dir/locales" ]; then cp -r "$dir/locales" "$package/"; fi
if [ -d "$dir/resources" ]; then cp -r "$dir/resources" "$package/"; fi
cp "$wasm" "$package/plugin.wasm"
rm -f "$dir/build/$name.hammerplugin"
(cd "$package" && zip -q -X -r "../$name.hammerplugin" .)
