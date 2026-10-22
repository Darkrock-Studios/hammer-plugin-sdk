#!/bin/sh
# Packs the C kit into <out>/hammer-plugin-sdk-c-<version>.tar.gz, with a .sha256 beside it: include/,
# build.sh, test.sh, package.sh, and the SDK's license (Extism's is in include/). Needs tar and sha256sum.
# Usage: pack-c.sh <version> <out directory>
set -e
[ $# -eq 2 ] || { echo "Usage: $0 <version> <out directory>" >&2; exit 2; }
version="$1"
out=$(mkdir -p "$2" && cd "$2" && pwd)
sdk=$(cd "$(dirname "$0")/.." && pwd)
name="hammer-plugin-sdk-c-$version"
staging=$(mktemp -d)
trap 'rm -rf "$staging"' EXIT
mkdir "$staging/$name"
cp -r "$sdk/c/include" "$sdk/c/build.sh" "$sdk/c/test.sh" "$sdk/tools/package.sh" "$sdk/LICENSE" "$staging/$name/"
tar -czf "$out/$name.tar.gz" -C "$staging" "$name"
(cd "$out" && sha256sum "$name.tar.gz" > "$name.tar.gz.sha256")
echo "Packed $out/$name.tar.gz"
